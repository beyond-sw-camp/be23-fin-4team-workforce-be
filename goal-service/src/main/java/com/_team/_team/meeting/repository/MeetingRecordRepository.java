package com._team._team.meeting.repository;

import com._team._team.meeting.domain.MeetingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRecordRepository extends JpaRepository<MeetingRecord, UUID> {

    List<MeetingRecord> findByMemberIdOrderByScheduledAtDesc(UUID memberId);

    List<MeetingRecord> findByManagerIdOrderByScheduledAtDesc(UUID managerId);

    List<MeetingRecord> findByMemberIdAndManagerIdOrderByScheduledAtDesc(UUID memberId, UUID managerId);

    List<MeetingRecord> findByParentRecord_MeetingRecordIdOrderByScheduledAtAsc(UUID parentRecordId);
}
