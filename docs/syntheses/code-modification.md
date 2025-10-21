# Code Modification Strategies

This document compares code modification tools, patterns, and best practices across different systems.

---

## 1. Modification Tool Landscape

### 1.1 Tool Matrix

| Tool Type | Source 1 Platform Examples | Source 2 | Source 3 |
|----------|-------------------|----------|----------|
| **Exact string replacement** | edit_file (Amp, Same.dev, Orchids, Qoder, Claude Code) | edit_file (planned) | None (bash only) |
| **Insert edit** | insert_edit_into_file (VSCode Agent) | - | - |
| **Line number replace** | lov-line-replace (Lovable) | - | - |
| **Search replace** | search_replace (Qoder), string_replace (Same.dev) | - | - |
| **Block content replace** | replace_file_content (Windsurf) | - | - |
| **XML substring replace** | proposed_file_replace_substring (Replit) | - | - |
| **File create/overwrite** | create_file (Amp, Qoder), lov-write (Lovable) | - | - |

---

## 2. edit_file Pattern Details

### 2.1 Core Parameters

**Generic signature**:
```
edit_file(
    file_path: string,
    old_str: string,
    new_str: string,
    replace_all: bool = false  // optional
)
```

### 2.2 Platform Implementation Comparison

#### Amp

**Traits**:
- Returns git-style diff
- `old_str` must exist in file
- `old_str` and `new_str` must differ
- `replace_all` replaces all matches

**Limitations**:
- For full-file replacement prefer `create_file`

**Error Handling**:
- `old_str` absent → error
- `old_str == new_str` → error

#### Same.dev

**Usage**:
- Use when file < 2500 lines

**Flag**:
- `smart_apply`: allows edit retry

**Alternatives**:
- File > 2500 lines → use `string_replace`
- Small edits also recommend `string_replace`

#### Orchids.app

**Placeholder Convention**:
```typescript
// Before modification
function example() {
    // ... keep existing code ...
    const result = computeSomething();
    // ... rest of code ...
    return result;
}

// After modification
function example() {
    // ... keep existing code ...
    const result = computeOptimized();  // only this line changed
    // ... rest of code ...
    return result;
}
```

**Truncation Comments**:
- `// ... keep existing code ...`
- `// ... rest of code ...`

**Code Deletion**:
- Specific instructions provided to remove segments

#### Qoder

**Preference**:
- Strongly prefers `search_replace` over `edit_file` (unless explicit instruction)

**Placeholder**:
- `// ... existing code ...`

**Usage**:
- Propose edits to existing file

#### Claude Code 2.0

**Emphasis**:
- **Preserve exact indentation**: when copying `old_string` from `Read` tool output keep exact indentation after line number prefix.
- **Uniqueness requirement**: ensure `old_string` unique or use `replace_all`.

**Best Practices**:
```python
# Read tool output
    1→def example():
    2→    result = old_value  # note: 2 spaces indentation
    3→    return result

# old_string (correct) - preserves exact indentation
old_string = "    result = old_value"  # 4 spaces (after line number)

# old_string (incorrect) - indentation mismatch
old_string = "  result = old_value"  # 2 spaces
```

### 2.3 Source 2: edit_file Design

**Input Struct**:
```go
type EditFileInput struct {
    Path   string `json:"path"`
    OldStr string `json:"old_str"`
    NewStr string `json:"new_str"`
}
```

**Functional Description** (from README):
- Handle file creation
- Handle directory creation
- String replacement with uniqueness validation

**Note**: Full Go implementation not included in snippet

---

## 3. insert_edit Pattern

### 3.1 VSCode Agent Implementation

**Tool Name**: `insert_edit_into_file`

**Parameters**:
```
{
    explanation: string,  // edit explanation
    filePath: string,
    code: string
}
```

**Design Philosophy**:
- "Smart" tool; needs minimal prompting
- Automatically infers insertion position

**Placeholder Convention**:
```javascript
// ...existing code...
NEW CODE INSERTED HERE
// ...existing code...
```

**Example**:
```javascript
// Original file
function calculate() {
    const a = 1;
    const b = 2;
    return a + b;
}

// code parameter for insert_edit_into_file
function calculate() {
    const a = 1;
    // ...existing code...
    console.log('Debugging:', a, b);  // inserted line
    // ...existing code...
}
```

---

## 4. replace Mode Variants

### 4.1 Line-number Replace (Lovable)

**Tool Name**: `lov-line-replace`

**Parameters**:
```
{
    file_path: string,
    search: string,
    first_replaced_line: number,
    last_replaced_line: number,
    replace: string
}
```

**Traits**:
- Explicit line numbers: precise replacement range
- Ellipsis support: large unchanged sections use `...`

**Example**:
```python
# Original file (line numbers)
1: def process_data(data):
2:     validate(data)
3:     transform(data)
4:     save(data)
5:     return data

# lov-line-replace invocation
{
    "search": "validate(data)\n    transform(data)",
    "first_replaced_line": 2,
    "last_replaced_line": 3,
    "replace": "# Data processing steps\n    validate_and_transform(data)"
}

# Result
1: def process_data(data):
2:     # Data processing steps
3:     validate_and_transform(data)
4:     save(data)
5:     return data
```

### 4.2 String Replace (Same.dev)

**Tool Name**: `string_replace`

**Usage**:
- File > 2500 lines
- Small edits (even if file small)

**Parameters**: same as `edit_file`

### 4.3 Block Content Replace (Windsurf)

**Tool Name**: `replace_file_content`

**Parameters**:
```
{
    file_path: string,
    replacements: ReplacementChunk[],
}

ReplacementChunk {
    target_content: string,   // target content to replace
    new_content: string       // new content
}
```

**Traits**:
- Supports multiple replacement chunks
- `TargetContent` exact match

### 4.4 Search Replace (Qoder)

**Tool Name**: `search_replace`

**Purpose**: Efficient string replacement in design docs

**Parameters**:
```
{
    file_path: string,
    replacements: [
        {
            original_text: string,
            new_text: string,
            replace_all: bool = false  // optional
        },
        ...
    ]
}
```

**Strict Requirements**:
1. **Uniqueness**: `original_text` must appear exactly once (unless `replace_all: true`)
2. **Exact match**: must match including whitespace and newlines
3. **Ordered processing**: replacements applied in array order

**Example**:
```json
{
  "file_path": "design.md",
  "replacements": [
    {
      "original_text": "## Old Section Title",
      "new_text": "## New Section Title"
    },
    {
      "original_text": "- Feature A\n- Feature B",
      "new_text": "- Feature A (enhanced)\n- Feature B\n- Feature C (new)",
      "replace_all": false
    }
  ]
}
```

### 4.5 XML Substring Replace (Replit)

**Tool Name**: `<proposed_file_replace_substring>`

**XML Structure**:
```xml
<proposed_file_replace_substring>
    <file_path>path/to/file.js</file_path>
    <change_summary>Brief description of change</change_summary>
    <old_str>code to replace</old_str>
    <new_str>new code</new_str>
</proposed_file_replace_substring>
```

**Also**: `<proposed_file_replace>` replaces entire file content

---

## 5. File Create/Overwrite

### 5.1 create_file（Amp）

**Purpose**:
- Create new file
- Overwrite existing file

**Parameters**:
```
{
    file_path: string,
    content: string
}
```

**Usage**:
- Replace entire file content (better than `edit_file`)

### 5.2 create_file（Qoder）

**Purpose**: Create new design file

**Limit**: content up to 600 lines

### 5.3 lov-write（Lovable）

**Purpose**:
- Mainly for creating new file
- Fallback when `lov-line-replace` fails

**Placeholder Encouragement**:
```javascript
function example() {
    // ... keep existing code
    newCode();
    // ... keep existing code
}
```

---

## 6. Placeholder Convention Comparison

### 6.1 Placeholder Variants

| Platform | Placeholder Format | Purpose |
|------|------------|------|
| **Qoder** | `// ... existing code ...` | unchanged code |
| **Same.dev** | `// ... existing code ...` | same |
| **Trae** | `// ... existing code ...` | same |
| **VSCode Agent** | `// ...existing code...` | same (no spaces) |
| **Orchids.app** | `// ... keep existing code ...` <br> `// ... rest of code ...` | explicit keep / rest code |
| **Lovable** | `// ... keep existing code` | simplified version |

### 6.2 Best Practices

**When to Use Placeholders**:
- Editing large files; show only nearby code
- Reduce token consumption
- Improve readability

**When to Avoid Placeholders**:
- Small files (< 50 lines)
- Edits dispersed in many places
- Need full context to understand change

**Example (Recommended)**:
```python
# Large file edit (300 lines)
def complex_function():
    # ... existing code ...

# Only this section changed
    new_logic = optimized_approach()

    # ... existing code ...
```

**Example (Not Recommended)**:
```python
# Small file edit (10 lines)
def simple_function():
# ... existing code ...  # meaningless here; show full content instead
    result = new_value
    # ... existing code ...
```

---

## 7. Source 3: Bash Native Modification

### 7.1 No Tool Abstraction

mini-swe-agent **provides no code modification tools**, only bash commands:

```bash
# sed replacement
sed -i 's/old_text/new_text/g' file.py

# create file with cat + heredoc
cat > file.py << 'EOF'
def new_function():
    pass
EOF

# append with echo
echo "new_line" >> file.py

# edit with vim/ed (interactive)
# not suitable for agent (needs automation)
```

### 7.2 Advantages & Disadvantages

**Advantages**:
- Minimal: no tool abstraction definition needed
- Flexible: any bash command usable
- General: not tied to specific API

**Disadvantages**:
- Error prone: string escaping & regex complexity
- Hard to validate: no type checking
- Lower readability: bash syntax less intuitive for LLM & humans

---

## 8. Modification Strategy Decision Tree

### 8.1 File Size Driven

```
File size < 2500 lines?
├─ Yes → edit_file
└─ No → string_replace or line-replace
```

**Source**: Same.dev explicit threshold

### 8.2 Modification Scope Driven

```
Modification type?
├─ Single-point exact replace → edit_file / search_replace
├─ Consecutive block replace → lov-line-replace
├─ Multiple dispersed replaces → search_replace (array)
├─ Full file rewrite → create_file / proposed_file_replace
└─ Insert new code → insert_edit_into_file
```

### 8.3 Validation Need Driven

```
Need uniqueness guarantee?
├─ Yes → edit_file (default) / search_replace (replace_all: false)
└─ No  → edit_file (replace_all: true) / bash sed
```

---

## 9. Error Handling & Validation

### 9.1 Pre-Edit Validation

| Platform | Validation Mechanism | Error Handling |
|----------|----------------------|----------------|
| **Amp** | `old_str` existence check | Absent → error |
| **Qoder** | `original_text` uniqueness check | Non-unique → error (unless replace_all) |
| **Claude Code** | Exact indentation match | Mismatch → replace failure |

### 9.2 Post-Edit Validation

**Mandatory Validation** (all platforms):
1. **Same.dev**: `run_linter` after each edit
2. **Amp**: `get_diagnostics` after task completion
3. **VSCode Agent**: `get_errors` after file edit

**Validation Loop**:
```
Modify code → validation tool → errors found
    ↓                         ↓
 Continue next ←───── Fix ←────┘
                  ↑
                  └── Up to 3 attempts then escalate to user
```

---

## 10. Key Insights

### 10.1 Tool Selection Strategy

| Scenario | Recommended Tool | Reason |
|------|----------|------|
| Small file global replace | `edit_file` + `replace_all` | Simple & direct |
| Large file single-point edit | `string_replace` | Performance optimization |
| Known line number edit | `lov-line-replace` | Precise control |
| Multiple related edits | `search_replace` (array) | Atomic operation |
| Full file rewrite | `create_file` | Avoid complex replacement logic |
| Insert new code | `insert_edit_into_file` | Smart position inference |

### 10.2 Placeholder Usage Principles

1. Use only when needed: small files (< 50 lines) show full content
2. Maintain consistency: uniform placeholder format across project
3. Clarify boundaries: `... keep existing code ...` clearer than `...`
4. Avoid deep nesting: do not place placeholders inside placeholders

### 10.3 Validation-Driven Development

**Golden Rule**:
> Never skip post-edit validation even if you are "sure" code is correct.

**Reasons**:
- LLM may produce syntax errors
- Indentation/spacing issues are subtle
- Cross-file dependencies may break

**Validation Order**:
1. Syntax check (linter)
2. Type check (tsc, mypy)
3. Build test (build)
4. Unit tests (test)

---

## References

- [Source 1: System Prompts - Tool Integration](../sources/notes-system-prompts.md#3-tool-integration--execution-patterns)
- [Source 2: Coding Agent - Edit Tool](../sources/notes-coding-agent.md#321-edit_file)
- [Source 3: Mini SWE Agent - Bash Commands](../sources/notes-mini-swe-agent.md#71-no-tool-abstraction)

**Last Updated**: 2025-10-19
