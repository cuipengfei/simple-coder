package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import com.simplecoder.tool.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentService routing logic (explicit and auto modes).
 */
class AgentServiceTest {

    private Tool readTool;
    private Tool searchTool;
    private ToolSelectionStrategy strategy;

    @BeforeEach
    void setUp() {
        readTool = Mockito.mock(Tool.class);
        when(readTool.getName()).thenReturn("read");
        searchTool = Mockito.mock(Tool.class);
        when(searchTool.getName()).thenReturn("search");
        strategy = Mockito.mock(ToolSelectionStrategy.class);
    }

    private AgentService buildService(Map<String, Tool> tools) {
        return new AgentService(tools, strategy);
    }

    @Test
    @DisplayName("Explicit toolType=read routes to read tool and prefixes message")
    void testExplicitReadToolSuccess() {
        ToolRequest req = ToolRequest.builder()
                .prompt("Read src/Main.java")
                .toolType("read")
                .build();
        when(readTool.execute(req)).thenReturn(ToolResponse.success("Read src/Main.java (lines 1-10 of 200 total)", "content"));

        AgentService service = buildService(Map.of("read", readTool));
        ToolResponse resp = service.process(req);

        assertTrue(resp.isSuccess());
        assertNotNull(resp.getMessage());
        assertTrue(resp.getMessage().startsWith("[tool=read]"), "Message should be prefixed with tool name");
    }

    @Test
    @DisplayName("Fallback routing: bean name differs from tool.getName()")
    void testFallbackRoutingByToolGetName() {
        ToolRequest req = ToolRequest.builder()
                .prompt("Read src/Main.java")
                .toolType("read")
                .build();
        // Simulate Spring bean name 'readFileTool' while protocol name is 'read'
        Map<String, Tool> beanNameMap = Map.of("readFileTool", readTool);
        when(readTool.execute(req)).thenReturn(ToolResponse.success("Read src/Main.java (lines 1-10 of 200 total)", "content"));

        AgentService service = buildService(beanNameMap);
        ToolResponse resp = service.process(req);

        assertTrue(resp.isSuccess(), "Should succeed via fallback");
        assertTrue(resp.getMessage().startsWith("[tool=read]"), "Fallback should still prefix with logical tool name");
    }

    @Test
    @DisplayName("Auto mode selects search and executes search tool")
    void testAutoSelectSearch() {
        ToolRequest req = ToolRequest.builder()
                .prompt("Search 'Agent' in src/")
                .toolType("auto")
                .build();
        when(strategy.selectTool(req)).thenReturn("search");
        when(searchTool.execute(req)).thenReturn(ToolResponse.success("Found 3 matches for 'Agent' in src/", java.util.List.of("A", "B", "C")));

        AgentService service = buildService(Map.of("search", searchTool));
        ToolResponse resp = service.process(req);

        assertTrue(resp.isSuccess());
        assertTrue(resp.getMessage().startsWith("[tool=search]"));
    }

    @Test
    @DisplayName("Auto mode invalid strategy output -> error response")
    void testAutoInvalidStrategyOutput() {
        ToolRequest req = ToolRequest.builder()
                .prompt("Something ambiguous")
                .toolType("auto")
                .build();
        when(strategy.selectTool(req)).thenThrow(new IllegalArgumentException("Invalid tool selection: foo"));

        AgentService service = buildService(Map.of("read", readTool));
        ToolResponse resp = service.process(req);

        assertFalse(resp.isSuccess());
        assertEquals("AgentService error", resp.getMessage());
        assertNotNull(resp.getError());
        assertTrue(resp.getError().contains("Invalid tool selection"));
    }

    @Test
    @DisplayName("Unknown explicit toolType returns error response")
    void testUnknownToolType() {
        ToolRequest req = ToolRequest.builder()
                .prompt("Do something")
                .toolType("nonexistent")
                .build();
        AgentService service = buildService(Map.of("read", readTool));
        ToolResponse resp = service.process(req);

        assertFalse(resp.isSuccess());
        assertEquals("Unknown tool: nonexistent", resp.getMessage());
    }

    @Test
    @DisplayName("Tool execution throws exception -> error response")
    void testToolExecutionException() {
        ToolRequest req = ToolRequest.builder()
                .prompt("Read src/Main.java")
                .toolType("read")
                .build();
        when(readTool.execute(req)).thenThrow(new RuntimeException("IO failure"));

        AgentService service = buildService(Map.of("read", readTool));
        ToolResponse resp = service.process(req);

        assertFalse(resp.isSuccess());
        assertEquals("AgentService error", resp.getMessage());
        assertTrue(resp.getError().contains("IO failure"));
    }
}
