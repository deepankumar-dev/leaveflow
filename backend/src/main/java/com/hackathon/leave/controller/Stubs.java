package com.hackathon.leave.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Contract phase: every endpoint answers 501 until its service is implemented. */
final class Stubs {
    private Stubs() {}

    static <T> ResponseEntity<T> notImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
