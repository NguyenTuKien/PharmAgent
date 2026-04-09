package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.mapper.UserMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.repository.UserRepository;
import ct01.n07.backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
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

    @Override
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toAdminResponse);
    }

    @Override
    public AdminUserResponse adminCreateUser(AdminUserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User user = userMapper.toModel(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        return userMapper.toAdminResponse(saved);
    }

    @Override
    public AdminUserResponse adminUpdateUser(String id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // Check email conflict if changed
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }

        user.setUserStatus(request.getUserStatus());
        User saved = userRepository.save(user);
        return userMapper.toAdminResponse(saved);
    }

    @Override
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng");
        }
        
        // Cascade delete profiles
        userProfileRepository.deleteAllByUserId(id);
        
        userRepository.deleteById(id);
    }

    @Override
    public AdminUserResponse lockUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        
        user.setUserStatus(UserStatus.LOCKED);
        User saved = userRepository.save(user);
        return userMapper.toAdminResponse(saved);
    }

    @Override
    public AdminUserResponse unlockUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        
        user.setUserStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        return userMapper.toAdminResponse(saved);
    }
}
