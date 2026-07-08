# API Contract Template

## 1. Metadata

- Initiative name:
- API name:
- Contract version:
- Status:

## 2. Consumer and business summary

- primary consumer:
- business objective:
- business capability exposed:
- payload style: JSON | multipart | text | file | other

## 3. Assumptions

-

## 3.1 Current technical baseline

- Does the initiative start from an already generated API baseline?: yes | no
- Which entry points already exist in the repo?: none | restmvc | other
- Which real business capabilities already exist in the repo?:
- Which scaffold placeholders or sample endpoints will be replaced in this initiative?:
- Which scaffold placeholders will be intentionally retained outside the critical path?:
- Is a new driven adapter required?: yes | no
- Are new domain model and use case artifacts required?: yes | no

## 4. Operation inventory

| ID     | Consumer operation | Method | Route | Objective | Notes |
|--------|--------------------|--------|-------|-----------|-------|
| API-01 |                    |        |       |           |       |

## 5. API design rules

### Resource-oriented endpoint

Use it when the capability:

- primarily exposes data
- is queryable or retrievable by identifier
- should remain predictable and idempotent

### Action-oriented endpoint

Use it when the capability:

- triggers business behavior
- validates commands or intent
- may create side effects

## 6. Capability matrix

| API ID | Entry point type | Proposed endpoint | Associated use case | Main input | Output type    | Main output | Required role | Validation | Error policy | Timeout | Critical config | Placeholder impact | Notes |
|--------|------------------|-------------------|---------------------|------------|----------------|-------------|---------------|------------|--------------|---------|-----------------|--------------------|-------|
| API-01 | REST endpoint    |                   |                     |            | JSON/text/file |             |               |            |              |         |                 | replace/retain/n-a |       |

## 7. Derived domain design

### Candidate models

-

### Candidate use cases

-

### Candidate gateways or adapters

-

### Entry points to create or refine

-

## 8. Security considerations

- authentication
- authorization
- sensitive data
- audit

## 9. Observability

- logs
- metrics
- traces
- audit events

## 9.1 Output toward the API client

- response type: JSON | text | file | other
- output formatting rule:
- metadata that must be preserved:
- allowed error degradation:
- errors that must not be degraded:
- source fields that must remain verbatim:

## 10. Scaffold generation plan

- models to generate:
- use cases to generate:
- entry point to generate:
- adapters to generate:
- additional wiring / configuration to adjust:

## 11. Acceptance criteria

- [ ] Every relevant operation was mapped to an API endpoint.
- [ ] Every endpoint is traceable to a use case.
- [ ] Validation, authorization, and output policy were defined per capability.
- [ ] Baseline state and placeholder impact were made explicit before implementation.
- [ ] Critical configuration keys were identified for each externally backed capability.

## 12. Open questions

- 

