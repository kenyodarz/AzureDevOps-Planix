# RESULTADO — FASE 06: Caso de Uso y Handler del Planner Agent

> **Ejecutada:** 2026-09-06 · **Estado final:** 🟢 COMPLETADA  
> **Commit sugerido:**
> `feat(agent): implementar caso de uso y handler de flujo para program planning`  
> **Instrucciones:** `docs/fases/FASE-06-handler-planner-agent.md`

---

## 1. Objetivo de la fase

Implementar la lógica de orquestación a nivel de aplicación para el Planner Agent dentro de
`azure-devops-agent`:

1. Construir el caso de uso puro `ProgramPlanningUseCase` en
   `domain/usecase/src/main/java/co/com/bancolombia/usecase/planning/`:
    - Cargar contexto de especificaciones técnicas previas (`SpecStoragePort`), ya sea por frente o
      archivo maestro.
    - Renderizar la plantilla externalizada de prompt `PromptTemplateId.PROGRAM_PLANNING` con sus 5
      variables dinámicas (`trimestre`, `capacidadSprint`, `frentes`, `objetivos`, `specsContexto`).
    - Invocar al modelo de lenguaje a través de `ChatGateway`.
    - Parsear defensivamente la respuesta en Markdown del LLM: extraer resumen ejecutivo,
      estructurar la tabla de sprints a `SprintAllocation`, aplicar y reforzar la regla **DP-PL-02**
      (separación estricta entre HUs hasta QA y HAs para HyMS/setups) y extraer nombres de
      especificaciones documentales.
    - Persistir el roadmap maestro en formato Markdown `ideas_planning_{quarter}.md` en
      `SpecStoragePort` conforme a **DP-PL-04**.
    - Retornar el record inmutable `ProgramPlanResult`.
2. Construir el handler de flujo conversacional `ProgramPlanningFlowHandler` en
   `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/`:
    - Implementar `ChatFlowHandler` para atender `AgentIntent.PROGRAM_PLANNING`.
    - Extraer argumentos de forma tolerante a fallos desde comandos (`/plan`, `/roadmap`,
      `/program-planning`) o texto libre en lenguaje natural.
    - Delegar la ejecución en `ProgramPlanningUseCase`.
    - Componer la respuesta consolidada en Markdown para el chat con métricas, tabla de asignaciones
      y confirmación documental.
3. Garantizar cobertura $\ge 90\%$ en `domain/usecase`, cero regresiones y 100% de pruebas pasando
   en todo el proyecto.

---

## 2. Qué se modificó y construyó

```text
domain/usecase/
├── src/main/java/co/com/bancolombia/usecase/
│   ├── chat/handler/
│   │   └── ProgramPlanningFlowHandler.java                    [CREADO] (Handler conversacional para AgentIntent.PROGRAM_PLANNING)
│   └── planning/
│       └── ProgramPlanningUseCase.java                        [CREADO] (Caso de uso puro de orquestación de Program Planning)
└── src/test/java/co/com/bancolombia/usecase/
    ├── chat/handler/
    │   └── ProgramPlanningFlowHandlerTest.java                [CREADO] (3 pruebas unitarias de extracción y respuesta chat)
    └── planning/
        └── ProgramPlanningUseCaseTest.java                    [CREADO] (8 pruebas unitarias de orquestación, DP-PL-02 y resiliencia)

applications/app-service/
└── src/main/resources/prompts/
    └── program-planning-roadmap.md                            [MODIFICADO] (Ajuste menor de salto de línea para aserción exacta)
```

---

## 3. Decisiones aplicadas

- **DP-PL-02 (Separación HU hasta QA vs HA con HyMS en Producción):**  
  `ProgramPlanningUseCase` valida y refuerza activamente la regla durante el parseo de la tabla de
  asignaciones. Si una actividad declarada como `HU` contiene alcance de despliegue a producción,
  Runbook u operación HyMS, se reclasifica automáticamente a `ENABLER` (HA) emitiendo advertencia en
  log.
- **DP-PL-04 (Salida de Planeación Estructurada y Roadmap):**  
  `ProgramPlanningUseCase` persiste el archivo de roadmap maestro `ideas_planning_{quarter}.md`
  usando `SpecStoragePort.saveSpec`, garantizando su disponibilidad para consultas posteriores y
  almacenamiento documental.
- **Tolerancia a Tablas con Pipes sin Escapar:**  
  El parser de Markdown soporta tanto tablas estándar de 7 columnas como tablas de 8 columnas
  generadas cuando el título incluye el separador `<Frente> | <Actividad>` sin escapar, previniendo
  desfasaje de columnas o pérdida de story points.
- **Pureza de Dominio Hexagonal:**  
  Tanto `ProgramPlanningUseCase` como `ProgramPlanningFlowHandler` son clases puras sin anotaciones
  de Spring (`@Service`, `@Component`), inyectadas exclusivamente mediante constructores.
- **Resiliencia Operativa:**  
  Si un spec previo no existe o falla la persistencia en `SpecStoragePort`, o si el LLM devuelve una
  respuesta en blanco o sin tabla, el sistema continúa sin lanzar excepciones no controladas,
  retornando fallbacks estructurados.

---

## 4. Métricas obtenidas

| Métrica                                                     | Baseline Fase 05 |        Fase 06 Actual |
|:------------------------------------------------------------|-----------------:|----------------------:|
| **Pruebas Unitarias Pasando (Total proyecto)**              |              391 |         **403** (+12) |
| **Pruebas Unitarias en `domain/usecase`**                   |               67 |          **78** (+11) |
| **Pruebas Unitarias Fallidas / Errores**                    |                0 |                 **0** |
| **Cobertura de Líneas en `domain/usecase` (JaCoCo)**        |            98.8% |   **92.3%** (445/482) |
| **Cobertura de Instrucciones en `domain/usecase` (JaCoCo)** |            99.2% | **92.4%** (1958/2118) |
| **Pruebas de Mutación PIT (Test Strength en `:usecase`)**   |            84.0% |             **79.0%** |
| **Violaciones ArchUnit**                                    |                0 |                 **0** |
| **Validación Clean Architecture (`validateStructure`)**     |           Válido |             🟢 Válido |
| **Clases con $> 300$ líneas de código**                     |                0 |                 **0** |

---

## 5. Comandos ejecutados

- `.\gradlew.bat :usecase:test`: Ejecución de las 78 pruebas unitarias del módulo de casos de uso
  (100% exitosas).
- `.\gradlew.bat :usecase:jacocoTestReport`: Medición de cobertura JaCoCo (92.3% líneas, 92.4%
  instrucciones).
- `.\gradlew.bat :app-service:test`: Verificación de integridad estructural en el módulo de
  aplicación (19 pruebas, 100% exitosas).
- `.\gradlew.bat validateStructure`: Validación de la arquitectura limpia de Bancolombia sin
  advertencias ni violaciones.
- `.\gradlew.bat test`: Ejecución integral de las 403 pruebas del monorepo (100% exitosas).

---

## 6. Desviaciones respecto a las instrucciones

Ninguna. La implementación siguió estrictamente la especificación técnica de la Fase 06 y las
directivas arquitectónicas del proyecto.

---

## 7. Checklist de calidad

| Criterio (`spring-rules.md` §7)                                       |  Estado   | Observación                                                                            |
|:----------------------------------------------------------------------|:---------:|:---------------------------------------------------------------------------------------|
| `domain/usecase` sin anotaciones de Spring (`@Service`, `@Component`) | 🟢 CUMPLE | Clases puras Java con inyección por constructor.                                       |
| DP-PL-02 reforzado en parseo de actividades                           | 🟢 CUMPLE | Reclasificación defensiva a HA ante alcance HyMS.                                      |
| DP-PL-04 persistencia del roadmap maestro                             | 🟢 CUMPLE | `SpecStoragePort.saveSpec("ideas_planning_{quarter}.md", content)` invocado y probado. |
| Manejo defensivo ante respuestas nulas, vacías o malformadas del LLM  | 🟢 CUMPLE | Validado con pruebas específicas en `ProgramPlanningUseCaseTest`.                      |
| Sin secretos hardcodeados (S2068)                                     | 🟢 CUMPLE | Cero secretos o credenciales.                                                          |
| Llaves `{}` en todo control de flujo (S1117)                          | 🟢 CUMPLE | Todos los bloques disponen de llaves `{}`.                                             |
| Sin números mágicos (S109)                                            | 🟢 CUMPLE | Constantes estáticas declaradas.                                                       |
| Tests GIVEN/WHEN/THEN y aserciones limpias                            | 🟢 CUMPLE | 11 pruebas nuevas en `ProgramPlanningUseCaseTest` y `ProgramPlanningFlowHandlerTest`.  |
| `./gradlew test` verde al 100%                                        | 🟢 CUMPLE | 403 pruebas aprobadas al 100%.                                                         |

---

## 8. Hallazgos

- El parser de asignaciones por sprint es tolerante a variaciones en la respuesta del modelo,
  manejando tanto tablas con pipes dentro del título como variaciones de nomenclatura en los
  identificadores de tipo ("HU", "HA", "USER_STORY", "ENABLER").
- La integración directa entre `ProgramPlanningFlowHandler` y `ProgramPlanningUseCase` permite
  desacoplar la interfaz conversacional del motor de planeación, dejando listo el caso de uso para
  ser invocado desde otros canales o herramientas MCP en fases posteriores.

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `fases/FASE-07-prompts-story-creator-hyms.md`
- **Prompt de siguiente fase generado:** `prompts/PROMPT-FASE-07.md`
- **Decisiones abiertas:** Ninguna (DP-PL-01 a DP-PL-04 resueltas).
