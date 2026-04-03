package ct01.web.backend.model;

import ct01.web.backend.model.enums.ViewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PillImage {
    @Builder.Default
    private String id = new ObjectId().toString();
    private String imageUrl;
    private ViewType viewType;
    private boolean isPrimary;
}
