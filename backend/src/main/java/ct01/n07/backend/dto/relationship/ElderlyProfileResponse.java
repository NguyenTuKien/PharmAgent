package ct01.n07.backend.dto.relationship;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElderlyProfileResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String avatarUrl;
}
