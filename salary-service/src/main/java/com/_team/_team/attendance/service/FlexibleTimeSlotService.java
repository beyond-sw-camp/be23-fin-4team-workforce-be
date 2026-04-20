package com._team._team.attendance.service;

import com._team._team.attendance.domain.FlexibleTimeSlot;
import com._team._team.attendance.dto.reqDto.FlexibleTimeSlotCreateReqDto;
import com._team._team.attendance.dto.reqDto.FlexibleTimeSlotUpdateReqDto;
import com._team._team.attendance.dto.resDto.FlexibleTimeSlotResDto;
import com._team._team.attendance.repository.FlexibleTimeSlotRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 시차출퇴근제 슬롯 관리 서비스
 * 관리자가 FLEXIBLE 스케줄 내의 슬롯 CRUD
 */
@Service
@Transactional
public class FlexibleTimeSlotService {

    private static final String ACTIVE = "Y";

    private final FlexibleTimeSlotRepository flexibleTimeSlotRepository;

    @Autowired
    public FlexibleTimeSlotService(FlexibleTimeSlotRepository flexibleTimeSlotRepository) {
        this.flexibleTimeSlotRepository = flexibleTimeSlotRepository;
    }

    /**
     * 슬롯 생성
     */
    public FlexibleTimeSlotResDto create(UUID companyId,
                                         FlexibleTimeSlotCreateReqDto reqDto) {
        // 같은 스케줄 내 slotCode 중복 방지
        if (flexibleTimeSlotRepository.existsByWorkScheduleIdAndSlotCode(
                reqDto.getWorkScheduleId(), reqDto.getSlotCode())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "이미 존재하는 슬롯 코드입니다.");
        }

        // 기본 슬롯 지정 요청 시 기존 기본 슬롯 자동 해제
        if (Boolean.TRUE.equals(reqDto.getIsDefault())) {
            unsetExistingDefault(reqDto.getWorkScheduleId());
        }

        FlexibleTimeSlot saved = flexibleTimeSlotRepository.save(reqDto.toEntity(companyId));
        return FlexibleTimeSlotResDto.fromEntity(saved);
    }

    /**
     * 슬롯 수정
     */
    public FlexibleTimeSlotResDto update(UUID slotId,
                                         UUID companyId,
                                         FlexibleTimeSlotUpdateReqDto reqDto) {
        FlexibleTimeSlot slot = findSlot(slotId, companyId);
        slot.update(
                reqDto.getSlotLabel(),
                reqDto.getStartTime(),
                reqDto.getEndTime(),
                reqDto.getWorkMinutes(),
                reqDto.getBreakMinutes()
        );
        return FlexibleTimeSlotResDto.fromEntity(slot);
    }

    /**
     * 슬롯 폐지 (소프트 삭제)
     */
    public void deactivate(UUID slotId, UUID companyId) {
        FlexibleTimeSlot slot = findSlot(slotId, companyId);
        slot.deactivate();
    }

    /**
     * 기본 슬롯 지정
     * 기존 기본 슬롯 자동 해제 후 이 슬롯을 기본으로 설정
     */
    public FlexibleTimeSlotResDto setAsDefault(UUID slotId, UUID companyId) {
        FlexibleTimeSlot target = findSlot(slotId, companyId);
        unsetExistingDefault(target.getWorkScheduleId());
        target.setAsDefault();
        return FlexibleTimeSlotResDto.fromEntity(target);
    }

    /**
     * 슬롯 단건 조회
     */
    @Transactional(readOnly = true)
    public FlexibleTimeSlotResDto findById(UUID slotId, UUID companyId) {
        return FlexibleTimeSlotResDto.fromEntity(findSlot(slotId, companyId));
    }

    /**
     * 특정 스케줄의 활성 슬롯 목록
     */
    @Transactional(readOnly = true)
    public List<FlexibleTimeSlotResDto> findActiveByWorkSchedule(UUID workScheduleId) {
        return flexibleTimeSlotRepository
                .findAllByWorkScheduleIdAndActiveYn(workScheduleId, ACTIVE)
                .stream()
                .map(FlexibleTimeSlotResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 슬롯 찾기
    private FlexibleTimeSlot findSlot(UUID slotId, UUID companyId) {
        return flexibleTimeSlotRepository
                .findBySlotIdAndCompanyId(slotId, companyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "슬롯을 찾을 수 없습니다."));
    }

    // 기존 기본 슬롯 해제
    private void unsetExistingDefault(UUID workScheduleId) {
        flexibleTimeSlotRepository
                .findByWorkScheduleIdAndIsDefaultAndActiveYn(workScheduleId, true, ACTIVE)
                .ifPresent(FlexibleTimeSlot::unsetDefault);
    }
}
