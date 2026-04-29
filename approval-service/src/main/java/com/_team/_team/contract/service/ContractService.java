package com._team._team.contract.service;

import com._team._team.approval.feignclients.MemberServiceClient;
import com._team._team.contract.domain.Contract;
import com._team._team.contract.domain.ContractBatch;
import com._team._team.contract.domain.ContractParty;
import com._team._team.contract.domain.ContractTemplate;
import com._team._team.contract.domain.enums.ContractStatus;
import com._team._team.contract.domain.enums.PartyRole;
import com._team._team.contract.domain.enums.SignStatus;
import com._team._team.contract.dto.reqdto.ContractBatchSendReqDto;
import com._team._team.contract.dto.reqdto.ContractSendReqDto;
import com._team._team.contract.dto.reqdto.ContractSignReqDto;
import com._team._team.contract.dto.resdto.ContractBatchResDto;
import com._team._team.contract.dto.resdto.ContractResDto;
import com._team._team.contract.feignclients.SalaryServiceClient;
import com._team._team.contract.feignclients.dto.SalaryApiResponse;
import com._team._team.contract.feignclients.dto.SalaryInfoResDto;
import com._team._team.contract.repository.ContractBatchRepository;
import com._team._team.contract.repository.ContractPartyRepository;
import com._team._team.contract.repository.ContractRepository;
import com._team._team.contract.repository.ContractTemplateRepository;
import com._team._team.dto.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractTemplateRepository templateRepository;
    private final ContractBatchRepository batchRepository;
    private final ContractPartyRepository partyRepository;
    private final SalaryServiceClient salaryServiceClient;
    private final MemberServiceClient memberServiceClient;

    @Autowired
    public ContractService(ContractRepository contractRepository, ContractTemplateRepository templateRepository, ContractBatchRepository batchRepository, ContractPartyRepository partyRepository, SalaryServiceClient salaryServiceClient, MemberServiceClient memberServiceClient) {
        this.contractRepository = contractRepository;
        this.templateRepository = templateRepository;
        this.batchRepository = batchRepository;
        this.partyRepository = partyRepository;
        this.salaryServiceClient = salaryServiceClient;
        this.memberServiceClient = memberServiceClient;
    }

    //    개별 발송
    public ContractResDto sendToEmployee(UUID companyId, UUID senderMemberId, ContractSendReqDto reqDto) {
        ContractTemplate template = findActiveTemplate(companyId, reqDto.getTemplateId());

        // salary/member 데이터로 AUTO 필드 채우기
        SalaryInfoResDto salaryInfo = getCurrentSalary(companyId, reqDto.getEmployeeMemberId());

        // contentJson 생성 (AUTO + ADMIN_INPUT 합치기)
        String contentJson = buildContentJson(template.getFormSchema(), salaryInfo, reqDto.getAdminInputJson());

        // Contract 생성
        Contract contract = Contract.builder()
                .companyId(companyId)
                .contractTemplate(template)
                .employeeMemberId(reqDto.getEmployeeMemberId())
                .contractType(template.getContractType())
                .contentJson(contentJson)
                .formSchemaSnapshot(template.getFormSchema())
                .contractStatus(ContractStatus.SENT)
                .employeeName(salaryInfo.getName())
                .employeeSabun(salaryInfo.getSabun())
                .organizationName(salaryInfo.getOrganizationName())
                .jobTitleName(salaryInfo.getJobTitleName())
                .build();

        Contract saved = contractRepository.save(contract);

        // ContractParty 2건 (회사측 자동서명 + 직원측 대기)
        ContractParty companyParty = ContractParty.builder()
                .contract(saved)
                .memberId(senderMemberId)
                .partyRole(PartyRole.COMPANY)
                .signStatus(SignStatus.SIGNED)
                .build();
        companyParty.sign(null); // 회사측은 발송 시 자동 서명

        ContractParty employeeParty = ContractParty.builder()
                .contract(saved)
                .memberId(reqDto.getEmployeeMemberId())
                .partyRole(PartyRole.EMPLOYEE)
                .build();

        List<ContractParty> savedParties = partyRepository.saveAll(List.of(companyParty, employeeParty));

        // TODO: Kafka 알림 발송 (직원에게 "계약서가 도착했습니다")

        return ContractResDto.fromEntity(saved, savedParties);
    }

    //    일괄 발송
    public ContractBatchResDto sendBatch(UUID companyId, UUID senderMemberId, ContractBatchSendReqDto reqDto) {
        ContractTemplate template = findActiveTemplate(companyId, reqDto.getTemplateId());

//        배치 생성
        ContractBatch batch = ContractBatch.builder()
                .companyId(companyId)
                .contractTemplate(template)
                .batchName(reqDto.getBatchName())
                .totalCount(reqDto.getItems().size())
                .createdBy(senderMemberId)
                .build();

        ContractBatch savedBatch = batchRepository.save(batch);

        // 직원별 계약 생성
        for (ContractBatchSendReqDto.BatchItem item : reqDto.getItems()) {
            SalaryInfoResDto salaryInfo = getCurrentSalary(companyId, item.getEmployeeMemberId());

            String contentJson = buildContentJson(template.getFormSchema(), salaryInfo, item.getAdminInputJson());

            Contract contract = Contract.builder()
                    .companyId(companyId)
                    .contractTemplate(template)
                    .contractBatch(savedBatch)
                    .employeeMemberId(item.getEmployeeMemberId())
                    .contractType(template.getContractType())
                    .contentJson(contentJson)
                    .formSchemaSnapshot(template.getFormSchema())
                    .contractStatus(ContractStatus.SENT)
                    .employeeName(salaryInfo.getName())
                    .employeeSabun(salaryInfo.getSabun())
                    .organizationName(salaryInfo.getOrganizationName())
                    .jobTitleName(salaryInfo.getJobTitleName())
                    .build();

            Contract savedContract = contractRepository.save(contract);

            // 회사측 자동서명 + 직원측 대기
            ContractParty companyParty = ContractParty.builder()
                    .contract(savedContract)
                    .memberId(senderMemberId)
                    .partyRole(PartyRole.COMPANY)
                    .signStatus(SignStatus.SIGNED)
                    .build();
            companyParty.sign(null);

            ContractParty employeeParty = ContractParty.builder()
                    .contract(savedContract)
                    .memberId(item.getEmployeeMemberId())
                    .partyRole(PartyRole.EMPLOYEE)
                    .build();

            partyRepository.saveAll(List.of(companyParty, employeeParty));
        }

        // TODO: Kafka 알림 일괄 발송

        return ContractBatchResDto.fromEntity(savedBatch);
    }

//    직원 서명
    public ContractResDto sign(UUID companyId, UUID memberId, UUID contractId, ContractSignReqDto reqDto){
        Contract contract = findContractWithPermission(companyId, contractId);

        if (contract.getContractStatus() != ContractStatus.SENT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "서명할 수 있는 상태가 아닙니다.");
        }

        // 본인 파티 찾기
        ContractParty party = partyRepository.findByContractContractIdAndMemberId(contractId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "서명 권한이 없습니다."));

        if (party.getSignStatus() == SignStatus.SIGNED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 서명했습니다.");
        }

        // 서명 처리
        party.sign(reqDto.getSignatureImageUrl());

        // 전원 서명 완료 체크
        boolean allSigned = !partyRepository.existsByContractContractIdAndSignStatusNot(contractId, SignStatus.SIGNED);

        if (allSigned) {
            contract.sign();

            // 배치에 속한 계약이면 서명 카운트 증가
            if (contract.getContractBatch() != null) {
                contract.getContractBatch().incrementSignedCount();
            }

            // TODO: PDF 생성 + S3 저장
            // TODO: Kafka 알림 (인사팀에게 "OOO님이 서명했습니다")
        }

        List<ContractParty> parties = partyRepository.findByContractContractId(contractId);
        return ContractResDto.fromEntity(contract, parties);
    }

    private ContractTemplate findActiveTemplate(UUID companyId, UUID templateId) {
        ContractTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계약서 템플릿을 찾을 수 없습니다."));

        if (!template.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        if ("N".equals(template.getIsActiveYn())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비활성화된 템플릿입니다.");
        }

        return template;
    }

    private Contract findContractWithPermission(UUID companyId, UUID contractId) {
        Contract contract = contractRepository.findByIdWithTemplate(contractId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "계약을 찾을 수 없습니다."));

        if (!contract.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        return contract;
    }

    private SalaryInfoResDto getCurrentSalary(UUID companyId, UUID memberId) {
        SalaryApiResponse response;
        try {
            response = salaryServiceClient.getSalaryByMemberId(companyId, memberId);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "급여 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "해당 직원의 급여 정보를 찾을 수 없습니다.");
        }

        return response.getData().stream()
                .filter(s -> s.getEffectiveTo() == null)
                .findFirst()
                .orElse(response.getData().get(0));
    }

    private String buildContentJson(String formSchema, SalaryInfoResDto salaryInfo, String adminInputJson) {
        // formSchema에서 AUTO 필드를 salaryInfo로 채우고,
        // ADMIN_INPUT 필드를 adminInputJson으로 채워서 최종 contentJson 생성

        // 간단한 구현: adminInputJson을 기반으로 AUTO 필드를 추가
        // 실제로는 formSchema를 파싱해서 source별로 분기 처리
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"employeeName\":\"").append(salaryInfo.getName()).append("\",");
        sb.append("\"sabun\":\"").append(salaryInfo.getSabun()).append("\",");
        sb.append("\"organizationName\":\"").append(salaryInfo.getOrganizationName()).append("\",");
        sb.append("\"jobTitleName\":\"").append(salaryInfo.getJobTitleName()).append("\",");
        sb.append("\"baseSalary\":").append(salaryInfo.getBaseSalary());

        // adminInputJson 머지
        if (adminInputJson != null && !adminInputJson.isBlank()) {
            String trimmed = adminInputJson.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                String inner = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!inner.isEmpty()) {
                    sb.append(",").append(inner);
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
