# RESULTADO — FASE 04: Casos de Uso de Gestión Documental y Planeación (`domain/usecase`)

> **Ejecutada:** 2026-09-06 · **Estado final:** 🟢 COMPLETADA  
> **Commit sugerido:**
> `feat(backend): implementar casos de uso para gestion documental y program planning`  
> **Instrucciones:** `docs/fases/FASE-04-casos-de-uso-planeacion.md`

---

## 1. Objetivo de la Fase

Implementar en la capa de aplicación/casos de uso (`domain/usecase`) los orquestadores de negocio
reactivos y puros para:

1. **Gestión Documental:** Consulta de documento individual por nombre (`GetSpecDocumentUseCase`) y
   listado de especificaciones disponibles (`ListAvailableSpecsUseCase`), consumiendo el puerto
   `SpecStoragePort` (`co.com.bancolombia.model.spec.gateways.SpecStoragePort`).
2. **Program Planning:** Orquestación de la planeación trimestral macro
   (`TriggerProgramPlanningUseCase`), resolviendo la decisión **DP-BFF-03** mediante la validación
   del `ProgramPlanRequest`, ensamblado del `ProgramPlanningCommand` canónico, empaquetado en un
   `AgentCommand.nonBlocking(...)` y despacho asíncrono hacia el agente autónomo vía
   `TrackAgentTaskUseCase.sendAndTrack(...)`.

---

## 2. Qué se construyó

```text
azure-devops-backend/
└── domain/usecase/
    ├── src/main/java/co/com/bancolombia/usecase/
    │   ├── spec/
    │   │   ├── GetSpecDocumentUseCase.java                         [NUEVO] (Consulta reactiva con validación de nombre)
    │   │   └── ListAvailableSpecsUseCase.java                      [NUEVO] (Listado reactivo vía SpecStoragePort)
    │   └── planning/
    │       └── TriggerProgramPlanningUseCase.java                  [NUEVO] (Orquestador canónico A2A según DP-BFF-03)
    └── src/test/java/co/com/bancolombia/usecase/
        ├── spec/
        │   ├── GetSpecDocumentUseCaseTest.java                     [NUEVO] (6 pruebas unitarias con JUnit 5 y StepVerifier)
        │   └── ListAvailableSpecsUseCaseTest.java                  [NUEVO] (3 pruebas unitarias con JUnit 5 y StepVerifier)
        └── planning/
            └── TriggerProgramPlanningUseCaseTest.java              [NUEVO] (6 pruebas unitarias validando comando canónico)
```

---

## 3. Decisiones Aplicadas

- **DP-BFF-03 (Protocolo de Invocación de Program Planning hacia el Agente):**  
  `TriggerProgramPlanningUseCase` valida la entrada, transforma `ProgramPlanRequest` a
  `ProgramPlanningCommand` y emite el comando canónico formateado
  (`/plan [Quarter] [Sprints] [Capacidad] [Frentes] Objetivos: [Objetivos]`) dentro de un
  `AgentCommand.nonBlocking(...)`, delegando en `TrackAgentTaskUseCase.sendAndTrack(...)`.
- **Pureza Arquitectural:**  
  Cero anotaciones técnicas o de Spring en `domain/usecase`. Inyección de dependencias
  exclusivamente mediante constructores nativos de Java asistidos por `@RequiredArgsConstructor` de
  Lombok.

---

## 4. Métricas Obtenidas

| Métrica                                                          | Antes (Fase 03) |                  Después (Fase 04)                  |
|:-----------------------------------------------------------------|:---------------:|:---------------------------------------------------:|
| **Pruebas Unitarias Pasando (Total proyecto)**                   |       376       |                    **391 (+15)**                    |
| **Pruebas Unitarias en `domain/usecase`**                        |       55        |                **70 (100% pasando)**                |
| **Pruebas Unitarias en `usecase.spec`**                          |       N/A       |                **9 (100% pasando)**                 |
| **Pruebas Unitarias en `usecase.planning`**                      |       N/A       |                **6 (100% pasando)**                 |
| **Cobertura en `usecase.spec` (Líneas / Ramas / Instrucciones)** |       N/A       | **100%** líneas (4/4), **100%** ram, **100%** inst. |
| **Cobertura en `usecase.planning` (Líneas / Instrucciones)**     |       N/A       |   **100%** líneas (11/11), **100%** instrucciones   |
| **Cobertura Global en `domain/usecase` (Líneas / Instruc.)**     |      98.9%      |     **98.7%** líneas (152/154), **98.9%** inst.     |
| **Pruebas de Mutación PIT en `:usecase` (Test Strength)**        |       N/A       |          **98%** (90/92 mutaciones killed)          |
| **Violaciones de Arquitectura (`validateStructure`)**            |        0        |                        **0**                        |
| **Estado del Build (`./gradlew test`)**                          |       OK        |               **🟢 BUILD SUCCESSFUL**               |

---

## 5. Comandos Ejecutados y Trazabilidad

- `./gradlew :usecase:test`:
    - 70 pruebas unitarias ejecutadas con éxito (0 fallos, 0 errores, 0 ignoradas).
    - Cobertura integral de `domain/usecase` del 98.7% de líneas y 98% en mutation test strength
      (PIT).
- `./gradlew validateStructure`:
    - Validación del plugin Clean Architecture v4.5.0: **Válida**.
    - Verificación de pureza en `domain/usecase`: solo depende de `:model` y dependencias base
      reactivas sin acoplamiento a frameworks de infraestructura.
- `./gradlew test`:
    - 391 pruebas totales ejecutadas en todos los submódulos. 100% de éxito. Cero regresiones.

---

## 6. Desviaciones Respecto a las Instrucciones

| Desviación | Justificación                                                                                                                |
|:-----------|:-----------------------------------------------------------------------------------------------------------------------------|
| Ninguna    | Se implementaron rigurosamente las tareas T-01 a T-03 de acuerdo con la especificación `FASE-04-casos-de-uso-planeacion.md`. |

---

## 7. Checklist de Calidad (`spring-rules.md` §7)

| Criterio                                     |  Estado   | Observación                                                                           |
|:---------------------------------------------|:---------:|:--------------------------------------------------------------------------------------|
| Estructura en `domain/usecase`               | 🟢 CUMPLE | Paquetes `usecase.spec` y `usecase.planning` creados correctamente.                   |
| Cero anotaciones de Spring en casos de uso   | 🟢 CUMPLE | Sin `@Service`, `@Component` ni `@Autowired`. Wiring pospuesto a Fase 06.             |
| Inyección de dependencias pura               | 🟢 CUMPLE | Atributos `final` con constructor `@RequiredArgsConstructor`.                         |
| Manejo reactivo y no bloqueante              | 🟢 CUMPLE | Uso de `Mono`, `Flux` y retorno no bloqueante con `AgentCommand.nonBlocking(...)`.    |
| Validación defensiva                         | 🟢 CUMPLE | Verificaciones tempranas emitiendo `Mono.error(IllegalArgumentException)` ante nulos. |
| Complejidad cognitiva $\le 15$               | 🟢 CUMPLE | Métodos concisos con complejidad cognitiva $\le 2$.                                   |
| Llaves `{}` en control de flujo              | 🟢 CUMPLE | Bloques `if` con llaves explícitas.                                                   |
| Pruebas GIVEN / WHEN / THEN con StepVerifier | 🟢 CUMPLE | 15 casos cubriendo happy path, comandos canónicos, validaciones y errores del agente. |
| `./gradlew test` verde al 100%               | 🟢 CUMPLE | 391/391 pruebas pasando exitosamente.                                                 |

---

## 8. Estado al Cerrar

- **Fase 04:** 🟢 **COMPLETADA**
- **Siguiente Fase:** 🟡 **Fase 05 — Entry-Points Reactivos y DTOs (
  `infrastructure/entry-points/reactive-web`)**
