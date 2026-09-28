package com.hackathon.leave.service;

import com.hackathon.leave.dto.ChangePasswordRequest;
import com.hackathon.leave.dto.LoginRequest;
import com.hackathon.leave.dto.LoginResponse;
import com.hackathon.leave.dto.UserDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.UserRepository;
import com.hackathon.leave.security.JwtService;
import com.hackathon.leave.security.LoginAttemptService;
import com.hackathon.leave.security.PasswordPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LeaveMapper mapper;
    private final LoginAttemptService attempts;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, LeaveMapper mapper,
                       LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mapper = mapper;
        this.attempts = attempts;
    }

    public LoginResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        attempts.assertNotLocked(email);
        // Same error for unknown email, inactive account and wrong password, so accounts cannot be probed.
        User u = users.findByEmail(email).filter(User::isActive)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash())).orElse(null);
        if (u == null) {
            attempts.recordFailure(email);
            log.warn("Failed sign-in for {}", email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        attempts.recordSuccess(email);
        return new LoginResponse(jwt.issue(u), jwt.expirySeconds(), mapper.toDto(u));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return mapper.toDto(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
    }

    /** Returns a fresh token, because the old one carries the "must change password" flag. */
    public LoginResponse changePassword(Long userId, ChangePasswordRequest req) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "The current password is incorrect");
        }
        PasswordPolicy.validate(req.newPassword());
        if (req.newPassword().equals(req.currentPassword())) {
            throw ApiException.badRequest("SAME_PASSWORD", "The new password must be different from the current one");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        u.setMustChangePassword(false);
        log.info("Password changed for {}", u.getEmail());
        return new LoginResponse(jwt.issue(u), jwt.expirySeconds(), mapper.toDto(u));
    }
}
