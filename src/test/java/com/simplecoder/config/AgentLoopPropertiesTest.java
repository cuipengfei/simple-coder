package com.simplecoder.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-5 Property load test for simple-coder.agent.max-steps.
 * Uses ApplicationContextRunner for lightweight property binding verification.
 */
class AgentLoopPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("simple-coder.agent.max-steps=10");

    @EnableConfigurationProperties(AgentLoopProperties.class)
    static class TestConfig { }

    @Test
    @DisplayName("Property simple-coder.agent.max-steps binds to AgentLoopProperties.maxSteps")
    void propertyLoadsSuccessfully() {
        contextRunner.run(ctx -> {
            AgentLoopProperties props = ctx.getBean(AgentLoopProperties.class);
            assertThat(props.getMaxSteps()).isEqualTo(10);
        });
    }
}
