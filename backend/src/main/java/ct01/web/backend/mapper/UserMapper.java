package ct01.web.backend.mapper;

import ct01.web.backend.dto.auth.LoginRequest;
import ct01.web.backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", source = "loginRequest.email")
    @Mapping(target = "password", source = "loginRequest.password")
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toModel(LoginRequest loginRequest);
}
