package ct01.n07.backend.controller;

import ct01.n07.backend.dto.message.MessageCreateRequest;
import ct01.n07.backend.dto.message.MessageResponse;
import ct01.n07.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(request));
    }

    @GetMapping
    public ResponseEntity<Page<MessageResponse>> getMyMessages(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(messageService.getMyMessages(pageable));
    }
}
