package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.message.MessageCreateRequest;
import ct01.n07.backend.dto.message.MessageResponse;
import ct01.n07.backend.mapper.MessageMapper;
import ct01.n07.backend.model.Message;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.MessageStatus;
import ct01.n07.backend.repository.MessageRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.MessageService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final MessageMapper messageMapper;

    @Override
    public MessageResponse sendMessage(MessageCreateRequest request) {
        UserProfile sender = userProfileService.getCurrentUserProfile();
        
        // Validate receiver exists
        if (!userProfileRepository.existsById(request.getReceiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người nhận không tồn tại");
        }

        Message message = messageMapper.toEntity(request, sender.getId());
        message.setStatus(MessageStatus.SUCCESS);
        
        Message saved = messageRepository.save(message);
        log.info("Message sent from {} to {}", sender.getId(), request.getReceiverId());
        
        return messageMapper.toResponse(saved);
    }

    @Override
    public Page<MessageResponse> getMyMessages(Pageable pageable) {
        UserProfile currentUser = userProfileService.getCurrentUserProfile();
        
        Page<Message> messages = messageRepository.findByReceiverIdAndStatus(
                currentUser.getId(), MessageStatus.SUCCESS, pageable);
        
        return messages.map(messageMapper::toResponse);
    }
}
