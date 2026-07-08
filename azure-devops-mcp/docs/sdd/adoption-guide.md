# Adoption guide

## Purpose

This staging area exists to make the future extraction into a standalone portable repository mechanical and low-risk.

## Core vs overlays vs examples

### Core

Belongs in the portable repo by default:

- `AGENT.md`
- `docs/playbooks/scaffold-baseline.md`
- `docs/playbooks/security-baseline.md`
- `docs/templates/*`
- `skills/spec-intake/`
- `skills/traceability/`
- `skills/api-design/`
- high-level SDD guidance

### Overlays

Belong in optional packages or folders because not every target repo needs them:

- API -> MCP rules
- MCP-specific exposure guidance
- security-baseline guidance tied to claim-based authorization

### Examples

Belong outside the core because they are evidence, not default rules:

- repository-specific history
- slice-specific naming
- slice-specific roles, paths, URLs, and YAML keys
- analysis notes and backlog history

## Current extraction status

Already prepared in this staging area:

- root `README.md`
- root `AGENT.md`
- `docs/playbooks/scaffold-baseline.md`
- `docs/playbooks/security-baseline.md`
- `docs/sdd/README.md`
- `docs/templates/*`
- `skills/spec-intake/`
- `skills/api-design/`
- `skills/traceability/`
- overlay guides
- examples index stubs

Still pending for a final standalone repo:

- copy or rewrite overlay content into stable portable form
- bring example-only evidence into the final repo or keep it linked from the source

## Source references

During preparation, the original source remains under:

- `../../../../AGENT.md`
- `../../../../docs/sdd/`
- `../../../../docs/specs/templates/`
- `../../../../skills/`
- `../../../../docs/examples/untitled/`

