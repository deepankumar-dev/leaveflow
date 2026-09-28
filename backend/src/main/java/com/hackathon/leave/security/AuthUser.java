package com.hackathon.leave.security;

import com.hackathon.leave.model.Role;

/** Authenticated principal placed in the SecurityContext by the JWT filter. */
public record AuthUser(Long id, String email, Role role, boolean mustChangePassword) {}
