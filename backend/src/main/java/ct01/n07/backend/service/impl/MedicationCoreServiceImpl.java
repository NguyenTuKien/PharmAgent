package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.medication.MedicationCreateRequest;
import ct01.n07.backend.dto.medication.MedicationResponse;
import ct01.n07.backend.dto.medication.MedicationUpdateRequest;
import ct01.n07.backend.mapper.MedicationMapper;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.MedicationRepository;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.MedicationPermissionValidator;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.EventDoseSyncService;
import ct01.n07.backend.service.MedicationCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MedicationCoreServiceImpl implements MedicationCoreService {

    private final MedicationRepository medicationRepository;
    private final PillRepository pillRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileAccessContext profileAccessContext;
    private final MedicationMapper medicationMapper;
    private final MedicationPermissionValidator permissionValidator;
    private final EventDoseSyncService eventDoseSyncService;

    @Override
    public MedicationResponse createMedication(MedicationCreateRequest request) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);

        if (currentProfile.getRole() == Role.ELDERLY && !currentProfile.getId().equals(request.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Elderly can only create medication for their own profile");
        }

        if (!userProfileRepository.existsByIdAndRole(request.getPatientId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient profile is invalid");
        }

        if (!pillRepository.existsById(request.getPillId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
        }

        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), request.getPatientId());

        Medication medication = Medication.builder()
                .patientId(request.getPatientId())
                .pillId(request.getPillId())
                .nickname(request.getNickname())
                .dosageAmount(request.getDosageAmount())
                .dosageUnit(request.getDosageUnit())
                .route(request.getRoute())
                .mealRelation(request.getMealRelation())
                .instruction(request.getInstruction())
                .prescribedBy(request.getPrescribedBy())
                .purpose(request.getPurpose())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalQuantity(request.getTotalQuantity())
                .isActive(true)
                .medicationSchedules(medicationMapper.toModels(request.getSchedules()))
                .build();

        Medication savedPm = medicationRepository.save(medication);
        eventDoseSyncService.syncDoseEvents(savedPm);
        return toMedicationResponse(savedPm);
    }

    @Override
    public Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);
        permissionValidator.verifyAccessToPatient(currentProfile.getRole(), currentProfile.getId(), patientId);

        Page<Medication> page = isActive == null
                ? medicationRepository.findByPatientId(patientId, pageable)
                : medicationRepository.findByPatientIdAndIsActive(patientId, isActive, pageable);

        return page.map(this::toMedicationResponse);
    }

    @Override
    public MedicationResponse updateMedication(String id, MedicationUpdateRequest request) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);

        Medication medication = requireMedication(id);

        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), medication.getPatientId());

        if (request.getPillId() != null) {
            if (!pillRepository.existsById(request.getPillId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
            }
            medication.setPillId(request.getPillId());
        }

        if (request.getSchedules() != null) {
            medication.setMedicationSchedules(medicationMapper.toModels(request.getSchedules()));
        }

        if (request.getTotalQuantity() != null) {
            medication.setTotalQuantity(request.getTotalQuantity());
        }

        if (request.getIsActive() != null) {
            medication.setActive(request.getIsActive());
        }

        if (request.getNickname() != null)
            medication.setNickname(request.getNickname());
        if (request.getDosageAmount() != null)
            medication.setDosageAmount(request.getDosageAmount());
        if (request.getDosageUnit() != null)
            medication.setDosageUnit(request.getDosageUnit());
        if (request.getRoute() != null)
            medication.setRoute(request.getRoute());
        if (request.getMealRelation() != null)
            medication.setMealRelation(request.getMealRelation());
        if (request.getInstruction() != null)
            medication.setInstruction(request.getInstruction());
        if (request.getPrescribedBy() != null)
            medication.setPrescribedBy(request.getPrescribedBy());
        if (request.getPurpose() != null)
            medication.setPurpose(request.getPurpose());
        if (request.getStartDate() != null)
            medication.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            medication.setEndDate(request.getEndDate());

        Medication savedPm = medicationRepository.save(medication);
        if (request.getSchedules() != null) {
            eventDoseSyncService.syncDoseEvents(savedPm);
        }
        return toMedicationResponse(savedPm);
    }

    @Override
    public MedicationResponse getMedicationById(String id) {
        Medication medication = requireMedication(id);
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifyAccessToPatient(currentProfile.getRole(), currentProfile.getId(), medication.getPatientId());
        return toMedicationResponse(medication);
    }

    @Override
    public void deleteMedication(String id) {
        Medication pm = requireMedication(id);
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        medicationRepository.deleteById(id);
        eventDoseSyncService.deleteByMedicationId(id);
    }

    private Medication requireMedication(String id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found"));
    }

    private MedicationResponse toMedicationResponse(Medication medication) {
        return medicationMapper.toResponse(medication);
    }
}
