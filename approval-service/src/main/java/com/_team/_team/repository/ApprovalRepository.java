package com._team._team.repository;

import com._team._team.domain.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    Optional<Approval> findByRequest_RequestId(UUID requestId);
}
