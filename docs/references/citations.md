# Citations - References & Links

This document provides complete citations and traceable anchors for all conclusions.

---

## DeepWiki Sources

### Source 1: System Prompts and Models of AI Tools

**Repository**: x1xhlol/system-prompts-and-models-of-ai-tools
**DeepWiki URL**: https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools

**Query Log**:

1. **System prompt design & communication strategies**
   - Query: "What are the core system prompt design patterns and communication strategies used across different AI coding tools?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-core-system-promp_e0022435-fd4a-4294-8859-705f42cd4d04
   - Key findings: identity definition, parallel execution model, communication strategies, standardized code placeholders

2. **Tool integration & code modification strategies**
   - Query: "What are the tool integration and execution patterns? How do different systems handle code modification strategies?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-tool-integration_7419c90c-11b1-4b92-b28d-45ac213a5a7a
   - Key findings: edit_file mode, insert_edit mode, replace mode variants, file operation tools

3. **Validation & error handling**
   - Query: "What are the validation and error handling patterns used?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-validation-and-er_caed8262-89e3-419f-8194-e508dc79dc74
   - Key findings: diagnostic tools, error recovery strategies, verification loops, execution sequences

4. **Task planning & memory systems**
   - Query: "What task planning and memory systems are used?"
   - DeepWiki search URL: https://deepwiki.com/search/what-task-planning-and-memory_8a23489a-5d7a-4eee-8bad-e0c52b31e0f6
   - Key findings: TODO management, AGENTS.md, create_memory tool

**Related Wiki Pages**:
- [Major AI Development Platforms](https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools#3)
- [System Prompt Design and Communication Patterns](https://deepwiki.com/x1xhlol/system-prompts-and-models-of-ai-tools#5.5)

---

### Source 2: How to Build a Coding Agent

**Repository**: ghuntley/how-to-build-a-coding-agent
**DeepWiki URL**: https://deepwiki.com/ghuntley/how-to-build-a-coding-agent

**Query Log**:

1. **Complete architecture**
   - Query: "What is the complete architecture of the coding agent system?"
   - DeepWiki search URL: https://deepwiki.com/search/what-is-the-complete-architect_5cc97ea2-c5ff-4cf6-a9de-74b31cd913e1
   - Key findings: Agent struct, tool system design, Agent type evolution

2. **Event loop & API interaction**
   - Query: "How is the getUserMessage function and shared event loop pattern implemented?"
   - DeepWiki search URL: https://deepwiki.com/search/how-is-the-getusermessage-func_3485a7cd-5b30-458a-83bb-a5341f58dd29
   - Key findings: getUserMessage closure, event loop, tool result processing, conversation history management, API interaction

3. **Tool implementation details**
   - Query: "What are the detailed implementations of individual tools?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-detailed-implemen_02af4427-1a50-4ec4-9a03-8d80d2663051
   - Key findings: read_file, list_files, bash, edit_file, code_search implementations

4. **Development environment setup**
   - Query: "What are the development environment setup requirements?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-development-envir_0a3d3a44-b647-468c-a569-a152d9efe4fa
   - Key findings: devenv config, build system, dependency management, environment variables

**Related Wiki Pages**:
- [Agent System Architecture](https://deepwiki.com/ghuntley/how-to-build-a-coding-agent#2)
- [Agent Implementations](https://deepwiki.com/ghuntley/how-to-build-a-coding-agent#3)

---

### Source 3: Mini SWE Agent

**Repository**: SWE-agent/mini-swe-agent
**DeepWiki URL**: https://deepwiki.com/SWE-agent/mini-swe-agent

**Query Log**:

1. **Complete architecture**
   - Query: "What is the complete architecture of the SWE agent system?"
   - DeepWiki search URL: https://deepwiki.com/search/what-is-the-complete-architect_47c56d2d-1207-454c-9c7c-4942b6f109c8
   - Key findings: three‑component architecture, agent system design, model integration, execution environment, CLI & batch processing

2. **Agent step() & run() methods**
   - Query: "What are the specific patterns used in the Agent's step() and run() methods?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-specific-patterns_5dfffdca-16da-48b4-8d61-85eb4780ecc5
   - Key findings: template system, message history, exception handling, configuration management

3. **Docker & Singularity environments**
   - Query: "What are the specific implementation details of Docker and Singularity environments?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-specific-implemen_17764d06-50d6-4659-8647-24bf4fa445af
   - Key findings: DockerEnvironment lifecycle, SingularityEnvironment sandbox, SWE-Bench dataset handling, parallel processing, result persistence

4. **Interactive mode details**
   - Query: "What are the specific details about the Textual User Interface and Interactive modes?"
   - DeepWiki search URL: https://deepwiki.com/search/what-are-the-specific-details_cbd074d8-bf93-460f-9c9c-30cfcfe9931f
   - Key findings: InteractiveAgent, TextualAgent, confirm / yolo / human modes

**Related Wiki Pages**:
- [Overview](https://deepwiki.com/SWE-agent/mini-swe-agent#1)
- [Configuration System](https://deepwiki.com/SWE-agent/mini-swe-agent#6)
- [Template System](https://deepwiki.com/SWE-agent/mini-swe-agent#6.2)
- [SWE-Bench Integration](https://deepwiki.com/SWE-agent/mini-swe-agent#7)
- [Interactive Modes](https://deepwiki.com/SWE-agent/mini-swe-agent#3.3)
- [User Interfaces](https://deepwiki.com/SWE-agent/mini-swe-agent#5)
- [Textual User Interface](https://deepwiki.com/SWE-agent/mini-swe-agent#5.2)

---

## Citation Format Specification

### Source Attribution

All conclusions use the following formatting for source attribution:

**Direct quotation**:
> "You are AI coding assistant and agent manager, powered by gpt-4.1."
>
> —— Source 1: Same.dev system prompt

**Paraphrased citation**:
> Amp requires running `get_diagnostics` and any available lint/typecheck commands immediately upon task completion.
>
> —— Source 1: Amp validation rule

**Code citation**:
```go
type Agent struct {
    client         *anthropic.Client
    getUserMessage func() (string, bool)
    tools          []ToolDefinition
    verbose        bool
}
```
—— Source 2: `Agent` struct definition

### Anchor Conventions

- **Wiki page**: use DeepWiki URL + section anchor (e.g. `#3.2`)
- **Internal document**: use relative path + Markdown anchor (e.g. `../sources/notes-system-prompts.md#31-parallel-execution-model`)

---

## Citation Coverage

### Source 1 Key Conclusions

| Conclusion | Anchor |
|------|----------|
| Same.dev default parallel strategy | DeepWiki search e0022435, docs/sources/notes-system-prompts.md#121 |
| Amp TODO system | DeepWiki search 8a23489a, docs/sources/notes-system-prompts.md#13 |
| Code placeholder standardization | DeepWiki search e0022435, docs/sources/notes-system-prompts.md#22 |
| Validation enforcement rules | DeepWiki search caed8262, docs/sources/notes-system-prompts.md#23 |
| edit_file platform comparison | DeepWiki search 7419c90c, docs/sources/notes-system-prompts.md#32 |

### Source 2 Key Conclusions

| Conclusion | Anchor |
|------|----------|
| Agent event loop | DeepWiki search 3485a7cd, docs/sources/notes-coding-agent.md#12 |
| ToolDefinition structure | DeepWiki search 5cc97ea2, docs/sources/notes-coding-agent.md#21 |
| GenerateSchema implementation | DeepWiki search 02af4427, docs/sources/notes-coding-agent.md#22 |
| code_search tool | DeepWiki search 02af4427, docs/sources/notes-coding-agent.md#325 |
| devenv configuration | DeepWiki search 0a3d3a44, docs/sources/notes-coding-agent.md#42 |

### Source 3 Key Conclusions

| Conclusion | Anchor |
|------|----------|
| Three-component architecture | DeepWiki search 47c56d2d, docs/sources/notes-mini-swe-agent.md#11 |
| DefaultAgent.run() | DeepWiki search 5dfffdca, docs/sources/notes-mini-swe-agent.md#21 |
| Exception-driven control flow | DeepWiki search 5dfffdca, docs/sources/notes-mini-swe-agent.md#22 |
| Jinja2 template system | DeepWiki search 5dfffdca, docs/sources/notes-mini-swe-agent.md#31 |
| DockerEnvironment | DeepWiki search 17764d06, docs/sources/notes-mini-swe-agent.md#54 |
| SWE-Bench batch processing | DeepWiki search 17764d06, docs/sources/notes-mini-swe-agent.md#62 |
| InteractiveAgent | DeepWiki search cbd074d8, docs/sources/notes-mini-swe-agent.md#71 |
| TextualAgent | DeepWiki search cbd074d8, docs/sources/notes-mini-swe-agent.md#72 |

---

## Verification Checklist

- [x] DeepWiki URLs for all three sources recorded
- [x] Search URLs for all queries saved
- [x] Key conclusions linked to DeepWiki anchors
- [x] Citation formats unified (direct, paraphrased, code)
- [x] Internal document cross references correct

---

## References

- [Source 1: System Prompts](../sources/notes-system-prompts.md)
- [Source 2: Coding Agent](../sources/notes-coding-agent.md)
- [Source 3: Mini SWE Agent](../sources/notes-mini-swe-agent.md)
- [Architecture Patterns](../syntheses/architecture-patterns.md)
- [Code Modification Strategies](../syntheses/code-modification.md)
- [Open Questions](../syntheses/open-questions.md)
- [Glossary](./glossary.md)

**Last Updated**: 2025-10-19
