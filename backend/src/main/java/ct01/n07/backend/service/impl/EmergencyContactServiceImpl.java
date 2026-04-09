package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.userProfile.EmergencyContactRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.EmergencyContact;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.EmergencyContactService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse addContact(EmergencyContactRequest request) {
        UserProfile profile = userProfileService.getCurrentUserProfile();
        if (profile.getEmergencyContacts() == null) {
            profile.setEmergencyContacts(new ArrayList<>());
        }

        java.util.Optional<EmergencyContact> existingContact = profile.getEmergencyContacts().stream()
                .filter(c -> c.getPhone().equals(request.getPhone()))
                .findFirst();

        if (existingContact.isPresent()) {
            existingContact.get().setName(request.getName());
        } else {
            EmergencyContact contact = EmergencyContact.builder()
                    .name(request.getName())
                    .phone(request.getPhone())
                    .build();
            profile.getEmergencyContacts().add(contact);
        }

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse updateContact(String contactId, EmergencyContactRequest request) {
        UserProfile profile = userProfileService.getCurrentUserProfile();
        List<EmergencyContact> contacts = profile.getEmergencyContacts();
        if (contacts == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact list is empty");
        }
        EmergencyContact contactToUpdate = contacts.stream()
                .filter(c -> c.getId().equals(contactId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));

        contactToUpdate.setName(request.getName());
        contactToUpdate.setPhone(request.getPhone());

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse deleteContact(String contactId) {
        UserProfile profile = userProfileService.getCurrentUserProfile();
        List<EmergencyContact> contacts = profile.getEmergencyContacts();
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
    public List<EmergencyContact> getMyContacts() {
        UserProfile profile = userProfileService.getCurrentUserProfile();
        return profile.getEmergencyContacts() != null ? profile.getEmergencyContacts() : new ArrayList<>();
    }
}
