# Overlay: MCP security baseline

## Purpose

This overlay groups security guidance for repositories that expose MCP capabilities and also adopt a claim-based authorization baseline.

It remains optional because not every target repository uses the same identity provider, authorization model, or runtime security posture.

## When to adopt it

Adopt this overlay when the target repository needs most of the following:

- MCP capabilities protected by an OAuth2 Resource Server or equivalent
- authorization enforced at capability level
- claim-to-role mapping
- explicit auditability of the exposed capability
- fail-closed behavior when role claims are missing or empty

This overlay matches the **MCP with security baseline** adoption mode.

## What this overlay adds

Compared to the core package, this overlay adds guidance for:

- treating generated security as an existing baseline to validate
- documenting capability-level authorization explicitly
- validating claim-to-role mapping behavior
- ensuring missing claims fail closed
- keeping audit, configuration, and runtime behavior aligned with the spec

## Reusable rules

### 1. Existing security baseline must be validated, not ignored

If the scaffold or repository already provides security-related runtime pieces, treat that as an existing baseline to validate and refine.

Do not document security as “still missing” before verifying what is already present.

### 2. Authorization must be explicit at capability level

For each exposed MCP capability, make explicit:

- who can invoke it
- what authorization rule applies
- what sensitive output requires protection
- what should happen on denied access

### 3. Claim-to-role mapping is part of the contract

If roles are derived from token claims, the spec and traceability artifacts should identify:

- where roles come from conceptually
- which capabilities depend on them
- what configuration and runtime pieces must remain aligned

### 4. Missing claims must fail closed

If the expected role claim is missing or empty:

- no business role should be granted from that path
- the system should avoid runtime failures caused only by the missing claim

Fail-closed behavior is a reusable rule, not an example-only preference.

### 5. Audit is not optional when authorization matters

When business capabilities are protected, the overlay should also require explicit audit thinking around:

- caller identity
- invoked capability
- outcome
- timing or execution context when relevant

### 6. Config, spec, and runtime behavior must agree

Security decisions are incomplete unless they are aligned across:

- spec artifacts
- runtime configuration
- authorization implementation
- tests
- operational documentation

## Minimum adoption checklist

- [ ] The repository really adopts MCP plus a security baseline.
- [ ] Capability-level authorization rules are explicit in the spec.
- [ ] Claim-to-role behavior is documented where relevant.
- [ ] Missing or empty claims fail closed.
- [ ] Audit expectations are explicit.
- [ ] Security-related configuration is identified and traceable.
- [ ] Security behavior is verified through tests at the appropriate level.

## What stays example-only

The following should remain example-only unless the target repository intentionally adopts the same context:

- exact identity provider setup from one repository
- concrete role names
- exact JWT claim keys or JSON path expressions from one implementation
- exact config class names, property names, or YAML keys from one repo
- exact audience, client, issuer, or token-shape assumptions from one environment
- security decisions tied to a single business slice

Those details belong in examples or repo-specific runtime docs, not in the reusable overlay rule set.

## How it aligns with the package

Use this overlay together with:

- `../../AGENT.md`
- `../../docs/playbooks/scaffold-baseline.md`
- `../../docs/playbooks/security-baseline.md`
- `../../docs/sdd/README.md`
- `../../skills/traceability/SKILL.md`
- the applicable API or API -> MCP design artifacts

This overlay specializes the package for secured MCP exposure.
It does not replace:

- baseline verification
- capability design
- traceability closure
- repository-specific runtime validation

## Source references during preparation

Until the standalone portable repo is finalized, the source material for this overlay remains in the original repository under:

- `../../../../docs/scaffold-baseline.md`
- `../../../../docs/sdd/README.md`
- `../../../../docs/examples/untitled/current-baseline.md`
- `../../../../docs/examples/untitled/terms-and-conditions/extracted-learnings.md`

## Boundary rule

If a repository-specific security example conflicts with the reusable guidance in this overlay, prefer the reusable guidance and treat the example as context-specific evidence.
