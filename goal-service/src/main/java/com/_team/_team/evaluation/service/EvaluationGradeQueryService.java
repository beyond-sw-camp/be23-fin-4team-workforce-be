package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.EvaluationStage;
import com._team._team.evaluation.dto.resdto.MemberFinalGradeResDto;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 회사 + 시즌 단위 직원별 최종 등급 일괄 조회 - 성과급 발행 전용
@Service
@Transactional(readOnly = true)
public class EvaluationGradeQueryService {

    private final EvaluationSeasonRepository evaluationSeasonRepository;
    private final EvaluationResponseRepository evaluationResponseRepository;

    @Autowired
    public EvaluationGradeQueryService(
            EvaluationSeasonRepository evaluationSeasonRepository,
            EvaluationResponseRepository evaluationResponseRepository
    ) {
        this.evaluationSeasonRepository = evaluationSeasonRepository;
        this.evaluationResponseRepository = evaluationResponseRepository;
    }

    public List<MemberFinalGradeResDto> findFinalGrades(UUID companyId, UUID seasonId) {
        EvaluationSeason season = evaluationSeasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 사이클을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 평가 사이클은 조회할 수 없습니다.");
        }

        List<EvaluationResponse> responses = evaluationResponseRepository
                .findByGroup_Season_SeasonId(seasonId);

        // 가장 최근 1건만 사용 - 평가자 다수일 때 최종 1건 선택
        Map<UUID, EvaluationResponse> latestPerMember = new HashMap<>();
        for (EvaluationResponse r : responses) {
            if (r.getStage() != EvaluationStage.CONFIRMED) continue;
            if (r.getConfirmedGrade() == null) continue;
            UUID memberId = r.getTargetMemberId();
            EvaluationResponse cur = latestPerMember.get(memberId);
            LocalDateTime rAt = r.getConfirmedAt();
            LocalDateTime curAt = cur == null ? null : cur.getConfirmedAt();
            if (cur == null || (rAt != null && (curAt == null || rAt.isAfter(curAt)))) {
                latestPerMember.put(memberId, r);
            }
        }

        List<MemberFinalGradeResDto> result = new ArrayList<>();
        for (EvaluationResponse r : latestPerMember.values()) {
            result.add(MemberFinalGradeResDto.builder()
                    .memberId(r.getTargetMemberId())
                    .finalGrade(r.getConfirmedGrade())
                    .confirmedAt(r.getConfirmedAt())
                    .build());
        }
        return result;
    }
}
