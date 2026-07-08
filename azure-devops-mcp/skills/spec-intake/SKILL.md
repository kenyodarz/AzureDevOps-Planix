# Skill: spec-intake

## When to use it

Use this when a new need arrives and there is not yet a clear spec in the repository.

## Expected input

- initial problem or idea
- business context
- known constraints
- API contract, if it exists

## Expected output

An initiative folder with the minimum artifacts required for design and implementation.

## Procedure

1. Create an initiative folder using the format `YYYYMMDD-short-slug`.
2. Copy `docs/templates/feature-spec.md`.
3. If the target is an API, also copy `docs/templates/api-contract.md`.
4. If the source is an API and the target is MCP, also copy `docs/templates/api-contract-to-mcp.md`.
5. Copy `docs/templates/tasks-checklist.md` and `docs/templates/traceability-matrix.md`.
6. Fill problem, objective, scope, acceptance criteria, architectural impact, baseline assumptions, and cross-cutting controls.
7. Mark open questions before moving into implementation.

