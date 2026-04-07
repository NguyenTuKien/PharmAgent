package ct01.n07.backend.dto.userProfile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EmergencyContactRequest {
    @NotBlank(message = "Contact name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone format is invalid")
    private String phone;
}
