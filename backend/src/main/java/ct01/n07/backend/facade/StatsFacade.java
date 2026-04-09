package ct01.n07.backend.facade;

import ct01.n07.backend.dto.stats.AdherenceResponse;
import ct01.n07.backend.dto.stats.InventoryWarningResponse;
import ct01.n07.backend.dto.medication.MedicationResponse;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.service.EventDoseService;
import ct01.n07.backend.service.MedicationService;
import ct01.n07.backend.service.PillService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.security.MedicationPermissionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsFacade {

    // [REFACTOR FIX]: Gỡ bỏ EventDoseRepository, MedicationRepository khỏi Facade
    // Facade chỉ được giao tiếp trực tiếp với các Service (Clean Architecture)
    private final EventDoseService eventDoseService;
    private final MedicationService medicationService;
    private final PillService pillService;
    
    // [REFACTOR FIX]: Thêm Relationship để kiểm tra phân quyền (Security) thông qua Validator
    private final MedicationPermissionValidator permissionValidator;
    private final UserProfileService userProfileService;

    private static final int INVENTORY_WARNING_THRESHOLD = 7;

    private void validatePermission(String patientId) {
        UserProfile currentUser = userProfileService.getCurrentUserProfile();
        if (currentUser.getId().equals(patientId)) {
            return; // Người dùng gọi xem thống kê của chính bản thân.
        }
        
        try {
            permissionValidator.verifyCaregiverPermission(currentUser.getId(), patientId);
        } catch (ResponseStatusException e) {
            log.warn("IDOR attempt detected: User {} tried to access stats of Patient {}", currentUser.getId(), patientId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thống kê của bệnh nhân này.");
        }
    }

    public AdherenceResponse getAdherence(String patientId, LocalDate startDate, LocalDate endDate) {
        log.info("StatsFacade: calculating adherence for patientId={}, from={} to={}", patientId, startDate, endDate);
        
        // [REFACTOR FIX]: Kiểm tra IDOR trước khi tiến hành logic hệ thống
        validatePermission(patientId);

        List<String> medicationIds = getMedicationIds(patientId);

        if (medicationIds.isEmpty()) {
            return AdherenceResponse.builder()
                    .total(0).taken(0).skipped(0).adherencePercent(0.0)
                    .build();
        }

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        // [REFACTOR FIX]: fix OOM (không dùng .size() nữa, dùng count service trả về trực tiếp số int)
        long totalLong = eventDoseService.countTotalDoseEvents(medicationIds, startTime, endTime);
        int total = (int) totalLong;

        if (total == 0) {
            return AdherenceResponse.builder()
                    .total(0).taken(0).overdue(0).missed(0).skipped(0).pending(0).adherencePercent(0.0)
                    .build();
        }

        // [REFACTOR FIX]: Lấy danh sách map các Status Group thay vì count 5 lần rời rạc
        Map<DoseStatus, Long> countsByStatus = eventDoseService.countDoseEventsByStatus(medicationIds, startTime, endTime);

        long taken = countsByStatus.getOrDefault(DoseStatus.TAKEN, 0L);
        long overdue = countsByStatus.getOrDefault(DoseStatus.OVERDUE, 0L);
        long missed = countsByStatus.getOrDefault(DoseStatus.MISSED, 0L);
        long skipped = countsByStatus.getOrDefault(DoseStatus.SKIPPED, 0L);
        long pending = countsByStatus.getOrDefault(DoseStatus.PENDING, 0L);

        double adherencePercent = Math.round(((double) (taken + overdue) / total) * 10000.0) / 100.0;

        return AdherenceResponse.builder()
                .total(total)
                .taken((int) taken)
                .overdue((int) overdue)
                .missed((int) missed)
                .skipped((int) skipped)
                .pending((int) pending)
                .adherencePercent(adherencePercent)
                .build();
    }

    public List<InventoryWarningResponse> getInventoryWarnings(String patientId) {
        log.info("StatsFacade: fetching inventory warnings for patientId={}", patientId);
        
        // [REFACTOR FIX]: IDOR verification
        validatePermission(patientId);

        List<MedicationResponse> medications = medicationService
                .getMedications(patientId, null, Pageable.unpaged()).getContent();

        if (medications.isEmpty()) {
            return List.of();
        }
        
        // Lọc các thuốc cần cảnh báo
        List<MedicationResponse> warnings = medications.stream()
                .filter(med -> med.getIsActive() != null && med.getIsActive()
                        && med.getTotalQuantity() != null
                        && med.getTotalQuantity() <= INVENTORY_WARNING_THRESHOLD)
                .toList();
                
        // [REFACTOR FIX]: Loại bỏ Query N+1. Lấy tất cả ID tiến hành query 1 lần vào DB        
        List<String> pillIds = warnings.stream().map(MedicationResponse::getPillId).distinct().toList();
        
        Map<String, String> pillNameMap = pillService.getPillsByIds(pillIds).stream()
                .collect(Collectors.toMap(Pill::getId, Pill::getName));

        return warnings.stream()
                .map(med -> {
                    // [REFACTOR FIX]: Try-catch chỉ in warning chứ không nuốt lỗi, sử dụng Dictionary cache (Map) phía trên
                    String pillName = pillNameMap.getOrDefault(med.getPillId(), "Thuốc không xác định");
                    if (pillName.equals("Thuốc không xác định")) {
                        log.warn("System consistency warning: Pill metadata not found for pillId={}", med.getPillId());
                    }
                    return InventoryWarningResponse.builder()
                            .pillName(pillName)
                            .remainingQuantity(med.getTotalQuantity())
                            .build();
                })
                .toList();
    }

    public long getActivePatientsCount() {
        log.info("StatsFacade: counting active medication users");
        // [REFACTOR FIX]: Fix OOM, thay vì map và distinct trên Stream RAM Java, lấy kết quả Count Aggregation trả thẳng từ DB.
        Long count = medicationService.countDistinctActivePatients();
        return count != null ? count : 0L;
    }

    private List<String> getMedicationIds(String patientId) {
        List<MedicationResponse> medications = medicationService
                .getMedications(patientId, null, Pageable.unpaged()).getContent();
        return medications.stream().map(MedicationResponse::getId).toList();
    }
}
