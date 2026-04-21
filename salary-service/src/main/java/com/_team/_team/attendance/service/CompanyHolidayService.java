package com._team._team.attendance.service;

import com._team._team.attendance.domain.CompanyHoliday;
import com._team._team.attendance.dto.reqDto.CompanyHolidayCreateReqDto;
import com._team._team.attendance.dto.reqDto.CompanyHolidayUpdateReqDto;
import com._team._team.attendance.dto.resDto.CompanyHolidayResDto;
import com._team._team.attendance.feignClients.MemberCalendarClient;
import com._team._team.attendance.repository.CompanyHolidayRepository;
import com._team._team.dto.BusinessException;
import com._team._team.attendance.dto.resDto.LegalHolidayResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class CompanyHolidayService {

    private final CompanyHolidayRepository companyHolidayRepository;
    private final MemberCalendarClient memberCalendarClient;

    @Autowired
    public CompanyHolidayService(CompanyHolidayRepository companyHolidayRepository, MemberCalendarClient memberCalendarClient) {
        this.companyHolidayRepository = companyHolidayRepository;
        this.memberCalendarClient = memberCalendarClient;
    }

    /**
     * 공휴일 등록
     * 같은 회사, 같은 날짜 이미 등록된 공휴일이 있으면 409 에러
     * 기본값 isPaidYn='Y', isLegalYn='N' (커스텀 휴일)
     */
    public CompanyHolidayResDto createHoliday(UUID companyId, CompanyHolidayCreateReqDto reqDto) {
        boolean duplicated = companyHolidayRepository
                .existsByCompanyIdAndHolidayDateAndDelYn(
                        companyId, reqDto.getHolidayDate(), "N");
        if (duplicated) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 등록된 공휴일입니다: " + reqDto.getHolidayDate());
        }

        CompanyHoliday holiday = reqDto.toEntity(companyId);
        CompanyHoliday saved = companyHolidayRepository.save(holiday);
        return CompanyHolidayResDto.fromEntity(saved);
    }

    /** 공휴일 전체 목록 조회, 법정 + 커스텀 모두 포함 */
    @Transactional(readOnly = true)
    public List<CompanyHolidayResDto> findHolidays(UUID companyId) {
        List<CompanyHoliday> holidays = companyHolidayRepository
                .findByCompanyIdAndDelYnOrderByHolidayDate(companyId, "N");
        return holidays.stream()
                .map(CompanyHolidayResDto::fromEntity)
                .toList();
    }

    /**
     * 공휴일 수정, 커스텀만 수정 허용
     * - 공휴일명, 날짜, 유급여부 부분 업데이트
     */
    public CompanyHolidayResDto updateHoliday(UUID companyId, UUID companyHolidayId, CompanyHolidayUpdateReqDto reqDto) {
        CompanyHoliday holiday = findActive(companyId, companyHolidayId);

        if ("Y".equals(holiday.getIsLegalYn())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "법정 공휴일은 수정할 수 없습니다.");
        }

        holiday.update(reqDto);
        return CompanyHolidayResDto.fromEntity(holiday);
    }

    /**
     * 공휴일 삭제, 커스텀만 삭제 허용
     */
    public void deleteHoliday(UUID companyId, UUID companyHolidayId) {
        CompanyHoliday holiday = findActive(companyId, companyHolidayId);

        if ("Y".equals(holiday.getIsLegalYn())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "법정 공휴일은 삭제할 수 없습니다. 필요시 새로고침으로 재수집하세요.");
        }
        holiday.delete();
    }

    // 법정 공휴일 연동

    /**
     * 회사 가입 시 법정 공휴일 일괄 복사, 올해 + 내년
     */
    public void importPublicHolidays(UUID companyId) {
        int currentYear = LocalDate.now().getYear();
        importYear(companyId, currentYear);
        importYear(companyId, currentYear + 1);
    }


    /**
     * 관리자 수동 새로고침, 지정 연도 법정 공휴일 재수집
     * isLegalYn='Y' 만 하드 삭제 후 재삽입, 커스텀 휴일은 건드리지 않음
     */
    public int refreshLegalHolidays(UUID companyId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // 1. 해당 연도 법정 공휴일 하드 삭제
        List<CompanyHoliday> existing = companyHolidayRepository
                .findByCompanyIdAndIsLegalYnAndHolidayDateBetween(
                        companyId, "Y", yearStart, yearEnd);
        companyHolidayRepository.deleteAll(existing);

        // 2. 최신 데이터(공휴일) 조회
        List<LegalHolidayResDto> holidays;
        try {
            holidays = memberCalendarClient.getHolidays(year).getData();
        } catch (Exception e) {
            log.error("법정 공휴일 조회 실패 year={}", year, e);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "공휴일 API 호출에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        if (holidays == null || holidays.isEmpty()) {
            log.warn("공공 API 에 {}년 공휴일 데이터 없음", year);
            return 0;
        }

        // 3. CompanyHoliday 로 재삽입
        List<CompanyHoliday> toSave = holidays.stream()
                .map(h -> CompanyHoliday.builder()
                        .companyId(companyId)
                        .holidayDate(h.getHolidayDate())
                        .holidayName(h.getHolidayName())
                        .isPaidYn("Y")
                        .isLegalYn("Y")
                        .build())
                .toList();

        companyHolidayRepository.saveAll(toSave);
        log.info("법정 공휴일 새로고침 완료 companyId={}, year={}, count={}",
                companyId, year, toSave.size());
        return toSave.size();
    }

    private void importYear(UUID companyId, int year) {
        try {
            List<LegalHolidayResDto> holidays = memberCalendarClient
                    .getHolidays(year).getData();

            if (holidays == null) return;

            List<CompanyHoliday> toSave = holidays.stream()
                    .filter(h -> !exists(companyId, h.getHolidayDate()))
                    .map(h -> CompanyHoliday.builder()
                            .companyId(companyId)
                            .holidayDate(h.getHolidayDate())
                            .holidayName(h.getHolidayName())
                            .isPaidYn("Y")
                            .isLegalYn("Y")              // 법정 태그
                            .build())
                    .toList();

            if (!toSave.isEmpty()) {
                companyHolidayRepository.saveAll(toSave);
            }
        } catch (Exception e) {
            log.warn("법정 공휴일 복사 실패 companyId={}, year={}, error={}",
                    companyId, year, e.getMessage());
        }
    }

    private boolean exists(UUID companyId, LocalDate date) {
        return companyHolidayRepository
                .existsByCompanyIdAndHolidayDateAndDelYn(companyId, date, "N");
    }

    private CompanyHoliday findActive(UUID companyId, UUID companyHolidayId) {
        return companyHolidayRepository
                .findByCompanyIdAndCompanyHolidayIdAndDelYn(
                        companyId, companyHolidayId, "N")
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "공휴일을 찾을 수 없습니다."));
    }
}