# Feature Spec Template

## 1. Metadata

- Name:
- Slug:
- Status: draft | ready | implemented | verified
- Date:
- Author:

## 2. Problem

Describe the real problem to be solved.

## 3. Objective

Expected outcome in business terms or technical capability terms.

## 4. Scope

### Includes

- 

### Excludes

- 

## 4.1 Current technical baseline

- Does this initiative start from an already generated MCP baseline?: yes | no
- Which MCP capabilities are already enabled in the repo?: tools | resources | prompts | completion
- Existing real business capabilities already present in the repo:
- MCP placeholders involved:
- MCP placeholders expected to be replaced in this initiative:
- MCP placeholders intentionally retained outside the critical path:
- New modules required:
- New domain artifacts required:

## 5. Domain language

- term:
- definition:

## 6. Actors or consumers

- internal user
- external system
- MCP client
- source service

## 7. Main scenarios

1. 
2. 
3. 

## 8. Functional requirements

- RF-01:
- RF-02:
- RF-03:

## 9. Non-functional requirements

- RNF-01:
- RNF-02:
- RNF-03:

### Expected cross-cutting controls

- auth / roles:
- auth edge cases (missing claims, empty roles, denied roles):
- output policy:
- error policy:
- audit:
- resilience:
- observability:

## 10. Constraints

- regulatory
- technology
- security
- performance

## 11. Expected impact on Clean Architecture

### Domain / model

- 

### Domain / usecase

- 

### Infrastructure / entry-points

- 

### Infrastructure / driven-adapters

- 

### Wiring / assembly

- expected explicit wiring:
- expected configuration classes to review:

## 12. Acceptance criteria

- [ ] 
- [ ] 
- [ ] 
- [ ] The initiative defines which scaffold placeholders will be replaced and which will not.
- [ ] The initiative makes the MCP baseline state and placeholder scope explicit before implementation.
- [ ] The initiative defines the minimum cross-cutting controls required for the slice.
- [ ] The initiative identifies expected wiring and critical configuration impact.

## 13. Validation strategy

- unit tests:
- integration tests:
- contract tests:
- manual validation:

### Expected slice closure evidence

- code path aligned with the spec
- critical configuration aligned with the spec
- placeholder replacement or isolation aligned with the spec
- traceability and checklist updated

## 14. Risks and open questions

- 

