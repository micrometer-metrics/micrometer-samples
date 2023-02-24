package com.example.micrometer;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.BDDAssertions.then;

@Component
class ProjectDeployer {

    private static final Logger log = LoggerFactory.getLogger(ProjectDeployer.class);

    // testname -> list of running apps
    private final Map<String, List<String>> ids = new ConcurrentHashMap<>();

    private final AppDeployer appDeployer;

    private final ProjectRebuilder projectRebuilder;

    public ProjectDeployer(AppDeployer appDeployer, ProjectRebuilder projectRebuilder) {
        this.appDeployer = appDeployer;
        this.projectRebuilder = projectRebuilder;
    }

    Map<String, List<String>> getIds() {
        return ids;
    }

    String deployWebApp(TestInfo testInfo, String appName, int port) {
        return deployWebApp(testInfo, appName, Map.of("server.port", String.valueOf(port)));
    }

    String deployWebApp(TestInfo testInfo, String appName, Map<String, String> props) {
        AppDeploymentRequest mvcRequest = appRequest(appName, props);
        return rebuildAndDeploy(testInfo, appName, mvcRequest);
    }

    String deploy(TestInfo testInfo, String appName, Map<String, String> props) {
        AppDeploymentRequest request = appRequest(appName, props);
        return rebuildAndDeploy(testInfo, appName, request);
    }

    String waitUntilStarted(Callable<String> callable) throws Exception {
        AtomicReference<String> app = new AtomicReference<>();
        try {
            app.set(callable.call());
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                log.info("Waiting for the application with id [{}] to start...", app);
                then(this.appDeployer.status(app.get()).getState()).isEqualTo(DeploymentState.deployed);
            });
            log.info("Application with id [{}] has started successfully", app);
            return app.get();
        }
        catch (Throwable er) {
            if (app.get() != null) {
                String id = app.get();
                log.error("Application with id [" + id + "] failed to start. These are its logs\n\n");
                log.error(this.appDeployer.getLog(id));
                throw er;
            }
            log.error("The app failed to start and no application id was passed - can't fetch the logs");
            throw er;
        }
    }

    void clean(TestInfo testInfo) {
        this.ids.getOrDefault(testInfo.getDisplayName(), new ArrayList<>()).forEach(this::undeploy);
        this.ids.remove(testInfo.getDisplayName());
    }

    AppStatus status(String id) {
        return this.appDeployer.status(id);
    }

    void undeploy(String id) {
        log.info("Undeploying app with id [{}]...", id);
        this.appDeployer.undeploy(id);
    }

    private String rebuildAndDeploy(TestInfo testInfo, String appName, AppDeploymentRequest mvcRequest) {
        this.projectRebuilder.rebuildProjectBeforeDeployment(appName);
        String id = this.appDeployer.deploy(mvcRequest);
        List<String> ids = this.ids.getOrDefault(testInfo.getDisplayName(), new ArrayList<>());
        ids.add(id);
        this.ids.put(testInfo.getDisplayName(), ids);
        return id;
    }

    private AppDeploymentRequest appRequest(String appName, Map<String, String> props) {
        Map<String, String> map = new HashMap<>(props);
        attachWavefrontConfigurationIfPresent(map);
        makeCloud2022WorkWithBoot3_1(map);
        AppDefinition appDefinition = new AppDefinition(appName, map);
        return deploymentRequest(appName, appDefinition);
    }

    private void attachWavefrontConfigurationIfPresent(Map<String, String> map) {
        if (System.getenv().containsKey("WAVEFRONT_API_TOKEN")) {
            if (System.getenv().containsKey("WAVEFRONT_URI")) {
                map.put("management.metrics.export.wavefront.uri", System.getenv("WAVEFRONT_URI"));
            }
            map.put("management.metrics.export.wavefront.api-token", System.getenv("WAVEFRONT_API_TOKEN"));
        }
    }

    private void makeCloud2022WorkWithBoot3_1(Map<String, String> map) {
        map.put("spring.cloud.compatibility-verifier.compatible-boot-versions", "3.0.x,3.1.x");
    }

    private AppDeploymentRequest deploymentRequest(String appName, AppDefinition appDefinition) {
        return new AppDeploymentRequest(appDefinition, new MavenResource.Builder()
                .groupId("com.example.micrometersamples").artifactId(appName).version("0.1.0-SNAPSHOT").build());
    }

    String getLog(String id) {
        return this.appDeployer.getLog(id);
    }

}
