package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.doseEvent.MedicationScheduleRequest;
import ct01.n07.backend.dto.doseEvent.PatientMedicationRequest;
import ct01.n07.backend.dto.doseEvent.ScheduleTimeRequest;
import ct01.n07.backend.model.MedicationSchedule;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.ScheduleTime;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PatientMedicationMapper {

    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "medicationSchedules", expression = "java(new java.util.ArrayList<>())")
    PatientMedication toModel(PatientMedicationRequest request);

    void updateModel(@MappingTarget PatientMedication target, PatientMedicationRequest request);

    PatientMedicationRequest toRequest(PatientMedication model);

    @Mapping(source = "scheduleTimeRequests", target = "scheduleTimeList")
    @Mapping(target = "isActive", constant = "true")
    MedicationSchedule toModel(MedicationScheduleRequest request);

    @Mapping(source = "scheduleTimeRequests", target = "scheduleTimeList")
    void updateModel(@MappingTarget MedicationSchedule target, MedicationScheduleRequest request);

    @Mapping(source = "scheduleTimeList", target = "scheduleTimeRequests")
    MedicationScheduleRequest toRequest(MedicationSchedule model);

    ScheduleTime toModel(ScheduleTimeRequest request);

    void updateModel(@MappingTarget ScheduleTime target, ScheduleTimeRequest request);

    ScheduleTimeRequest toRequest(ScheduleTime model);

    List<ScheduleTime> toModels(List<ScheduleTimeRequest> requests);

    List<ScheduleTimeRequest> toRequests(List<ScheduleTime> models);

    @AfterMapping
    default void ensureScheduleTimeList(MedicationScheduleRequest request, @MappingTarget MedicationSchedule target) {
        if (request.getScheduleTimeRequests() == null) {
            target.setScheduleTimeList(new java.util.ArrayList<>());
        }
    }
}

