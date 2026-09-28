package com.hackathon.leave.service;

import com.hackathon.leave.dto.LoginRequest;
import com.hackathon.leave.dto.LoginResponse;
import com.hackathon.leave.dto.UserDto;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.User;
import com.hackathon.leave.repository.UserRepository;
import com.hackathon.leave.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LeaveMapper mapper;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, LeaveMapper mapper) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mapper = mapper;
    }

    public LoginResponse login(LoginRequest req) {
        // Same error for unknown email and wrong password, so accounts cannot be probed.
        User u = users.findByEmail(req.email().trim().toLowerCase()).filter(User::isActive)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Invalid email or password"));
        return new LoginResponse(jwt.issue(u), jwt.expirySeconds(), mapper.toDto(u));
    }

    public UserDto me(Long userId) {
        return mapper.toDto(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
    }
}
