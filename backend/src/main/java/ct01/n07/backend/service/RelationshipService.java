package ct01.n07.backend.service;

import ct01.n07.backend.dto.relationship.CaregiverProfileResponse;
import ct01.n07.backend.dto.relationship.ElderlyProfileResponse;
import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.enums.PermissionLevel;

import java.util.List;

public interface RelationshipService {

    // --- Raw domain data methods (used by Facade to aggregate profiles) ---
    List<Relationship> getAcceptedElderlyRelationships();
    List<Relationship> getAcceptedCaregiverRelationships();
    List<Relationship> getPendingElderlyRelationships();
    List<Relationship> getAcceptedCaregiverRelationshipsByElderly(String elderlyId);

    // --- Profile-mapped convenience methods ---
    List<ElderlyProfileResponse> getAcceptedElderlyProfiles();
    List<CaregiverProfileResponse> getAcceptedCaregiverProfiles();
    List<ElderlyProfileResponse> getPendingElderlyProfiles();
    List<CaregiverProfileResponse> getPendingCaregiverProfiles();

    String sendInvitation(RelationshipInviteRequest request);
    void acceptInvitation(String relationshipId);
    void refuseInvitation(String relationshipId);
    void updateRelationship(String elderlyId, PermissionLevel permissionLevel);
    void createRelationship(String caregiverId, String elderlyId, String caregiverTitle, String elderlyTitle, PermissionLevel permissionLevel);
}
