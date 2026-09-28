package com.hackathon.leave.dto;

import com.hackathon.leave.model.Role;
import java.time.LocalDate;

public record UserDto(Long id, String name, String email, Role role, Long teamId, String teamName,
                      Long managerId, String managerName, LocalDate joinDate, boolean active,
                      boolean mustChangePassword) {}
