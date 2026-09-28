package com.hackathon.leave.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** A user who must change their password may only call these until they have. */
    private static final Set<String> ALLOWED_BEFORE_PASSWORD_CHANGE = Set.of("/api/auth/change-password", "/api/auth/me");

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Optional<AuthUser> user = jwt.parse(header.substring(7));
            if (user.isPresent()) {
                AuthUser u = user.get();
                if (u.mustChangePassword() && !ALLOWED_BEFORE_PASSWORD_CHANGE.contains(req.getRequestURI())) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                            + "\"message\":\"You must change your password before continuing\",\"details\":[]}");
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name()))));
            }
        }
        chain.doFilter(req, res);
    }
}
