package com.simplecoder.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionContextTest {

    @Test
    @DisplayName("Construction defaults test: values passed to canonical constructor are accessible and unchanged")
    void testInitialConstructionValues() {
        // Given
        ExecutionContext ctx = new ExecutionContext(0, 25, false, null);
        // Then
        assertEquals(0, ctx.stepCount(), "stepCount should match constructor argument");
        assertEquals(25, ctx.maxSteps(), "maxSteps should match constructor argument");
        assertFalse(ctx.terminated(), "terminated should match constructor argument");
        assertNull(ctx.reason(), "reason should be null initially");
    }

    @Test
    @DisplayName("Max steps value is accessible independently")
    void testMaxStepsAccessible() {
        ExecutionContext ctx = new ExecutionContext(0, 10, false, null);
        assertEquals(10, ctx.maxSteps(), "maxSteps should be retrievable for later loop control");
    }
}
