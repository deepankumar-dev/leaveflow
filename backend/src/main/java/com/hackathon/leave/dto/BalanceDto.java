package com.hackathon.leave.dto;

import java.math.BigDecimal;

/** One leave type's balance. {@code explanation} shows the pro-rata formula, e.g. "24 × 6/12 = 12.0". */
public record BalanceDto(String leaveTypeCode, String leaveTypeName, int year, BigDecimal entitled, BigDecimal used,
                         BigDecimal pending, BigDecimal available, boolean tracked, String explanation) {}
