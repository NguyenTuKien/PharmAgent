package ct01.web.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pills")
public class Pill {
    @Id
    private String id;

    @Indexed
    private String name;
    private String genericName;
    private String brandName;
    private String strength;
    private String dosageForm;
    private String color;
    private String shape;
    private String description;
    private String usageInstructions;
    private String warning;
    private String sideEffects;
    private String manufacturer;
    private boolean isActive;

    // Embedded Array cho danh sách ảnh
    private List<PillImage> images;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
