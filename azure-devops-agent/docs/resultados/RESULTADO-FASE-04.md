# RESULTADO — FASE 04: Modelos de Dominio para Program Planning (`domain/model`)

> **Ejecutada:** 2026-09-06 · **Estado final:** 🟢 COMPLETADA  
> **Commit sugerido:**
> `feat(agent): agregar modelos de dominio y resolutor para program planning`
> **Instrucciones:** `docs/fases/FASE-04-modelos-dominio-planeacion.md`

---

## 1. Objetivo de la fase

Incorporar en el núcleo puro del dominio (`domain/model`) los modelos conceptuales, enums y reglas
de negocio requeridas para orquestar la planeación macro a nivel de programa (Program Planning y
generación de roadmaps trimestrales/sprints con distribución de HU y HA bajo **DP-PL-02** y
**DP-PL-04**).

Extender el enum `AgentIntent` con `PROGRAM_PLANNING`, implementar el enum `ActivityType`
tipificando
Historias de Usuario (`USER_STORY` / `"HU"`) e Historias Habilitadoras (`ENABLER` / `"HA"`), y crear
los records inmutables `SprintAllocation`, `ProgramPlanRequest` y `ProgramPlanResult` con validación
defensiva de invariantes en constructores compactos.

Actualizar el servicio puro `IntentResolver` para reconocer comandos explícitos (`/plan`,
`/roadmap`,
`/program-planning`) y expresiones compuestas de planeación trimestral (`planear q3`, `roadmap q1`,
`generar roadmap`), asegurando que la tabla de precedencia funcione de manera determinista y sin
regresiones.

---

## 2. Qué se modificó y construyó

```text
domain/model/
├── src/main/java/co/com/bancolombia/model/
│   ├── agent/
│   │   ├── AgentIntent.java                                [MODIFICADO] (Agregado PROGRAM_PLANNING con javadoc)
│   │   └── IntentResolver.java                             [MODIFICADO] (Resolución de comandos y frases de planeación macro)
│   └── planning/
│       ├── ActivityType.java                               [CREADO] (Enum con USER_STORY/HU y ENABLER/HA según DP-PL-02)
│       ├── SprintAllocation.java                           [CREADO] (Record inmutable de asignación a sprint con invariantes)
│       ├── ProgramPlanRequest.java                         [CREADO] (Record inmutable de solicitud de planeación trimestral)
│       └── ProgramPlanResult.java                          [CREADO] (Record inmutable de resultado estructurado con helpers)
└── src/test/java/co/com/bancolombia/model/
    ├── agent/
    │   └── IntentResolverTest.java                         [MODIFICADO] (Suite exhaustiva de pruebas para PROGRAM_PLANNING)
    └── planning/
        └── ProgramPlanningModelsTest.java                  [CREADO] (Pruebas de invariantes, inmutabilidad y métodos de conveniencia)

domain/usecase/
└── src/main/java/co/com/bancolombia/usecase/chat/handler/
    └── ChatFlowDispatcher.java                             [MODIFICADO] (Omitir PROGRAM_PLANNING en fail-fast hasta Fase 06)

applications/app-service/
└── src/test/java/co/com/bancolombia/config/
    └── UseCasesConfigWiringTest.java                       [MODIFICADO] (Alineación con los handlers activos excluyendo PROGRAM_PLANNING hasta Fase 08)
```

---

## 3. Decisiones aplicadas

- **DP-PL-02 (Separación de Alcance: HU hasta QA vs HA con HyMS en Producción):**  
  `ActivityType` define explícitamente `USER_STORY` (tag `"HU"`) para construir funcionalidad hasta
  QA y `ENABLER` (tag `"HA"`) para setups técnicos y el proceso formal de paso a producción con
  HyMS.
  El método `fromTag()` resuelve de forma flexible y segura ante tags en mayúsculas o minúsculas.
- **DP-PL-04 (Salida de Planeación Estructurada y Roadmap):**  
  `ProgramPlanResult` y `SprintAllocation` proporcionan el modelo tipado que soportará la generación
  del archivo `ideas_planning_QX.md` y los specs por frente en las Fases 05 y 06, incluyendo métodos
  de cálculo como `totalStoryPoints()`, `allocationsForSprint()` y `allocationsForType()`.
- **Invariantes Defensivos en Records de Dominio:**  
  Todos los records (`SprintAllocation`, `ProgramPlanRequest`, `ProgramPlanResult`) validan sus
  invariantes en constructores compactos arrojando `IllegalArgumentException` ante campos nulos,
  vacíos o fuera de rango, y normalizan listas a copias defensivas inmutables (`List.copyOf`).
- **Precedencia en `IntentResolver`:**  
  La planeación de programa se evalúa con alta precedencia (antes de las consultas generales),
  permitiendo que comandos (`/plan`, `/roadmap`) y expresiones (`planear q3`, `generar roadmap`)
  prevalezcan incluso cuando coexisten con palabras clave débiles como `"lista"`.
- **Pureza Hexagonal:**  
  `domain/model` permanece 100% puro: cero anotaciones de Spring, Jackson, JPA ni dependencias
  externas.

---

## 4. Métricas obtenidas

| Métrica                                                 | Baseline Fase 03 |             Fase 04 Actual |
|:--------------------------------------------------------|-----------------:|---------------------------:|
| **Pruebas Unitarias Pasando (Total proyecto)**          |              334 |              **389** (+55) |
| **Pruebas Unitarias Fallidas / Errores**                |                0 |                      **0** |
| **Cobertura de Líneas en `domain/model`**               |            93.2% |       **95.09%** (252/265) |
| **Cobertura de Instrucciones en `domain/model`**        |            93.0% |     **95.05%** (1209/1272) |
| **Pruebas de Mutación PIT (Test Strength en `:model`)** |            95.2% | **100.00%** (93/93 killed) |
| **Violaciones ArchUnit**                                |                0 |                      **0** |
| **Validación Clean Architecture (`validateStructure`)** |           Válido |                  🟢 Válido |
| **Clases con $> 300$ líneas de código**                 |                0 |                      **0** |

---

## 5. Comandos ejecutados

- `.\gradlew.bat :model:test`: Verificación de compilación, ejecución de pruebas unitarias y
  mutación
  PIT en el submódulo de modelo (95.09% cobertura, 100% mutaciones eliminadas).
- `.\gradlew.bat validateStructure`: Validación de la arquitectura limpia de Bancolombia sin
  advertencias.
- `.\gradlew.bat :app-service:test`: Verificación del arranque del contexto de Spring Boot y
  cableado.
- `.\gradlew.bat test`: Ejecución integral de las 389 pruebas del monorepo (100% exitosas).

---

## 6. Desviaciones respecto a las instrucciones

| Desviación                                                                               | Justificación                                                                                                                                                                                                                                                                                                                                                                                                 |
|:-----------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Ajuste preventivo en `ChatFlowDispatcher.requireExhaustive` y `UseCasesConfigWiringTest` | Al agregar `AgentIntent.PROGRAM_PLANNING`, el fail-fast exhaustivo del dispatcher habría provocado un fallo de arranque en los tests de contexto de Spring debido a que el handler formal (`ProgramPlanningFlowHandler`) está planificado para implementarse en la Fase 06 y cablearse en la Fase 08. Se exceptuó `PROGRAM_PLANNING` temporalmente, protegiendo las pruebas sin alterar la pureza del modelo. |

---

## 7. Checklist de calidad

| Criterio (`spring-rules.md` §7)                                          |  Estado   | Observación                                                                              |
|:-------------------------------------------------------------------------|:---------:|:-----------------------------------------------------------------------------------------|
| `domain/model` sin dependencias de Spring, persistencia ni serialización | 🟢 CUMPLE | 100% puro Java y Lombok básico (`@Builder`).                                             |
| Invariantes defensivos en constructores compactos                        | 🟢 CUMPLE | Validación exhaustiva con `IllegalArgumentException` y copias inmutables defensivas.     |
| Sin secretos hardcodeados (S2068)                                        | 🟢 CUMPLE | Cero secretos o credenciales.                                                            |
| Llaves `{}` en todo control de flujo (S1117)                             | 🟢 CUMPLE | Todos los bloques `if` y `for` disponen de llaves explícitas.                            |
| Sin números mágicos (S109)                                               | 🟢 CUMPLE | Constantes estáticas declaradas para patrones regex y umbrales.                          |
| Tests GIVEN/WHEN/THEN y aserciones limpias                               | 🟢 CUMPLE | Pruebas estructuradas en `ProgramPlanningModelsTest` e `IntentResolverTest` con AssertJ. |
| Expresiones regulares deterministas                                      | 🟢 CUMPLE | Patrones estáticos precompilados sin riesgo de backtracking catastrófico.                |
| `./gradlew build` / `./gradlew test` verde                               | 🟢 CUMPLE | 389 pruebas aprobadas al 100%.                                                           |

---

## 8. Hallazgos

- El modelado explícito de `ActivityType` resuelve definitivamente la ambigüedad operativa entre
  historias de construcción funcional (`HU`) e historias habilitadoras/despliegue HyMS (`HA`),
  estableciendo una base sólida para que el prompt de la Fase 05 y el agente de la Fase 06
  distribuyan
  el trabajo con claridad.
- La incorporación de métodos de análisis en `ProgramPlanResult` (`totalStoryPoints()`,
  `allocationsForSprint()`, etc.) enriquece el modelo evitando modelos anémicos y facilitando la
  futura generación de tablas Markdown estructuradas.

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `fases/FASE-05-prompt-externalizado-planeacion.md`
- **Prompt de siguiente fase generado:** `prompts/PROMPT-FASE-05.md`
- **Decisiones abiertas:** Ninguna (DP-PL-01 a DP-PL-04 resueltas).
