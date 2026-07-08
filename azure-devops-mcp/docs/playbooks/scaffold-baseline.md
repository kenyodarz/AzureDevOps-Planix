# Playbook: scaffold baseline

## Purpose

This playbook defines the **portable baseline guidance** for repositories built on top of the **Bancolombia Clean Architecture scaffold**.

Use it to answer four questions:

1. what baseline states are valid
2. what the scaffold usually solves for you
3. what still requires business refinement after generation
4. what must be reviewed before generated structure is treated as delivery-ready

This playbook is part of the reusable core.
Repository-specific evidence belongs under `docs/examples/`.

## Baseline rule

The real structural baseline is the **Bancolombia scaffold**.

Do not assume a repository already has the same entry points, security setup, generated modules, or runtime configuration as another repository. Verify the real baseline first.

## Recognized baseline states

A scaffold-based repository may validly be in one of these states.

### 1. Clean Architecture baseline

The repository contains the core scaffolded structure and is ready for domain-driven evolution.

Typical expectation:

- `applications/app-service`
- `domain/model`
- `domain/usecase`
- `infrastructure/`

This is the minimum structural baseline.

### 2. Clean Architecture + REST entry point baseline

Use this state when the repository must expose HTTP APIs.

At this point, the scaffold may already have created:

- REST entry-point structure
- application module dependencies
- base runtime configuration for the selected entry point

This is still **structural bootstrap**, not business completion.

### 3. Clean Architecture + MCP entry point baseline

Use this state when the repository must expose MCP capabilities.

At this point, the scaffold may already have created:

- MCP entry-point structure
- application module dependencies
- MCP runtime configuration
- placeholder Tool / Resource / Prompt capabilities
- base cross-cutting support such as security, CORS, or audit, depending on scaffold version and options

This is also structural bootstrap, not business completion.

## Baseline-first verification

Before generating or implementing anything, verify explicitly:

- current registered modules
- current entry points
- current driven adapters
- current runtime configuration
- current cross-cutting baseline, if any:
  - security
  - authorization
  - audit
  - CORS
  - resilience

## Generic scaffold command families

Exact parameters depend on the target repository and current plugin version, so verify them before use.

Typical command families include:

```bat
gradlew.bat cleanArchitecture ...
gradlew.bat gep --type restmvc
gradlew.bat generateEntryPoint --type=mcp
gradlew.bat gm --name <ModelName>
gradlew.bat guc --name <UseCaseName>
```

Treat these as **structural generation commands**, not as proof that a business slice is complete.

## What the scaffold usually solves

When used correctly, the scaffold usually solves a meaningful part of the technical baseline, such as:

- modular Clean Architecture structure
- Gradle module registration
- application assembly baseline
- entry-point bootstrap
- driven-adapter bootstrap
- initial configuration wiring
- sample code that illustrates the generated path

Depending on selected options and entry points, it may also generate:

- security baseline
- CORS baseline
- audit baseline
- sample controllers, tools, resources, prompts, consumers, DTOs, or tests

## What the scaffold does not solve for you

Even when generation succeeds, the scaffold does **not** complete the business slice.

Business refinement still includes work such as:

- filling real business fields and rules in generated models
- refining use-case orchestration
- renaming repository-like or placeholder contracts into domain language when needed
- replacing sample adapter methods with real integration behavior
- replacing placeholder entry-point capabilities with real business exposure
- externalizing adapter-specific configuration
- defining auth, audit, resilience, and error policy for the real capability
- aligning tests, documentation, and traceability with the implemented slice

## Invariants when working on top of the scaffold

- Prefer scaffold generation over manual structure creation when a suitable generator exists.
- Treat generated output as a starter structure, not as final business design.
- Keep business logic out of controllers, tools, prompts, resources, and adapters.
- Keep the domain free of infrastructure-specific concerns.
- Preserve scaffold naming conventions and module boundaries unless a documented reason justifies divergence.
- Do not leave sample or placeholder code in the business-critical path once a real capability replaces it.
- Do not assume default wiring is always sufficient for gateway-backed use cases.

## Post-generation review checklist

After any relevant scaffold step, review at least these areas.

### Structure

- `settings.gradle`
- generated or updated modules
- module dependencies in the application layer

### Runtime configuration

- `application.yaml` or equivalent config files
- generated endpoint properties
- adapter-specific properties that still need real values
- timeout or resilience settings when the slice needs them

### Assembly and wiring

- application configuration classes
- explicit bean wiring when a use case depends on a gateway implementation
- whether generated scanning rules still match the real design

### Placeholders and samples

- generated sample classes
- placeholder controllers, tools, resources, or prompts
- sample consumer methods or DTOs
- generated tests that no longer match the real slice

### Cross-cutting controls

- security behavior
- authorization coverage
- role extraction behavior when applicable
- audit interception
- transport controls such as CORS when applicable

## Specialization boundaries

This playbook is core guidance.

Use overlays when the repository needs specialization such as:

- REST API delivery: `../../overlays/api/README.md`
- API -> MCP delivery: `../../overlays/api-to-mcp/README.md`
- secured MCP with claim-based authorization: `../../overlays/mcp-security-entra-id/README.md`

Use the companion core playbook for reusable security guidance:

- `security-baseline.md`

Use examples when you need historical evidence rather than reusable rules:

- `../examples/README.md`
- `../examples/untitled/README.md`

