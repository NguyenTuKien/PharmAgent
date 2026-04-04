package ct01.web.backend.mapper;

import ct01.web.backend.dto.auth.SignupRequest;
import ct01.web.backend.dto.auth.UserInfoResponse;
import ct01.web.backend.dto.auth.UserProfileSummaryResponse;
import ct01.web.backend.dto.userProfile.CreateProfileRequest;
import ct01.web.backend.dto.userProfile.UpdateProfileRequest;
import ct01.web.backend.dto.userProfile.UserProfileResponse;
import ct01.web.backend.model.User;
import ct01.web.backend.model.UserProfile;
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
    @Mapping(target = "firstName", source = "signupRequest.firstName")
    @Mapping(target = "lastName", source = "signupRequest.lastName")
    @Mapping(target = "phone", source = "signupRequest.phone")
    @Mapping(target = "dateOfBirth", source = "signupRequest.dateOfBirth")
    @Mapping(target = "gender", source = "signupRequest.gender")
    @Mapping(target = "address", source = "signupRequest.address")
    @Mapping(target = "avatarUrl", source = "signupRequest.avatarUrl")
    @Mapping(target = "role", source = "signupRequest.role")
    @Mapping(target = "emergencyContacts", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "userDevices", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toUserProfile(SignupRequest signupRequest, String userId);

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

    UserProfileSummaryResponse toSummary(UserProfile userProfile);

    UserProfileResponse toResponse(UserProfile userProfile);

    ct01.web.backend.dto.userProfile.UserProfileSummaryResponse toProfileSummary(UserProfile userProfile);

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
