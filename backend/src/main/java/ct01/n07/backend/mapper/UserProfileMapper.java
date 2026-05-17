package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.user.UserProfileSummaryResponse;
import ct01.n07.backend.dto.auth.RegisterRequest;
import ct01.n07.backend.dto.auth.UserInfoResponse;
import ct01.n07.backend.dto.user.CreateProfileRequest;
import ct01.n07.backend.dto.user.UpdateProfileRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "firstName", source = "req.caregiver.firstName")
    @Mapping(target = "lastName", source = "req.caregiver.lastName")
    @Mapping(target = "phone", source = "req.caregiver.phone")
    @Mapping(target = "dateOfBirth", source = "req.caregiver.dateOfBirth")
    @Mapping(target = "gender", source = "req.caregiver.gender")
    @Mapping(target = "address", source = "req.caregiver.address")
    @Mapping(target = "avatarUrl", source = "req.caregiver.avatarUrl")
    @Mapping(target = "role", constant = "CAREGIVER")
    @Mapping(target = "userContacts", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toCaregiverProfile(RegisterRequest req, String userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "firstName", source = "req.elderly.firstName")
    @Mapping(target = "lastName", source = "req.elderly.lastName")
    @Mapping(target = "phone", source = "req.elderly.phone")
    @Mapping(target = "dateOfBirth", source = "req.elderly.dateOfBirth")
    @Mapping(target = "gender", source = "req.elderly.gender")
    @Mapping(target = "address", source = "req.elderly.address")
    @Mapping(target = "avatarUrl", source = "req.elderly.avatarUrl")
    @Mapping(target = "role", constant = "ELDERLY")
    @Mapping(target = "userContacts", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toElderlyProfile(RegisterRequest req, String userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userContacts", expression = "java(new java.util.ArrayList<>())")
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
    @Mapping(target = "userContacts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserProfile(UpdateProfileRequest request, @MappingTarget UserProfile userProfile);
}
