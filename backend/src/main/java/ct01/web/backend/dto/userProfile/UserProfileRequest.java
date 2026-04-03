package ct01.web.backend.dto.userProfile;

import ct01.web.backend.model.enums.Gender;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class UserProfileRequest {
    private String phone;

    private String firstName;
    private String lastName;

    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String avatarUrl;
}
