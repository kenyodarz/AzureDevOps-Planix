# API Contract to MCP Template

## 1. Metadata

- Initiative name:
- Source API:
- Contract version:
- Status:

## 2. Source contract summary

- API type: REST | GraphQL | SOAP | internal | external
- base path:
- authentication:
- payload format:

## 3. Assumptions

- 

## 3.1 Current technical baseline

- Does the initiative start from an already generated MCP baseline?: yes | no
- Which MCP capabilities are already enabled in `application.yaml`?: tools | resources | prompts | completion
- Which real business capabilities already exist in the repo?:
- Which scaffold placeholders will be replaced in this initiative?:
- Which scaffold placeholders will be intentionally retained outside the critical path?:
- Is a new driven adapter required?: yes | no
- Are new domain model and use case artifacts required?: yes | no

## 4. Operation inventory

| ID | Source operation | Method / verb | Route or identifier | Objective | Notes |
|----|------------------|---------------|---------------------|-----------|-------|
| API-01 |  |  |  |  |  |

## 5. MCP mapping rules

### Tool

Use it when the operation:

- performs an action
- changes state
- triggers business logic
- requires strong validation or side effects

### Resource

Use it when the operation:

- exposes queryable information
- is mostly read-only
- is stable or idempotent
- provides useful context to the MCP client

### Prompt

Use it when the operation:

- guides interactions
- encapsulates conversational context
- builds reusable instructions

## 6. API -> MCP matrix

| API ID | MCP Primitive | Proposed MCP name | Associated use case | Main input | MCP output type | Main output | Required role | Sensitivity | Timeout | Error/Fallback | Resilience | Critical config | Placeholder impact | Notes |
|--------|---------------|-------------------|---------------------|------------|-----------------|-------------|---------------|-------------|---------|----------------|------------|-----------------|--------------------|-------|
| API-01 | Tool/Resource/Prompt |  |  |  | text/JSON/resource/prompt |  |  |  |  |  |  |  | replace/retain/n-a |  |

## 7. Derived domain design

### Candidate models

- 

### Candidate use cases

- 

### Candidate gateways or adapters

- 

### MCP placeholders to replace

- 

### MCP placeholders intentionally retained

- 

## 8. Security considerations

- authentication
- authorization
- sensitive data
- audit

## 9. Observability

- logs
- metrics
- traces
- audit events

## 9.1 Output toward the MCP client

- response type: text | JSON | resource | structured prompt
- output formatting rule:
- metadata that must be preserved:
- allowed error degradation:
- errors that must not be degraded:
- source fields that must remain verbatim:

## 10. Scaffold generation plan

- models to generate:
- use cases to generate:
- entry point to generate:
- adapters to generate:
- additional wiring / configuration to adjust:

## 11. Acceptance criteria

- [ ] Every relevant operation was classified as Tool, Resource, or Prompt.
- [ ] Every MCP primitive is traceable to a use case.
- [ ] The mapping preserves authentication, validation, and source contract errors.
- [ ] Authorization, resilience, and MCP output policy were defined per capability.
- [ ] Baseline state and placeholder impact were made explicit before implementation.
- [ ] Critical configuration keys were identified for each external-API-backed capability.

## 12. Open questions

- 

