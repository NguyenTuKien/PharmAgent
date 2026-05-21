package ct01.n07.backend.dto.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAvatarRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void allowsNullAvatarUrlSoUsersCanRemoveTheirAvatar() {
        UpdateAvatarRequest request = new UpdateAvatarRequest();
        request.setAvatarUrl(null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsNonHttpAvatarUrl() {
        UpdateAvatarRequest request = new UpdateAvatarRequest();
        request.setAvatarUrl("ftp://example.com/avatar.png");

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("avatarUrl"));
    }
}
