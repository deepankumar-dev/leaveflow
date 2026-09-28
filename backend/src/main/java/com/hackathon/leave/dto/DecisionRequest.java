package com.hackathon.leave.dto;

import jakarta.validation.constraints.Size;

/** Body for approve / reject / cancel. A comment is mandatory when rejecting. */
public record DecisionRequest(@Size(max = 1000) String comment) {}
