package com._team._team.attendance.Service;

import com._team._team.attendance.Repository.WorkScheduleRepository;
import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.dto.reqDto.WorkScheduleCreateReqDto;
import com._team._team.attendance.dto.reqDto.WorkScheduleUpdateReqDto;
import com._team._team.attendance.dto.resDto.WorkScheduleResDto;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;

    @Autowired
    public WorkScheduleService(WorkScheduleRepository workScheduleRepository) {
        this.workScheduleRepository = workScheduleRepository;
    }

    /** 스케줄 생성 */
    public WorkScheduleResDto createSchedule(UUID companyId, WorkScheduleCreateReqDto reqDto){
        WorkSchedule workSchedule = reqDto.toEntity(companyId);
        WorkSchedule saveWorkSchedule = workScheduleRepository.save(workSchedule);
        return WorkScheduleResDto.fromEntity(saveWorkSchedule);
    }

    /** 스케줄 목록 조회 (회사별) */
    @Transactional(readOnly = true)
    public List<WorkScheduleResDto> findSchedules(UUID companyId){
        return workScheduleRepository.findByCompanyIdAndDelYn(companyId, "N")
                .stream().map(WorkScheduleResDto::fromEntity).toList();
    }

    /** 스케줄 단건 조회 */
    @Transactional(readOnly = true)
    public WorkScheduleResDto findSchedule(UUID companyId, UUID scheduleId) {
        WorkSchedule workSchedule = workScheduleRepository
                .findByCompanyIdAndWorkScheduleIdAndDelYn(companyId, scheduleId, "N")
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "근무 스케줄을 찾을 수 없습니다."));
        return WorkScheduleResDto.fromEntity(workSchedule);
    }

    /** 스케줄 수정 */
    public WorkScheduleResDto updateSchedule(UUID companyId, UUID scheduleId, WorkScheduleUpdateReqDto reqDto){
        WorkSchedule workSchedule = workScheduleRepository
                .findByCompanyIdAndWorkScheduleIdAndDelYn(companyId, scheduleId, "N")
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "근무 스케줄을 찾을 수 없습니다."));
        workSchedule.update(reqDto);
        return WorkScheduleResDto.fromEntity(workSchedule);
    }

    /** 스케줄 삭제 */
    public void deleteSchedule(UUID companyId, UUID scheduleId){
        WorkSchedule workSchedule = workScheduleRepository
                .findByCompanyIdAndWorkScheduleIdAndDelYn(companyId, scheduleId, "N")
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "근무 스케줄을 찾을 수 없습니다."));
        workSchedule.delete();
    }
}
