package com._team._team.hr.batch.config;

import com._team._team.hr.batch.attendance.worker.DailyAttendanceCloseWorker;
import com._team._team.hr.batch.attendance.worker.LeaveExpireWorker;
import com._team._team.hr.batch.attendance.worker.LeaveGrantWorker;
import com._team._team.hr.batch.salary.worker.PayrollCalculateWorker;
import com._team._team.hr.batch.salary.worker.PayslipSendWorker;
import com._team._team.hr.batch.salary.worker.SeverancePayWorker;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * HR 도메인 Spring Batch Job /Step  빈을 모아 두는 설정 클래스
 */
@Configuration
@EnableBatchProcessing
public class HrBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final LeaveGrantWorker leaveGrantWorker;
    private final DailyAttendanceCloseWorker dailyAttendanceCloseWorker;
    private final LeaveExpireWorker leaveExpireWorker;
    private final PayrollCalculateWorker payrollCalculateWorker;
    private final SeverancePayWorker severancePayWorker;
    private final PayslipSendWorker payslipSendWorker;

    @Autowired
    public HrBatchJobConfig(
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

    /**
     * 연차·회계연도 연차·월차 부여를 한 번에 수행하는 Batch Job.
     */
    @Bean
    public Job leaveGrantJob() {
        return new JobBuilder("leaveGrantJob", jobRepository)
                .start(leaveGrantStep())
                .build();
    }

    /**
     * 기준일은 현재 LocalDate now()
     */
    @Bean
    public Step leaveGrantStep() {
        return new StepBuilder("leaveGrantStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    leaveGrantWorker.run(LocalDate.now());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 일일 근태 마감 Batch Job (구현 예정).
     */
    @Bean
    public Job dailyAttendanceCloseJob() {
        return new JobBuilder("dailyAttendanceCloseJob", jobRepository)
                .start(dailyAttendanceCloseStep())
                .build();
    }

    /** 일일 근태 마감 Step. */
    @Bean
    public Step dailyAttendanceCloseStep() {
        return new StepBuilder("dailyAttendanceCloseStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    dailyAttendanceCloseWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 휴가 잔액 만료(소멸) 처리 Batch Job (구현 예정).
     */
    @Bean
    public Job leaveExpireJob() {
        return new JobBuilder("leaveExpireJob", jobRepository)
                .start(leaveExpireStep())
                .build();
    }

    /** 휴가 만료 처리 Step. */
    @Bean
    public Step leaveExpireStep() {
        return new StepBuilder("leaveExpireStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    leaveExpireWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 월 급여 일괄 계산 Batch Job (구현 예정).
     */
    @Bean
    public Job payrollCalculateJob() {
        return new JobBuilder("payrollCalculateJob", jobRepository)
                .start(payrollCalculateStep())
                .build();
    }

    /** 급여 계산 Step. */
    @Bean
    public Step payrollCalculateStep() {
        return new StepBuilder("payrollCalculateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    payrollCalculateWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 퇴직금 산정 Batch Job (구현 예정).
     */
    @Bean
    public Job severancePayJob() {
        return new JobBuilder("severancePayJob", jobRepository)
                .start(severancePayStep())
                .build();
    }

    /** 퇴직금 계산 Step. */
    @Bean
    public Step severancePayStep() {
        return new StepBuilder("severancePayStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    severancePayWorker.run();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 급여명세서 발송 Batch Job (구현 예정).
     */
    @Bean
    public Job payslipSendJob() {
        return new JobBuilder("payslipSendJob", jobRepository)
                .start(payslipSendStep())
                .build();
    }

    /** 명세서 발송 Step. */
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
