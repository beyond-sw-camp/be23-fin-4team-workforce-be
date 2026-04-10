package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.EvaluationSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EvaluationSeasonRepository extends JpaRepository<EvaluationSeason, UUID> {
    List<EvaluationSeason> findByCompanyIdOrderByStartDateDesc(UUID companyId);
}
