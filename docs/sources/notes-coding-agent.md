# Source 2: How to Build a Coding Agent (English Version)

**Source**: ghuntley/how-to-build-a-coding-agent
**DeepWiki**: https://deepwiki.com/ghuntley/how-to-build-a-coding-agent

This file replaces prior mixed-language sections with full English equivalents while preserving original technical meaning.

## 5.2 Error Handling Patterns

- Unmarshal failure:
  - `read_file` / `list_files`: `panic` (assumes schema validation ensures legality)
  - `bash` / `code_search`: return error (more conservative)
- Tool execution failure:
  - Log: `log.Printf("Error: %v", err)`
  - Return formatted error message + error object
  - `code_search` special case: exit code 1 treated as "no match" (not an error)

## 5.3 Centralized Tool Execution

In `Agent.Run()`:
- Iterate `message.Content` blocks
- Detect `tool_use` blocks
- Lookup `ToolDefinition` by `Name`
- Execute tool function with parsed input
- Collect results into `ToolResultBlock`
- Send results back as a user message

## 6. Key Observations

### 6.1 Design Advantages
- Type safety: Go types + JSON Schema generation
- Progressive enhancement: path from simple Chat to Code Search
- Simplicity: clear event loop
- Modular tools: independent, extensible definitions

### 6.2 Implementation Details
- `getUserMessage` closure: reads `os.Stdin` via `bufio.Scanner`
- Conversation persistence: full slice passed each API call
- Feedback loop: tool output fed back as user message

### 6.3 Limitations & Caveats
- `edit_file` full implementation absent (only input struct provided)
- Some tools shell out (`find`, `rg`) instead of pure stdlib
- Mixed error handling (panic vs returned error)

**Last Updated**: 2025-10-19
