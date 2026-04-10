package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.medication.*;
import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface MedicationMapper {

    @Mapping(source = "medDoseRequests", target = "scheduleTimeList")
    @Mapping(target = "isActive", source = "isActive", defaultValue = "true")
    @Mapping(target = "id", ignore = true)
    MedSchedule toModel(MedScheduleRequest request);

    @Mapping(source = "medDoseRequests", target = "scheduleTimeList")
    @Mapping(target = "isActive", source = "isActive")
    void updateModel(@MappingTarget MedSchedule target, MedScheduleRequest request);

    @Mapping(source = "scheduleTimeList", target = "medDoses")
    MedScheduleResponse toResponse(MedSchedule model);

    @Mapping(source = "medicationSchedules", target = "schedules")
    MedicationResponse toResponse(Medication medication);

    @Mapping(target = "id", ignore = true)
    MedDose toModel(MedDoseRequest request);

    void updateModel(@MappingTarget MedDose target, MedDoseRequest request);

    MedDoseResponse toResponse(MedDose model);

    List<MedSchedule> toModels(List<MedScheduleRequest> requests);

    List<MedScheduleResponse> toResponses(List<MedSchedule> models);

    List<MedDose> toTimeModels(List<MedDoseRequest> requests);

    List<MedDoseResponse> toTimeResponses(List<MedDose> models);
}

