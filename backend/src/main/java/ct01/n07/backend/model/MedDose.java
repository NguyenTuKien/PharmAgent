package ct01.n07.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedDose {
    @Builder.Default
    private String id = new ObjectId().toString();
    private LocalTime takenTime;
    private BigDecimal quantity;
}

