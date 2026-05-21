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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void updateRelationshipUpdatesAcceptedRelationWithoutCreatingPendingClone() {
        Relationship acceptedRelationship = Relationship.builder()
                .id("accepted-relationship")
                .caregiverId("caregiver-1")
                .elderlyId("elderly-1")
                .permissionLevel(PermissionLevel.MANAGE_ALL)
                .status(RelationStatus.ACCEPTED)
                .build();
        Relationship stalePendingRelationship = Relationship.builder()
                .id("stale-pending-relationship")
                .caregiverId("caregiver-1")
                .elderlyId("elderly-1")
                .status(RelationStatus.PENDING)
                .build();

        when(profileAccessContext.getCurrentUserProfile()).thenReturn(caregiverProfile());
        when(relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.ACCEPTED)).thenReturn(List.of(acceptedRelationship));
        when(relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.PENDING)).thenReturn(List.of(stalePendingRelationship));

        relationshipService.updateRelationship("elderly-1", FamilyRelation.OTHER, "Dì ruột");

        ArgumentCaptor<Relationship> relationshipCaptor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository).save(relationshipCaptor.capture());
        verify(relationshipRepository).deleteAll(List.of(stalePendingRelationship));
        Relationship updatedRelationship = relationshipCaptor.getValue();
        assertThat(updatedRelationship.getId()).isEqualTo("accepted-relationship");
        assertThat(updatedRelationship.getRelation()).isEqualTo(FamilyRelation.OTHER);
        assertThat(updatedRelationship.getCustomRelation()).isEqualTo("Dì ruột");
        assertThat(updatedRelationship.getPermissionLevel()).isEqualTo(PermissionLevel.MANAGE_ALL);
        assertThat(updatedRelationship.getStatus()).isEqualTo(RelationStatus.ACCEPTED);
    }

    @Test
    void updateRelationshipUpdatesPendingInvitationWhenNoAcceptedRelationExists() {
        Relationship pendingRelationship = Relationship.builder()
                .id("pending-relationship")
                .caregiverId("caregiver-1")
                .elderlyId("elderly-1")
                .status(RelationStatus.PENDING)
                .build();

        when(profileAccessContext.getCurrentUserProfile()).thenReturn(caregiverProfile());
        when(relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.ACCEPTED)).thenReturn(List.of());
        when(relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.PENDING)).thenReturn(List.of(pendingRelationship));

        relationshipService.updateRelationship("elderly-1", FamilyRelation.MOTHER, null);

        ArgumentCaptor<Relationship> relationshipCaptor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository).save(relationshipCaptor.capture());
        verify(relationshipRepository, never()).deleteAll(any());
        Relationship updatedRelationship = relationshipCaptor.getValue();
        assertThat(updatedRelationship.getId()).isEqualTo("pending-relationship");
        assertThat(updatedRelationship.getRelation()).isEqualTo(FamilyRelation.MOTHER);
        assertThat(updatedRelationship.getCustomRelation()).isNull();
        assertThat(updatedRelationship.getPermissionLevel()).isEqualTo(PermissionLevel.MANAGE_ALL);
        assertThat(updatedRelationship.getStatus()).isEqualTo(RelationStatus.PENDING);
    }

    @Test
    void updateCaregiverTitleLetsElderlyNameCaregiverWithoutChangingCaregiverRelation() {
        Relationship relationship = Relationship.builder()
                .id("relationship-1")
                .caregiverId("caregiver-1")
                .elderlyId("elderly-1")
                .caregiverTitle("Người chăm sóc")
                .elderlyTitle("Mẹ")
                .relation(FamilyRelation.MOTHER)
                .status(RelationStatus.ACCEPTED)
                .build();

        when(profileAccessContext.getCurrentUserProfile()).thenReturn(elderlyProfile());
        when(relationshipRepository.findByIdAndElderlyId("relationship-1", "elderly-1"))
                .thenReturn(Optional.of(relationship));

        relationshipService.updateCaregiverTitle("relationship-1", "Con gái");

        ArgumentCaptor<Relationship> relationshipCaptor = ArgumentCaptor.forClass(Relationship.class);
        verify(relationshipRepository).save(relationshipCaptor.capture());
        Relationship updatedRelationship = relationshipCaptor.getValue();
        assertThat(updatedRelationship.getCaregiverTitle()).isEqualTo("Con gái");
        assertThat(updatedRelationship.getElderlyTitle()).isEqualTo("Mẹ");
        assertThat(updatedRelationship.getRelation()).isEqualTo(FamilyRelation.MOTHER);
    }

    private UserProfile caregiverProfile() {
        return UserProfile.builder()
                .id("caregiver-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .build();
    }

    private UserProfile elderlyProfile() {
        return UserProfile.builder()
                .id("elderly-1")
                .userId("user-2")
                .role(Role.ELDERLY)
                .build();
    }
}
