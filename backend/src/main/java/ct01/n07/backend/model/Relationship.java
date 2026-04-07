package ct01.n07.backend.model;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.RelationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "caregiver_elderly_relations")
@CompoundIndex(name = "uq_caregiver_elderly_status", def = "{'caregiverId': 1, 'elderlyId': 1, 'status': 1}", unique = true)
public class Relationship {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String caregiverId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String elderlyId;

    private String relationshipName;
    private PermissionLevel permissionLevel;
    private RelationStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    @CreatedDate
    private Instant createdAt;
}
