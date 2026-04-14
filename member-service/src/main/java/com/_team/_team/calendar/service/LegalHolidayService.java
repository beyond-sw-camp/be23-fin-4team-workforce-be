package com._team._team.calendar.service;

import com._team._team.calendar.domain.LegalHoliday;
import com._team._team.calendar.dto.resdto.LegalHolidayResDto;
import com._team._team.calendar.repository.LegalHolidayRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.fasterxml.jackson.databind.JsonNode;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LegalHolidayService {

    private final LegalHolidayRepository legalHolidayRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${public.api.holiday.key}")
    private String apiKey;

    private static final String REDIS_KEY_PREFIX = "legal-holiday:";
    private static final String API_URL =
            "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

    @Autowired
    public LegalHolidayService(LegalHolidayRepository legalHolidayRepository, @Qualifier("holidayInventory") RedisTemplate<String, Object> redisTemplate, RestTemplate restTemplate) {
        this.legalHolidayRepository = legalHolidayRepository;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    // 공공 API 에서 공휴일 수집 후 RDB + Redis 저장
    public void collectAndSaveHolidays(int year) {
        try {
            List<LegalHoliday> holidays = new ArrayList<>();

            for (int month = 1; month <= 12; month++) {
                String url = String.format(
                        "%s?solYear=%d&solMonth=%02d&ServiceKey=%s&numOfRows=50",
                        API_URL, year, month, apiKey);

                String response = restTemplate.getForObject(url, String.class);
                log.info("공공 API 응답: {}", response);
                List<LegalHoliday> monthHolidays = parseHolidays(response, year);
                holidays.addAll(monthHolidays);

            }

            // RDB 저장 (기존 데이터 삭제 후 재저장)
            legalHolidayRepository.deleteByYear(year);
            legalHolidayRepository.saveAll(holidays);

            // Redis 캐싱 (TTL 400일)
            List<LegalHolidayResDto> resDtos = holidays.stream()
                    .map(LegalHolidayResDto::fromEntity)
                    .collect(Collectors.toList());

            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + year,
                    resDtos,
                    400, TimeUnit.DAYS);

            log.info("공휴일 수집 완료 year: {} count: {}", year, holidays.size());

        } catch (Exception e) {
            log.error("공휴일 수집 실패 year: {} error: {}", year, e.getMessage());
        }
    }

    // 공휴일 조회 (Redis → RDB → 공공 API 순서)
    @Transactional(readOnly = true)
    public List<LegalHolidayResDto> getHolidays(int year) {

        // 1. Redis 조회
        String redisKey = REDIS_KEY_PREFIX + year;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            List<LegalHolidayResDto> result = objectMapper.convertValue(
                    cached,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, LegalHolidayResDto.class));
            return result;
        }



        // 2. RDB 조회
        List<LegalHoliday> holidays = legalHolidayRepository.findByYear(year);
        if (!holidays.isEmpty()) {
            log.info("공휴일 RDB 조회 year: {}", year);
            List<LegalHolidayResDto> resDtos = holidays.stream()
                    .map(LegalHolidayResDto::fromEntity)
                    .collect(Collectors.toList());

            // Redis 재캐싱
            redisTemplate.opsForValue().set(
                    redisKey, resDtos, 400, TimeUnit.DAYS);

            return resDtos;
        }

        // 3. 공공 API 직접 호출
        log.info("공휴일 공공 API 조회 year: {}", year);
        collectAndSaveHolidays(year);
        return legalHolidayRepository.findByYear(year).stream()
                .map(LegalHolidayResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // XML 파싱
    private List<LegalHoliday> parseHolidays(String jsonResponse, int year) {
        List<LegalHoliday> holidays = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            // item 이 단건이면 객체, 여러 개면 배열로 옴
            if (items.isArray()) {
                for (JsonNode item : items) {
                    addHoliday(holidays, item, formatter, year);
                }
            } else if (!items.isMissingNode()) {
                addHoliday(holidays, items, formatter, year);
            }

        } catch (Exception e) {
            log.error("공휴일 JSON 파싱 실패: {}", e.getMessage());
        }
        return holidays;
    }

    private void addHoliday(List<LegalHoliday> holidays, JsonNode item,
                            DateTimeFormatter formatter, int year) {
        String isHoliday = item.path("isHoliday").asText();
        if ("Y".equals(isHoliday)) {
            String locdate = String.valueOf(item.path("locdate").asLong());
            String dateName = item.path("dateName").asText();
            holidays.add(LegalHoliday.builder()
                    .holidayDate(LocalDate.parse(locdate, formatter))
                    .holidayName(dateName)
                    .year(year)
                    .build());
        }
    }
    public boolean hasHolidays(int year) {
        return legalHolidayRepository.existsByYear(year);
    }
}