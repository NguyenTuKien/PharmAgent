package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.model.EventDose;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventDoseMapper {

    EventDose toModel(EventDose doseEvent);

    EventDoseResponse toResponse(EventDose doseEvent);
}

