package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(BatchApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    Tracer tracer;

    @Autowired
    StepBuilderFactory stepBuilderFactory;

    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    JobLauncher jobLauncher;

    @Override
    public void run(String... args) throws Exception {
        Job job = this.jobBuilderFactory.get("myJob").listener(new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {

            }
        }).start(this.stepBuilderFactory.get("myTask").tasklet((stepContribution, chunkContext) -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
            return RepeatStatus.FINISHED;
        }).build()).build();

        this.jobLauncher.run(job, new JobParameters());
        // To ensure that the spans got successfully reported
        Thread.sleep(500);
    }

}
