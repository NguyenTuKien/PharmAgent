package ct01.n07.backend.dto.chat;

import ct01.n07.backend.model.enums.SignalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalPayload {
    private String senderId;
    private String receiverId;
    private SignalType type;
    
    // Can be SDP Offer/Answer or ICE Candidate string
    private Object data;
}
