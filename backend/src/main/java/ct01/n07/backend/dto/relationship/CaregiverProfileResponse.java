package ct01.n07.backend.dto.relationship;

import ct01.n07.backend.model.enums.PermissionLevel;
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
    private RelationStatus status;
    private PermissionLevel permissionLevel;
}
