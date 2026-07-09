# AGENTS.md

Guía operativa para agentes de IA y desarrolladores del monorepo Azure DevOps.

Versión actualizada: 2026-07-08

## Qué debe hacer este archivo

Este documento reemplaza la guía general por una guía corta y accionable. Usa solo la fase que corresponda a la tarea que vas a ejecutar.

## Reglas obligatorias del repo

- No subir secretos, tokens, claves ni archivos `.env`.
- Mantener cambios coherentes si una modificación afecta más de un módulo.
- No dejar artefactos de compilación ni carpetas temporales en el repositorio.
- Usar el formato de commit: `tipo(scope): descripción`.
- Si cambias un contrato compartido, actualizar también la documentación o el consumidor correspondiente.

## Fase 1 - Frontend (`azure-devops-frontend`)

Objetivo: trabajar solo en la UI y su arquitectura.

- Colocar nuevas pantallas y lógica en `src/app/features/<feature>/`.
- Crear componentes como `standalone: true`.
- Usar `@if`, `@for` y `@switch` en vez de `*ngIf`, `*ngFor` y `*ngSwitch`.
- Usar `signal()` para estado simple de UI y `BehaviorSubject` solo para estado compartido.
- Evitar fugas de memoria con `takeUntilDestroyed()` o `AsyncPipe`.
- Todo botón icon-only debe tener `aria-label`.

## Fase 2 - Backend (`azure-devops-backend`)

Objetivo: trabajar solo en APIs, casos de uso y adaptadores del backend.

- Mantener `domain/model` y `domain/usecase` puros: sin Spring, Jackson ni anotaciones técnicas.
- Hacer el wiring en `applications/app-service`.
- Mapear DTOs o entidades técnicas a entidades de dominio en infraestructura.
- La lógica de negocio va en los casos de uso; los entry points solo validan y delegan.

## Fase 3 - Agente (`azure-devops-agent`)

Objetivo: trabajar solo en orquestación, chat, planeación o integración con vector store.

- Mantener la estructura limpia del backend.
- Los handlers y controladores deben delegar rápido al caso de uso correspondiente.
- No mezclar infraestructura con lógica de negocio en el dominio.

## Fase 4 - MCP (`azure-devops-mcp`)

Objetivo: trabajar solo en herramientas MCP y su delegación a casos de uso.

- Exponer herramientas con `@McpTool` en el entry point de MCP.
- Cada herramienta debe validar entrada y delegar a un caso de uso.
- La descripción y los parámetros deben ser claros para LLMs.
- Evitar lógica compleja dentro del adaptador MCP.

## Cómo trabajar con este archivo

- Empieza por la fase que corresponde a la tarea actual.
- No mezclar trabajo de frontend, backend, agente y MCP en una misma tarea.
- Usa `AI_WORK_PLAN.md` como plan operativo si necesitas ver el siguiente paso.

## Checklist de salida

- [ ] Sin secretos ni archivos sensibles.
- [ ] Cambios alineados con la fase activa.
- [ ] Validación ejecutada si aplica.
- [ ] Commit con formato correcto.
