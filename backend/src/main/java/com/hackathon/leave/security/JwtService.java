package com.hackathon.leave.security;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.model.Role;
import com.hackathon.leave.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(LeaveProperties props) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = props.jwt().expiryMinutes() * 60_000L;
    }

    public long expirySeconds() {
        return expiryMillis / 1000;
    }

    public String issue(User u) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("email", u.getEmail())
                .claim("role", u.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMillis))
                .signWith(key)
                .compact();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(Long.valueOf(c.getSubject()), c.get("email", String.class),
                    Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
