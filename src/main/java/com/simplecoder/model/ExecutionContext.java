package com.simplecoder.model;

/**
 * ExecutionContext: minimal immutable state holder for the upcoming ReAct loop skeleton.
 * Roadmap R-3 scope: structure only, no behavior / validation / mutation helpers.
 * Fields:
 *  - stepCount   : current executed step count (typically starts at 0)
 *  - maxSteps    : upper bound for loop steps (enforced in later tasks, not here)
 *  - terminated  : whether loop termination has been signaled (set later)
 *  - reason      : optional termination reason placeholder (null if none yet)
 */
public record ExecutionContext(
        int stepCount,
        int maxSteps,
        boolean terminated,
        String reason
) {}
