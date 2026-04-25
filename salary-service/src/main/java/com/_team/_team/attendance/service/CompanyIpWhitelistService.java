package com._team._team.attendance.service;

import com._team._team.attendance.domain.CompanyIpWhitelist;
import com._team._team.attendance.dto.reqDto.CompanyIpWhitelistCreateReqDto;
import com._team._team.attendance.dto.reqDto.CompanyIpWhitelistUpdateReqDto;
import com._team._team.attendance.repository.CompanyIpWhitelistRepository;
import com._team._team.attendance.util.CidrMatcher;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 회사에 등록된 IP 리스트 관리
 */
@Service
@Transactional
public class CompanyIpWhitelistService {

    private final CompanyIpWhitelistRepository repository;

    @Autowired
    public CompanyIpWhitelistService(CompanyIpWhitelistRepository repository) {
        this.repository = repository;
    }

    public CompanyIpWhitelist create(UUID companyId, CompanyIpWhitelistCreateReqDto reqDto) {
        CompanyIpWhitelist row = CompanyIpWhitelist.builder()
                .companyId(companyId)
                .cidr(reqDto.getCidr().trim())
                .label(reqDto.getLabel() == null ? null : reqDto.getLabel().trim())
                .build();
        return repository.save(row);
    }

    public CompanyIpWhitelist update(UUID companyId, UUID id, CompanyIpWhitelistUpdateReqDto reqDto) {
        CompanyIpWhitelist row = findActiveOrThrow(companyId, id);
        row.update(
                reqDto.getCidr().trim(),
                reqDto.getLabel() == null ? null : reqDto.getLabel().trim()
        );
        return row;
    }

    public void delete(UUID companyId, UUID id) {
        findActiveOrThrow(companyId, id).delete();
    }

    @Transactional(readOnly = true)
    public List<CompanyIpWhitelist> findAllByCompany(UUID companyId) {
        return repository.findByCompanyIdAndDelYnOrderByCreatedAtAsc(companyId, "N");
    }

    /**
     * 주어진 클라이언트 IP 가 회사 허용 목록에 매치되는지 검증
     * 목록이 비어있으면 true 반환 (검증 비활성)
     */
    @Transactional(readOnly = true)
    public boolean isIpAllowed(UUID companyId, String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }

        // loopback 자동 허용, 로컬 개발/Docker 내부 통신 대응
        if (isLoopback(clientIp)) {
            return true;
        }
        List<CompanyIpWhitelist> list = repository
                .findByCompanyIdAndDelYnOrderByCreatedAtAsc(companyId, "N");
        if (list.isEmpty()) {
            return true;
        }
        return list.stream()
                .anyMatch(w -> CidrMatcher.matches(clientIp, w.getCidr()));
    }

    /** IPv4/IPv6 loopback 체크, 운영 환경에서도 안전 (외부 IP 는 절대 이 범위 아님) */
    private boolean isLoopback(String ip) {
        if (ip == null) return false;
        String s = ip.trim().toLowerCase();
        return s.equals("::1")
                || s.equals("0:0:0:0:0:0:0:1")
                || s.equals("127.0.0.1")
                || s.startsWith("127.");
    }

    private CompanyIpWhitelist findActiveOrThrow(UUID companyId, UUID id) {
        return repository.findByCompanyIpWhitelistIdAndCompanyIdAndDelYn(id, companyId, "N")
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "허용 IP 정보를 찾을 수 없습니다."));
    }
}