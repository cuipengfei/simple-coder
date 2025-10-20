package com.simplecoder.controller;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import com.simplecoder.service.AgentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for AgentController without Spring context.
 */
class AgentControllerTest {

    @Test
    @DisplayName("POST /api/agent explicit toolType read returns prefixed message")
    void testExplicitRead() {
        AgentService service = mock(AgentService.class);
        ToolRequest req = ToolRequest.builder().prompt("Read src/Main.java").toolType("read").build();
        ToolResponse serviceResp = ToolResponse.success("[tool=read] Read src/Main.java (lines 1-10 of 200 total)");
        when(service.process(req)).thenReturn(serviceResp);

        AgentController controller = new AgentController(service);
        var respEntity = controller.handle(req);
        ToolResponse resp = respEntity.getBody();

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertTrue(resp.getMessage().startsWith("[tool=read]"));
    }

    @Test
    @DisplayName("POST /api/agent auto mode error path returns failure")
    void testAutoError() {
        AgentService service = mock(AgentService.class);
        ToolRequest req = ToolRequest.builder().prompt("something ambiguous").toolType("auto").build();
        ToolResponse errorResp = ToolResponse.error("AgentService error", "Invalid tool selection");
        when(service.process(req)).thenReturn(errorResp);

        AgentController controller = new AgentController(service);
        var respEntity = controller.handle(req);
        ToolResponse resp = respEntity.getBody();

        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertEquals("AgentService error", resp.getMessage());
        assertTrue(resp.getError().contains("Invalid tool selection"));
    }
}
