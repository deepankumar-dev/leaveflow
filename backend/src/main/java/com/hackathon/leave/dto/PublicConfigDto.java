package com.hackathon.leave.dto;

/** Non-sensitive settings the login page needs before anyone is signed in. */
public record PublicConfigDto(boolean demoMode, String appName) {}
