package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.relationship.ElderlyProfileResponse;
import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.service.RelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/relationship")
public class CaregiverRelationshipController {
    private final RelationshipService relationshipService;

    @GetMapping
    public ResponseEntity<List<ElderlyProfileResponse>> getRelativeElderlyProfiles() {
        return ResponseEntity.ok(relationshipService.getRelativeElderlyProfiles());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ElderlyProfileResponse>> getPendingElderlyProfiles() {
        return ResponseEntity.ok(relationshipService.getPendingElderlyProfiles());
    }

    @PostMapping("/invite")
    public ResponseEntity<Map<String, String>> sendInvitation(@Valid @RequestBody RelationshipInviteRequest request) {
        String relationshipId = relationshipService.sendInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", relationshipId));
    }

    @PutMapping("/{targetElderlyId}")
    public ResponseEntity<Void> updateRelationship(
            @PathVariable("targetElderlyId") String targetElderlyId,
            @Valid @RequestBody RelationshipInviteRequest request) {
        relationshipService.updateRelationship(targetElderlyId, request);
        return ResponseEntity.ok().build();
    }
}
