package ct01.n07.backend.controller.elderly;

import ct01.n07.backend.dto.relationship.CaregiverProfileResponse;
import ct01.n07.backend.facade.RelationshipProfileFacade;
import ct01.n07.backend.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/elderly/relationship")
public class ElderlyRelationshipController {
    private final RelationshipProfileFacade relationshipProfileFacade;
    private final RelationshipService relationshipService;

    @GetMapping
    public ResponseEntity<List<CaregiverProfileResponse>> getRelativeCaregiverProfiles() {
        return ResponseEntity.ok(relationshipProfileFacade.getAcceptedCaregiverProfiles());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CaregiverProfileResponse>> getPendingCaregiverProfiles() {
        return ResponseEntity.ok(relationshipService.getPendingCaregiverProfiles());
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable("id") String relationshipId) {
        relationshipService.acceptInvitation(relationshipId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/refuse")
    public ResponseEntity<Void> refuseInvitation(@PathVariable("id") String relationshipId) {
        relationshipService.refuseInvitation(relationshipId);
        return ResponseEntity.ok().build();
    }
}
