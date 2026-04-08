package com._team._team.attendance.Service;

import com._team._team.attendance.Repository.CompanyHolidayRepository;
import com._team._team.attendance.domain.CompanyHoliday;
import com._team._team.attendance.dto.reqDto.CompanyHolidayCreateReqDto;
import com._team._team.attendance.dto.reqDto.CompanyHolidayUpdateReqDto;
import com._team._team.attendance.dto.resDto.CompanyHolidayResDto;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CompanyHolidayService {
    private final CompanyHolidayRepository companyHolidayRepository;

    @Autowired
    public CompanyHolidayService(CompanyHolidayRepository companyHolidayRepository) {
        this.companyHolidayRepository = companyHolidayRepository;
    }

    /**
     * 공휴일 등록
     * 같은 회사, 같은 날짜 이미 등록된 공휴일이 있으면 409 에러
     * isPaidYn 미 전달시 기본값 'Y' (유급휴일)
     */
    public CompanyHolidayResDto createHoliday(UUID companyId, CompanyHolidayCreateReqDto reqDto){
        boolean exists = companyHolidayRepository.
                existsByCompanyIdAndHolidayDateAndDelYn(companyId, reqDto.getHolidayDate(), "N");

        if (exists) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "이미 등록된 공휴일입니다: " + reqDto.getHolidayDate());
        }

        CompanyHoliday holiday = reqDto.toEntity(companyId);
        CompanyHoliday savedHoliday = companyHolidayRepository.save(holiday);
        return CompanyHolidayResDto.fromEntity(savedHoliday);
    }

    /** 공휴일 전체 목록 조회 */
    public List<CompanyHolidayResDto> findHolidays(UUID companyId){
        List<CompanyHoliday> holidays = companyHolidayRepository
                .findByCompanyIdAndDelYnOrderByHolidayDate(companyId, "N");
        return holidays.stream()
                .map(CompanyHolidayResDto::fromEntity)
                .toList();
    }

    /**
     * 공휴일 수정
     * - 공휴일명, 날짜, 유급여부 부분 업데이트
     */
    public CompanyHolidayResDto updateHoliday(UUID companyId, UUID companyHolidayId, CompanyHolidayUpdateReqDto reqDto) {
        CompanyHoliday holiday = companyHolidayRepository
                .findByCompanyIdAndCompanyHolidayIdAndDelYn(companyId, companyHolidayId, "N")
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "공휴일을 찾을 수 없습니다."));

        holiday.update(reqDto);
        return CompanyHolidayResDto.fromEntity(holiday);
    }

    /**
     * 공휴일 삭제 (소프트 삭제)
     */
    public void deleteHoliday(UUID companyId, UUID companyHolidayId) {
        CompanyHoliday holiday = companyHolidayRepository
                .findByCompanyIdAndCompanyHolidayIdAndDelYn(companyId, companyHolidayId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "공휴일을 찾을 수 없습니다."));
        holiday.delete();
    }
}
