package com.hackathon.leave.service;

import com.hackathon.leave.dto.HolidayDto;
import com.hackathon.leave.dto.HolidayRequest;
import com.hackathon.leave.exception.ApiException;
import com.hackathon.leave.model.Holiday;
import com.hackathon.leave.repository.HolidayRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HolidayService {

    private final HolidayRepository holidays;

    public HolidayService(HolidayRepository holidays) {
        this.holidays = holidays;
    }

    @Transactional(readOnly = true)
    public List<HolidayDto> list() {
        return holidays.findAll().stream().sorted(Comparator.comparing(Holiday::getDate)).map(HolidayService::toDto)
                .toList();
    }

    public HolidayDto create(HolidayRequest req) {
        if (!holidays.findByDateBetweenOrderByDateAsc(req.date(), req.date()).isEmpty()) {
            throw ApiException.conflict("HOLIDAY_EXISTS", "There is already a holiday on " + req.date());
        }
        Holiday h = new Holiday();
        h.setDate(req.date());
        h.setName(req.name().trim());
        return toDto(holidays.save(h));
    }

    static HolidayDto toDto(Holiday h) {
        return new HolidayDto(h.getId(), h.getDate(), h.getName());
    }
}
