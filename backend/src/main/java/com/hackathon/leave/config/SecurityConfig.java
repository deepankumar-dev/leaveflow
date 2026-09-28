package com.hackathon.leave.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.dto.ErrorResponse;
import com.hackathon.leave.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless JWT security: RBAC via @PreAuthorize, ownership checks live in the services. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter jwtFilter, ObjectMapper mapper, LeaveProperties props)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsSource(props)))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // H2 console runs in a frame
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login", "/api/public/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/h2-console/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> write(mapper, res, HttpStatus.UNAUTHORIZED,
                                new ErrorResponse("UNAUTHENTICATED", "Please log in", List.of())))
                        .accessDeniedHandler((req, res, ex) -> write(mapper, res, HttpStatus.FORBIDDEN,
                                new ErrorResponse("FORBIDDEN", "You are not allowed to do that", List.of()))))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void write(ObjectMapper mapper, HttpServletResponse res, HttpStatus status, ErrorResponse body)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), body);
    }

    /** Cross-origin calls are only allowed from the origins listed in CORS_ORIGINS (none by default: same origin). */
    private CorsConfigurationSource corsSource(LeaveProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        if (props.corsOrigins() != null && !props.corsOrigins().isBlank()) {
            cfg.setAllowedOrigins(Arrays.stream(props.corsOrigins().split(",")).map(String::trim)
                    .filter(o -> !o.isEmpty()).toList());
            cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        }
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", cfg);
        return src;
    }

    @Bean
    PasswordEncoder passwordEncoder(@Value("${leave.bcrypt-cost:12}") int cost) {
        return new BCryptPasswordEncoder(cost);
    }
}
