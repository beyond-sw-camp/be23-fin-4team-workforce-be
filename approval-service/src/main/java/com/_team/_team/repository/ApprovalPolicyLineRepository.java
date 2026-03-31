package com._team._team.repository;

import com._team._team.domain.ApprovalDocument;
import com._team._team.domain.ApprovalPolicyLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApprovalPolicyLineRepository extends JpaRepository<ApprovalPolicyLine, UUID> {

}
