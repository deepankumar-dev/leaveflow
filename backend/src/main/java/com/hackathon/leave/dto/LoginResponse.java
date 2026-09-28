package com.hackathon.leave.dto;

public record LoginResponse(String token, long expiresInSeconds, UserDto user) {}
