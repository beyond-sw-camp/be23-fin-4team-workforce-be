package com._team._team.batch.config;

import com._team._team.batch.leave.worker.DailyAttendanceCloseWorker;
import com._team._team.batch.leave.worker.LeaveExpireWorker;
import com._team._team.batch.leave.worker.LeaveGrantWorker;
import com._team._team.batch.payroll.worker.PayrollCalculateWorker;
import com._team._team.batch.payroll.worker.PayslipSendWorker;
import com._team._team.batch.payroll.worker.SeverancePayWorker;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * Spring Batch Job / Step 빈 정의
 */
@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final LeaveGrantWorker leaveGrantWorker;
    private final DailyAttendanceCloseWorker dailyAttendanceCloseWorker;
    private final LeaveExpireWorker leaveExpireWorker;
    private final PayrollCalculateWorker payrollCalculateWorker;
    private final SeverancePayWorker severancePayWorker;
    private final PayslipSendWorker payslipSendWorker;

    @Autowired
    public BatchJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            LeaveGrantWorker leaveGrantWorker,
            DailyAttendanceCloseWorker dailyAttendanceCloseWorker,
            LeaveExpireWorker leaveExpireWorker,
            PayrollCalculateWorker payrollCalculateWorker,
            SeverancePayWorker severancePayWorker,
            PayslipSendWorker payslipSendWorker) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.leaveGrantWorker = leaveGrantWorker;
        this.dailyAttendanceCloseWorker = dailyAttendanceCloseWorker;
        this.leaveExpireWorker = leaveExpireWorker;
        this.payrollCalculateWorker = payrollCalculateWorker;
        this.severancePayWorker = severancePayWorker;
        this.payslipSendWorker = payslipSendWorker;
    }

    @Bean
    public Job leaveGrantJob() {
        return new JobBuilder("leaveGrantJob", jobRepository)
                .start(leaveGrantStep())
                .build();
    }

    @Bean
    public Step leaveGrantStep() {
        return new StepBuilder("leaveGrantStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    leaveGrantWorker.run(LocalDate.now());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job dailyAttendanceCloseJob() {
        return new JobBuilder("dailyAttendanceCloseJob", jobRepository)
                .start(dailyAttendanceCloseStep())
                .build();
    }

    @Bean
    public Step dailyAttendanceCloseStep() {
        return new StepBuilder("dailyAttendanceCloseStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    dailyAttendanceCloseWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job leaveExpireJob() {
        return new JobBuilder("leaveExpireJob", jobRepository)
                .start(leaveExpireStep())
                .build();
    }

    @Bean
    public Step leaveExpireStep() {
        return new StepBuilder("leaveExpireStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    leaveExpireWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job payrollCalculateJob() {
        return new JobBuilder("payrollCalculateJob", jobRepository)
                .start(payrollCalculateStep())
                .build();
    }

    @Bean
    public Step payrollCalculateStep() {
        return new StepBuilder("payrollCalculateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    payrollCalculateWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job severancePayJob() {
        return new JobBuilder("severancePayJob", jobRepository)
                .start(severancePayStep())
                .build();
    }

    @Bean
    public Step severancePayStep() {
        return new StepBuilder("severancePayStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    severancePayWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job payslipSendJob() {
        return new JobBuilder("payslipSendJob", jobRepository)
                .start(payslipSendStep())
                .build();
    }

    @Bean
    public Step payslipSendStep() {
        return new StepBuilder("payslipSendStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    payslipSendWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
