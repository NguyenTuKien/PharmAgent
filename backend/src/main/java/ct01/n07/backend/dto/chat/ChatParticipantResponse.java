package ct01.n07.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantResponse {
    private String profileId;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private String role;
}
