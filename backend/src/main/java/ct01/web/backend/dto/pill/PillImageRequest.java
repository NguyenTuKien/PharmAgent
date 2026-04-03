package ct01.web.backend.dto.pill;

import ct01.web.backend.model.enums.ViewType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PillImageRequest {
    private String imageUrl;
    private ViewType viewType;
    private boolean isPrimary;
}
