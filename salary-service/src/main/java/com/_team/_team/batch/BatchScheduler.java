package com._team._team.batch;

import com._team._team.batch.config.BatchSchedulerProperties;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.util.TimeZone;

/**
 * salary-service 배치용 Quartz 크론 등록 지점
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BatchSchedulerProperties.class)
public class BatchScheduler {

    private static final String QUARTZ_GROUP = "BATCH_GROUP";

    private final Scheduler scheduler;
    private final BatchSchedulerProperties schedulerProperties;

    @Autowired
    public BatchScheduler(Scheduler scheduler, BatchSchedulerProperties schedulerProperties) {
        this.scheduler = scheduler;
        this.schedulerProperties = schedulerProperties;
    }

    @PostConstruct
    public void scheduleJobs() {
        log.info("[BATCH-SCHED] PostConstruct 호출됨 enabled={} jobsSize={}",
                schedulerProperties.isEnabled(),
                schedulerProperties.getJobs() == null ? -1 : schedulerProperties.getJobs().size());
        try {
            if (!schedulerProperties.isEnabled()) {
                log.info("Quartz batch scheduler disabled (batch.scheduler.enabled=false). No triggers registered.");
                return;
            }
            var jobs = schedulerProperties.getJobs();
            if (jobs == null || jobs.isEmpty()) {
                log.warn("Quartz batch scheduler enabled but batch.scheduler.jobs is empty; nothing to register.");
                return;
            }
            for (var e : jobs.entrySet()) {
                registerJob(e.getKey(), e.getValue());
            }
        } catch (Exception e) {
            log.error("Failed to register Quartz jobs", e);
        }
    }

    private void registerJob(String jobName, String cronExpression) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(BatchScheduledJob.class)
                .withIdentity(jobName + "_Detail", QUARTZ_GROUP)
                .usingJobData("jobName", jobName)
                .storeDurably()
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "_Trigger", QUARTZ_GROUP)
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
