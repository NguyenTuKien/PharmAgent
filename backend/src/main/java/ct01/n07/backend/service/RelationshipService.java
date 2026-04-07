package ct01.n07.backend.service;

import ct01.n07.backend.dto.relationship.CaregiverProfileResponse;
import ct01.n07.backend.dto.relationship.ElderlyProfileResponse;
import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.model.enums.PermissionLevel;

import java.util.List;

public interface RelationshipService {
    List<ElderlyProfileResponse> getRelativeElderlyProfiles();

    List<CaregiverProfileResponse> getRelativeCaregiverProfiles();

    List<ElderlyProfileResponse> getPendingElderlyProfiles();

    List<CaregiverProfileResponse> getPendingCaregiverProfiles();

    String sendInvitation(RelationshipInviteRequest request);

    void acceptInvitation(String relationshipId);

    void refuseInvitation(String relationshipId);

    void updateRelationship(String elderlyId, RelationshipInviteRequest request);

    void createRelationship(String caregiverId, String elderlyId, String relationshipName, PermissionLevel permissionLevel);
}

