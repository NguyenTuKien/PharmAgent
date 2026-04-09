package ct01.n07.backend.service;

import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.dto.event.DoseStatusUpdateRequest;
import ct01.n07.backend.model.EventDose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventDoseService {

    List<EventDose> getAllDoseEvents();

    EventDose getEventDoseById(String id);

    EventDose saveEventDose(EventDose eventDose);

    void deleteEventDose(String id);

    // --- Nghiệp vụ mới ---

    /**
     * Lấy timeline cữ thuốc trong ngày cho một bệnh nhân (phân trang).
     */
    Page<EventDoseResponse> getTodayDoses(String patientId, Pageable pageable);

    /**
     * Lấy danh sách cữ thuốc đang chờ (PENDING) cho một bệnh nhân (phân trang).
     */
    Page<EventDoseResponse> getPendingDoses(String patientId, Pageable pageable);

    /**
     * Lấy danh sách cữ thuốc đã xử lý (khác PENDING) cho một bệnh nhân (phân trang).
     */
    Page<EventDoseResponse> getProcessedDoses(String patientId, Pageable pageable);



    /**
     * Cập nhật trạng thái một cữ thuốc (TAKEN / SKIPPED / REMIND).
     * Yêu cầu Profile Token (CAREGIVER hoặc ELDERLY).
     */
    EventDoseResponse updateDoseStatus(String id, DoseStatusUpdateRequest request);

    // [REFACTOR FIX]: Added for GroupBy status
    java.util.Map<ct01.n07.backend.model.enums.DoseStatus, Long> countDoseEventsByStatus(
            List<String> medicationIds,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime
    );

    // [REFACTOR FIX]: Added for total count instead of loading ALL records into memory via .size()
    long countTotalDoseEvents(
            List<String> medicationIds,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime
    );
}


