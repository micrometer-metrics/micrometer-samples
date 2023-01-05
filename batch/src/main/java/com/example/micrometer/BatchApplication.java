package com.example.micrometer;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class BatchApplication {

    private static final Logger log = LoggerFactory.getLogger(BatchApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(BatchApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    Job myJob(Step step, JobRepository jobRepository, Tracer tracer, ObservationRegistry observationRegistry) {
        return new JobBuilder("myJob", jobRepository).listener(new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {

            }
        }).observationRegistry(observationRegistry).start(step).build();
    }

    @Bean
    Step myStep(JobRepository jobRepository, Tracer tracer, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder("myTask", jobRepository).tasklet((stepContribution, chunkContext) -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());
            return RepeatStatus.FINISHED;
        }, platformTransactionManager).build();
    }

}
