package com._team._team.attendance.service;

import com._team._team.attendance.domain.CompanyLeaveType;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.dto.reqDto.CompanyLeaveTypeCreateReqDto;
import com._team._team.attendance.dto.reqDto.CompanyLeaveTypeUpdateReqDto;
import com._team._team.attendance.repository.CompanyLeaveTypeRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CompanyLeaveTypeService {

    private final CompanyLeaveTypeRepository repository;

    @Autowired
    public CompanyLeaveTypeService(CompanyLeaveTypeRepository repository) {
        this.repository = repository;
    }

    /**
     * 회사 생성 시 시스템 기본 8종 자동 시드, 이미 있으면 건너뜀 (멱등성)
     */
    public void initializeDefaults(UUID companyId) {
        if (repository.existsByCompanyIdAndIsSystemDefault(companyId, true)) {
            return;
        }

        List<CompanyLeaveType> defaults = List.of(
                buildDefault(companyId, "ANNUAL", "연차", BalanceType.ANNUAL,
                        1.0, "Y", null, "N", 1),
                buildDefault(companyId, "HALF_AM", "반차(오전)", BalanceType.ANNUAL,
                        0.5, "Y", null, "N", 2),
                buildDefault(companyId, "HALF_PM", "반차(오후)", BalanceType.ANNUAL,
                        0.5, "Y", null, "N", 3),
                buildDefault(companyId, "BEREAVEMENT", "경조휴가", null,
                        1.0, "Y", null, "Y", 4),
                buildDefault(companyId, "PUBLIC", "공가", null,
                        1.0, "Y", null, "N", 5),
                buildDefault(companyId, "SICK", "병가", BalanceType.ANNUAL,
                        1.0, "Y", null, "Y", 6),
                buildDefault(companyId, "RESERVE_TRAINING", "예비군", null,
                        1.0, "Y", null, "Y", 7),
                buildDefault(companyId, "CIVIL_DEFENSE", "민방위", null,
                        1.0, "Y", null, "Y", 8)
        );
        repository.saveAll(defaults);
    }

    /**
     * 관리자 커스텀 휴가 생성
     */
    public CompanyLeaveType create(UUID companyId, CompanyLeaveTypeCreateReqDto reqDto) {
        repository.findByCompanyIdAndCodeAndDelYn(companyId, reqDto.getCode(), "N")
                .ifPresent(existing -> {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "이미 존재하는 휴가 코드입니다: " + reqDto.getCode());
                });

        CompanyLeaveType entity = CompanyLeaveType.builder()
                .companyId(companyId)
                .code(reqDto.getCode())
                .name(reqDto.getName())
                .balanceType(reqDto.getBalanceType())
                .daysPerUse(reqDto.getDaysPerUse())
                .isSystemDefault(false)
                .isPaidYn(reqDto.getIsPaidYn())
                .maxDaysPerYear(reqDto.getMaxDaysPerYear())
                .requireEvidenceYn(reqDto.getRequireEvidenceYn())
                .displayOrder(reqDto.getDisplayOrder())
                .build();

        return repository.save(entity);
    }

    /**
     * 수정, 시스템 기본은 name, displayOrder 만 허용
     */
    public CompanyLeaveType update(UUID companyId, UUID companyLeaveTypeId,
                                   CompanyLeaveTypeUpdateReqDto reqDto) {
        CompanyLeaveType entity = findActive(companyId, companyLeaveTypeId);

        if (Boolean.TRUE.equals(entity.getIsSystemDefault())) {
            entity.update(reqDto.getName(), reqDto.getDisplayOrder());
        } else {
            entity.updateCustom(
                    reqDto.getName(),
                    reqDto.getBalanceType(),
                    reqDto.getDaysPerUse(),
                    reqDto.getIsPaidYn(),
                    reqDto.getMaxDaysPerYear(),
                    reqDto.getRequireEvidenceYn(),
                    reqDto.getDisplayOrder());
        }
        return entity;
    }

    /**
     * 삭제, 시스템 기본은 불가
     */
    public void delete(UUID companyId, UUID companyLeaveTypeId) {
        CompanyLeaveType entity = findActive(companyId, companyLeaveTypeId);
        entity.softDelete();
    }

    @Transactional(readOnly = true)
    public List<CompanyLeaveType> findAll(UUID companyId) {
        return repository.findAllByCompanyIdAndDelYnOrderByDisplayOrder(companyId, "N");
    }

    @Transactional(readOnly = true)
    public CompanyLeaveType findById(UUID companyId, UUID companyLeaveTypeId) {
        return findActive(companyId, companyLeaveTypeId);
    }

    // 휴가 신청 시 유효성 검증용
    @Transactional(readOnly = true)
    public CompanyLeaveType findActiveOrThrow(UUID companyId, UUID companyLeaveTypeId) {
        return findActive(companyId, companyLeaveTypeId);
    }

    private CompanyLeaveType findActive(UUID companyId, UUID companyLeaveTypeId) {
        return repository.findByCompanyLeaveTypeIdAndCompanyIdAndDelYn(
                        companyLeaveTypeId, companyId, "N")
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "휴가 종류를 찾을 수 없습니다."));
    }

    private CompanyLeaveType buildDefault(UUID companyId, String code, String name,
                                          BalanceType balanceType, Double daysPerUse,
                                          String isPaidYn, Double maxDaysPerYear,
                                          String requireEvidenceYn, Integer displayOrder) {
        return CompanyLeaveType.builder()
                .companyId(companyId)
                .code(code)
                .name(name)
                .balanceType(balanceType)
                .daysPerUse(daysPerUse)
                .isSystemDefault(true)
                .isPaidYn(isPaidYn)
                .maxDaysPerYear(maxDaysPerYear)
                .requireEvidenceYn(requireEvidenceYn)
                .displayOrder(displayOrder)
                .build();
    }
}


