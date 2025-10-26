package com.simplecoder.service;

import com.simplecoder.model.ExecutionStep;

import java.util.List;

/**
 * Result container for a completed loop run.
 * R-6 scope: holds steps + pre-formatted aggregated text.
 */
public record LoopResult(List<ExecutionStep> steps, String aggregated, com.simplecoder.model.ExecutionContext finalContext) {}
