package com.simplecoder.service;

import com.simplecoder.exception.RecoverableException;
import com.simplecoder.exception.TerminalException;

/**
 * Functional seam representing a single reasoning→tool→observation turn.
 *
 * R-6 scope: minimal single method returning a textual result for a prompt.
 * R-8 scope: may throw RecoverableException (loop continues) or TerminalException (loop aborts).
 * IF-4: Returns structured result including tool metadata for accurate loop reporting.
 * Implementations may be lambdas in tests.
 */
@FunctionalInterface
public interface SingleTurnExecutor {

    /**
     * Execute one turn given the current (user or loop) prompt.
     * @param prompt textual prompt/context driving the action
     * @return TurnResult containing textual summary and tool name (if tool was executed)
     * @throws RecoverableException when execution fails but loop can continue with error feedback
     * @throws TerminalException when execution fails and loop must terminate immediately
     */
    TurnResult execute(String prompt) throws RecoverableException, TerminalException;
}
