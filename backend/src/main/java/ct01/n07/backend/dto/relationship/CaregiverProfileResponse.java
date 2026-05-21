package ct01.n07.backend.dto.relationship;

import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.RelationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaregiverProfileResponse {
    private String relationshipId;
    private String profileId;
    private String firstName;
    private String lastName;
    private String phone;
    private String caregiverTitle;
    private String address;
    private String avatarUrl;
    private FamilyRelation relation;
    private String customRelation;
    private String relationLabel;
    private RelationStatus status;
}
