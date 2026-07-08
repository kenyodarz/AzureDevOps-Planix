# Overlay: API to MCP

## Purpose

This overlay groups the guidance needed when a repository must translate an existing API contract into an MCP experience.

It exists outside the core because not every repository needs:

- MCP primitive classification
- MCP-specific output decisions
- MCP transport compatibility rules
- MCP-specific placeholder cleanup

## When to adopt it

Adopt this overlay when the target repository needs all or most of the following:

- an upstream API contract already exists
- the target exposure is MCP
- the main design question is not only what the endpoint does, but what MCP primitive is the best fit
- the repository must decide explicit Tool / Resource / Prompt behavior

This overlay corresponds to the **API -> MCP** adoption mode.

## What this overlay adds

Compared to the core package, this overlay adds guidance for:

- classifying capabilities as **Tool**, **Resource**, or **Prompt**
- mapping upstream operations into MCP primitives without skipping Clean Architecture
- deciding MCP output type and compatibility behavior
- documenting capability-level auth, error, resilience, and metadata preservation
- treating MCP placeholders as structural bootstrap, not final business exposure

## Reusable rules

### 1. The MCP primitive is not the business layer

The primitive must not connect directly to the external integration.

Preferred pattern:

`Tool/Resource/Prompt -> Use Case -> Gateway -> Adapter -> External API`

### 2. Choose the simplest primitive that matches the business need

#### Tool

Prefer **Tool** when the capability:

- performs an action
- starts a business flow
- requires meaningful validation
- may cause side effects
- is most useful as an explicit operation from the client perspective

#### Resource

Prefer **Resource** when the capability:

- is read-only or predominantly idempotent
- returns reusable context or data
- is better navigated than invoked
- represents documentation, catalogs, configuration, or snapshots

#### Prompt

Prefer **Prompt** when the capability:

- organizes guidance for the client
- provides reusable instructions or templates
- supports the user indirectly instead of executing the business task itself

### 3. Do not apply MCP heuristics mechanically

A read-only API capability may still be exposed first as a **Tool** if client behavior, authorization strategy, or product constraints justify it.

If that happens, document the reason explicitly in the spec.

### 4. Output and error policy must be explicit

Before implementation, define at least:

- what the client receives first
- whether the output is text, JSON, or another MCP-friendly structure
- which upstream metadata must be preserved
- which errors are propagated
- which errors may degrade into controlled functional messages

### 5. Compatibility is not the same as business ownership

In `STATELESS` MCP, `resource` and `prompt` may remain enabled for protocol compatibility even before a real business slice owns them.

Preferred behavior:

- make the compatibility choice explicit
- avoid registrable sample capabilities in the business-critical path
- keep the real business path traceable to spec, use case, adapter, config, and tests

### 6. Placeholder cleanup is part of the overlay

If the scaffold generated MCP placeholders, treat them as starter structure only.

A real slice should:

- replace the placeholders it owns
- isolate the placeholders it does not yet own
- avoid leaving sample capabilities in the critical path

## Minimum adoption checklist

- [ ] The repository really needs MCP exposure.
- [ ] The source API contract is available or well understood.
- [ ] The initiative includes `api-contract-to-mcp.md`.
- [ ] Every mapped capability has a supporting use case.
- [ ] Output, auth, error, and resilience policy are explicit per capability.
- [ ] MCP compatibility behavior is documented when relevant.
- [ ] Placeholder impact is explicit before implementation.

## What stays example-only

The following do **not** belong in the reusable rule set unless the target repo explicitly adopts the same context:

- concrete slice names such as `TermsAndConditions`
- exact MCP primitive names from one implementation
- one repository's specific decision to expose a given read-only API as `Tool`
- one repository's exact upstream metadata choices
- one repository's exact functional error message strategy
- one repository's placeholder set, role names, endpoint paths, or YAML keys

That evidence should stay under examples, not in the core or overlay rules.

## How it aligns with the package

Use this overlay together with:

- `../../AGENT.md`
- `../../docs/playbooks/scaffold-baseline.md`
- `../../docs/sdd/README.md`
- `../../docs/templates/api-contract-to-mcp.md`
- core skills in `../../skills/`

This overlay specializes the package for MCP exposure.
It does not replace:

- spec intake
- API design for pure REST repositories
- scaffold baseline verification
- traceability closure

## Source references during preparation

Until the standalone portable repo is finalized, the source material for this overlay remains in the original repository under:

- `../../../../skills/api-to-mcp/SKILL.md`
- `../../../../skills/scaffold-generation/SKILL.md`
- `../../../../docs/sdd/README.md`
- `../../../../docs/scaffold-baseline.md`
- `../../../../docs/examples/untitled/`

## Boundary rule

If an example from one repository conflicts with the reusable guidance in this overlay, prefer the reusable guidance and treat the example as context-specific evidence.
