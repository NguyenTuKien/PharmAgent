package ct01.n07.backend.facade;

import ct01.n07.backend.dto.doseEvent.AdherenceResponse;
import ct01.n07.backend.dto.doseEvent.InventoryWarningResponse;
import ct01.n07.backend.dto.patientMedication.MedicationResponse;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.repository.DoseEventRepository;
import ct01.n07.backend.repository.PatientMedicationRepository;
import ct01.n07.backend.service.PatientMedicationService;
import ct01.n07.backend.service.PillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsFacade {

    private final DoseEventRepository doseEventRepository;
    private final PatientMedicationRepository patientMedicationRepository;
    private final PatientMedicationService patientMedicationService;
    private final PillService pillService;

    // Ngưỡng cảnh báo: tồn kho còn <= 7 ngày dùng thì cảnh báo
    private static final int INVENTORY_WARNING_THRESHOLD = 7;

    /**
     * Tính toán tỷ lệ tuân thủ uống thuốc trong khoảng thời gian cho một bệnh nhân.
     * Gọi PatientMedicationService để lấy danh sách medicationIds,
     * sau đó truy vấn DoseEventRepository để đếm TAKEN / SKIPPED / total.
     */
    public AdherenceResponse getAdherence(String patientId, LocalDate startDate, LocalDate endDate) {
        log.info("StatsFacade: calculating adherence for patientId={}, from={} to={}", patientId, startDate, endDate);

        // Lấy danh sách ID của tất cả thuốc của bệnh nhân qua PatientMedicationService
        List<String> medicationIds = getMedicationIds(patientId);

        if (medicationIds.isEmpty()) {
            return AdherenceResponse.builder()
                    .total(0).taken(0).skipped(0).adherencePercent(0.0)
                    .build();
        }

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // Tổng số cữ thuốc trong khoảng thời gian
        long total = doseEventRepository
                .countByPatientMedicationIdInAndScheduledAtBetween(medicationIds, startTime, endTime);

        if (total == 0) {
            return AdherenceResponse.builder()
                    .total(0).taken(0).overdue(0).missed(0).skipped(0).pending(0).adherencePercent(0.0)
                    .build();
        }

        // Đếm các trạng thái
        long taken = doseEventRepository
                .countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
                        medicationIds, startTime, endTime, DoseStatus.TAKEN);

        long overdue = doseEventRepository
                .countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
                        medicationIds, startTime, endTime, DoseStatus.OVERDUE);

        long missed = doseEventRepository
                .countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
                        medicationIds, startTime, endTime, DoseStatus.MISSED);

        long skipped = doseEventRepository
                .countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
                        medicationIds, startTime, endTime, DoseStatus.SKIPPED);

        long pending = doseEventRepository
                .countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
                        medicationIds, startTime, endTime, DoseStatus.PENDING);

        // Tính adherencePercent: (Taken + Overdue) / Total, làm tròn 2 chữ số thập phân
        double adherencePercent = Math.round(((double) (taken + overdue) / total) * 10000.0) / 100.0;

        return AdherenceResponse.builder()
                .total((int) total)
                .taken((int) taken)
                .overdue((int) overdue)
                .missed((int) missed)
                .skipped((int) skipped)
                .pending((int) pending)
                .adherencePercent(adherencePercent)
                .build();
    }

    /**
     * Lấy danh sách thuốc sắp hết tồn kho của một bệnh nhân.
     * Gọi PatientMedicationService để lấy danh sách thuốc,
     * sau đó gọi PillService để lấy tên thuốc.
     */
    public List<InventoryWarningResponse> getInventoryWarnings(String patientId) {
        log.info("StatsFacade: fetching inventory warnings for patientId={}", patientId);

        List<MedicationResponse> medications = patientMedicationService
                .getMedications(patientId, null, Pageable.unpaged()).getContent();

        if (medications == null || medications.isEmpty()) {
            return List.of();
        }
        return medications.stream()
                .filter(med -> med.getIsActive() != null && med.getIsActive()
                        && med.getTotalQuantity() != null
                        && med.getTotalQuantity() <= INVENTORY_WARNING_THRESHOLD)
                .map(med -> {
                    // Lấy tên thuốc từ PillService
                    String pillName;
                    try {
                        pillName = pillService.getPillById(med.getPillId()).getName();
                    } catch (Exception e) {
                        pillName = "Thuốc không xác định (ID: " + med.getPillId() + ")";
                        log.warn("StatsFacade: Pill not found for pillId={}", med.getPillId());
                    }
                    return InventoryWarningResponse.builder()
                            .pillName(pillName)
                            .remainingQuantity(med.getTotalQuantity())
                            .build();
                })
                .toList();
    }

    /**
     * Thống kê số lượng người đang sử dụng thuốc (có ít nhất 1 thuốc đang hoạt động).
     */
    public long getActivePatientsCount() {
        log.info("StatsFacade: counting active medication users");
        return patientMedicationRepository.findAllActiveMedications().stream()
                .map(PatientMedication::getPatientId)
                .distinct()
                .count();
    }

    // Helper: lấy danh sách medication IDs qua PatientMedicationService
    private List<String> getMedicationIds(String patientId) {
        List<MedicationResponse> medications = patientMedicationService
                .getMedications(patientId, null, Pageable.unpaged()).getContent();
        if (medications == null)
            return List.of();
        return medications.stream().map(MedicationResponse::getId).toList();
    }
}
