# Skill: api-to-mcp

## When to use it

Use this skill when:
- a source API already exists
- the target repository must expose that capability through MCP
- the main design question is how to translate an API contract into the right MCP primitive

This skill applies to repositories adopting the **API -> MCP** mode on top of the Clean Architecture baseline.

## Objective

Translate an API contract into an MCP experience **without breaking Clean Architecture responsibilities**.
The goal is not only to expose something through MCP, but to decide correctly:
- what becomes a **Tool**
- what becomes a **Resource**
- what becomes a **Prompt**
- what domain use case supports each exposed capability
- what auth, error, resilience, and output rules must remain explicit

## Expected input

Before using this skill, you should have at least:
- a source API contract or operation inventory
- a clear initiative spec
- a known repository baseline state
- a target MCP delivery need
- an initial view of the required business use cases and integrations

Useful references before proceeding:
- `AGENT.md`
- `docs/sdd/README.md`
- `docs/playbooks/scaffold-baseline.md`
- `docs/templates/api-contract-to-mcp.md`
- `skills/scaffold-generation/SKILL.md`

## Expected output

A clear API -> MCP mapping that makes explicit:
- which API capability maps to which MCP primitive
- which use case supports it
- which gateway and adapter path it depends on
- which validations, errors, metadata, and controls must be preserved
- why the chosen MCP primitive is the best fit

The expected result is a **traceable mapping** that reduces ambiguity before implementation.

## Target structural pattern

The MCP primitive must not connect directly to the external integration.
The target pattern is:
`Tool/Resource/Prompt -> Use Case -> Gateway -> Adapter -> External API`

If the scaffold already generated MCP placeholders, treat them only as structural references, not as the final business design.

## Classification heuristics
Use the simplest primitive that matches the business need.

### Tool
Prefer **Tool** when the capability:
- performs an action
- changes state
- starts a business flow
- requires meaningful validation
- may produce side effects
- must behave as an explicit operation from the client perspective

### Resource
Prefer **Resource** when the capability:
- queries information
- is idempotent or predominantly read-only
- returns reusable context
- represents documentation, catalogs, configuration, or snapshots
- is more useful as navigable data than as an invoked command

### Prompt
Prefer **Prompt** when the capability:
- provides guided instructions
- needs conversational templates
- organizes context for an MCP or LLM client
- helps a user perform a task indirectly rather than executing the task itself

## Procedure
### 1. Inventory the source contract
List endpoints, inputs, outputs, intent, and constraints. Do not start from the generated MCP classes.
### 2. Identify business intent
Clarify what business need it serves and whether it must preserve validation/metadata.
### 3. Classify each capability
Use the heuristics. A read-only API capability may still be exposed first as a **Tool** when client behavior, authorization strategy, or product constraints justify it. Ensure you explicitly report this.
### 4. Define the domain path
Specify the use case, gateway, adapter, and external API. Do not skip the use case.
### 5. Preserve capability-level controls
Clarify required metadata: Authentication, Authorization (e.g., Roles), Timeout expectations, Resilience, and Audit.
### 6. Define Output and Error Policies
Understand exactly what output formatting the LLM client needs (JSON/Text). Verify degraded message scenarios.
### 7. Complete the API -> MCP Matrix
Use the template matrix to finalize this design before writing code.

## Design Rules
- Do not let MCP exposure types dictate domain language.
- Consolidate matching business operations into one use cases when applicable.
- Evaluate Prompt seriously before defaulting to Tool for guidance tasks.
- Placeholder capabilities (e.g. Scaffolder defaults) must be cleaned out.

## Extracted example reference
The original baseline repository provided applied evidence for this skill:
- A read-only upstream API capability was exposed as a **Tool** because it perfectly mapped the intended LLM flow and authorization boundary.
- Upstream metadata preservation was critical for compliance.
- Scaffolder placeholder capabilities were successfully purged.
