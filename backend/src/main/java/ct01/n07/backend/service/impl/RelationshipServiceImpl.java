package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelationshipServiceImpl implements RelationshipService {

    private static final PermissionLevel DEFAULT_CAREGIVER_PERMISSION = PermissionLevel.MANAGE_ALL;

    private final RelationshipRepository relationshipRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileAccessContext profileAccessContext;

    @Override
    public List<Relationship> getAcceptedElderlyRelationships() {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);
        return relationshipRepository.findAllByCaregiverIdAndStatus(currentProfile.getId(), RelationStatus.ACCEPTED);
    }

    @Override
    public List<Relationship> getAcceptedCaregiverRelationships() {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);
        return relationshipRepository.findAllByElderlyIdAndStatus(currentProfile.getId(), RelationStatus.ACCEPTED);
    }

    @Override
    public List<Relationship> getPendingElderlyRelationships() {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);
        List<Relationship> pendingRelationships =
                relationshipRepository.findAllByCaregiverIdAndStatus(currentProfile.getId(), RelationStatus.PENDING);
        List<Relationship> acceptedRelationships =
                relationshipRepository.findAllByCaregiverIdAndStatus(currentProfile.getId(), RelationStatus.ACCEPTED);
        return excludeAlreadyAccepted(pendingRelationships, acceptedRelationships, Relationship::getElderlyId);
    }

    @Override
    public List<Relationship> getPendingCaregiverRelationships() {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);
        List<Relationship> pendingRelationships =
                relationshipRepository.findAllByElderlyIdAndStatus(currentProfile.getId(), RelationStatus.PENDING);
        List<Relationship> acceptedRelationships =
                relationshipRepository.findAllByElderlyIdAndStatus(currentProfile.getId(), RelationStatus.ACCEPTED);
        return excludeAlreadyAccepted(pendingRelationships, acceptedRelationships, Relationship::getCaregiverId);
    }

    @Override
    public List<Relationship> getAcceptedCaregiverRelationshipsByElderly(String elderlyId) {
        return relationshipRepository.findAllByElderlyIdAndStatus(elderlyId, RelationStatus.ACCEPTED);
    }

    @Override
    public String sendInvitation(RelationshipInviteRequest request) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        if (!userProfileRepository.existsByIdAndRole(request.getTargetElderlyId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target profile is not a valid elderly profile");
        }

        if (relationshipRepository.existsByCaregiverIdAndElderlyId(currentProfile.getId(),
                request.getTargetElderlyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Relationship already exists");
        }

        Relationship relationship = Relationship.builder()
                .caregiverId(currentProfile.getId())
                .elderlyId(request.getTargetElderlyId())
                .caregiverTitle(request.getCaregiverTitle())
                .elderlyTitle(resolveRelationLabel(request.getRelation(), request.getCustomRelation(), request.getElderlyTitle()))
                .relation(normalizeRelation(request.getRelation()))
                .customRelation(normalizeCustomRelation(request.getRelation(), request.getCustomRelation()))
                .permissionLevel(DEFAULT_CAREGIVER_PERMISSION)
                .status(RelationStatus.PENDING)
                .build();

        return relationshipRepository.save(relationship).getId();
    }

    @Override
    public void acceptInvitation(String relationshipId) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        Relationship relationship = relationshipRepository.findByIdAndElderlyId(relationshipId, currentProfile.getId())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship invitation not found"));

        if (relationship.getStatus() != RelationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending invitations can be accepted");
        }

        List<Relationship> acceptedRelationships = relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                relationship.getCaregiverId(), relationship.getElderlyId(), RelationStatus.ACCEPTED);
        if (acceptedRelationships.stream().anyMatch(rel -> !rel.getId().equals(relationship.getId()))) {
            relationshipRepository.delete(relationship);
            return;
        }

        relationship.setStatus(RelationStatus.ACCEPTED);
        relationshipRepository.save(relationship);
    }

    @Override
    public void refuseInvitation(String relationshipId) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        Relationship relationship = relationshipRepository.findByIdAndElderlyId(relationshipId, currentProfile.getId())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship invitation not found"));

        if (relationship.getStatus() != RelationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending invitations can be refused");
        }

        relationshipRepository.delete(relationship);
    }

    @Override
    public void updateRelationship(String elderlyId, FamilyRelation relation, String customRelation) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        String caregiverId = currentProfile.getId();

        List<Relationship> acceptedRelationships = relationshipRepository
                .findAllByCaregiverIdAndElderlyIdAndStatus(caregiverId, elderlyId, RelationStatus.ACCEPTED);
        List<Relationship> pendingRelationships = relationshipRepository
                .findAllByCaregiverIdAndElderlyIdAndStatus(caregiverId, elderlyId, RelationStatus.PENDING);

        Relationship targetRelationship;
        if (!acceptedRelationships.isEmpty()) {
            targetRelationship = acceptedRelationships.get(0);
            if (!pendingRelationships.isEmpty()) {
                relationshipRepository.deleteAll(pendingRelationships);
            }
        } else if (!pendingRelationships.isEmpty()) {
            targetRelationship = pendingRelationships.get(0);
            List<Relationship> duplicatePendingRelationships = pendingRelationships.stream()
                    .skip(1)
                    .toList();
            if (!duplicatePendingRelationships.isEmpty()) {
                relationshipRepository.deleteAll(duplicatePendingRelationships);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mối quan hệ hợp lệ để cập nhật");
        }

        targetRelationship.setRelation(normalizeRelation(relation));
        targetRelationship.setCustomRelation(normalizeCustomRelation(relation, customRelation));
        targetRelationship.setElderlyTitle(resolveRelationLabel(relation, customRelation, targetRelationship.getElderlyTitle()));
        if (targetRelationship.getPermissionLevel() == null) {
            targetRelationship.setPermissionLevel(DEFAULT_CAREGIVER_PERMISSION);
        }

        relationshipRepository.save(targetRelationship);
    }

    @Override
    public void updateCaregiverTitle(String relationshipId, String caregiverTitle) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        Relationship relationship = relationshipRepository.findByIdAndElderlyId(relationshipId, currentProfile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy mối quan hệ hợp lệ để cập nhật"));

        if (relationship.getStatus() != RelationStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể cập nhật cách gọi cho người hỗ trợ đã xác nhận");
        }

        relationship.setCaregiverTitle(caregiverTitle.trim());
        relationshipRepository.save(relationship);
    }

    @Override
    public void deleteRelationship(String elderlyId) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        List<Relationship> relationships = relationshipRepository
                .findAllByCaregiverIdAndElderlyId(currentProfile.getId(), elderlyId);
        if (relationships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mối quan hệ để xóa");
        }

        relationshipRepository.deleteAll(relationships);
    }

    @Override
    public void createRelationship(String caregiverId, String elderlyId, FamilyRelation relation, String customRelation,
            PermissionLevel permissionLevel) {
        Relationship relationship = Relationship.builder()
                .caregiverId(caregiverId)
                .elderlyId(elderlyId)
                .elderlyTitle(resolveRelationLabel(relation, customRelation, null))
                .relation(normalizeRelation(relation))
                .customRelation(normalizeCustomRelation(relation, customRelation))
                .permissionLevel(permissionLevel == null ? DEFAULT_CAREGIVER_PERMISSION : permissionLevel)
                .status(RelationStatus.ACCEPTED)
                .build();
        relationshipRepository.save(relationship);
    }

    private void requireRole(UserProfile profile, Role expectedRole) {
        if (profile.getRole() != expectedRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this action");
        }
    }

    private List<Relationship> excludeAlreadyAccepted(
            List<Relationship> pendingRelationships,
            List<Relationship> acceptedRelationships,
            Function<Relationship, String> relatedProfileId
    ) {
        if (pendingRelationships.isEmpty() || acceptedRelationships.isEmpty()) {
            return pendingRelationships;
        }

        Set<String> acceptedProfileIds = acceptedRelationships.stream()
                .map(relatedProfileId)
                .collect(Collectors.toSet());
        return pendingRelationships.stream()
                .filter(relationship -> !acceptedProfileIds.contains(relatedProfileId.apply(relationship)))
                .toList();
    }

    private FamilyRelation normalizeRelation(FamilyRelation relation) {
        return relation == null ? FamilyRelation.OTHER : relation;
    }

    private String normalizeCustomRelation(FamilyRelation relation, String customRelation) {
        String normalized = customRelation == null ? null : customRelation.trim();
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return normalizeRelation(relation) == FamilyRelation.OTHER ? normalized : null;
    }

    private String resolveRelationLabel(FamilyRelation relation, String customRelation, String fallback) {
        FamilyRelation normalizedRelation = normalizeRelation(relation);
        String normalizedCustomRelation = normalizeCustomRelation(normalizedRelation, customRelation);
        if (normalizedRelation == FamilyRelation.OTHER && normalizedCustomRelation != null) {
            return normalizedCustomRelation;
        }
        if (normalizedRelation != FamilyRelation.OTHER) {
            return normalizedRelation.getLabel();
        }
        return hasText(fallback) ? fallback.trim() : normalizedRelation.getLabel();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
