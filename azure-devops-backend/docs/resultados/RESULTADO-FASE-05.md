# RESULTADO — FASE 05: Entry-Points Reactivos y DTOs (`infrastructure/entry-points/reactive-web`)

> **Ejecutada:** 2026-09-06 · **Estado final:** 🟢 COMPLETADA  
> **Commit sugerido:**
> `feat(backend): exponer endpoints reactivos y DTOs para gestion documental y planeacion`  
> **Instrucciones:** `docs/fases/FASE-05-entry-points-reactivos-planning.md`

---

## 1. Objetivo de la Fase

Construir la capa de exposición HTTP funcional reactiva en
`infrastructure/entry-points/reactive-web` para los casos de uso documentales y de planeación macro
implementados en las fases anteriores:

1. **DTOs Inmutables:** Creación de DTOs en `co.com.bancolombia.api.dto` para desacoplar
   completamente los modelos de dominio (`ProgramPlanRequest`, `SpecDocument`) de los contratos JSON
   (`ProgramPlanRequestDTO`, `ProgramPlanResponseDTO`, `SpecDocumentDTO`, `SpecListDTO`).
2. **Handlers Reactivos Funcionales:** Implementación de `SpecHandler` (`listSpecs`, `getSpec`) y
   `ProgramPlanningHandler` (`triggerProgramPlanning`) con cero lógica de negocio, validación del
   contrato y delegación directa a los casos de uso correspondientes.
3. **Registro de Rutas en RouterRest:** Registro de las 3 nuevas rutas (`GET /api/planning/specs`,
   `GET /api/planning/specs/{specName}`, `POST /api/planning/program`) respetando estrictamente la
   decisión de arquitectura **DP-BFF-02** (coexistencia limpia con las rutas legadas de `pgvector`).
4. **Manejo Centralizado de Errores:** Incorporación de `SpecNotFoundException` en
   `ApiErrorTranslator` para responder consistentemente con `404 Not Found`.
5. **Pruebas Unitarias de Capa Web:** Suite completa con `WebTestClient` y Mockito
   alcanzando $\ge 90\%$ de cobertura y asegurando cero regresiones en la suite preexistente.

---

## 2. Qué se construyó

```text
azure-devops-backend/
└── infrastructure/entry-points/reactive-web/
    ├── src/main/java/co/com/bancolombia/api/
    │   ├── SpecHandler.java                                    [NUEVO] (Controlador reactivo para listado y detalle de specs)
    │   ├── ProgramPlanningHandler.java                         [NUEVO] (Controlador reactivo para activación de planeación macro)
    │   ├── RouterRest.java                                     [MODIFICADO] (Inyección de handlers y registro de nuevas rutas)
    │   ├── error/
    │   │   └── ApiErrorTranslator.java                         [MODIFICADO] (Mapeo de SpecNotFoundException a 404 Not Found)
    │   └── dto/
    │       ├── SpecDocumentDTO.java                            [NUEVO] (DTO inmutable de spec Markdown)
    │       ├── SpecListDTO.java                                [NUEVO] (DTO inmutable de listado de specs)
    │       ├── ProgramPlanRequestDTO.java                      [NUEVO] (DTO inmutable con toDomain() para planeación)
    │       └── ProgramPlanResponseDTO.java                     [NUEVO] (DTO inmutable con resumen, tarea y contexto)
    └── src/test/java/co/com/bancolombia/api/
        ├── SpecHandlerTest.java                                [NUEVO] (6 pruebas de integración web con WebTestClient)
        ├── ProgramPlanningHandlerTest.java                     [NUEVO] (6 pruebas de integración web con WebTestClient)
        ├── dto/
        │   └── PlanningDtosTest.java                           [NUEVO] (4 pruebas unitarias de mapeo y validación de DTOs)
        ├── error/
        │   └── ApiErrorTranslatorTest.java                     [MODIFICADO] (Verificación de SpecNotFoundException -> 404)
        ├── TaskRoutesTest.java                                 [MODIFICADO] (Ajuste de wiring en @ContextConfiguration)
        ├── PlanningRoutesCharacterizationTest.java             [MODIFICADO] (Ajuste de wiring en @ContextConfiguration)
        └── DashboardRoutesCharacterizationTest.java            [MODIFICADO] (Ajuste de wiring en @ContextConfiguration)
```

---

## 3. Decisiones Aplicadas

- **DP-BFF-02 (Coexistencia de Rutas Legadas de pgvector):**  
  Las rutas `/api/planning/ingest`, `/api/planning/initiatives` y `/api/planning/search` se
  mantuvieron exactamente intactas en `RouterRest.java`. Las nuevas rutas documentales y de
  planeación se montaron bajo `/api/planning/specs/**` y `/api/planning/program` antes del fallback
  de SPA.
- **DP-BFF-03 (Despacho No Bloqueante con Tarea Asíncrona):**  
  `ProgramPlanningHandler` mapea el `ProgramPlanRequestDTO` a `ProgramPlanRequest`, invoca
  `TriggerProgramPlanningUseCase.execute(...)` y responde con `202 Accepted` emitiendo el
  `ProgramPlanResponseDTO` que encapsula la `Task` trazable para la UI de Angular.
- **Mapeo Centralizado de Errores (DP-06):**  
  `SpecNotFoundException` fue incorporada en `ApiErrorTranslator.toStatus(...)`, retornando de forma
  transparente `404 Not Found` en `GET /api/planning/specs/{specName}` cuando el archivo no existe
  en el almacenamiento.

---

## 4. Métricas Obtenidas

| Métrica                                                          | Antes (Fase 04) |          Después (Fase 05)          |
|:-----------------------------------------------------------------|:---------------:|:-----------------------------------:|
| **Pruebas Unitarias Pasando (Total proyecto)**                   |       391       |            **408 (+17)**            |
| **Pruebas Unitarias en `:reactive-web`**                         |       55        |        **72 (100% pasando)**        |
| **Pruebas Nuevas en Capa Web (Fase 05)**                         |       N/A       |        **17 (100% pasando)**        |
| **Cobertura en `co.com.bancolombia.api.dto`**                    |       N/A       |    **100%** (Líneas e Instrucc.)    |
| **Cobertura en `co.com.bancolombia.api` (Handlers & Rutas)**     |      98.6%      |     **99%** (854/854 instrucc.)     |
| **Cobertura en `co.com.bancolombia.api.error`**                  |       N/A       |        **90%** (68/68 inst.)        |
| **Cobertura Global en `:reactive-web` (Líneas / Instrucciones)** |      92.0%      |     **93%** (1.755/1.870 inst.)     |
| **Pruebas de Mutación PIT en `:reactive-web` (Test Strength)**   |       N/A       | **96%** (163/184 mutaciones killed) |
| **Violaciones de Arquitectura (`validateStructure`)**            |        0        |                **0**                |
| **Estado del Build (`./gradlew test`)**                          |       OK        |       **🟢 BUILD SUCCESSFUL**       |

---

## 5. Comandos Ejecutados y Trazabilidad

- `./gradlew :reactive-web:test`:
    - 72 pruebas unitarias ejecutadas con éxito (0 fallos, 0 errores, 0 ignoradas).
    - Cobertura global de `reactive-web` al 93% y fortaleza de mutación PIT del 96%.
- `./gradlew validateStructure`:
    - Clean Architecture plugin v4.5.0: **The project is valid**.
    - Estructura limpia y sin dependencias no permitidas.
- `./gradlew test`:
    - 408 pruebas unitarias ejecutadas en todos los submódulos. 100% de éxito. Cero regresiones.

---

## 6. Desviaciones Respecto a las Instrucciones

| Desviación | Justificación                                                                                                            |
|:-----------|:-------------------------------------------------------------------------------------------------------------------------|
| Ninguna    | Se ejecutaron estrictamente las tareas T-01 a T-04 según la especificación `FASE-05-entry-points-reactivos-planning.md`. |

---

## 7. Próximo Paso

Avanzar a la **Fase 06**: Cableado en Spring (`applications/app-service`), Validación E2E y Cierre
Definitivo del plan maestro.
