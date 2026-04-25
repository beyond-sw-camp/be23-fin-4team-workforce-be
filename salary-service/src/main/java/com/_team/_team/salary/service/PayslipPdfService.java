package com._team._team.salary.service;

import com._team._team.annotation.Action;
import com._team._team.annotation.Resource;
import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.Payroll;
import com._team._team.salary.domain.PayrollItem;
import com._team._team.salary.domain.enums.ItemType;
import com._team._team.salary.repository.PayrollItemRepository;
import com._team._team.salary.repository.PayrollRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 급여대장 기반 급여명세서 PDF 생성
 * 한글: classpath fonts/NotoSansKR-Regular.otf 또는 환경변수 SALARY_PAYSLIP_FONT_PATH
 * 다운로드: 본인 급여대장 이거나 급여 조회(SALARY:READ) 권한 또는 시스템 관리자
 */
@Slf4j
@Service
public class PayslipPdfService {

    private static final String FONT_RESOURCE = "/fonts/NotoSansKR-Regular.otf";
    private static final String FONT_ENV = "SALARY_PAYSLIP_FONT_PATH";
    private static final String SALARY_READ = Resource.SALARY.name() + ":" + Action.READ.name();

    private final PayrollRepository payrollRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final RedisTemplate<String, String> permissionRedisTemplate;

    @Autowired
    public PayslipPdfService(
            PayrollRepository payrollRepository,
            PayrollItemRepository payrollItemRepository,
            @Qualifier("permissionInventory") RedisTemplate<String, String> permissionRedisTemplate) {
        this.payrollRepository = payrollRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.permissionRedisTemplate = permissionRedisTemplate;
    }

    @Transactional(readOnly = true)
    public byte[] buildPdf(
            UUID companyId,
            UUID payrollId,
            UUID requesterMemberId,
            UUID requesterPositionId,
            String isSystemAdminHeader) {

        Payroll payroll = payrollRepository
                .findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));

        assertPayslipDownloadAllowed(payroll, requesterMemberId, requesterPositionId, isSystemAdminHeader);

        List<PayrollItem> items = payrollItemRepository
                .findByPayroll_PayrollIdAndDelYnOrderByDisplayOrder(payrollId, "N");

        try {
            return renderPdf(payroll, items);
        } catch (DocumentException | IOException e) {
            log.error("[PayslipPdf] 생성 실패 payrollId={}", payrollId, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "급여명세서 PDF 생성에 실패했습니다.");
        }
    }

    /**
     * PermissionAspect 와 동일 규칙: 시스템 관리자 통과, 본인 명세서 통과, 그 외 SALARY:READ 필요
     */
    private void assertPayslipDownloadAllowed(
            Payroll payroll,
            UUID requesterMemberId,
            UUID requesterPositionId,
            String isSystemAdminHeader) {

        if ("YES".equals(isSystemAdminHeader)) {
            return;
        }
        if (payroll.getMemberId().equals(requesterMemberId)) {
            return;
        }

        String permissionStr = permissionRedisTemplate.opsForValue()
                .get("PERMISSION:" + requesterPositionId);
        if (permissionStr == null || permissionStr.isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "권한 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }
        boolean hasSalaryRead = List.of(permissionStr.split(",")).stream()
                .anyMatch(p -> p.startsWith(SALARY_READ));
        if (!hasSalaryRead) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인 명세서만 다운로드할 수 있습니다.");
        }
    }

    private byte[] renderPdf(Payroll payroll, List<PayrollItem> items) throws DocumentException, IOException {
        FontSet fontSet = resolveFontSet();
        BaseFont baseFont = fontSet.baseFont();
        boolean korean = fontSet.korean();
        Font titleFont = new Font(baseFont, 16, Font.BOLD);
        Font bodyFont = new Font(baseFont, 10, Font.NORMAL);
        Font smallFont = new Font(baseFont, 9, Font.NORMAL);

        NumberFormat nf = NumberFormat.getInstance(Locale.KOREA);

        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        String title = korean ? "급여명세서" : "PAYSLIP";
        Paragraph head = new Paragraph(title, titleFont);
        head.setSpacingAfter(12f);
        document.add(head);

        document.add(new Paragraph((korean ? "정산일" : "Settlement date") + ": "
                + payroll.getPayrollYearMonthDay(), bodyFont));
        document.add(new Paragraph((korean ? "직원 ID" : "Employee ID") + ": "
                + payroll.getMemberId(), bodyFont));
        document.add(new Paragraph((korean ? "상태" : "Status") + ": "
                + payroll.getPayrollStatus().name(), bodyFont));
        document.add(new Paragraph(" ", smallFont));

        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(55);
        summary.setHorizontalAlignment(Element.ALIGN_LEFT);
        addSummaryRow(summary, korean ? "총 지급" : "Gross payment", nf.format(payroll.getTotalPayment()), bodyFont);
        addSummaryRow(summary, korean ? "총 공제" : "Total deduction", nf.format(payroll.getTotalDeduction()), bodyFont);
        addSummaryRow(summary, korean ? "실수령액" : "Net pay", nf.format(payroll.getNetPay()), bodyFont);
        document.add(summary);
        document.add(new Paragraph(" ", smallFont));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 2f, 3f});
        addHeaderCell(table, korean ? "항목명" : "Item", bodyFont);
        addHeaderCell(table, korean ? "구분" : "Type", bodyFont);
        addHeaderCell(table, korean ? "금액" : "Amount", bodyFont);

        for (PayrollItem it : items) {
            addBodyCell(table, it.getItemName(), bodyFont, Element.ALIGN_LEFT);
            String typeLabel = it.getItemType() == ItemType.EARNING
                    ? (korean ? "지급" : "Earning")
                    : (korean ? "공제" : "Deduction");
            addBodyCell(table, typeLabel, bodyFont, Element.ALIGN_CENTER);
            addBodyCell(table, nf.format(it.getAmount()), bodyFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(0);
        c1.setPaddingBottom(4f);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(value, font));
        c2.setBorder(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPaddingBottom(4f);
        table.addCell(c2);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private FontSet resolveFontSet() throws DocumentException, IOException {
        String envPath = System.getenv(FONT_ENV);
        if (envPath != null && !envPath.isBlank()) {
            Path p = Path.of(envPath);
            if (Files.isReadable(p)) {
                byte[] bytes = Files.readAllBytes(p);
                BaseFont bf = BaseFont.createFont(
                        "PayslipFont", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                return new FontSet(bf, true);
            }
            log.warn("[PayslipPdf] SALARY_PAYSLIP_FONT_PATH 가 읽을 수 없습니다: {}", envPath);
        }
        try (InputStream in = PayslipPdfService.class.getResourceAsStream(FONT_RESOURCE)) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                BaseFont bf = BaseFont.createFont(
                        "NotoSansKR", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
                return new FontSet(bf, true);
            }
        }
        log.warn("[PayslipPdf] 한글 폰트 없음 — resources{} 또는 {} 를 설정하세요.", FONT_RESOURCE, FONT_ENV);
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        return new FontSet(bf, false);
    }

    private record FontSet(BaseFont baseFont, boolean korean) {}
}
