package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.model.DoseEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DoseEventMapper {

    DoseEvent toModel(DoseEvent doseEvent);

    DoseEventResponse toResponse(DoseEvent doseEvent);
}
