package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.n07.backend.dto.auth.SignupRequest;
import ct01.n07.backend.dto.auth.UserInfoResponse;
import ct01.n07.backend.dto.userProfile.CreateProfileRequest;
import ct01.n07.backend.dto.userProfile.UpdateProfileRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "firstName", source = "req.caregiverFirstName")
    @Mapping(target = "lastName", source = "req.caregiverLastName")
    @Mapping(target = "phone", source = "req.caregivePhone")
    @Mapping(target = "dateOfBirth", source = "req.caregiverDateOfBirth")
    @Mapping(target = "gender", source = "req.caregiverGender")
    @Mapping(target = "address", source = "req.caregiverAddress")
    @Mapping(target = "avatarUrl", source = "req.caregiverAvatarUrl")
    @Mapping(target = "role", constant = "CAREGIVER")
    @Mapping(target = "emergencyContacts", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "userDevices", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toCaregiverProfile(SignupRequest req, String userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "firstName", source = "req.elderlyFirstName")
    @Mapping(target = "lastName", source = "req.elderlyLastName")
    @Mapping(target = "phone", source = "req.elderlyphone")
    @Mapping(target = "dateOfBirth", source = "req.elderlyDateOfBirth")
    @Mapping(target = "gender", source = "req.elderlyGender")
    @Mapping(target = "address", source = "req.elderlyAddress")
    @Mapping(target = "avatarUrl", source = "req.elderlyAvatarUrl")
    @Mapping(target = "role", constant = "ELDERLY")
    @Mapping(target = "emergencyContacts", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "userDevices", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toElderlyProfile(SignupRequest req, String userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "emergencyContacts", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "userDevices", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toUserProfile(CreateProfileRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "userProfile.phone")
    @Mapping(target = "firstName", source = "userProfile.firstName")
    @Mapping(target = "lastName", source = "userProfile.lastName")
    @Mapping(target = "gender", source = "userProfile.gender")
    @Mapping(target = "dateOfBirth", source = "userProfile.dateOfBirth")
    @Mapping(target = "address", source = "userProfile.address")
    @Mapping(target = "avatarUrl", source = "userProfile.avatarUrl")
    @Mapping(target = "role", source = "userProfile.role")
    UserInfoResponse toUserInfoResponse(User user, UserProfile userProfile);

    ct01.n07.backend.dto.auth.UserProfileSummaryResponse toSummary(UserProfile userProfile);

    UserProfileResponse toResponse(UserProfile userProfile);

    UserProfileSummaryResponse toProfileSummary(UserProfile userProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "emergencyContacts", ignore = true)
    @Mapping(target = "userDevices", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserProfile(UpdateProfileRequest request, @MappingTarget UserProfile userProfile);
}
