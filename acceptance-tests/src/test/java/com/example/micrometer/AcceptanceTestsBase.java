package com.example.micrometer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;

@SpringBootTest
class AcceptanceTestsBase {

    private static final Logger log = LoggerFactory.getLogger(AcceptanceTestsBase.class);

    @Autowired
    ProjectDeployer projectDeployer;

    @Autowired
    TracingAssertions tracingAssertions;

    AcceptanceTestsBase() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> projectDeployer.getIds().forEach((key, values) -> values.forEach(id -> {
                    if (projectDeployer.status(id).getState() != DeploymentState.undeployed) {
                        undeploy(id);
                    }
                }))));
    }

    String deployWebApp(TestInfo testInfo, String appName, int port) {
        return this.projectDeployer.deployWebApp(testInfo, appName, port);
    }

    String deployWebApp(TestInfo testInfo, String appName, Map<String, String> props) {
        return this.projectDeployer.deployWebApp(testInfo, appName, props);
    }

    String deploy(TestInfo testInfo, String appName, Map<String, String> props) {
        return this.projectDeployer.deploy(testInfo, appName, props);
    }

    String deploy(TestInfo testInfo, String appName) {
        return this.projectDeployer.deploy(testInfo, appName, Map.of());
    }

    String waitUntilStarted(Callable<String> callable) throws Exception {
        return this.projectDeployer.waitUntilStarted(callable);
    }

    String wait10seconds(Callable<String> callable) throws Exception {
        try {
            String id = callable.call();
            long millis = Duration.ofSeconds(10).toMillis();
            log.info("Will wait for [{}] millis before the app starts", millis);
            Thread.sleep(millis);
            return id;
        }
        catch (InterruptedException e) {
            throw new IllegalStateException();
        }
    }

    private void undeploy(String id) {
        this.projectDeployer.undeploy(id);
    }

    @AfterEach
    void cleanup(TestInfo testInfo) {
        this.projectDeployer.clean(testInfo);
    }

    void assertThatTraceIdGotPropagated(String... appIds) {
        this.tracingAssertions.assertThatTraceIdGotPropagated(appIds);
    }

    void assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(String appId, String springAppName, int minOccurrence) {
        this.tracingAssertions.assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(appId, springAppName,
                minOccurrence);
    }

    @SpringBootApplication
    static class Config {

    }

}
