package ct01.n07.backend.repository;

import ct01.n07.backend.model.DoseEvent;
import ct01.n07.backend.model.enums.DoseStatus;
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

    // Lấy dose events theo patientMedicationIds, khoảng thời gian và trạng thái (cho stats)
    List<DoseEvent> findByPatientMedicationIdInAndScheduledAtBetween(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    long countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
            List<String> patientMedicationIds,
            LocalDateTime startTime,
            LocalDateTime endTime,
            DoseStatus status
    );
}
