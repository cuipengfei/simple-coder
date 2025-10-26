package com.simplecoder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds agent loop related configuration properties.
 * Roadmap R-5 scope: only maxSteps exposure; no validation or loop logic here.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "simple-coder.agent")
public class AgentLoopProperties {

    /**
     * Maximum number of steps the ReAct loop is allowed to execute.
     * Default supplied via application.yml (simple-coder.agent.max-steps).
     */
    private int maxSteps;

}
