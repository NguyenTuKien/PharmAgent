package ct01.web.backend.service.impl;

import ct01.web.backend.dto.doseEvent.MedicationScheduleRequest;
import ct01.web.backend.dto.doseEvent.PatientMedicationRequest;
import ct01.web.backend.dto.doseEvent.ScheduleTimeRequest;
import ct01.web.backend.mapper.PatientMedicationMapper;
import ct01.web.backend.model.MedicationSchedule;
import ct01.web.backend.model.PatientMedication;
import ct01.web.backend.model.ScheduleTime;
import ct01.web.backend.model.User;
import ct01.web.backend.repository.PatientMedicationRepository;
import ct01.web.backend.repository.PillRepository;
import ct01.web.backend.repository.UserRepository;
import ct01.web.backend.service.PatientMedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientMedicationServiceImpl implements PatientMedicationService {
    private final PatientMedicationRepository patientMedicationRepository;
    private final PillRepository pillRepository;
    private final UserRepository userRepository;
    private final PatientMedicationMapper patientMedicationMapper;

    @Override
    public List<PatientMedication> getAllPatientMedications() {
        return patientMedicationRepository.findAll();
    }

    @Override
    public PatientMedication getPatientMedicationById(String id) {
        return requirePatientMedication(id);
    }

    private PatientMedication requirePatientMedication(String id) {
        return patientMedicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient Medication not found"));
    }

    @Override
    public List<PatientMedication> getPatientMedicationsByPatientId(String patientId) {
        return patientMedicationRepository.findAll().stream()
                .filter(pm -> pm.getPatientId().equals(patientId))
                .collect(Collectors.toList());
    }

    @Override
    public PatientMedication createPatientMedication(PatientMedicationRequest patientMedicationRequest) {
        if (!userRepository.existsById(patientMedicationRequest.getPatientId())) {
            throw new RuntimeException("Patient not found");
        }
        if (!pillRepository.existsById(patientMedicationRequest.getPillId())) {
            throw new RuntimeException("Pill not found");
        }

        return patientMedicationRepository.save(patientMedicationMapper.toModel(patientMedicationRequest));
    }

    @Override
    public PatientMedication updatePatientMedication(String id, PatientMedicationRequest request) {
        PatientMedication existing = requirePatientMedication(id);

        if (!userRepository.existsById(request.getPatientId())) {
            throw new RuntimeException("Patient not found");
        }
        if (!pillRepository.existsById(request.getPillId())) {
            throw new RuntimeException("Pill not found");
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
            throw new RuntimeException("Schedule not found");
        }

        MedicationSchedule scheduleToUpdate = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        patientMedicationMapper.updateModel(scheduleToUpdate, request);
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication deleteMedicationSchedule(String patientMedicationId, String scheduleId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new RuntimeException("Schedule not found");
        }
        boolean removed = schedules.removeIf(s -> Objects.equals(s.getId(), scheduleId));
        if (!removed) {
            throw new RuntimeException("Schedule not found");
        }
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication addScheduleTime(String patientMedicationId, String scheduleId, ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new RuntimeException("Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

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
            throw new RuntimeException("Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        List<ScheduleTime> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes == null) {
            throw new RuntimeException("Time not found");
        }

        ScheduleTime timeToUpdate = scheduleTimes.stream()
                .filter(t -> Objects.equals(t.getId(), timeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Time not found"));

        patientMedicationMapper.updateModel(timeToUpdate, request);
        return patientMedicationRepository.save(pm);
    }

    @Override
    public PatientMedication deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);
        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new RuntimeException("Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        List<ScheduleTime> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes != null) {
            boolean removed = scheduleTimes.removeIf(t -> Objects.equals(t.getId(), timeId));
            if (!removed) throw new RuntimeException("Time not found");
            return patientMedicationRepository.save(pm);
        }
        throw new RuntimeException("Time not found");
    }
}
