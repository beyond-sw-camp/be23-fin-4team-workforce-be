package com._team._team.hr.batch;

import com._team._team.hr.batch.config.HrBatchSchedulerProperties;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.util.TimeZone;

/**
 * salary-service HR 배치의 유일한 Quartz 크론 등록 지점
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(HrBatchSchedulerProperties.class)
public class HrBatchScheduler {

    /** Spring Boot가 주입하는 Quartz */
    private final Scheduler scheduler;
    private final HrBatchSchedulerProperties schedulerProperties;

    @Autowired
    public HrBatchScheduler(Scheduler scheduler, HrBatchSchedulerProperties schedulerProperties) {
        this.scheduler = scheduler;
        this.schedulerProperties = schedulerProperties;
    }

    /**
     * HrBatchSchedulerProperties isEnabled 가 true일 때만
     * HrBatchSchedulerProperties getJobs() 항목을 Quartz에 등록
     */
    @PostConstruct
    public void scheduleJobs() {
        try {
            if (!schedulerProperties.isEnabled()) {
                log.info("HR Quartz scheduler disabled (hr.batch.scheduler.enabled=false). No triggers registered.");
                return;
            }
            var jobs = schedulerProperties.getJobs();
            if (jobs == null || jobs.isEmpty()) {
                log.warn("HR Quartz scheduler enabled but hr.batch.scheduler.jobs is empty; nothing to register.");
                return;
            }
            for (var e : jobs.entrySet()) {
                registerJob(e.getKey(), e.getValue());
            }
        } catch (Exception e) {
            log.error("Failed to register Quartz jobs", e);
        }
    }

    /**
     * Quartz JobDetail 및 Trigger를 등록하거나, 기존 항목이 있으면 크론을 갱신
     */
    private void registerJob(String jobName, String cronExpression) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(BatchScheduledJob.class)
                .withIdentity(jobName + "_Detail", "HR_BATCH_GROUP")
                .usingJobData("jobName", jobName)
                .storeDurably()
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "_Trigger", "HR_BATCH_GROUP")
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .inTimeZone(TimeZone.getTimeZone("Asia/Seoul")))
                .build();

        if (!scheduler.checkExists(jobDetail.getKey())) {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Registered Quartz Schedule: {} with Cron: {}", jobName, cronExpression);
            return;
        }
        scheduler.addJob(jobDetail, true, true);
        TriggerKey triggerKey = trigger.getKey();
        if (scheduler.checkExists(triggerKey)) {
            scheduler.rescheduleJob(triggerKey, trigger);
        } else {
            scheduler.scheduleJob(trigger);
        }
        log.info("Updated existing Quartz Schedule: {}", jobName);
    }
}