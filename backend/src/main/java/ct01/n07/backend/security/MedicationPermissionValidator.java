package ct01.n07.backend.security;

import ct01.n07.backend.model.Relationship;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class MedicationPermissionValidator {

    private final RelationshipRepository relationshipRepository;

    public void verifySchedulePermission(Role profileRole, String profileId, String patientId) {
        if (profileRole == Role.CAREGIVER) {
            verifyCaregiverPermission(profileId, patientId,
                    PermissionLevel.EDIT_SCHEDULE,
                    PermissionLevel.MANAGE_ALL);
        } else {
            verifyAccessToPatient(profileRole, profileId, patientId);
        }
    }

    public void verifyAccessToPatient(Role currentProfileRole, String currentProfileId, String patientId) {
        if (currentProfileRole == Role.ELDERLY) {
            if (!currentProfileId.equals(patientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot access medications of another profile");
            }
            return;
        }

        verifyCaregiverPermission(currentProfileId, patientId);
    }

    public void verifyCaregiverPermission(String caregiverId, String elderlyId) {
        verifyCaregiverPermission(caregiverId, elderlyId, (PermissionLevel[]) null);
    }

    public void verifyCaregiverPermission(String caregiverId, String elderlyId,
            PermissionLevel... requiredLevels) {
        Relationship relationship = relationshipRepository
                .findAllByCaregiverIdAndElderlyIdAndStatus(
                        caregiverId,
                        elderlyId,
                        RelationStatus.ACCEPTED)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Caregiver does not have an accepted relationship with this patient"));

        if (requiredLevels != null && requiredLevels.length > 0) {
            boolean hasRequiredLevel = false;
            for (PermissionLevel level : requiredLevels) {
                if (relationship.getPermissionLevel() == level) {
                    hasRequiredLevel = true;
                    break;
                }
            }
            if (!hasRequiredLevel) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Caregiver does not have the required permission level to perform this action");
            }
        }
    }

    public void requireRole(Role profileRole, Role... allowedRoles) {
        for (Role role : allowedRoles) {
            if (profileRole == role) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }
}

