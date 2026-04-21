package com._team._team.approval.service;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.enums.RequestType;
import com._team._team.approval.dto.reqdto.ApprovalDocumentCreateReqDto;
import com._team._team.approval.dto.reqdto.ApprovalDocumentUpdateReqDto;
import com._team._team.approval.dto.resdto.ApprovalDocumentResDto;
import com._team._team.approval.repository.ApprovalDocumentRepository;
import com._team._team.dto.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApprovalDocumentService(ApprovalDocumentRepository approvalDocumentRepository, ObjectMapper objectMapper) {
        this.approvalDocumentRepository = approvalDocumentRepository;
        this.objectMapper = objectMapper;
    }

    public void initDefaultDocuments(UUID companyId) {
        // 중복 방지 - 이미 해당 회사 양식이 있으면 건너뛰기
        if (!approvalDocumentRepository.findByCompanyIdAndIsActiveYn(companyId, "Y").isEmpty()) {
            return;
        }

        // 2. 지출결의서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("지출결의서")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":[{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},{\"name\":\"amount\",\"label\":\"금액\",\"type\":\"number\"},{\"name\":\"purpose\",\"label\":\"지출 목적\",\"type\":\"textarea\"},{\"name\":\"paymentDate\",\"label\":\"지출일\",\"type\":\"date\"}]}")
                .build());

        // 3. 업무보고서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("업무보고서")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":[{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},{\"name\":\"content\",\"label\":\"보고 내용\",\"type\":\"textarea\"},{\"name\":\"reportDate\",\"label\":\"보고일\",\"type\":\"date\"}]}")
                .build());

        // ========== 지원 카테고리 ==========

// 증명신청서(회사)
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("증명신청서(회사)")
                .requestType(RequestType.CERTIFICATE)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"corporateRegistration\",\"label\":\"법인 등기부 등본\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"sealCertificate\",\"label\":\"법인 인감 증명서\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"businessLicense\",\"label\":\"사업자등록증 사본\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"delegationLetter\",\"label\":\"위임장\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"companyStamp\",\"label\":\"사용인감계\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"nationalTaxPayment\",\"label\":\"국세완납증명서\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"localTaxPayment\",\"label\":\"시세완납증명서\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"ownerSealCert\",\"label\":\"본인인감날인\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"companySealCert\",\"label\":\"사용인감날인\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"swBizReport\",\"label\":\"S/W사업자신고확인서\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"bizRegistration\",\"label\":\"정보통신공사사업등록증\",\"type\":\"number\",\"placeholder\":\"부\"},"
                        + "{\"name\":\"etcDocument\",\"label\":\"기타\",\"type\":\"text\"},"
                        + "{\"name\":\"submitTo\",\"label\":\"제출처\",\"type\":\"text\"},"
                        + "{\"name\":\"purpose\",\"label\":\"용도\",\"type\":\"text\"},"
                        + "{\"name\":\"receivingLocation\",\"label\":\"수령장소\",\"type\":\"select\",\"options\":[\"본사\",\"서울 사무소\"]}"
                        + "]}")
                .build());

// 도서구입신청서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("도서구입신청서")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"budget\",\"label\":\"예산\",\"type\":\"select\",\"options\":[\"내\",\"외\",\"초과\",\"무관\"]},"
                        + "{\"name\":\"bookName\",\"label\":\"도서명\",\"type\":\"text\"},"
                        + "{\"name\":\"author\",\"label\":\"저자/출판사\",\"type\":\"text\"},"
                        + "{\"name\":\"purchaser\",\"label\":\"구입자\",\"type\":\"text\"},"
                        + "{\"name\":\"purchasePurpose\",\"label\":\"구입목적\",\"type\":\"textarea\"},"
                        + "{\"name\":\"amount\",\"label\":\"금액\",\"type\":\"number\"},"
                        + "{\"name\":\"paymentRequestDate\",\"label\":\"지급요청일\",\"type\":\"date\"},"
                        + "{\"name\":\"paymentMethod\",\"label\":\"지급방법\",\"type\":\"text\"},"
                        + "{\"name\":\"paymentBank\",\"label\":\"지급은행\",\"type\":\"text\"},"
                        + "{\"name\":\"accountHolder\",\"label\":\"예금주\",\"type\":\"text\"},"
                        + "{\"name\":\"accountNumber\",\"label\":\"계좌번호\",\"type\":\"text\"}"
                        + "]}")
                .build());

// 운송서비스 이용신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("운송서비스 이용신청")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"transportType\",\"label\":\"운송수단\",\"type\":\"select\",\"options\":[\"오토바이\",\"다마스\",\"홈달\",\"그외\"]},"
                        + "{\"name\":\"departureDateTime\",\"label\":\"출발일시\",\"type\":\"datetime-local\"},"
                        + "{\"name\":\"departureAddress\",\"label\":\"출발지 주소\",\"type\":\"text\"},"
                        + "{\"name\":\"departureContact\",\"label\":\"출발지 담당자\",\"type\":\"text\"},"
                        + "{\"name\":\"departurePhone\",\"label\":\"출발지 연락처\",\"type\":\"text\"},"
                        + "{\"name\":\"arrivalDateTime\",\"label\":\"도착일시\",\"type\":\"datetime-local\"},"
                        + "{\"name\":\"arrivalAddress\",\"label\":\"도착지 주소\",\"type\":\"text\"},"
                        + "{\"name\":\"arrivalContact\",\"label\":\"도착지 담당자\",\"type\":\"text\"},"
                        + "{\"name\":\"arrivalPhone\",\"label\":\"도착지 연락처\",\"type\":\"text\"},"
                        + "{\"name\":\"cargoType\",\"label\":\"운송물건\",\"type\":\"select\",\"options\":[\"서류\",\"제품\",\"기타\"]},"
                        + "{\"name\":\"cargoDetail\",\"label\":\"상세내역\",\"type\":\"textarea\"},"
                        + "{\"name\":\"activeCenter\",\"label\":\"활동센터\",\"type\":\"text\"},"
                        + "{\"name\":\"requestReason\",\"label\":\"신청사유\",\"type\":\"textarea\"},"
                        + "{\"name\":\"note\",\"label\":\"비고\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// 증명신청서(개인)
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("증명신청서(개인)")
                .requestType(RequestType.CERTIFICATE)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"employmentCertKo\",\"label\":\"재직증명(국문) 수량\",\"type\":\"number\"},"
                        + "{\"name\":\"employmentCertEn\",\"label\":\"재직증명(영문) 수량\",\"type\":\"number\"},"
                        + "{\"name\":\"careerCertKo\",\"label\":\"경력증명(국문) 수량\",\"type\":\"number\"},"
                        + "{\"name\":\"careerCertEn\",\"label\":\"경력증명(영문) 수량\",\"type\":\"number\"},"
                        + "{\"name\":\"incomeTaxCert\",\"label\":\"원천징수영수증 수량\",\"type\":\"number\"},"
                        + "{\"name\":\"laborIncomeCert\",\"label\":\"갑종근로소득납세필증명서 수량\",\"type\":\"number\"},"
                        + "{\"name\":\"issueDate\",\"label\":\"발행일자\",\"type\":\"date\"},"
                        + "{\"name\":\"purpose\",\"label\":\"용도\",\"type\":\"select\",\"options\":[\"대출용\",\"기타\"]},"
                        + "{\"name\":\"submitTo\",\"label\":\"제출처\",\"type\":\"text\"},"
                        + "{\"name\":\"englishAddress\",\"label\":\"영문주소\",\"type\":\"text\"},"
                        + "{\"name\":\"note\",\"label\":\"비고\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// 차량배차신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("차량배차신청")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"requestDepartment\",\"label\":\"신청 부서\",\"type\":\"text\"},"
                        + "{\"name\":\"usageVehicle\",\"label\":\"사용 차량\",\"type\":\"text\"},"
                        + "{\"name\":\"requestReason\",\"label\":\"신청 사유\",\"type\":\"textarea\"},"
                        + "{\"name\":\"usageDateTime\",\"label\":\"사용 일시\",\"type\":\"datetime-local\"},"
                        + "{\"name\":\"destination\",\"label\":\"행선지\",\"type\":\"textarea\"},"
                        + "{\"name\":\"passengerCount\",\"label\":\"탑승인원\",\"type\":\"number\"},"
                        + "{\"name\":\"driverName\",\"label\":\"운전자 성명\",\"type\":\"text\"},"
                        + "{\"name\":\"driverLicense\",\"label\":\"운전면허등급\",\"type\":\"text\"}"
                        + "]}")
                .build());

// 사무용품신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("사무용품신청")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"itemName1\",\"label\":\"상품명 1\",\"type\":\"text\"},"
                        + "{\"name\":\"quantity1\",\"label\":\"수량 1\",\"type\":\"number\"},"
                        + "{\"name\":\"unitPrice1\",\"label\":\"단가 1\",\"type\":\"number\"},"
                        + "{\"name\":\"user1\",\"label\":\"사용자 1\",\"type\":\"text\"},"
                        + "{\"name\":\"itemName2\",\"label\":\"상품명 2\",\"type\":\"text\"},"
                        + "{\"name\":\"quantity2\",\"label\":\"수량 2\",\"type\":\"number\"},"
                        + "{\"name\":\"unitPrice2\",\"label\":\"단가 2\",\"type\":\"number\"},"
                        + "{\"name\":\"user2\",\"label\":\"사용자 2\",\"type\":\"text\"},"
                        + "{\"name\":\"itemName3\",\"label\":\"상품명 3\",\"type\":\"text\"},"
                        + "{\"name\":\"quantity3\",\"label\":\"수량 3\",\"type\":\"number\"},"
                        + "{\"name\":\"unitPrice3\",\"label\":\"단가 3\",\"type\":\"number\"},"
                        + "{\"name\":\"user3\",\"label\":\"사용자 3\",\"type\":\"text\"},"
                        + "{\"name\":\"note\",\"label\":\"비고\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// ========== 근태 카테고리 ==========

// 연장근무신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("연장근무신청")
                .requestType(RequestType.ATTENDANCE)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"workType\",\"label\":\"근무구분\",\"type\":\"select\",\"options\":[\"연장\",\"야간\",\"휴일\"]},"
                        + "{\"name\":\"workDate\",\"label\":\"근무일자\",\"type\":\"date\"},"
                        + "{\"name\":\"startTime\",\"label\":\"시작시간\",\"type\":\"time\"},"
                        + "{\"name\":\"endTime\",\"label\":\"종료시간\",\"type\":\"time\"},"
                        + "{\"name\":\"requestReason\",\"label\":\"신청 사유\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// 휴가신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("연차신청서")
                .requestType(RequestType.VACATION)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"vacationType\",\"label\":\"휴가 종류\",\"type\":\"select\","
                        + "\"options\":[\"연차\",\"반차(오전)\",\"반차(오후)\",\"경조휴가\",\"공가\",\"병가\",\"예비군\",\"민방위\"]},"
                        + "{\"name\":\"startDate\",\"label\":\"시작일\",\"type\":\"date\"},"
                        + "{\"name\":\"endDate\",\"label\":\"종료일\",\"type\":\"date\"},"
                        + "{\"name\":\"halfDayStart\",\"label\":\"반차여부 - 시작일\",\"type\":\"select\","
                        + "\"options\":[\"해당없음\",\"오전前\",\"오전後\",\"오후前\",\"오후後\"]},"
                        + "{\"name\":\"halfDayEnd\",\"label\":\"반차여부 - 종료일\",\"type\":\"select\","
                        + "\"options\":[\"해당없음\",\"오전前\",\"오전後\",\"오후前\",\"오후後\"]},"
                        + "{\"name\":\"usageDays\",\"label\":\"사용일수\",\"type\":\"number\"},"
                        + "{\"name\":\"reason\",\"label\":\"휴가 사유\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// ========== 일반 카테고리 ==========

// 업무기안
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("업무기안")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"effectiveDate\",\"label\":\"시행일자\",\"type\":\"date\"},"
                        + "{\"name\":\"relatedDepartment\",\"label\":\"협조부서\",\"type\":\"text\"},"
                        + "{\"name\":\"agreement\",\"label\":\"합의\",\"type\":\"text\"},"
                        + "{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},"
                        + "{\"name\":\"content\",\"label\":\"내용\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// 업무협조
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("업무협조")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"receivingDepartment\",\"label\":\"수신부서\",\"type\":\"text\"},"
                        + "{\"name\":\"agreement\",\"label\":\"합의\",\"type\":\"text\"},"
                        + "{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},"
                        + "{\"name\":\"content\",\"label\":\"내용\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// ========== 인사 카테고리 ==========

// 휴일근무신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("휴일근무신청")
                .requestType(RequestType.ATTENDANCE)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"agreement\",\"label\":\"합의\",\"type\":\"text\"},"
                        + "{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},"
                        + "{\"name\":\"workDate\",\"label\":\"근무일자\",\"type\":\"date\"},"
                        + "{\"name\":\"startTime\",\"label\":\"시작시간\",\"type\":\"time\"},"
                        + "{\"name\":\"endTime\",\"label\":\"종료시간\",\"type\":\"time\"},"
                        + "{\"name\":\"reason\",\"label\":\"사유\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// 결근사유서
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("결근사유서")
                .requestType(RequestType.ATTENDANCE)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"department\",\"label\":\"부서\",\"type\":\"text\"},"
                        + "{\"name\":\"position\",\"label\":\"직급\",\"type\":\"text\"},"
                        + "{\"name\":\"name\",\"label\":\"성명\",\"type\":\"text\"},"
                        + "{\"name\":\"contact\",\"label\":\"연락처\",\"type\":\"text\"},"
                        + "{\"name\":\"absentDate\",\"label\":\"결근일\",\"type\":\"date\"},"
                        + "{\"name\":\"reason\",\"label\":\"사유\",\"type\":\"textarea\"}"
                        + "]}")
                .build());

// ========== 출장 카테고리 ==========

// 국내출장신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("국내출장신청")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"tripStartDate\",\"label\":\"출장 시작일\",\"type\":\"date\"},"
                        + "{\"name\":\"tripEndDate\",\"label\":\"출장 종료일\",\"type\":\"date\"},"
                        + "{\"name\":\"destination\",\"label\":\"출장지\",\"type\":\"text\"},"
                        + "{\"name\":\"transportation\",\"label\":\"교통편\",\"type\":\"text\"},"
                        + "{\"name\":\"purpose\",\"label\":\"출장목적\",\"type\":\"textarea\"},"
                        + "{\"name\":\"note\",\"label\":\"비고\",\"type\":\"textarea\"},"
                        + "{\"name\":\"travelers\",\"label\":\"출장자 정보(성명/직급/소속)\",\"type\":\"textarea\"},"
                        + "{\"name\":\"accommodationFee\",\"label\":\"외화체제비 숙박비\",\"type\":\"number\"},"
                        + "{\"name\":\"mealFee\",\"label\":\"원화체제비 숙박비\",\"type\":\"number\"}"
                        + "]}")
                .build());

// 해외출장신청
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("해외출장신청")
                .requestType(RequestType.GENERAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"tripStartDate\",\"label\":\"출장 시작일\",\"type\":\"date\"},"
                        + "{\"name\":\"tripEndDate\",\"label\":\"출장 종료일\",\"type\":\"date\"},"
                        + "{\"name\":\"destinationCountry\",\"label\":\"출장국가\",\"type\":\"text\"},"
                        + "{\"name\":\"destination\",\"label\":\"출장지\",\"type\":\"text\"},"
                        + "{\"name\":\"transportation\",\"label\":\"교통편\",\"type\":\"text\"},"
                        + "{\"name\":\"purpose\",\"label\":\"출장목적\",\"type\":\"textarea\"},"
                        + "{\"name\":\"note\",\"label\":\"비고\",\"type\":\"textarea\"},"
                        + "{\"name\":\"travelers\",\"label\":\"출장자 정보(사번/성명/직급/소속)\",\"type\":\"textarea\"},"
                        + "{\"name\":\"foreignAccommodationFee\",\"label\":\"외화체제비 숙박비\",\"type\":\"number\"},"
                        + "{\"name\":\"foreignDailyFee\",\"label\":\"외화체제비 일당\",\"type\":\"number\"},"
                        + "{\"name\":\"exchangeRate\",\"label\":\"환율\",\"type\":\"number\"},"
                        + "{\"name\":\"krwAccommodationFee\",\"label\":\"원화체제비 숙박비\",\"type\":\"number\"},"
                        + "{\"name\":\"krwDailyFee\",\"label\":\"원화체제비 일당\",\"type\":\"number\"},"
                        + "{\"name\":\"cashFee\",\"label\":\"현금\",\"type\":\"number\"}"
                        + "]}")
                .build());

        // ========== 공문 카테고리 ==========

// 공문
        approvalDocumentRepository.save(ApprovalDocument.builder()
                .companyId(companyId)
                .documentName("공문")
                .requestType(RequestType.OFFICIAL)
                .formSchema("{\"fields\":["
                        + "{\"name\":\"title\",\"label\":\"제목\",\"type\":\"text\"},"
                        + "{\"name\":\"content\",\"label\":\"본문\",\"type\":\"textarea\"},"
                        + "{\"name\":\"note\",\"label\":\"비고\",\"type\":\"textarea\"}"
                        + "]}")
                .build());
    }

    // 결재 양식 생성
    public ApprovalDocumentResDto create(UUID companyId, ApprovalDocumentCreateReqDto reqDto) {

        // 같은 회사 내 이름 중복 체크
        if (approvalDocumentRepository.existsByCompanyIdAndDocumentName(companyId, reqDto.getDocumentName())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 동일한 이름의 결재 양식이 존재합니다: " + reqDto.getDocumentName());
        }

        ApprovalDocument document = reqDto.toEntity(companyId);
        ApprovalDocument saved = approvalDocumentRepository.save(document);
        return ApprovalDocumentResDto.fromEntity(saved);
    }

    public ApprovalDocumentResDto update(UUID companyId, UUID documentId,
                                         ApprovalDocumentUpdateReqDto reqDto) {

        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // locked 필드 검증
        validateLockedFields(document.getFormSchema(), reqDto.getFormSchema());

        // formSchema 업데이트
        document.updateFormSchema(reqDto.getFormSchema());

        return ApprovalDocumentResDto.fromEntity(document);
    }

    /**
     * locked 필드 검증
     * 기존 formSchema에서 locked:true 인 필드가
     * 새 formSchema에서 빠지거나, name/label/type/순서가 변경되었는지 확인
     */
    private void validateLockedFields(String oldSchemaJson, String newSchemaJson) {
        try {
            Map<String, Object> oldSchema = objectMapper.readValue(
                    oldSchemaJson, new TypeReference<>() {});
            Map<String, Object> newSchema = objectMapper.readValue(
                    newSchemaJson, new TypeReference<>() {});

            List<Map<String, Object>> oldFields = (List<Map<String, Object>>) oldSchema.get("fields");
            List<Map<String, Object>> newFields = (List<Map<String, Object>>) newSchema.get("fields");

            if (oldFields == null) return;

            // locked 필드만 추출
            List<Map<String, Object>> lockedFields = new ArrayList<>();
            for (Map<String, Object> field : oldFields) {
                Boolean locked = (Boolean) field.getOrDefault("locked", false);
                if (Boolean.TRUE.equals(locked)) {
                    lockedFields.add(field);
                }
            }

            if (lockedFields.isEmpty()) return;

            if (newFields == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "필수 항목이 포함된 formSchema가 필요합니다.");
            }

            // 새 formSchema에서 locked 필드 순서·내용 검증
            int newIndex = 0;
            for (Map<String, Object> locked : lockedFields) {
                String lockedName = (String) locked.get("name");
                String lockedLabel = (String) locked.get("label");
                String lockedType = (String) locked.get("type");

                boolean found = false;
                for (int ni = newIndex; ni < newFields.size(); ni++) {
                    Map<String, Object> newField = newFields.get(ni);
                    if (lockedName.equals(newField.get("name"))) {
                        // label 변경 검증
                        if (!lockedLabel.equals(newField.get("label"))) {
                            throw new BusinessException(HttpStatus.BAD_REQUEST,
                                    "필수 항목 '" + lockedLabel + "'의 이름(label)은 변경할 수 없습니다.");
                        }
                        // type 변경 검증
                        if (!lockedType.equals(newField.get("type"))) {
                            throw new BusinessException(HttpStatus.BAD_REQUEST,
                                    "필수 항목 '" + lockedLabel + "'의 타입(type)은 변경할 수 없습니다.");
                        }
                        // locked 플래그 유지 검증
                        Boolean newLocked = (Boolean) newField.getOrDefault("locked", false);
                        if (!Boolean.TRUE.equals(newLocked)) {
                            throw new BusinessException(HttpStatus.BAD_REQUEST,
                                    "필수 항목 '" + lockedLabel + "'의 잠금 상태는 해제할 수 없습니다.");
                        }
                        // 순서 검증: locked 필드끼리의 상대 순서 유지
                        if (ni < newIndex) {
                            throw new BusinessException(HttpStatus.BAD_REQUEST,
                                    "필수 항목의 순서는 변경할 수 없습니다.");
                        }
                        newIndex = ni + 1;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "필수 항목 '" + lockedLabel + "'은(는) 삭제할 수 없습니다.");
                }
            }

        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "formSchema JSON 형식이 올바르지 않습니다.");
        }
    }



    // 결재 양식 단건 조회
    @Transactional(readOnly = true)
    public ApprovalDocumentResDto findById(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        return ApprovalDocumentResDto.fromEntity(document);
    }

    // 회사별 활성 양식 목록 조회
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResDto> findActiveByCompanyId(UUID companyId) {
        return approvalDocumentRepository
                .findByCompanyIdAndIsActiveYn(companyId, "Y")
                .stream()
                .map(ApprovalDocumentResDto::fromEntity)
                .toList();
    }

    // 회사별 전체 양식 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResDto> findAllByCompanyId(UUID companyId) {
        return approvalDocumentRepository
                .findByCompanyId(companyId)
                .stream()
                .map(ApprovalDocumentResDto::fromEntity)
                .toList();
    }

    // 결재 양식 활성화
    public ApprovalDocumentResDto activate(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        document.activate();
        return ApprovalDocumentResDto.fromEntity(document);
    }

    // 결재 양식 비활성화
    public ApprovalDocumentResDto deactivate(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        document.deactivate();
        return ApprovalDocumentResDto.fromEntity(document);
    }


}
