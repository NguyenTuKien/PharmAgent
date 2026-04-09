package ct01.n07.backend.service;

import ct01.n07.backend.dto.userProfile.EmergencyContactRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.model.EmergencyContact;

import java.util.List;

public interface EmergencyContactService {
    UserProfileResponse addContact(EmergencyContactRequest request);
    UserProfileResponse updateContact(String contactId, EmergencyContactRequest request);
    UserProfileResponse deleteContact(String contactId);
    List<EmergencyContact> getMyContacts();
}
