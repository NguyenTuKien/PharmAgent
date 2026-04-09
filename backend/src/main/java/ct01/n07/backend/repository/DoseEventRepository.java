package ct01.n07.backend.repository;

import ct01.n07.backend.model.DoseEvent;
import ct01.n07.backend.model.enums.DoseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DoseEventRepository extends MongoRepository<DoseEvent, String> {

    // Lấy tất cả dose events của danh sách medication trong khoảng thời gian (cho timeline ngày)
    List<DoseEvent> findByPatientMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<DoseEvent> findByPatientMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // Lấy dose events theo patientMedicationIds, khoảng thời gian và trạng thái (cho stats)
    List<DoseEvent> findByPatientMedicationIdInAndScheduledAtBetween(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    long countByPatientMedicationIdInAndScheduledAtBetween(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<DoseEvent> findByPatientMedicationIdInAndStatus(
            List<String> patientMedicationIds,
            DoseStatus status,
            Pageable pageable
    );

    Page<DoseEvent> findByPatientMedicationIdInAndStatusNot(
            List<String> patientMedicationIds,
            DoseStatus status,
            Pageable pageable
    );

    long countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            DoseStatus status
    );

    void deleteByScheduleTimeId(String scheduleTimeId);

    void deleteByScheduleId(String scheduleId);

    void deleteByPatientMedicationId(String patientMedicationId);

    java.util.Optional<DoseEvent> findByScheduleTimeId(String scheduleTimeId);
}
