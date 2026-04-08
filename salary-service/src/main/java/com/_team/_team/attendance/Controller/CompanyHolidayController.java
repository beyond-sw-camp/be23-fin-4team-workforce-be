package com._team._team.attendance.Controller;

import com._team._team.attendance.Service.CompanyHolidayService;
import com._team._team.attendance.dto.reqDto.CompanyHolidayCreateReqDto;
import com._team._team.attendance.dto.reqDto.CompanyHolidayUpdateReqDto;
import com._team._team.attendance.dto.resDto.CompanyHolidayResDto;
import com._team._team.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/company-holidays")
public class CompanyHolidayController {
    private final CompanyHolidayService companyHolidayService;

    @Autowired
    public CompanyHolidayController(CompanyHolidayService companyHolidayService) {
        this.companyHolidayService = companyHolidayService;
    }

    /** 공휴일 등록 */
    @PostMapping("/create")
    public ResponseEntity<?> createHoliday(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody CompanyHolidayCreateReqDto reqDto) {
        CompanyHolidayResDto resDto = companyHolidayService.createHoliday(companyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "공휴일이 등록되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 공휴일 전체 목록 조회 */
    @GetMapping
    public ResponseEntity<?> findHolidays(
            @RequestHeader("X-User-CompanyId") UUID companyId){
        List<CompanyHolidayResDto> resDtoList = companyHolidayService.findHolidays(companyId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoList, "공휴일 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 공휴일 수정 */
    @PutMapping("/{holidayId}")
    public ResponseEntity<?> updateHoliday(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID holidayId,
            @Valid @RequestBody CompanyHolidayUpdateReqDto reqDto){
        CompanyHolidayResDto resDto = companyHolidayService.updateHoliday(companyId, holidayId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "공휴일이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 공휴일 삭제 */
    @DeleteMapping("/{holidayId}")
    public ResponseEntity<?> deleteHoliday(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID holidayId){
        companyHolidayService.deleteHoliday(companyId, holidayId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "공휴일이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
