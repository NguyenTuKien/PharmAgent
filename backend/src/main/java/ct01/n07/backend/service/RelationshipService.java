package ct01.n07.backend.service;

import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.PermissionLevel;

import java.util.List;

public interface RelationshipService {

    // --- Raw domain data methods (used by Facade to aggregate profiles) ---
    List<Relationship> getAcceptedElderlyRelationships();
    List<Relationship> getAcceptedCaregiverRelationships();
    List<Relationship> getPendingElderlyRelationships();
    List<Relationship> getPendingCaregiverRelationships();
    List<Relationship> getAcceptedCaregiverRelationshipsByElderly(String elderlyId);

    String sendInvitation(RelationshipInviteRequest request);
    void acceptInvitation(String relationshipId);
    void refuseInvitation(String relationshipId);
    void updateRelationship(String elderlyId, FamilyRelation relation, String customRelation);
    void deleteRelationship(String elderlyId);
    void createRelationship(String caregiverId, String elderlyId, FamilyRelation relation, String customRelation, PermissionLevel permissionLevel);
}
