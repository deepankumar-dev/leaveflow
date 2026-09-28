package com.hackathon.leave.dto;

import com.hackathon.leave.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Create or update a person. {@code password} is the temporary password (required on create, ignored on update). */
public record AdminUserRequest(@NotBlank @Size(max = 100) String name, @NotBlank @Email @Size(max = 200) String email,
                               @NotNull Role role, Long teamId, Long managerId, @NotNull LocalDate joinDate,
                               String password, Boolean active) {}
