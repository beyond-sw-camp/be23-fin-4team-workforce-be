package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.converter.EvaluatorMapping;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * EvaluationGroupTranslator
 *
 *  기존 EvaluationGroup 구조 (targetMemberIdsJson + List&lt;EvaluatorMapping&gt;) 를
 *  SeasonActivationService.TargetSpec 리스트로 변환.
 *
 *  매핑 규칙:
 *   - 한 시즌의 모든 EvaluationGroup 을 합쳐서 평탄화
 *   - 각 (group, targetMemberId) 쌍이 하나의 TargetSpec 이 됨
 *   - LEAD = EvaluatorMapping(targetMemberId, evaluationType=DOWNWARD).evaluatorId
 *            (DOWNWARD = 상사가 부하를 평가 → 상위자가 LEAD)
 *            DOWNWARD 매핑이 없으면 null → SeasonActivationService 에서 직속 조직장 fallback
 *   - ASSISTANT = UPWARD/PEER 매핑된 evaluatorId 들
 *   - SELF 매핑은 평가 자체와 별개라 무시 (자기평가는 항상 본인이 함)
 */
@Component
public class EvaluationGroupTranslator {

    private final EvaluationGroupRepository groupRepository;
    private final ObjectMapper objectMapper;

    public EvaluationGroupTranslator(EvaluationGroupRepository groupRepository,
                                     ObjectMapper objectMapper) {
        this.groupRepository = groupRepository;
        this.objectMapper = objectMapper;
    }

    public List<SeasonActivationService.TargetSpec> translate(UUID seasonId) {
        List<EvaluationGroup> groups = groupRepository.findBySeason_SeasonId(seasonId);
        if (groups.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "시즌에 평가 그룹이 없습니다. 활성화 전 EvaluationGroup 을 먼저 구성하세요.");
        }

        List<SeasonActivationService.TargetSpec> specs = new ArrayList<>();
        for (EvaluationGroup g : groups) {
            List<UUID> targetMembers = parseUuidList(g.getTargetMemberIdsJson());
            if (targetMembers.isEmpty()) continue;

            Map<UUID, UUID> leadMap = new HashMap<>();          // target → LEAD evaluator
            Map<UUID, Set<UUID>> assistantMap = new HashMap<>(); // target → ASSISTANT evaluators

            for (EvaluatorMapping m : safe(g.getEvaluatorMaps())) {
                if (m.getTargetMemberId() == null || m.getEvaluatorId() == null) continue;
                UUID t = m.getTargetMemberId();
                EvalType type = m.getEvaluationType();
                if (type == null) continue;

                if (type == EvalType.DOWNWARD) {
                    // 한 명만 LEAD — 마지막 매핑 우선
                    leadMap.put(t, m.getEvaluatorId());
                } else if (type == EvalType.UPWARD || type == EvalType.PEER) {
                    assistantMap
                            .computeIfAbsent(t, k -> new HashSet<>())
                            .add(m.getEvaluatorId());
                }
                // SELF 는 무시
            }

            for (UUID target : targetMembers) {
                UUID leadId = leadMap.get(target); // null 가능 → activation 에서 fallback
                List<UUID> assistants = new ArrayList<>(assistantMap.getOrDefault(target, Set.of()));
                if (leadId != null) {
                    assistants.remove(leadId); // LEAD 가 ASSISTANT 에도 있으면 제외
                }
                specs.add(new SeasonActivationService.TargetSpec(g, target, leadId, assistants));
            }
        }
        return specs;
    }

    // -----------------------------------------------------------------
    private List<UUID> parseUuidList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "targetMemberIdsJson 역직렬화 실패");
        }
    }

    private List<EvaluatorMapping> safe(List<EvaluatorMapping> list) {
        return list == null ? List.of() : list;
    }
}
