package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.model.PillImage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PillMapper {

    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "images", expression = "java(new java.util.ArrayList<>())")
    Pill toModel(PillRequest request);

    void updateModel(@MappingTarget Pill target, PillRequest request);

    PillRequest toRequest(Pill model);

    PillImage toModel(PillImageRequest request);

    void updateModel(@MappingTarget PillImage target, PillImageRequest request);

    PillImageRequest toRequest(PillImage model);

    List<PillImageRequest> toRequests(List<PillImage> models);
}

