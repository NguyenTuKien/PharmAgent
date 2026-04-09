package ct01.n07.backend.service;

import ct01.n07.backend.dto.message.MessageCreateRequest;
import ct01.n07.backend.dto.message.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import ct01.n07.backend.model.Message;

public interface MessageService {
    MessageResponse sendMessage(MessageCreateRequest request);
    Page<MessageResponse> getMyMessages(Pageable pageable);
    void saveAllMessages(List<Message> messages);
}
