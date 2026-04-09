package ct01.n07.backend.service;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.dto.doseEvent.DoseStatusUpdateRequest;
import ct01.n07.backend.model.DoseEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DoseEventService {

    List<DoseEvent> getAllDoseEvents();

    DoseEvent getDoseEventById(String id);

    DoseEvent saveDoseEvent(DoseEvent doseEvent);

    void deleteDoseEvent(String id);

    // --- Nghiệp vụ mới ---

    /**
     * Lấy timeline cữ thuốc trong ngày cho một bệnh nhân (phân trang).
     */
    Page<DoseEventResponse> getTodayDoses(String patientId, Pageable pageable);

    /**
     * Lấy danh sách cữ thuốc đang chờ (PENDING) cho một bệnh nhân (phân trang).
     */
    Page<DoseEventResponse> getPendingDoses(String patientId, Pageable pageable);

    /**
     * Lấy danh sách cữ thuốc đã xử lý (khác PENDING) cho một bệnh nhân (phân trang).
     */
    Page<DoseEventResponse> getProcessedDoses(String patientId, Pageable pageable);



    /**
     * Cập nhật trạng thái một cữ thuốc (TAKEN / SKIPPED / REMIND).
     * Yêu cầu Profile Token (CAREGIVER hoặc ELDERLY).
     */
    DoseEventResponse updateDoseStatus(String id, DoseStatusUpdateRequest request);

    // [REFACTOR FIX]: Added for GroupBy status
    java.util.Map<ct01.n07.backend.model.enums.DoseStatus, Long> countDoseEventsByStatus(
            List<String> patientMedicationIds,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime
    );

    // [REFACTOR FIX]: Added for total count instead of loading ALL records into memory via .size()
    long countTotalDoseEvents(
            List<String> patientMedicationIds,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime
    );
}
