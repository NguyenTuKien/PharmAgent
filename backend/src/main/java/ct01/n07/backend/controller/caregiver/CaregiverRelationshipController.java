package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.relationship.ElderlyProfileResponse;
import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.dto.relationship.RelationshipRelationRequest;
import ct01.n07.backend.facade.RelationshipProfileFacade;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.service.RelationshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/relationship")
public class CaregiverRelationshipController {
    private final RelationshipProfileFacade relationshipProfileFacade;
    private final RelationshipService relationshipService;

    @GetMapping
    public ResponseEntity<List<ElderlyProfileResponse>> getRelativeElderlyProfiles() {
        return ResponseEntity.ok(relationshipProfileFacade.getAcceptedElderlyProfiles());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ElderlyProfileResponse>> getPendingElderlyProfiles() {
        return ResponseEntity.ok(relationshipProfileFacade.getPendingElderlyProfiles());
    }

    @PostMapping("/invite")
    public ResponseEntity<Map<String, String>> sendInvitation(@Valid @RequestBody RelationshipInviteRequest request) {
        String relationshipId = relationshipService.sendInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", relationshipId));
    }

    @PatchMapping("/{targetElderlyId}")
    public ResponseEntity<Void> updateRelationship(
            @PathVariable("targetElderlyId") String targetElderlyId,
            @RequestParam(value = "permissionLevel", required = false) PermissionLevel legacyPermissionLevel,
            @Valid @RequestBody RelationshipRelationRequest request) {
        relationshipService.updateRelationship(targetElderlyId, request.getRelation(), request.getCustomRelation());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetElderlyId}")
    public ResponseEntity<Void> deleteRelationship(@PathVariable("targetElderlyId") String targetElderlyId) {
        relationshipService.deleteRelationship(targetElderlyId);
        return ResponseEntity.noContent().build();
    }
}
