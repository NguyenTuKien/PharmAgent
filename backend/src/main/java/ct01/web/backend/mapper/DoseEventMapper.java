package ct01.web.backend.mapper;

import ct01.web.backend.model.DoseEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DoseEventMapper {

    DoseEvent toModel(DoseEvent doseEvent);
}


