package ct01.n07.backend.dto.relationship;

import ct01.n07.backend.model.enums.FamilyRelation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RelationshipRelationRequest {
    @NotNull(message = "Quan hệ không được để trống")
    private FamilyRelation relation;

    @Size(max = 50, message = "Quan hệ khác không được vượt quá 50 ký tự")
    private String customRelation;
}
