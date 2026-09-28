package com.hackathon.leave.dto;

import java.util.List;
import java.util.Map;

public record AnalyticsSummaryDto(long totalRequests, Map<String, Long> byStatus, Map<String, Long> byLeaveType,
                                  long flaggedCount, long escalatedCount, double avgDecisionHours,
                                  List<MonthlyPoint> monthlyTrend, List<TeamLoad> teamLoad) {

    /** Approved leave days starting in a given month (yyyy-MM). */
    public record MonthlyPoint(String month, double days, long requests) {}

    public record TeamLoad(String teamName, long pending, long approved, long flagged) {}
}
