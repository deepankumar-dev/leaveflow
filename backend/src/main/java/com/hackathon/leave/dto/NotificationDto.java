package com.hackathon.leave.dto;

import java.time.Instant;

public record NotificationDto(Long id, String message, boolean read, Instant createdAt, Long relatedRequestId) {}
