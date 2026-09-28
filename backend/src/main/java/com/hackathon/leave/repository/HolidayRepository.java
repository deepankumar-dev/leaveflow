package com.hackathon.leave.repository;

import com.hackathon.leave.model.Holiday;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
