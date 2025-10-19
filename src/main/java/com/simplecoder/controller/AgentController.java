package com.simplecoder.controller;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import com.simplecoder.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing single-turn agent endpoint.
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    public ResponseEntity<ToolResponse> handle(@RequestBody ToolRequest request) {
        log.info("Incoming agent request toolType='{}' prompt='{}'", request.getToolType(), abbreviate(request.getPrompt()));
        ToolResponse response = agentService.process(request);
        return ResponseEntity.ok(response);
    }

    private String abbreviate(String s) {
        if (s == null) return "";
        return s.length() <= 120 ? s : s.substring(0, 117) + "...";
    }
}
