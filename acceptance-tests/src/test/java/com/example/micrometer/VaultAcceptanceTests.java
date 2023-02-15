package com.example.micrometer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.util.Map;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"io.micrometer.samples.rebuild-projects=true", "io.micrometer.samples.project-root=${user.home}/repo/micrometer-samples"}
)
@Testcontainers
// @formatter:on
class VaultAcceptanceTests extends AcceptanceTestsBase {

    @Container
    static VaultContainer vault = new VaultContainer("vault:1.7.0").withVaultToken("vault-plaintext-root-token");

    @Test
    void should_pass_tracing_context_with_vault(TestInfo testInfo) {
        // when
        String producerId = deploy(testInfo, "vault-resttemplate", port());

        // then
        assertThatTraceIdGotPropagated(producerId);
    }

    @Disabled("Doesn't work on CI, works locally")
    @Test
    void should_pass_tracing_context_with_vault_reactive(TestInfo testInfo) {
        // when
        String producerId = deploy(testInfo, "vault-webclient", port());

        // then
        assertThatTraceIdGotPropagated(producerId);
    }

    private Map<String, String> port() {
        return Map.of("spring.cloud.vault.port", String.valueOf(vault.getFirstMappedPort()));
    }

}
