# Overlay: API

## Purpose

This overlay groups the guidance needed when a repository must expose or refine a REST API on top of the Bancolombia scaffold.

It exists outside the core because not every repository needs:

- REST endpoint design decisions
- HTTP contract-specific rules
- REST baseline bootstrap guidance
- API-focused route, validation, and output decisions

## When to adopt it

Adopt this overlay when the target repository needs all or most of the following:

- a REST API entry point as the primary delivery channel
- explicit API contract design before implementation
- endpoint-level validation, error, and output decisions
- scaffold-guided generation for REST structure

This overlay corresponds to the **API-only** adoption mode.

## What this overlay adds

Compared to the core package, this overlay adds guidance for:

- designing traceable REST contracts before coding
- deciding resource-oriented vs action-oriented endpoints
- mapping endpoints to use cases, gateways, and adapters
- treating REST entry-point generation as structural bootstrap rather than business completion
- keeping API output, validation, config, and observability explicit

## Reusable rules

### 1. Start from the business capability, not from routes

Do not begin API design from controller classes or transport naming.

Start from:

- business objective
- target consumer
- expected capability
- expected output

### 2. Every endpoint must map cleanly to architecture

Preferred pattern:

`REST endpoint -> Use Case -> Gateway -> Adapter -> External dependency`

Do not map endpoint -> business logic directly.

### 3. Choose resource-oriented vs action-oriented design deliberately

#### Resource-oriented endpoint

Prefer it when the capability:

- primarily exposes data
- is retrievable, listable, or queryable
- should remain predictable and idempotent
- is best understood as a stable representation

#### Action-oriented endpoint

Prefer it when the capability:

- triggers business behavior
- validates intent or commands
- may create side effects
- is better represented as an operation than as a retrievable resource

### 4. Transport design should express intent clearly

HTTP method and route shape should help the consumer predict behavior.

Reusable rules:

- retrieval flows should look like reads
- business actions should not be disguised as generic reads
- route names should reflect business language rather than infrastructure names
- the API surface should be as small as possible while still expressing the real business need

### 5. Contract decisions must be explicit before implementation

For each endpoint, define at least:

- validation rules
- error policy
- output formatting
- metadata preservation
- authorization and audit expectations
- critical configuration

If the API depends on external systems, make timeout and resilience expectations explicit too.

### 6. Scaffold generation is structural follow-up, not design replacement

If the repository lacks the required REST baseline or supporting structure, scaffold generation is valid.

But generation does **not** mean the business slice is complete.

Preferred sequence:

- clarify the contract first
- generate only the missing structure
- review generated files immediately
- refine generated output into business-aligned code

### 7. Placeholders and samples are not business implementation

If the scaffold generates sample endpoints, sample controllers, or starter code, treat them as bootstrap artifacts only.

A real slice should:

- replace the pieces it owns
- isolate any sample code not yet replaced
- avoid leaving placeholders in the business-critical path

## Minimum adoption checklist

- [ ] The repository really needs REST API exposure.
- [ ] The initiative includes `api-contract.md`.
- [ ] Every relevant endpoint is mapped to a use case.
- [ ] Resource-oriented vs action-oriented decisions are explicit.
- [ ] Validation, error, auth, and output policy are explicit per endpoint.
- [ ] Critical configuration is identified for externally backed flows.
- [ ] Scaffold generation is justified only for missing structure.
- [ ] Placeholder impact is explicit before implementation.

## What stays example-only

The following do **not** belong in the reusable rule set unless the target repo explicitly adopts the same context:

- concrete endpoint paths from one repository
- one repository's exact DTOs, controller names, or adapter names
- one repository's exact output formatting choices
- one repository's exact error messages
- one repository's exact YAML keys, role names, or environment assumptions
- business-slice-specific transport decisions

That material belongs in examples or repo-specific runtime documentation, not in the reusable overlay.

## How it aligns with the package

Use this overlay together with:

- `../../AGENT.md`
- `../../docs/playbooks/scaffold-baseline.md`
- `../../docs/sdd/README.md`
- `../../docs/templates/api-contract.md`
- core skills in `../../skills/`

This overlay specializes the package for REST API delivery.
It does not replace:

- spec intake
- scaffold baseline verification
- traceability closure
- repository-specific runtime validation

If the repository later needs MCP exposure too, combine this overlay with:

- `../api-to-mcp/README.md`

## Source references during preparation

Until the standalone portable repo is finalized, the source material for this overlay remains in the original repository under:

- `../../../../skills/api-design/SKILL.md`
- `../../../../skills/scaffold-generation/SKILL.md`
- `../../../../docs/sdd/README.md`
- `../../../../docs/scaffold-baseline.md`
- `../../../../docs/examples/untitled/`

## Boundary rule

If an example from one repository conflicts with the reusable guidance in this overlay, prefer the reusable guidance and treat the example as context-specific evidence.
