package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.auth.AdminUserCreateRequest;
import ct01.n07.backend.dto.auth.AdminUserResponse;
import ct01.n07.backend.dto.auth.LoginRequest;
import ct01.n07.backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "loginRequest.email")
    @Mapping(target = "password", source = "loginRequest.password")
    @Mapping(target = "googleSubject", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toModel(LoginRequest loginRequest);

    AdminUserResponse toAdminResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "googleSubject", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toModel(AdminUserCreateRequest request);
}
