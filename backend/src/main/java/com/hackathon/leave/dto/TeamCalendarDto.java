package com.hackathon.leave.dto;

import java.util.List;

/** Team calendar for one month (month = yyyy-MM). */
public record TeamCalendarDto(String month, Long teamId, String teamName, int teamSize,
                              List<CalendarEntryDto> entries, List<HolidayDto> holidays) {}
