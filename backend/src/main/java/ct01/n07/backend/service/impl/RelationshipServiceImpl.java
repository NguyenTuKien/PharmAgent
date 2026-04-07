package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.relationship.CaregiverProfileResponse;
import ct01.n07.backend.dto.relationship.ElderlyProfileResponse;
import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelationshipServiceImpl implements RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    @Override
    public List<ElderlyProfileResponse> getRelativeElderlyProfiles() {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        List<Relationship> relationships = relationshipRepository
                .findAllByCaregiverIdAndStatus(currentProfile.getId(), RelationStatus.ACCEPTED);

        if (relationships.isEmpty()) {
            return List.of();
        }

        List<String> elderlyIds = relationships.stream()
                .map(Relationship::getElderlyId)
                .toList();

        Map<String, UserProfile> profileMap = userProfileRepository.findAllById(elderlyIds).stream()
                .filter(profile -> profile.getRole() == Role.ELDERLY)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getElderlyId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getElderlyId());
                    return ElderlyProfileResponse.builder()
                            .id(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .avatarUrl(profile.getAvatarUrl())
                            .build();
                })
                .toList();
    }

    @Override
    public List<CaregiverProfileResponse> getRelativeCaregiverProfiles() {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        List<Relationship> relationships = relationshipRepository
                .findAllByElderlyIdAndStatus(currentProfile.getId(), RelationStatus.ACCEPTED);

        if (relationships.isEmpty()) {
            return List.of();
        }

        List<String> caregiverIds = relationships.stream()
                .map(Relationship::getCaregiverId)
                .toList();

        Map<String, UserProfile> profileMap = userProfileRepository.findAllById(caregiverIds).stream()
                .filter(profile -> profile.getRole() == Role.CAREGIVER)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getCaregiverId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getCaregiverId());
                    return CaregiverProfileResponse.builder()
                            .id(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .relationshipName(rel.getRelationshipName())
                            .build();
                })
                .toList();
    }

    @Override
    public List<ElderlyProfileResponse> getPendingElderlyProfiles() {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        List<Relationship> relationships = relationshipRepository
                .findAllByCaregiverIdAndStatus(currentProfile.getId(), RelationStatus.PENDING);

        if (relationships.isEmpty()) {
            return List.of();
        }

        List<String> elderlyIds = relationships.stream()
                .map(Relationship::getElderlyId)
                .toList();

        Map<String, UserProfile> profileMap = userProfileRepository.findAllById(elderlyIds).stream()
                .filter(profile -> profile.getRole() == Role.ELDERLY)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getElderlyId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getElderlyId());
                    return ElderlyProfileResponse.builder()
                            .id(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .avatarUrl(profile.getAvatarUrl())
                            .build();
                })
                .toList();
    }

    @Override
    public List<CaregiverProfileResponse> getPendingCaregiverProfiles() {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        List<Relationship> relationships = relationshipRepository
                .findAllByElderlyIdAndStatus(currentProfile.getId(), RelationStatus.PENDING);

        if (relationships.isEmpty()) {
            return List.of();
        }

        List<String> caregiverIds = relationships.stream()
                .map(Relationship::getCaregiverId)
                .toList();

        Map<String, UserProfile> profileMap = userProfileRepository.findAllById(caregiverIds).stream()
                .filter(profile -> profile.getRole() == Role.CAREGIVER)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getCaregiverId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getCaregiverId());
                    return CaregiverProfileResponse.builder()
                            .id(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .avatarUrl(profile.getAvatarUrl())
                            .relationshipName(rel.getRelationshipName())
                            .build();
                })
                .toList();
    }

    @Override
    public String sendInvitation(RelationshipInviteRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        if (!userProfileRepository.existsByIdAndRole(request.getTargetElderlyId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target profile is not a valid elderly profile");
        }

        if (relationshipRepository.existsByCaregiverIdAndElderlyId(currentProfile.getId(), request.getTargetElderlyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Relationship already exists");
        }

        Relationship relationship = Relationship.builder()
                .caregiverId(currentProfile.getId())
                .elderlyId(request.getTargetElderlyId())
                .relationshipName(request.getRelationshipName())
                .permissionLevel(request.getPermissionLevel())
                .status(RelationStatus.PENDING)
                .build();

        return relationshipRepository.save(relationship).getId();
    }

    @Override
    public void acceptInvitation(String relationshipId) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        Relationship relationship = relationshipRepository.findByIdAndElderlyId(relationshipId, currentProfile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship invitation not found"));

        if (relationship.getStatus() != RelationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending invitations can be accepted");
        }

        relationship.setStatus(RelationStatus.ACCEPTED);
        relationshipRepository.save(relationship);

        // Delete any existing ACCEPTED relationship between this pair
        relationshipRepository.findByCaregiverIdAndElderlyIdAndStatus(
                        relationship.getCaregiverId(), relationship.getElderlyId(), RelationStatus.ACCEPTED)
                .ifPresent(oldRel -> {
                    if (!oldRel.getId().equals(relationship.getId())) {
                        relationshipRepository.delete(oldRel);
                    }
                });
    }

    @Override
    public void refuseInvitation(String relationshipId) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.ELDERLY);

        Relationship relationship = relationshipRepository.findByIdAndElderlyId(relationshipId, currentProfile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship invitation not found"));

        if (relationship.getStatus() != RelationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending invitations can be refused");
        }

        relationshipRepository.delete(relationship);
    }

    @Override
    public void updateRelationship(String elderlyId, RelationshipInviteRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER);

        // Ensure an accepted relationship already exists
        if (!relationshipRepository.existsByCaregiverIdAndElderlyIdAndStatus(
                currentProfile.getId(), elderlyId, RelationStatus.ACCEPTED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active relationship found to update");
        }

        // If a pending update already exists, delete it first to replace with new one
        relationshipRepository.findByCaregiverIdAndElderlyIdAndStatus(
                        currentProfile.getId(), elderlyId, RelationStatus.PENDING)
                .ifPresent(relationshipRepository::delete);

        Relationship newRequest = Relationship.builder()
                .caregiverId(currentProfile.getId())
                .elderlyId(elderlyId)
                .relationshipName(request.getRelationshipName())
                .permissionLevel(request.getPermissionLevel())
                .status(RelationStatus.PENDING)
                .build();

        relationshipRepository.save(newRequest);
    }

    @Override
    public void createRelationship(String caregiverId, String elderlyId, String relationshipName, PermissionLevel permissionLevel) {
        Relationship relationship = Relationship.builder()
                .caregiverId(caregiverId)
                .elderlyId(elderlyId)
                .relationshipName(relationshipName)
                .permissionLevel(permissionLevel)
                .status(RelationStatus.ACCEPTED)
                .build();
        relationshipRepository.save(relationship);
    }

    private void requireRole(UserProfile profile, Role expectedRole) {
        if (profile.getRole() != expectedRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
    }
}

