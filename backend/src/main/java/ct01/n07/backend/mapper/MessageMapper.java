package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.message.MessageCreateRequest;
import ct01.n07.backend.dto.message.MessageResponse;
import ct01.n07.backend.model.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public Message toEntity(MessageCreateRequest request, String senderId) {
        if (request == null) {
            return null;
        }
        return Message.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .build();
    }

    public MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .status(message.getStatus())
                .sentAt(message.getSentAt())
                .build();
    }
}
