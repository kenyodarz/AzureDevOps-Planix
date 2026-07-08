# Playbook: security baseline

## Purpose

This playbook defines the **portable security baseline guidance** for repositories built on top of the Bancolombia scaffold.

Use it to answer these questions:

1. when security is already part of the repository baseline
2. what must be verified before calling security “complete”
3. which security rules are reusable across delivery modes
4. what must remain repository-specific or example-only

This playbook belongs to the reusable core.
Identity-provider specifics, token-shape specifics, and repository-specific runtime details belong in overlays or examples.

## When to use it

Use this playbook whenever the repository exposes business capabilities and any of these concerns apply:

- authentication
- authorization
- claim-based identity or role mapping
- auditability
- sensitive output protection
- cross-cutting runtime security configuration

This playbook is relevant for API repositories, API -> MCP repositories, and secured MCP repositories.

## Baseline-first verification

Before describing security as missing, verify what already exists in the repository baseline.

Check at least:

- runtime security configuration already present
- authentication mechanism already present
- authorization mechanism already present
- method-level or endpoint-level protection already present
- audit behavior already present
- role extraction or claim mapping already present
- related tests or validation already present

If the scaffold or current repository already generated security-related runtime pieces, treat that as an **existing baseline to validate and refine**, not as something absent by default.

## Reusable rules

### 1. Security must be explicit per capability

For each exposed business capability, make explicit:

- who can invoke it
- what authorization rule applies
- what sensitive data or output requires protection
- what should happen on denied access
- what security behavior must never be silently degraded

### 2. Authorization belongs in spec, code, config, and tests

A security decision is incomplete unless it is aligned across:

- specification artifacts
- runtime configuration
- authorization implementation
- tests
- operational or navigable documentation

### 3. Claim-based authorization is part of the contract

If business roles depend on claims, the repository should document:

- which capabilities depend on claim-derived authorization
- what configuration or mapping mechanism is involved
- what runtime behavior is expected if claims are missing, empty, malformed, or insufficient

### 4. Missing claims must fail closed

If expected role claims are missing or empty:

- no business role should be granted from that path
- the request should not fail with a runtime exception solely because the claim is absent

Fail-closed behavior is a reusable rule.

### 5. Audit matters when authorization matters

When protected business capabilities exist, the security baseline should also define explicit audit expectations around:

- caller identity when available
- invoked capability
- outcome
- relevant timing or execution metadata when needed

### 6. Sensitive failures should not be silently downgraded

Functional error degradation may be valid for some business paths, but authentication and authorization failures should remain governed by platform security behavior unless the spec explicitly defines another reviewed policy.

### 7. Environment-specific values are not reusable defaults

Values such as issuer, audience, client identifiers, claim keys, JSON paths, or trust assumptions may be necessary at runtime, but they should never be documented as universal defaults for all repositories.

## Minimum adoption checklist

- [ ] Authentication behavior is explicit.
- [ ] Authorization rules are explicit per protected capability.
- [ ] Sensitive outputs or protected paths are identified.
- [ ] Claim-based behavior is documented where relevant.
- [ ] Missing or empty claims fail closed.
- [ ] Audit expectations are explicit.
- [ ] Critical security configuration is identified and traceable.
- [ ] Security behavior is covered by the appropriate test scope.
- [ ] Spec, config, runtime behavior, and tests are aligned.

## Traceability expectations

Security should be traceable like any other important requirement.

At minimum, traceability artifacts should identify:

- the protected capability
- the required role or authorization rule
- the critical configuration involved
- the relevant assembly or wiring point when it matters
- the expected test scope
- the expected audit or resilience interaction when applicable

## Specialization boundaries

This playbook is core security guidance.

Use overlays when the repository needs specialization such as:

- REST API delivery concerns: `../../overlays/api/README.md`
- API -> MCP exposure concerns: `../../overlays/api-to-mcp/README.md`
- secured MCP with claim-based authorization and provider-specific details: `../../overlays/mcp-security-entra-id/README.md`

Use examples when you need historical evidence rather than reusable rules:

- `../examples/README.md`
- `../examples/untitled/README.md`

## What stays example-only

The following should remain outside the reusable rule set unless the target repository explicitly adopts the same context:

- concrete role names
- concrete claim keys or JSON path expressions
- exact issuer, audience, client, or tenant assumptions
- exact property names or YAML keys from one repository
- exact security configuration class names from one implementation
- security choices tied to a single business slice

## Source references during preparation

Until the standalone portable repo is finalized, the source material for this playbook remains in the original repository under:

- `../../../../docs/scaffold-baseline.md`
- `../../../../docs/sdd/README.md`
- `../../../../skills/traceability/SKILL.md`
- `../../../../docs/examples/untitled/current-baseline.md`
- `../../../../docs/examples/untitled/terms-and-conditions/extracted-learnings.md`

