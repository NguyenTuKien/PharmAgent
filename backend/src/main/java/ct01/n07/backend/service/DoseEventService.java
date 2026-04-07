package ct01.n07.backend.service;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.dto.doseEvent.DoseStatusUpdateRequest;
import ct01.n07.backend.model.DoseEvent;

import java.time.LocalDate;
import java.util.List;

public interface DoseEventService {

    List<DoseEvent> getAllDoseEvents();

    DoseEvent getDoseEventById(String id);

    DoseEvent saveDoseEvent(DoseEvent doseEvent);

    void deleteDoseEvent(String id);

    // --- Nghiệp vụ mới ---

    /**
     * Lấy timeline cữ thuốc trong ngày cho một bệnh nhân.
     * Yêu cầu Profile Token (CAREGIVER hoặc ELDERLY).
     */
    List<DoseEventResponse> getTodayTimeline(String patientId, LocalDate date);

    /**
     * Cập nhật trạng thái một cữ thuốc (TAKEN / SKIPPED / REMIND).
     * Yêu cầu Profile Token (CAREGIVER hoặc ELDERLY).
     */
    DoseEventResponse updateDoseStatus(String id, DoseStatusUpdateRequest request);
}
