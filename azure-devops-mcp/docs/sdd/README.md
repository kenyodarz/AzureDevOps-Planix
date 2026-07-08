# SDD guide

## Purpose

This is the portable SDD overview for the future extracted repository.

Use it to understand:

- the reusable method
- the available adoption modes
- the core scaffold baseline playbook
- the core security baseline playbook
- where example-only evidence belongs

## Rule

The preferred approach is a lightweight, repo-local, traceable SDD model based on:

- versioned Markdown
- reusable templates
- reusable skills
- explicit architectural mapping
- explicit traceability between spec, generation, code, config, and tests

Each relevant initiative should have its own folder and should start from specification artifacts before implementation.

## Adoption modes

### Mode A: API on Bancolombia scaffold

Use when the target repo mainly needs REST contract design, Clean Architecture traceability, and scaffold-guided generation.

### Mode B: API -> MCP on Bancolombia scaffold

Use when an upstream API contract must be translated into MCP primitives.

### Mode C: MCP with security baseline

Use when the repo also adopts explicit security baseline concerns such as authorization, claim mapping, and fail-closed role extraction.

## Example-only evidence

Historical evidence should remain outside the reusable core.

For reusable baseline guidance inside this staging area, use:

- `../playbooks/scaffold-baseline.md`
- `../playbooks/security-baseline.md`

In the source repository, that evidence currently lives under:

- `../../../../docs/examples/untitled/`

