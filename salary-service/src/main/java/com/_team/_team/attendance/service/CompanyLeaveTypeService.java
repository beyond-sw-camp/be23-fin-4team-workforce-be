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

import java.util.ArrayList;
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
     * 시스템 기본 휴가 추가
     * - 기존 회사에도 신규 종류는 추가 가능
     */
    public void initializeDefaults(UUID companyId) {

        List<DefaultSpec> specs = List.of(
                // 기본 연차
                new DefaultSpec("ANNUAL",           "연차",               BalanceType.ANNUAL, 1.0, "Y", null, "N", 1),
                new DefaultSpec("HALF_AM",          "반차(오전)",          BalanceType.ANNUAL, 0.5, "Y", null, "N", 2),
                new DefaultSpec("HALF_PM",          "반차(오후)",          BalanceType.ANNUAL, 0.5, "Y", null, "N", 3),

                // 경조사
                new DefaultSpec("BEREAVEMENT",      "경조휴가(일반)",       null,               1.0, "Y", null, "Y", 10),

                // 경조사
                new DefaultSpec("MARRIAGE_SELF",    "결혼(본인)",          null,               1.0, "Y", 5.0,  "Y", 11),
                new DefaultSpec("MARRIAGE_CHILD",   "결혼(자녀)",          null,               1.0, "Y", 1.0,  "Y", 12),
                new DefaultSpec("BIRTH_SPOUSE",     "배우자 출산휴가",      null,               1.0, "Y", 10.0, "Y", 13),
                new DefaultSpec("DEATH_PARENT",     "부모 사망",           null,               1.0, "Y", 5.0,  "Y", 14),
                new DefaultSpec("DEATH_SPOUSE",     "배우자 사망",          null,               1.0, "Y", 5.0,  "Y", 15),
                new DefaultSpec("DEATH_CHILD",      "자녀 사망",           null,               1.0, "Y", 3.0,  "Y", 16),
                new DefaultSpec("DEATH_SIBLING",    "형제자매 사망",        null,               1.0, "Y", 3.0,  "Y", 17),

                // 기타 법정
                new DefaultSpec("PUBLIC",           "공가",                null,               1.0, "Y", null, "N", 20),
                new DefaultSpec("SICK",             "병가",                null,               1.0, "Y", 15.0, "Y", 21),
                new DefaultSpec("RESERVE_TRAINING", "예비군",              null,               1.0, "Y", null, "Y", 22),
                new DefaultSpec("CIVIL_DEFENSE",    "민방위",              null,               1.0, "Y", null, "Y", 23),

                // 여성 보호
                new DefaultSpec("MENSTRUATION",     "생리휴가",            null,               1.0, "N", 12.0, "N", 30)
        );

        List<CompanyLeaveType> toSave = new ArrayList<>();
        for (DefaultSpec spec : specs) {
            if (repository.findByCompanyIdAndCodeAndDelYn(companyId, spec.code(), "N").isPresent()) {
                continue;
            }
            toSave.add(CompanyLeaveType.builder()
                    .companyId(companyId)
                    .code(spec.code())
                    .name(spec.name())
                    .balanceType(spec.balanceType())
                    .daysPerUse(spec.daysPerUse())
                    .isSystemDefault(true)
                    .isPaidYn(spec.isPaidYn())
                    .maxDaysPerYear(spec.maxDaysPerYear())
                    .requireEvidenceYn(spec.requireEvidenceYn())
                    .displayOrder(spec.displayOrder())
                    .build());
        }
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
    }

    private record DefaultSpec(
            String code,
            String name,
            BalanceType balanceType,
            Double daysPerUse,
            String isPaidYn,
            Double maxDaysPerYear,
            String requireEvidenceYn,
            Integer displayOrder
    ) {}

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
        if (Boolean.TRUE.equals(entity.getIsSystemDefault())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "시스템 기본 휴가는 삭제할 수 없습니다.");
        }
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


