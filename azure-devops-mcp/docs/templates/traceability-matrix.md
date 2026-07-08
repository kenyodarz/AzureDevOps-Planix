# Traceability Matrix Template

| Requirement ID | Description | Spec artifact | Exposure type                            | Model / entity | Use case | Entry point | Adapter | Placeholder impact | Auth / role | Wiring / assembly             | Critical config | Resilience / audit | Test | Status |
|----------------|-------------|---------------|------------------------------------------|----------------|----------|-------------|---------|--------------------|-------------|-------------------------------|-----------------|--------------------|------|--------|
| RF-01          |             |               | REST endpoint / Tool / Resource / Prompt |                |          |             |         | replace/retain/n-a |             | `UseCasesConfig` / auto / n-a |                 |                    |      | draft  |

## Usage notes

- `Exposure type` should identify how the capability reaches the consumer.
- `Placeholder impact` should record whether a scaffold placeholder is replaced, intentionally retained outside the critical path, or not affected.
- `Wiring / assembly` should identify the class or mechanism that connects the use case to its dependencies when that matters.
- `Critical config` should point to the exact keys or config areas required by the requirement.
- `Test` should name the module level expected to prove the requirement.

