package ct01.n07.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContact {
    @Builder.Default
    private String id = new ObjectId().toString();
    private String name;
    private String phone;
}
