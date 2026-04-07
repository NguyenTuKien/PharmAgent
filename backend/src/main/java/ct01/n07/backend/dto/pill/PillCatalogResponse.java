package ct01.n07.backend.dto.pill;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PillCatalogResponse {
    private String id;
    private String name;
    private String description;
    private List<String> imageUrls;
    private Instant createdAt;
}

