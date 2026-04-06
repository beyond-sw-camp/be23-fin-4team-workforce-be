package com._team._team.attendance.Service;

import com._team._team.attendance.Repository.DailyAttendanceRepository;
import com._team._team.attendance.Repository.WorkScheduleRepository;
import com._team._team.attendance.domain.DailyAttendance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class DailyAttendanceService {
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final WorkScheduleRepository workScheduleRepository;

    @Autowired
    public DailyAttendanceService(DailyAttendanceRepository dailyAttendanceRepository, WorkScheduleRepository workScheduleRepository) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.workScheduleRepository = workScheduleRepository;
    }
}
