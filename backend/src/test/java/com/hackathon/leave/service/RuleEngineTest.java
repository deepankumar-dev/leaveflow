package com.hackathon.leave.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pure-function tests for the frozen pro-rata and working-day rules (docs/RULES.md sections 2 and 3). */
class RuleEngineTest {

    private static BigDecimal prorata(int quota, String joined, int year) {
        return RuleEngine.proRata(BigDecimal.valueOf(quota), LocalDate.parse(joined), year);
    }

    @ParameterizedTest(name = "quota {0}, joined {1} -> {2}")
    @CsvSource({
            "24, 2026-07-20, 12.0",   // the documented example: July..December = 6 months
            "24, 2026-07-01, 12.0",   // first of the month counts the same as the last
            "24, 2026-07-31, 12.0",   // the join month counts fully whatever the day
            "24, 2026-01-01, 24.0",   // joined on 1 Jan: full year
            "24, 2026-01-31, 24.0",
            "24, 2026-12-01, 2.0",    // December only: 1 month
            "24, 2026-12-31, 2.0",
            "24, 2026-11-15, 4.0",
            "24, 2025-12-31, 24.0",   // joined before the year: full quota
            "24, 2020-01-06, 24.0",
            "24, 2027-01-01, 0.0",    // joined after the year: nothing
            "10, 2026-08-10, 4.0",    // 10 * 5/12 = 4.17 -> nearest 0.5 is 4.0
            "10, 2026-06-10, 6.0",    // 10 * 7/12 = 5.83 -> nearest 0.5 is 6.0
    })
    void proRata(int quota, String joined, String expected) {
        assertThat(prorata(quota, joined, 2026)).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("rounds to the nearest 0.5, ties go up (3 * 1/12 = 0.25 -> 0.5)")
    void roundsToNearestHalf() {
        assertThat(prorata(7, "2026-12-05", 2026)).isEqualByComparingTo("0.5");
        assertThat(prorata(3, "2026-12-05", 2026)).isEqualByComparingTo("0.5");
        assertThat(prorata(5, "2026-12-05", 2026)).isEqualByComparingTo("0.5");
        assertThat(prorata(25, "2026-07-05", 2026)).isEqualByComparingTo("12.5");
    }

    @Test
    @DisplayName("months remaining counts the join month fully")
    void monthsRemaining() {
        assertThat(RuleEngine.monthsRemaining(LocalDate.of(2026, 7, 20), 2026)).isEqualTo(6);
        assertThat(RuleEngine.monthsRemaining(LocalDate.of(2026, 1, 1), 2026)).isEqualTo(12);
        assertThat(RuleEngine.monthsRemaining(LocalDate.of(2026, 12, 31), 2026)).isEqualTo(1);
        assertThat(RuleEngine.monthsRemaining(LocalDate.of(2025, 12, 31), 2026)).isEqualTo(12);
        assertThat(RuleEngine.monthsRemaining(LocalDate.of(2027, 1, 1), 2026)).isZero();
    }

    // ------------------------------------------------------------------ working days

    @Test
    @DisplayName("a Monday-to-Friday week has 5 working days")
    void fullWeek() {
        assertThat(RuleEngine.workingDays(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 20), Set.of())).hasSize(5);
    }

    @Test
    @DisplayName("weekends are skipped, both ends inclusive")
    void skipsWeekends() {
        // Fri 13 Nov .. Mon 16 Nov = Fri + Mon
        List<LocalDate> days = RuleEngine.workingDays(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 16), Set.of());
        assertThat(days).containsExactly(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 16));
    }

    @Test
    @DisplayName("a weekend-only range has no working days")
    void weekendOnly() {
        assertThat(RuleEngine.workingDays(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15), Set.of())).isEmpty();
    }

    @Test
    @DisplayName("holidays are excluded (R08: 19-21 Oct with Dussehra on the 20th = 2 days)")
    void skipsHolidays() {
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 10, 20));
        assertThat(RuleEngine.workingDays(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21), holidays)).hasSize(2);
    }

    @Test
    @DisplayName("a range that is only a holiday has no working days")
    void holidayOnly() {
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 11, 9));
        assertThat(RuleEngine.workingDays(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9), holidays)).isEmpty();
    }

    @Test
    @DisplayName("a single ordinary day counts as one")
    void singleDay() {
        assertThat(RuleEngine.workingDays(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), Set.of())).hasSize(1);
    }
}
