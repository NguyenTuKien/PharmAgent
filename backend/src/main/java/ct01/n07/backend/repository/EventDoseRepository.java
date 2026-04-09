package ct01.n07.backend.repository;

import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.enums.DoseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventDoseRepository extends MongoRepository<EventDose, String> {

    // Lấy tất cả dose events của danh sách medication trong khoảng thời gian (cho timeline ngày)
    List<EventDose> findByMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
            List<String> medicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<EventDose> findByMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
            List<String> medicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // Lấy dose events theo medicationIds, khoảng thời gian và trạng thái (cho stats)
    List<EventDose> findByMedicationIdInAndScheduledAtBetween(
            List<String> medicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    long countByMedicationIdInAndScheduledAtBetween(
            List<String> medicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<EventDose> findByMedicationIdInAndStatus(
            List<String> medicationIds,
            DoseStatus status,
            Pageable pageable
    );

    Page<EventDose> findByMedicationIdInAndStatusNot(
            List<String> medicationIds,
            DoseStatus status,
            Pageable pageable
    );

    long countByMedicationIdInAndScheduledAtBetweenAndStatus(
            List<String> medicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            DoseStatus status
    );

    void deleteByMedDoseId(String medDoseId);

    void deleteByScheduleIdAndStatus(String scheduleId, DoseStatus status);

    void deleteByScheduleId(String scheduleId);

    void deleteByMedicationId(String medicationId);

    java.util.Optional<EventDose> findByMedDoseId(String medDoseId);
}

