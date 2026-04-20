package com._team._team.salary.service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.SimplifiedTaxTable;
import com._team._team.salary.repository.SimplifiedTaxTableRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SimplifiedTaxTableService {

    // 간이세액표 엑셀 포맷 상수
    private static final int HEADER_ROW_COUNT = 3;   // 상단 헤더 스킵
    private static final int COL_LOWER_BOUND = 0;
    private static final int COL_UPPER_BOUND = 1;
    private static final int COL_DEPENDENT_START = 2;   // 0명부터 시작
    private static final int DEPENDENT_MAX = 11;        // 0~11명

    private final SimplifiedTaxTableRepository repository;

    @Autowired
    public SimplifiedTaxTableService(SimplifiedTaxTableRepository repository) {
        this.repository = repository;
    }

    /**
     * 국세청 간이세액표 엑셀 업로드
     * 기존 연도 데이터가 있으면 소프트 삭제 후 새로 삽입
     */
    public int uploadTaxTable(Integer effectiveYear, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "파일이 비어있습니다.");
        }

        // 기존 연도 데이터 소프트 삭제, 재업로드 대응
        repository.softDeleteByYear(effectiveYear);

        List<SimplifiedTaxTable> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int r = HEADER_ROW_COUNT; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Long lower = readLong(row.getCell(COL_LOWER_BOUND));
                Long upper = readLong(row.getCell(COL_UPPER_BOUND));
                if (lower == null || upper == null) continue;

                // 부양가족수 0~11 열 순회, 각각 세액을 Row 로 저장
                for (int d = 0; d <= DEPENDENT_MAX; d++) {
                    Long tax = readLong(row.getCell(COL_DEPENDENT_START + d));
                    if (tax == null) continue;

                    rows.add(SimplifiedTaxTable.builder()
                            .effectiveYear(effectiveYear)
                            .salaryLowerBound(lower)
                            .salaryUpperBound(upper)
                            .dependentCount(d)
                            .monthlyTaxAmount(tax)
                            .build());
                }
            }

            repository.saveAll(rows);
            return rows.size();

        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "엑셀 파일 읽기 실패, " + e.getMessage());
        }
    }

    // 셀에서 Long 안전 추출, 빈 셀이거나 숫자 아니면 null
    private Long readLong(Cell cell) {
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (long) cell.getNumericCellValue();
                case STRING -> {
                    String s = cell.getStringCellValue().replaceAll("[,\\s]", "");
                    yield s.isBlank() ? null : Long.parseLong(s);
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 급여 계산 시 호출, 간이세액표 조회
    @Transactional(readOnly = true)
    public Long findTax(int year, long monthlyTaxableSalary, int dependents) {
        return repository.findTaxFor(year, monthlyTaxableSalary, dependents)
                .map(SimplifiedTaxTable::getMonthlyTaxAmount)
                .orElse(0L);
    }
}