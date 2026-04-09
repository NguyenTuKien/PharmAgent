package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.patientMedication.*;
import ct01.n07.backend.model.MedicationSchedule;
import ct01.n07.backend.model.ScheduleTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface PatientMedicationMapper {

    @Mapping(source = "scheduleTimeRequests", target = "scheduleTimeList")
    @Mapping(target = "isActive", source = "isActive", defaultValue = "true")
    @Mapping(target = "id", ignore = true)
    MedicationSchedule toModel(ScheduleRequest request);

    @Mapping(source = "scheduleTimeRequests", target = "scheduleTimeList")
    @Mapping(target = "isActive", source = "isActive")
    void updateModel(@MappingTarget MedicationSchedule target, ScheduleRequest request);

    @Mapping(source = "scheduleTimeList", target = "scheduleTimes")
    ScheduleResponse toResponse(MedicationSchedule model);

    @Mapping(target = "id", ignore = true)
    ScheduleTime toModel(ScheduleTimeRequest request);

    void updateModel(@MappingTarget ScheduleTime target, ScheduleTimeRequest request);

    ScheduleTimeResponse toResponse(ScheduleTime model);

    List<MedicationSchedule> toModels(List<ScheduleRequest> requests);

    List<ScheduleResponse> toResponses(List<MedicationSchedule> models);

    List<ScheduleTime> toTimeModels(List<ScheduleTimeRequest> requests);

    List<ScheduleTimeResponse> toTimeResponses(List<ScheduleTime> models);
}
