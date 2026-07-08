# Skill: scaffold-generation

## When to use it

Use this skill when:
- a spec is clear enough to justify structural generation
- the repository lacks a required baseline such as REST or MCP
- models, use cases, entry points, or driven adapters should be scaffolded instead of created manually

This skill is about working correctly on top of the **Bancolombia Clean Architecture scaffold baseline**.

## Objective

Use the scaffold to create or extend structural building blocks **without confusing generated structure with finished business implementation**.

This skill covers:
- **bootstrap generation**: establishing required technical baseline
- **business-oriented generation**: creating structural starters for a concrete initiative

## Expected input

Before using this skill, you should have at least:
- a repository that uses the Clean Architecture scaffold
- a known baseline state
- target delivery mode (domain-only, REST API, API -> MCP, etc.)

## Baseline-first checks
Confirm whether the required baseline already exists or if it's missing (bootstrap vs. refinement).

## Typical build commands
*Parameters may vary depending on plugin version. Verify them locally.*

```bash
gradlew.bat cleanArchitecture ...
gradlew.bat gep --type restmvc
gradlew.bat generateEntryPoint --type=mcp
gradlew.bat gm --name DomainModelName
gradlew.bat guc --name BusinessUseCaseName
```
Treat these commands as **structural generation actions**, not as proof that the feature is implemented.

## Procedure
1. **Diagnose the baseline**: Check current entry points and driven adapters.
2. **Confirm generation readiness**: The spec must exist with clear architecture mapped.
3. **Decide if it's bootstrap vs business**: Generating the MCP module is technical setup. Generating a Use Case is business-oriented.
4. **Generate only what the initiative needs**: Do not over-scaffold.
5. **Review output immediately**: Validate `settings.gradle` and application configuration classes immediately.
6. **Refine structure into business code**: Remove placeholders, fill real business fields, rename template definitions in the adapters, and decouple business rules from networking constraints.
7. **Validate cross-cutting controls**: Review authentication overrides, CORS, timeout definitions, and audit setups. **Fail-closed** behaves on security.
8. **Update traceability and tests**: Align generated tests with the newly injected business constraints.

## Post-generation review checklist
- [ ] Generated structure matches spec requirements.
- [ ] The repository baseline is coherent (`settings.gradle`).
- [ ] Runtime config (`application.yaml`) was reviewed.
- [ ] Placeholder code was removed or isolated.
- [ ] No business logic lives in controllers or adapters.
- [ ] Security behaviors fail closed.
- [ ] Missing claim rules in Entra ID fail closed.

## Common pitfalls
- Generating modules without an assigned spec need.
- Leaving placeholder or sample components (e.g., `SampleController` or `SampleTool`) in the business context.
- Assuming the generated scaffold logic requires zero configuration.
- Wiring gateways blindly without checking `UseCasesConfig`.
