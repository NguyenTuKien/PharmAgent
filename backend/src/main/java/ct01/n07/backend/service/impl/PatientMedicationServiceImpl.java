package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.doseEvent.MedicationCreateRequest;
import ct01.n07.backend.dto.doseEvent.MedicationResponse;
import ct01.n07.backend.dto.doseEvent.MedicationScheduleRequest;
import ct01.n07.backend.dto.doseEvent.MedicationUpdateRequest;
import ct01.n07.backend.dto.doseEvent.PatientMedicationRequest;
import ct01.n07.backend.dto.doseEvent.ScheduleTimeRequest;
import ct01.n07.backend.mapper.PatientMedicationMapper;
import ct01.n07.backend.model.MedicationSchedule;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.ScheduleTime;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.ScheduleType;
import ct01.n07.backend.repository.PatientMedicationRepository;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.PatientMedicationService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientMedicationServiceImpl implements PatientMedicationService {
    private final PatientMedicationRepository patientMedicationRepository;
    private final PillRepository pillRepository;
    private final UserProfileRepository userProfileRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserProfileService userProfileService;
    private final PatientMedicationMapper patientMedicationMapper;

    @Override
    public MedicationResponse createMedication(MedicationCreateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER, Role.ELDERLY);

        if (currentProfile.getRole() == Role.ELDERLY && !currentProfile.getId().equals(request.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Elderly can only create medication for their own profile");
        }

        if (!userProfileRepository.existsByIdAndRole(request.getPatientId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient profile is invalid");
        }

        if (!pillRepository.existsById(request.getPillId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
        }

        if (currentProfile.getRole() == Role.CAREGIVER) {
            verifyCaregiverPermission(currentProfile.getId(), request.getPatientId());
        }

        PatientMedication medication = PatientMedication.builder()
                .patientId(request.getPatientId())
                .pillId(request.getPillId())
                .totalQuantity(request.getTotalQuantity())
                .isActive(true)
                .medicationSchedules(List.of(toSimpleDailySchedule(request.getSchedules())))
                .build();

        return toMedicationResponse(patientMedicationRepository.save(medication));
    }

    @Override
    public Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER, Role.ELDERLY);
        verifyAccessToPatient(currentProfile, patientId);

        Page<PatientMedication> page = isActive == null
                ? patientMedicationRepository.findByPatientId(patientId, pageable)
                : patientMedicationRepository.findByPatientIdAndIsActive(patientId, isActive, pageable);

        return page.map(this::toMedicationResponse);
    }

    @Override
    public MedicationResponse updateMedication(String id, MedicationUpdateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER, Role.ELDERLY);

        PatientMedication medication = requirePatientMedication(id);
        verifyAccessToPatient(currentProfile, medication.getPatientId());

        if (request.getPillId() != null) {
            if (!pillRepository.existsById(request.getPillId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
            }
            medication.setPillId(request.getPillId());
        }

        if (request.getSchedules() != null) {
            medication.setMedicationSchedules(List.of(toSimpleDailySchedule(request.getSchedules())));
        }

        if (request.getTotalQuantity() != null) {
            medication.setTotalQuantity(request.getTotalQuantity());
        }

        if (request.getIsActive() != null) {
            medication.setActive(request.getIsActive());
        }

        return toMedicationResponse(patientMedicationRepository.save(medication));
    }

    @Override
    public List<PatientMedication> getAllPatientMedications() {
        return patientMedicationRepository.findAll();
    }

    @Override
    public PatientMedication getPatientMedicationById(String id) {
        return requirePatientMedication(id);
    }

    @Override
    public List<PatientMedication> getPatientMedicationsByPatientId(String patientId) {
        return patientMedicationRepository.findByPatientId(patientId, Pageable.unpaged()).getContent();
    }

    @Override
    public PatientMedication createPatientMedication(PatientMedicationRequest request) {
        if (!userProfileRepository.existsByIdAndRole(request.getPatientId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient not found");
        }
        if (!pillRepository.existsById(request.getPillId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
        }

        return patientMedicationRepository.save(patientMedicationMapper.toModel(request));
    }

    @Override
    public PatientMedication updatePatientMedication(String id, PatientMedicationRequest request) {
        PatientMedication existing = requirePatientMedication(id);

        if (!userProfileRepository.existsByIdAndRole(request.getPatientId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient not found");
        }
        if (!pillRepository.existsById(request.getPillId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
        }

        patientMedicationMapper.updateModel(existing, request);
        return patientMedicationRepository.save(existing);
    }

    @Override
    public PatientMedication addMedicationSchedule(MedicationScheduleRequest medicationScheduleRequest) {
        PatientMedication patientMedication = requirePatientMedication(medicationScheduleRequest.getPatientMedicationId());

        if (patientMedication.getMedicationSchedules() == null) {
            patientMedication.setMedicationSchedules(new ArrayList<>());
        }

        patientMedication.getMedicationSchedules().add(patientMedicationMapper.toModel(medicationScheduleRequest));
        return patientMedicationRepository.save(patientMedication);
    }

    @Override
    public void deletePatientMedication(String id) {
        patientMedicationRepository.deleteById(id);
    }

    @Override
    public PatientMedication updateMedicationSchedule(String patientMedicationId, String scheduleId, MedicationScheduleRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule scheduleToUpdate = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        patientMedicationMapper.updateModel(scheduleToUpdate, request);
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication deleteMedicationSchedule(String patientMedicationId, String scheduleId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        boolean removed = schedules.removeIf(s -> Objects.equals(s.getId(), scheduleId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication addScheduleTime(String patientMedicationId, String scheduleId, ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        if (schedule.getScheduleTimeList() == null) {
            schedule.setScheduleTimeList(new ArrayList<>());
        }

        schedule.getScheduleTimeList().add(patientMedicationMapper.toModel(request));
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication updateScheduleTime(String patientMedicationId, String scheduleId, String timeId, ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        List<ScheduleTime> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
        }

        ScheduleTime timeToUpdate = scheduleTimes.stream()
                .filter(t -> Objects.equals(t.getId(), timeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found"));

        patientMedicationMapper.updateModel(timeToUpdate, request);
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        List<ScheduleTime> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes != null) {
            boolean removed = scheduleTimes.removeIf(t -> Objects.equals(t.getId(), timeId));
            if (!removed) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
            }
            return patientMedicationRepository.save(pm);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
    }

    private PatientMedication requirePatientMedication(String id) {
        return patientMedicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient medication not found"));
    }

    private void verifyAccessToPatient(UserProfile currentProfile, String patientId) {
        if (currentProfile.getRole() == Role.ELDERLY) {
            if (!currentProfile.getId().equals(patientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access medications of another profile");
            }
            return;
        }

        verifyCaregiverPermission(currentProfile.getId(), patientId);
    }

    private void verifyCaregiverPermission(String caregiverId, String elderlyId) {
        boolean hasPermission = relationshipRepository.existsByCaregiverIdAndElderlyIdAndStatus(
                caregiverId,
                elderlyId,
                RelationStatus.ACCEPTED
        );

        if (!hasPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Caregiver does not have access to this patient");
        }
    }

    private void requireRole(UserProfile profile, Role... allowedRoles) {
        for (Role role : allowedRoles) {
            if (profile.getRole() == role) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    private MedicationSchedule toSimpleDailySchedule(List<String> scheduleTimes) {
        if (scheduleTimes == null || scheduleTimes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one schedule time is required");
        }

        List<ScheduleTime> times = scheduleTimes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseScheduleTime)
                .toList();

        if (times.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid schedule time is required");
        }

        return MedicationSchedule.builder()
                .scheduleType(ScheduleType.DAILY)
                .isActive(true)
                .scheduleTimeList(times)
                .build();
    }

    private ScheduleTime parseScheduleTime(String value) {
        try {
            LocalTime parsed = LocalTime.parse(value);
            return ScheduleTime.builder().takenTime(parsed).quantity(BigDecimal.ONE).build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid schedule time format: " + value + " (expected HH:mm)");
        }
    }

    private MedicationResponse toMedicationResponse(PatientMedication medication) {
        List<String> schedules = medication.getMedicationSchedules() == null
                ? List.of()
                : medication.getMedicationSchedules().stream()
                .flatMap(schedule -> schedule.getScheduleTimeList() == null
                        ? java.util.stream.Stream.empty()
                        : schedule.getScheduleTimeList().stream())
                .map(ScheduleTime::getTakenTime)
                .filter(Objects::nonNull)
                .map(LocalTime::toString)
                .toList();

        return MedicationResponse.builder()
                .id(medication.getId())
                .patientId(medication.getPatientId())
                .pillId(medication.getPillId())
                .schedules(schedules)
                .totalQuantity(medication.getTotalQuantity())
                .isActive(medication.isActive())
                .build();
    }
}
