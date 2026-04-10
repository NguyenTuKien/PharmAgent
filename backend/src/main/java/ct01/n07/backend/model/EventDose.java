package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.DoseStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "dose_events")
public class EventDose {
    @Id
    private String id;

    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String medicationId;

    private String scheduleId; // ID cua MedSchedule nhung ben trong Medication
    private String medDoseId; // ID cua MedDose

    @Indexed
    private LocalDateTime scheduledAt;
    private DoseStatus status;
    private LocalDateTime takenAt;

    @Field(targetType = FieldType.OBJECT_ID)
    private String confirmedBy; // Trỏ đến User(Caregiver/Elderly) đã xác nhận
    private String note;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
