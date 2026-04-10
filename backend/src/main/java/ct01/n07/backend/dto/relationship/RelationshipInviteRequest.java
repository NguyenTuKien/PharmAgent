package ct01.n07.backend.dto.relationship;

import ct01.n07.backend.model.enums.PermissionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RelationshipInviteRequest {
    @NotBlank(message = "Target elderly ID is required")
    private String targetElderlyId;

    private String caregiverTitle;
    private String elderlyTitle;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
}

