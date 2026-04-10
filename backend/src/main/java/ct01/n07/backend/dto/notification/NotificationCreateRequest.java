package ct01.n07.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {
    @NotBlank(message = "Người nhận không được để trống")
    private String receiverId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}
