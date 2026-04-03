package com._team._team.salary.Service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.Repository.TaxRateRepository;
import com._team._team.salary.domain.TaxRate;
import com._team._team.salary.dto.reqdto.TaxRateCreateReqDto;
import com._team._team.salary.dto.reqdto.TaxRateUpdateReqDto;
import com._team._team.salary.dto.resdto.TaxRateResDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 세율 관리 서비스
 * - 연도별 4대보험 및 소득세 세율 CRUD
 * - 동일 연도/유형 중복 등록 방지
 */
@Service
@Transactional
public class TaxRateService {

    private final TaxRateRepository taxRateRepository;

    @Autowired
    public TaxRateService(TaxRateRepository taxRateRepository) {
        this.taxRateRepository = taxRateRepository;
    }

    // 세율 생성
    public TaxRateResDto save(TaxRateCreateReqDto reqDto){
        // 동일 연도, 동일 유형 중복 체크
        if(taxRateRepository.existsByApplyYearAndTaxType(reqDto.getApplyYear(), reqDto.getTaxType())){
            throw new BusinessException(HttpStatus.CONFLICT, "해당 연도 동일한 세금 유형의 세율이 존재합니다.");
        }

        TaxRate taxRate = reqDto.toEntity();
        TaxRate saved = taxRateRepository.save(taxRate);
        return TaxRateResDto.fromEntity(saved);
    }

    // 세율 단건 조회
    @Transactional(readOnly = true)
    public TaxRateResDto findById(UUID taxRateId){
        TaxRate taxRate = taxRateRepository.findById(taxRateId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND,"세율 정보를 찾을 수 없습니다."));
        return TaxRateResDto.fromEntity(taxRate);
    }

    // 연도별 세율 목록 조회
    @Transactional(readOnly = true)
    public List<TaxRateResDto> findByApplyYear(Integer applyYear){
        return taxRateRepository.findByApplyYear(applyYear).stream()
                .map(TaxRateResDto::fromEntity).toList();
    }

    // 세율 수정
    public TaxRateResDto update(UUID taxRateId , TaxRateUpdateReqDto reqDto){
        TaxRate taxRate = taxRateRepository.findById(taxRateId)
                .orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"세율 정보를 찾을 수 없습니다."));
        taxRate.update(reqDto);
        return TaxRateResDto.fromEntity(taxRate);
    }

    // 세율 삭제
    public void delete(UUID taxRateId){
        TaxRate taxRate = taxRateRepository.findById(taxRateId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "세율 정보를 찾을 수 없습니다."));
        taxRateRepository.delete(taxRate);
    }
}
