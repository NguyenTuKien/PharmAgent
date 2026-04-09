package ct01.n07.backend.service;

import ct01.n07.backend.dto.user.UserContactRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.model.UserContact;

import java.util.List;

public interface UserContactService {
    UserProfileResponse addUserContact(UserContactRequest request);
    UserProfileResponse updateUserContact(String contactId, UserContactRequest request);
    UserProfileResponse deleteContact(String contactId);
    List<UserContact> getMyContacts();
}

