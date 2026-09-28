package com.hackathon.leave.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Team capacity for the next N days. {@code away*} count people; {@code projectedAwayPercent} adds approved and
 * pending leave. {@code historicalAvgAway} is the mean number of people away per working day over the previous
 * 90 days (approved leave only), shown as a reference line.
 */
public record ForecastDto(String scope, int teamSize, double thresholdPercent, double historicalAvgAway,
                          List<Day> days) {
    public record Day(LocalDate date, boolean workingDay, int approvedAway, int pendingAway,
                      double projectedAwayPercent) {}
}
