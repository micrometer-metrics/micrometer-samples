package com.example.micrometer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
class ProjectRebuilder {

    private static final Logger log = LoggerFactory.getLogger(ProjectRebuilder.class);

    private final boolean rebuildProjects;

    private final String projectRoot;

    ProjectRebuilder(@Value("${io.micrometer.samples.rebuild-projects:false}") boolean rebuildProjects,
            @Value("${io.micrometer.samples.project-root:}") String projectRoot) {
        this.rebuildProjects = rebuildProjects;
        this.projectRoot = projectRoot;
    }

    void rebuildProjectBeforeDeployment(String appName) {
        if (!this.rebuildProjects) {
            log.info("The flag [io.micrometer.samples.rebuild-projects] was set to false - won't rebuild the projects");
            return;
        }
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", "./gradlew", ":" + appName + ":pTML");
        builder.directory(new File(this.projectRoot));
        Process process = null;
        try {
            process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), log::info);
            Future<?> future = Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Failed to build the application");
            }
            future.get(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class StreamGobbler implements Runnable {

        private InputStream inputStream;

        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }

    }

}
