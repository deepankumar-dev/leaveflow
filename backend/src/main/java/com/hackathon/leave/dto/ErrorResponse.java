package com.hackathon.leave.dto;

import java.util.List;

/** Uniform error body: {code, message, details}. */
public record ErrorResponse(String code, String message, List<String> details) {}
