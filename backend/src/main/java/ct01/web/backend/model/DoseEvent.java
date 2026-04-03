package ct01.web.backend.model;

import ct01.web.backend.model.enums.DoseStatus;
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
public class DoseEvent {
    @Id
    private String id;

    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String patientMedicationId; // Thêm trường này để dễ filter theo đơn thuốc

    private String scheduleId; // ID của MedicationSchedule nhúng bên trong Medication
    private String scheduleTimeId; // ID của ScheduleTime

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