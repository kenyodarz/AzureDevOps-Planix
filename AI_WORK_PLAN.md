# Plan global de trabajo por fases

Este documento es la fuente de verdad para ejecutar cambios en el monorepo. Trabaja una fase a la vez y mantén el scope alineado con la fase activa.

## Fase 1 - Frontend

Objetivo: ajustar la experiencia de usuario y la arquitectura del frontend.

- Revisar `azure-devops-frontend/src/app/features/` y localizar la feature afectada.
- Implementar cambios en componentes, páginas o servicios de forma aislada.
- Mantener componentes standalone y usar `@if/@for/@switch`.
- Validar que el estado de UI use Signals y que las suscripciones RxJS no generen fugas.
- Ejecutar la validación del frontend al terminar.

## Fase 2 - Backend

Objetivo: ajustar APIs, casos de uso y adaptadores del backend.

- Revisar `azure-devops-backend/domain/`, `applications/` e `infrastructure/`.
- Mantener el dominio puro y mover el wiring a `applications/app-service`.
- Implementar o corregir casos de uso y mapeos de infraestructura.
- Validar que la lógica de negocio no viva en los entry points.
- Ejecutar la verificación del módulo backend al terminar.

## Fase 3 - Agente

Objetivo: ajustar la lógica del ciberagente y su integración con la planeación o chat.

- Revisar `azure-devops-agent/domain/`, `applications/` e `infrastructure/`.
- Mantener handlers y entry points delgados.
- Delegar la orquestación a los casos de uso del dominio.
- Verificar que los cambios no mezclen infraestructura con lógica de negocio.
- Ejecutar la verificación del módulo agente al terminar.

## Fase 4 - MCP

Objetivo: ajustar herramientas MCP y su integración con el backend.

- Revisar `azure-devops-mcp/infrastructure/entry-points/` y los casos de uso del dominio.
- Exponer o corregir herramientas con `@McpTool`.
- Mantener la herramienta enfocada en validación y delegación.
- Validar que la descripción y los parámetros de cada herramienta sean claros.
- Ejecutar la verificación del módulo MCP al terminar.

## Entregable por fase

- Cambios limitados al módulo de la fase activa.
- Documentación o contratos actualizados si aplica.
- Validación ejecutada antes de cerrar la tarea.
- Commit con formato `tipo(scope): descripción`.

