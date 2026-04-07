package ct01.n07.backend.dto.doseEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryWarningResponse {
    private String pillName;
    private int remainingQuantity;
}
