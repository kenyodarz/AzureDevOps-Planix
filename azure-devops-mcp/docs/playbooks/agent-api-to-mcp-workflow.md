# Master Agent Instructions: API -> MCP Workflow (SDD)

## Purpose

This document is a **Functional Prompt / Master Playbook** designed for any AI Agent. Its goal is to take a base repository and, if required, provide it with **Model Context Protocol (MCP)** capabilities. This is done by analyzing an existing API and applying the Spec-Driven Development (SDD) framework dictated by Bancolombia.

**IMPORTANT:** As an Agent, your responsibility is not just to "write code", but to enforce Clean Architecture traceability at all times.

---

## 🚦 Phase 1: Intake and Traceability (Diagnosis)

As soon as you receive a request to create new capabilities, **DO NOT start coding or injecting classes.** Your first responsibility is to define the specification.

### 1.1 Understand the Current Baseline
Execute directory listing and parse build files (e.g., `build.gradle` or `application.yaml`) to answer:
- Does the project already have the `mcp` entry point instantiated?
### 1.2 Parse the Target API Contract (Postman/OpenAPI)
When the user provides a Postman Collection or OpenAPI specification (JSON/YAML) for the API being converted into an MCP, treat it as the definitive source of truth. Do NOT ask the user to fill out Markdown specifications manually. 

Instead, perform the following automated routine:
1. **Iterate per Method:** For each valid HTTP request defined in the file (e.g., `POST /customers/actions/customer-management`), treat it as an isolated business capability.
2. **One Method = One MCP Primitive:** Generally, each parsed method should immediately map to a single, distinct **Tool** (or Resource/Prompt if it perfectly fits the read-only criteria). 
   - *Example:* A collection with 3 requests produces 3 distinct `@McpTool` methods, 3 distinct Use Cases, and 3 Adapter operations.
3. **Auto-Extract Metadata:** Pull the exact headers (e.g., `Client-Id`), payload structure (Body), and description for each endpoint, and use them to construct the Java Models automatically.

Classify each endpoint according to the standard rules:
- **Tools**: Actions, transactions, operations with side effects, or strong validation logic.
- **Resources**: Stable state queries, system configuration, or read-only operations where the main goal is to provide context.
- **Prompts**: Flows that require conversational structuring before executing a technical action.

*Design Rule*: If a "Read-Only" operation requires strong conditional authorization via a token, it might be safer to expose it as a **Tool**. Document this explicitly.

---

## 🏗️ Phase 2: Scaffolding and Bootstrap

If the repository lacks the required scaffolding or the new capabilities need their own Domain Blocks, you must generate them using the Bancolombia plugin. **Do not create the folder structure manually.**

When working from a Postman/OpenAPI parsing step, **execute the structure generation iteratively per method of the target API:**

Command examples (adapt version and scope based on the project):
1. **Prepare architecture (Run Once per Project):**
   ```bash
   gradlew.bat cleanArchitecture --package=co.com.bancolombia --type=...
   ```
2. **Enable MCP exposure (Run Once per Project):**
   ```bash
   gradlew.bat generateEntryPoint --type=mcp
   ```
3. **Generate Domain blocks (Run for EACH method extracted):**
   For every endpoint parsed in Phase 1 (e.g., if you have 3 endpoints, run this 3 times):
   ```bash
   gradlew.bat gm --name DomainNameForMethod # e.g. CustomerProductsConsolidate
   gradlew.bat guc --name UseCaseNameForMethod # e.g. CustomerProductsConsolidateUseCase
   ```

*(Note: Model names must originate from the Domain and MUST NOT contain technical jargon like "Http", "Controller", or "Dto").*

---

## ⚙️ Phase 3: Clean Architecture Refinement (Iterative)

The Scaffolder generates *placeholders* (e.g., `SampleTool`, `UseCaseSample`). **You must remove or isolate them.**

The architectural flow for implementation must remain unbroken. **For every single endpoint parsed in Phase 1, build the following chain:**
**`MCP Primitive (Tool/Resource) -> Use Case -> Gateway -> Driven Adapter -> External API`**

### Tasks in this step (Repeat per Endpoint):
1. **Models / Gateways**: Translate the API JSON payload into pure Domain Entities. Configure the contract in the interfaces.
2. **Driven Adapter**: Implement the HTTP consumption (e.g., via `WebClient` or `RestTemplate`). Read credentials and URLs from `application.yaml` (NEVER hardcode them!).
3. **Use Case**: Write the orchestration. There should be no network `try-catch` blocks here; that is the Adapter's responsibility.
4. **App Service**: Ensure that `config/UseCasesConfig.java` initializes and injects your Use Case and the Adapter. This is especially important for experimental libraries requiring "Lazy" initialization.

---

## 🛡️ Phase 4: MCP Integration and Security

Exposing the system to the outside world via the Model Context Protocol requires strict security boundaries.

1. **Correct Annotation**: Each Tool must contain its technical annotation and human-readable descriptions (vital for LLM semantic invocation), for example: `@McpTool(name = "...", description = "...")`.
2. **Security Definitions (Entra ID)**:
   - Add explicit validation using `@PreAuthorize("hasRole('MCP.SOME.ROLE')")`.
   - Remember the golden rule: **"Fail Closed"**. If a *claim* fails or is missing from the Entra ID Token, the response must be explicitly rejected. Never allow access by default.
3. **Audit**: Verify if there is an interceptor/Aspect to log which ClientID accessed which Tool and when.

---

## 🧹 Phase 5: Clean Up and Approval (Definition of Done)

You cannot declare the task completed (Definition of Done) until you validate the following:

- [ ] All "Sample/Placeholder" classes generated by the Scaffolder lacking real business value have been removed or excluded from the compilation classpath.
- [ ] The matrix in `api-contract-to-mcp.md` exists and maps every newly created MCP Tool to the Use Case you just wrote.
- [ ] Your code compiles without errors (run `./gradlew test` or `./gradlew build` to verify).
- [ ] Audit metrics or functional error messages are in place to return clear semantic formats to the LLM (text, json, or readable fallbacks).

---

> [!IMPORTANT]
> **REMEMBER:** If you find that business rules conflict with the current technical schema or lack details, stop at Phase 1. **Tell the user specifying the gaps and require them to update the .md files before writing a single line of Java code.**
