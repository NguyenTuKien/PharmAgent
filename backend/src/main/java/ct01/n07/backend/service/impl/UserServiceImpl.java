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
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản chưa tồn tại"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mật khẩu không đúng");
        }

        if (user.getUserStatus() == UserStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vui lòng xác minh email trước khi đăng nhập");
        }

        if (user.getUserStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        return user;
    }


    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public User findByEmail(String mail) {
        return userRepository.findByEmail(normalizeEmail(mail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản chưa tồn tại"));
    }

    @Override
    public User createUser(LoginRequest loginRequest) {
        return createUser(loginRequest, UserStatus.ACTIVE);
    }

    @Override
    public User createUser(LoginRequest loginRequest, UserStatus status) {
        String email = normalizeEmail(loginRequest.getEmail());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        loginRequest.setEmail(email);
        User user = userMapper.toModel(loginRequest);
        user.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
        user.setUserStatus(status);

        return userRepository.save(user);
    }

    @Override
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toAdminResponse);
    }

    @Override
    public AdminUserResponse adminCreateUser(AdminUserCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        request.setEmail(email);
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
        String email = normalizeEmail(request.getEmail());
        if (!user.getEmail().equals(email)) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
            }
            user.setEmail(email);
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

    @Override
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public User updateStatus(String id, UserStatus status) {
        User user = findById(id);
        user.setUserStatus(status);
        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
