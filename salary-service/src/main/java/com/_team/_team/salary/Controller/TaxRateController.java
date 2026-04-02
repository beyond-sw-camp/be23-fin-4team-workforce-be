package com._team._team.salary.Controller;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.Service.TaxRateService;
import com._team._team.salary.dto.reqdto.TaxRateCreateReqDto;
import com._team._team.salary.dto.reqdto.TaxRateUpdateReqDto;
import com._team._team.salary.dto.resdto.TaxRateResDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/taxRate")
public class TaxRateController {

    private final TaxRateService taxRateService;

    @Autowired
    public TaxRateController(TaxRateService taxRateService) {
        this.taxRateService = taxRateService;
    }

    /** 세율 생성 */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TaxRateResDto>> create(
            @Valid @RequestBody TaxRateCreateReqDto reqDto){
        TaxRateResDto resDto = taxRateService.save(reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "세율이 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 세율 단건 조회 */
    @GetMapping("/{taxRateId}")
    public ResponseEntity<ApiResponse<TaxRateResDto>> findById(
            @PathVariable UUID taxRateId){
        TaxRateResDto resDto = taxRateService.findById(taxRateId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "세율 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 연도별 세율 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaxRateResDto>>> findByApplyYear(
            @RequestParam Integer applyYear){
        List<TaxRateResDto> resDtos = taxRateService.findByApplyYear(applyYear);
        return new ResponseEntity<>(
                ApiResponse.success(resDtos, "연도별 세율 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 세율 수정 */
    @PutMapping("/{taxRateId}")
    public ResponseEntity<ApiResponse<TaxRateResDto>> update(
            @PathVariable UUID taxRateId,
            @Valid @RequestBody TaxRateUpdateReqDto reqDto){
        TaxRateResDto resDto = taxRateService.update(taxRateId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "세율이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 세율 삭제 */
    @DeleteMapping("/{taxRateId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID taxRateId){
        taxRateService.delete(taxRateId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "세율이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
