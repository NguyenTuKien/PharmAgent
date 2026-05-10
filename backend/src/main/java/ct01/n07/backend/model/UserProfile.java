package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.Role;
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
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
public class UserProfile {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true, sparse = true)
    private String phone;

    private String firstName;
    private String lastName;

    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String avatarUrl;

    private List<UserContact> userContacts;

    private Role role;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
