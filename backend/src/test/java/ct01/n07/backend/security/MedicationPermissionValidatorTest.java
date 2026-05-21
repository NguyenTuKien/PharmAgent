package ct01.n07.backend.security;

import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationPermissionValidatorTest {

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private MedicationPermissionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MedicationPermissionValidator(relationshipRepository, userProfileRepository);
    }

    @Test
    void caregiverCanManageElderlyProfileOwnedBySameAccountWithoutRelationshipRow() {
        when(userProfileRepository.findById("caregiver-1"))
                .thenReturn(Optional.of(UserProfile.builder()
                        .id("caregiver-1")
                        .userId("user-1")
                        .role(Role.CAREGIVER)
                        .build()));
        when(userProfileRepository.findById("elderly-1"))
                .thenReturn(Optional.of(UserProfile.builder()
                        .id("elderly-1")
                        .userId("user-1")
                        .role(Role.ELDERLY)
                        .build()));

        validator.verifySchedulePermission(Role.CAREGIVER, "caregiver-1", "elderly-1");

        verify(relationshipRepository, never())
                .findAllByCaregiverIdAndElderlyIdAndStatus("caregiver-1", "elderly-1", RelationStatus.ACCEPTED);
    }

    @Test
    void caregiverStillNeedsEditPermissionForAcceptedExternalRelationship() {
        when(userProfileRepository.findById("caregiver-1"))
                .thenReturn(Optional.of(UserProfile.builder()
                        .id("caregiver-1")
                        .userId("user-1")
                        .role(Role.CAREGIVER)
                        .build()));
        when(userProfileRepository.findById("elderly-1"))
                .thenReturn(Optional.of(UserProfile.builder()
                        .id("elderly-1")
                        .userId("user-2")
                        .role(Role.ELDERLY)
                        .build()));
        when(relationshipRepository.findAllByCaregiverIdAndElderlyIdAndStatus(
                "caregiver-1", "elderly-1", RelationStatus.ACCEPTED))
                .thenReturn(List.of(Relationship.builder()
                        .caregiverId("caregiver-1")
                        .elderlyId("elderly-1")
                        .status(RelationStatus.ACCEPTED)
                        .permissionLevel(PermissionLevel.VIEW)
                        .build()));

        assertThatThrownBy(() -> validator.verifySchedulePermission(Role.CAREGIVER, "caregiver-1", "elderly-1"))
                .hasMessageContaining("required permission level");
    }
}
