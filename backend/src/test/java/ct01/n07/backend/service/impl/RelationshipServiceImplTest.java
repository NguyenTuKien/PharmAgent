package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.relationship.RelationshipInviteRequest;
import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceImplTest {

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ProfileAccessContext profileAccessContext;

    @InjectMocks
    private RelationshipServiceImpl relationshipService;

    @Test
    void sendInvitationStoresFamilyRelationAndGrantsFullManagementInternally() {
        RelationshipInviteRequest request = new RelationshipInviteRequest();
        request.setTargetElderlyId("elderly-1");
        request.setRelation(FamilyRelation.MOTHER);

        when(profileAccessContext.getCurrentUserProfile()).thenReturn(caregiverProfile());
        when(userProfileRepository.existsByIdAndRole("elderly-1", Role.ELDERLY)).thenReturn(true);
        when(relationshipRepository.existsByCaregiverIdAndElderlyId("caregiver-1", "elderly-1")).thenReturn(false);
        when(relationshipRepository.save(any(Relationship.class))).thenAnswer(invocation -> {
            Relationship relationship = invocation.getArgument(0);
            relationship.setId("relationship-1");
            return relationship;
        });

        String relationshipId = relationshipService.sendInvitation(request);

        ArgumentCaptor<Relationship> relationshipCaptor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository).save(relationshipCaptor.capture());
        Relationship savedRelationship = relationshipCaptor.getValue();
        assertThat(relationshipId).isEqualTo("relationship-1");
        assertThat(savedRelationship.getRelation()).isEqualTo(FamilyRelation.MOTHER);
        assertThat(savedRelationship.getCustomRelation()).isNull();
        assertThat(savedRelationship.getPermissionLevel()).isEqualTo(PermissionLevel.MANAGE_ALL);
        assertThat(savedRelationship.getStatus()).isEqualTo(RelationStatus.PENDING);
    }

    @Test
    void updateRelationshipCreatesPendingRelationChangeWithFullManagementInternally() {
        when(profileAccessContext.getCurrentUserProfile()).thenReturn(caregiverProfile());
        when(relationshipRepository.existsByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.ACCEPTED)).thenReturn(true);
        when(relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.PENDING)).thenReturn(List.of());

        relationshipService.updateRelationship("elderly-1", FamilyRelation.OTHER, "Dì ruột");

        ArgumentCaptor<Relationship> relationshipCaptor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository).save(relationshipCaptor.capture());
        Relationship pendingRelationship = relationshipCaptor.getValue();
        assertThat(pendingRelationship.getRelation()).isEqualTo(FamilyRelation.OTHER);
        assertThat(pendingRelationship.getCustomRelation()).isEqualTo("Dì ruột");
        assertThat(pendingRelationship.getPermissionLevel()).isEqualTo(PermissionLevel.MANAGE_ALL);
        assertThat(pendingRelationship.getStatus()).isEqualTo(RelationStatus.PENDING);
    }

    private UserProfile caregiverProfile() {
        return UserProfile.builder()
                .id("caregiver-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .build();
    }
}
