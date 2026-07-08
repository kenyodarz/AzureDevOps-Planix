# Skill: api-design

## When to use it

Use this skill when the target repository must expose or refine a REST API and the main design question is how to translate a business capability into HTTP endpoints.

## Objective

Design a traceable API contract that preserves Clean Architecture responsibilities.

## Primary artifact

Use `docs/templates/api-contract.md` as the main design artifact.

## Core procedure

1. Clarify the consumer and the business capability.
2. Inventory the expected operations.
3. Decide resource-oriented vs action-oriented design.
4. Map every endpoint to `REST endpoint -> Use Case -> Gateway -> Adapter`.
5. Define validation, error, auth, and output policy.
6. Define observability and critical configuration.
7. Decide whether scaffold generation is needed only after the design is clear.

## Design rules

- Do not start from routes before clarifying business intent.
- Do not put business logic in controllers.
- Do not let transport naming dictate domain language.
- Keep validation, auth, output, and error policy explicit per endpoint.

