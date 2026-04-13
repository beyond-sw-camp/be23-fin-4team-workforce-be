package com._team._team.hr.batch;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Quartz와 Spring Batch를 연결하는 공통 구현체
 */
@Slf4j
@Component
public class BatchScheduledJob extends QuartzJobBean {

    private final JobLocator jobLocator;
    private final JobLauncher jobLauncher;

    @Autowired
    public BatchScheduledJob(JobLocator jobLocator, JobLauncher jobLauncher) {
        this.jobLocator = jobLocator;
        this.jobLauncher = jobLauncher;
    }

    /**
     * Quartz 트리거 시 호출
     * Quartz 실행 컨텍스트(jobName은 JobDetail 등록 시 설정)
     */
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getMergedJobDataMap().getString("jobName");
        log.info("Quartz Triggered! Starting Spring Batch Job: {}", jobName);

        try {
            Job job = jobLocator.getJob(jobName);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("time", new Date())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);
            
            log.info("Successfully completed Spring Batch Job: {}", jobName);

        } catch (Exception e) {
            log.error("Failed to execute Spring Batch Job: {}", jobName, e);
            throw new JobExecutionException("Batch Job Execution Failed: " + jobName, e);
        }
    }
}