package ct01.web.backend.service.impl;

import ct01.web.backend.dto.auth.LoginRequest;
import ct01.web.backend.mapper.UserMapper;
import ct01.web.backend.model.User;
import ct01.web.backend.model.enums.UserStatus;
import ct01.web.backend.repository.UserRepository;
import ct01.web.backend.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User verifyUserCredentials(String email, String password){
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }


    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public User findByEmail(String mail) {
        return userRepository.findByEmail(mail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }

    @Override
    public User createUser(LoginRequest loginRequest) {
        if (userRepository.findByEmail(loginRequest.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = userMapper.toModel(loginRequest);
        user.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
        user.setUserStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }
}
