package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.user.UserContactRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.constant.ProfileConstant;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.UserContact;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.UserContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserContactServiceImpl implements UserContactService {

    private final ProfileAccessContext profileAccessContext;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse addUserContact(UserContactRequest request) {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        requireEditableCurrentProfile(profile);
        if (profile.getUserContacts() == null) {
            profile.setUserContacts(new ArrayList<>());
        }

        java.util.Optional<UserContact> existingContact = profile.getUserContacts().stream()
                .filter(c -> c.getPhone().equals(request.getPhone()))
                .findFirst();

        if (existingContact.isPresent()) {
            existingContact.get().setName(request.getName());
        } else {
            UserContact contact = UserContact.builder()
                    .name(request.getName())
                    .phone(request.getPhone())
                    .build();
            profile.getUserContacts().add(contact);
        }

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse updateUserContact(String contactId, UserContactRequest request) {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        requireEditableCurrentProfile(profile);
        List<UserContact> contacts = profile.getUserContacts();
        if (contacts == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact list is empty");
        }
        UserContact contactToUpdate = contacts.stream()
                .filter(c -> c.getId().equals(contactId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));

        contactToUpdate.setName(request.getName());
        contactToUpdate.setPhone(request.getPhone());

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse deleteContact(String contactId) {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        requireEditableCurrentProfile(profile);
        List<UserContact> contacts = profile.getUserContacts();
        if (contacts == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact list is empty");
        }
        boolean removed = contacts.removeIf(c -> c.getId().equals(contactId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found");
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public List<UserContact> getMyContacts() {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        return profile.getUserContacts() != null ? profile.getUserContacts() : new ArrayList<>();
    }

    private void requireEditableCurrentProfile(UserProfile profile) {
        if (profile.getRole() == Role.ELDERLY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ProfileConstant.ELDERLY_PROFILE_READ_ONLY);
        }
    }
}
