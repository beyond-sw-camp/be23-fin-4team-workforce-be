package com._team._team.saas.schedule;

import com._team._team.dto.BusinessException;
import com._team._team.saas.schedule.dto.SaasScheduleResDto;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * SaaS 운영자용 Quartz 스케줄 조회/수정/일시중지
 */
@Slf4j
@Service
public class SaasScheduleService {

    final Scheduler scheduler;

    @Autowired
    public SaasScheduleService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public List<SaasScheduleResDto> listAll() {
        List<SaasScheduleResDto> result = new ArrayList<>();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    for (Trigger t : triggers) {
                        String cron = (t instanceof CronTrigger ct) ? ct.getCronExpression() : null;
                        Trigger.TriggerState state = scheduler.getTriggerState(t.getKey());
                        boolean paused = state == Trigger.TriggerState.PAUSED;
                        result.add(SaasScheduleResDto.builder()
                                .jobKey(jobKey.getName() + "::" + jobKey.getGroup())
                                .triggerKey(t.getKey().getName() + "::" + t.getKey().getGroup())
                                .cronExpression(cron)
                                .nextFireTime(t.getNextFireTime())
                                .previousFireTime(t.getPreviousFireTime())
                                .paused(paused)
                                .build());
                    }
                }
            }
        } catch (SchedulerException e) {
            log.error("[SaaS][Schedule] 목록 조회 실패", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "스케줄 조회 실패");
        }
        return result;
    }

    public void updateCron(String jobKey, String newCron) {
        if (!CronExpression.isValidExpression(newCron)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "유효하지 않은 cron 표현식입니다.");
        }
        JobKey jk = parseJobKey(jobKey);
        try {
            if (!scheduler.checkExists(jk)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "잡을 찾을 수 없습니다.");
            }
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jk);
            for (Trigger t : triggers) {
                CronTrigger newTrigger = TriggerBuilder.newTrigger()
                        .withIdentity(t.getKey())
                        .forJob(jk)
                        .withSchedule(CronScheduleBuilder.cronSchedule(newCron)
                                .inTimeZone(TimeZone.getTimeZone("Asia/Seoul")))
                        .build();
                scheduler.rescheduleJob(t.getKey(), newTrigger);
                log.info("[SaaS][Schedule] cron 변경 jobKey={} cron={}", jobKey, newCron);
            }
        } catch (SchedulerException e) {
            log.error("[SaaS][Schedule] 변경 실패 jobKey={}", jobKey, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "스케줄 변경 실패");
        }
    }

    public boolean togglePause(String jobKey) {
        JobKey jk = parseJobKey(jobKey);
        try {
            if (!scheduler.checkExists(jk)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "잡을 찾을 수 없습니다.");
            }
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jk);
            if (triggers.isEmpty()) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "트리거가 없습니다.");
            }
            Trigger.TriggerState first = scheduler.getTriggerState(triggers.get(0).getKey());
            boolean willPause = first != Trigger.TriggerState.PAUSED;
            if (willPause) scheduler.pauseJob(jk);
            else scheduler.resumeJob(jk);
            log.info("[SaaS][Schedule] {} jobKey={}", willPause ? "일시중지" : "재개", jobKey);
            return willPause;
        } catch (SchedulerException e) {
            log.error("[SaaS][Schedule] 토글 실패 jobKey={}", jobKey, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "스케줄 토글 실패");
        }
    }

    private JobKey parseJobKey(String jobKey) {
        String[] parts = jobKey.split("::");
        if (parts.length != 2) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "jobKey 형식이 잘못되었습니다.");
        }
        return JobKey.jobKey(parts[0], parts[1]);
    }
}
