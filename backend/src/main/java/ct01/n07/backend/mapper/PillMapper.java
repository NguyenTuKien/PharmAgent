package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.pill.PillCreateRequest;
import ct01.n07.backend.dto.pill.PillImageRequest;
import ct01.n07.backend.dto.pill.PillRequest;
import ct01.n07.backend.dto.pill.PillResponse;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.model.PillImage;
import ct01.n07.backend.model.enums.ViewType;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface PillMapper {

    @Mapping(target = "active", constant = "true")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Pill toModel(PillCreateRequest request);

    @Mapping(target = "active", constant = "true")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Pill toModel(PillRequest request);

    @org.mapstruct.AfterMapping
    default void afterToModel(@MappingTarget Pill pill, PillCreateRequest request) {
        pill.setImages(mapImages(request.getImages()));
    }

    @org.mapstruct.AfterMapping
    default void afterToModel(@MappingTarget Pill pill, PillRequest request) {
        pill.setImages(new java.util.ArrayList<>());
    }

    default List<PillImage> mapImages(List<String> imageUrls) {
        if (imageUrls == null) return new java.util.ArrayList<>();
        return imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(url -> PillImage.builder()
                        .imageUrl(url.trim())
                        .viewType(ViewType.OTHER)
                        .isPrimary(false)
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    void updateModel(@MappingTarget Pill target, PillRequest request);

    PillRequest toRequest(Pill model);

    PillResponse toResponse(Pill model);

    PillResponse.PillImageResponse toImageResponse(PillImage model);

    PillImage toModel(PillImageRequest request);

    void updateModel(@MappingTarget PillImage target, PillImageRequest request);

    PillImageRequest toRequest(PillImage model);

    List<PillImageRequest> toRequests(List<PillImage> models);
}

