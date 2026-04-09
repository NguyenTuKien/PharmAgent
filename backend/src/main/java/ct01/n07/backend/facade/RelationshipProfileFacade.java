package ct01.n07.backend.facade;

import ct01.n07.backend.dto.relationship.CaregiverProfileResponse;
import ct01.n07.backend.dto.relationship.ElderlyProfileResponse;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelationshipProfileFacade {

    private final RelationshipService relationshipService;
    private final UserProfileService userProfileService;

    public List<ElderlyProfileResponse> getAcceptedElderlyProfiles() {
        List<Relationship> relationships = relationshipService.getAcceptedElderlyRelationships();
        if (relationships.isEmpty()) return List.of();

        List<String> elderlyIds = relationships.stream().map(Relationship::getElderlyId).toList();
        Map<String, UserProfile> profileMap = userProfileService.findAllById(elderlyIds).stream()
                .filter(p -> p.getRole() == Role.ELDERLY)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getElderlyId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getElderlyId());
                    return ElderlyProfileResponse.builder()
                            .relationshipId(rel.getId())
                            .profileId(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .avatarUrl(profile.getAvatarUrl())
                            .status(rel.getStatus())
                            .permissionLevel(rel.getPermissionLevel())
                            .build();
                })
                .toList();
    }

    public List<CaregiverProfileResponse> getAcceptedCaregiverProfiles() {
        List<Relationship> relationships = relationshipService.getAcceptedCaregiverRelationships();
        if (relationships.isEmpty()) return List.of();

        List<String> caregiverIds = relationships.stream().map(Relationship::getCaregiverId).toList();
        Map<String, UserProfile> profileMap = userProfileService.findAllById(caregiverIds).stream()
                .filter(p -> p.getRole() == Role.CAREGIVER)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getCaregiverId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getCaregiverId());
                    return CaregiverProfileResponse.builder()
                            .relationshipId(rel.getId())
                            .profileId(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .caregiverTitle(rel.getCaregiverTitle())
                            .avatarUrl(profile.getAvatarUrl())
                            .status(rel.getStatus())
                            .permissionLevel(rel.getPermissionLevel())
                            .build();
                })
                .toList();
    }

    public List<ElderlyProfileResponse> getPendingElderlyProfiles() {
        List<Relationship> relationships = relationshipService.getPendingElderlyRelationships();
        if (relationships.isEmpty()) return List.of();

        List<String> elderlyIds = relationships.stream().map(Relationship::getElderlyId).toList();
        Map<String, UserProfile> profileMap = userProfileService.findAllById(elderlyIds).stream()
                .filter(p -> p.getRole() == Role.ELDERLY)
                .collect(Collectors.toMap(UserProfile::getId, p -> p));

        return relationships.stream()
                .filter(rel -> profileMap.containsKey(rel.getElderlyId()))
                .map(rel -> {
                    UserProfile profile = profileMap.get(rel.getElderlyId());
                    return ElderlyProfileResponse.builder()
                            .relationshipId(rel.getId())
                            .profileId(profile.getId())
                            .firstName(profile.getFirstName())
                            .lastName(profile.getLastName())
                            .phone(profile.getPhone())
                            .address(profile.getAddress())
                            .avatarUrl(profile.getAvatarUrl())
                            .status(rel.getStatus())
                            .permissionLevel(rel.getPermissionLevel())
                            .build();
                })
                .toList();
    }
}
