# RESULTADO — FASE 05: Prompt Externalizado de Planeación (`roadmap`)

> **Ejecutada:** 2026-09-06 · **Estado final:** 🟢 COMPLETADA  
> **Commit sugerido:**
> `feat(agent): agregar plantilla externalizada de prompt para program planning y roadmap`  
> **Instrucciones:** `docs/fases/FASE-05-prompt-externalizado-planeacion.md`

---

## 1. Objetivo de la fase

Externalizar la plantilla de prompt para la planeación macro trimestral
(`program-planning-roadmap.md`) en el submódulo `applications/app-service`, incorporando las
directivas estratégicas del rol de Agile Planner y Enterprise Architect de Bancolombia.

Aplicar de forma estricta las reglas de negocio de **DP-PL-02**: separación taxativa entre Historias
de Usuario (`HU`, funcionales construidas y probadas hasta ambiente **QA**) e Historias
Habilitadoras (`HA`, setups técnicos y proceso formal de **Paso a Producción bajo marco HyMS** con
ciberseguridad, observabilidad y Runbook de soporte operativo).

Aplicar el formato estructurado de **DP-PL-04** para guiar al modelo a generar el resumen ejecutivo,
la tabla de roadmap por sprints con story points Fibonacci (1, 2, 3, 5, 8, 13) y dependencias
secuenciales, y las especificaciones por frente.

Extender el enum `PromptTemplateId` en `domain/model` con `PROGRAM_PLANNING` preservando la pureza
de dominio, registrar el archivo en `ClasspathPromptTemplateAdapter.TEMPLATE_FILES`, suministrar la
plantilla sintética de prueba en `test-prompts` y robustecer la suite de pruebas unitarias en
`:prompt-template` y `:app-service` para verificar la sustitución total de los placeholders:
`{{trimestre}}`, `{{objetivos}}`, `{{frentes}}`, `{{capacidadSprint}}` y `{{specsContexto}}`.

---

## 2. Qué se modificó y construyó

```text
domain/model/
└── src/main/java/co/com/bancolombia/model/prompt/
    └── PromptTemplateId.java                                   [MODIFICADO] (Agregado valor PROGRAM_PLANNING con javadoc)

applications/app-service/
├── src/main/resources/prompts/
│   └── program-planning-roadmap.md                            [CREADO] (Plantilla Markdown con reglas DP-PL-02 y DP-PL-04)
└── src/test/java/co/com/bancolombia/prompt/
    └── PromptTemplateContentTest.java                         [MODIFICADO] (Pruebas de contenido normativo DP-PL-02 y DP-PL-04)

infrastructure/driven-adapters/prompt-template/
├── src/main/java/co/com/bancolombia/prompttemplate/
│   └── ClasspathPromptTemplateAdapter.java                    [MODIFICADO] (Registro de PROGRAM_PLANNING en TEMPLATE_FILES)
├── src/test/resources/test-prompts/
│   └── program-planning-roadmap.md                            [CREADO] (Plantilla sintética de prueba para suite aislada)
└── src/test/java/co/com/bancolombia/prompttemplate/
    └── ClasspathPromptTemplateAdapterTest.java                [MODIFICADO] (Prueba de renderizado libre de placeholders y exhaustividad)
```

---

## 3. Decisiones aplicadas

- **DP-PL-02 (Separación HU hasta QA vs HA con HyMS en Producción):**  
  La plantilla de producción define con claridad no negociable que las `HU` solo contemplan diseño,
  desarrollo y pruebas hasta QA, mientras que las `HA` cubren los habilitadores técnicos y el
  despliegue a producción formal bajo HyMS (Runbook de operaciones, observabilidad y
  ciberseguridad). Se prohíbe explícitamente mezclar ambas en una misma historia.
- **DP-PL-04 (Salida de Planeación Estructurada y Roadmap):**  
  La plantilla delimita las secciones obligatorias: Resumen Ejecutivo, Tabla de Roadmap por Sprints
  (con columnas para Sprint, Tipo HU/HA, Título `<Frente> | <Nombre>`, Story Points Fibonacci,
  Frente, Dependencias y Criterio de Entrega), respetando la capacidad máxima por sprint
  `{{capacidadSprint}}`, y el desglose de especificaciones documentales por cada frente involucrado.
- **Validación Preventiva de Placeholders (Fail-Fast):**  
  `ClasspathPromptTemplateAdapter` asegura que todos los placeholders declarados (`{{trimestre}}`,
  `{{objetivos}}`, `{{frentes}}`, `{{capacidadSprint}}`, `{{specsContexto}}`) se encuentren
  presentes en el mapa de entrada antes del renderizado, impidiendo el envío de prompts a medio
  componer al LLM.
- **Pureza Hexagonal:**  
  `domain/model` (`PromptTemplateId`) se mantiene 100% libre de frameworks o dependencias técnicas.

---

## 4. Métricas obtenidas

| Métrica                                                           | Baseline Fase 04 |             Fase 05 Actual |
|:------------------------------------------------------------------|-----------------:|---------------------------:|
| **Pruebas Unitarias Pasando (Total proyecto)**                    |              389 |               **391** (+2) |
| **Pruebas Unitarias Fallidas / Errores**                          |                0 |                      **0** |
| **Cobertura de Líneas en `:prompt-template`**                     |            93.3% |         **93.33%** (42/45) |
| **Pruebas de Mutación PIT (Test Strength en `:prompt-template`)** |           100.0% | **100.00%** (10/10 killed) |
| **Violaciones ArchUnit**                                          |                0 |                      **0** |
| **Validación Clean Architecture (`validateStructure`)**           |           Válido |                  🟢 Válido |
| **Clases con $> 300$ líneas de código**                           |                0 |                      **0** |

---

## 5. Comandos ejecutados

- `.\gradlew.bat :prompt-template:test`: Verificación de renderizado, validación de variables
  requeridas y mutación PIT en el adaptador de plantillas (93.3% cobertura, 100% mutaciones
  eliminadas).
- `.\gradlew.bat :app-service:test`: Verificación de integridad estructural y reglas normativas de
  la plantilla real en `PromptTemplateContentTest`.
- `.\gradlew.bat validateStructure`: Validación de la arquitectura limpia de Bancolombia sin
  advertencias ni violaciones.
- `.\gradlew.bat test`: Ejecución integral de las 391 pruebas del monorepo (100% exitosas).

---

## 6. Desviaciones respecto a las instrucciones

Ninguna. La implementación siguió estrictamente la especificación técnica de la Fase 05 y los
requerimientos solicitados.

---

## 7. Checklist de calidad

| Criterio (`spring-rules.md` §7)                                          |  Estado   | Observación                                                                                  |
|:-------------------------------------------------------------------------|:---------:|:---------------------------------------------------------------------------------------------|
| `domain/model` sin dependencias de Spring, persistencia ni serialización | 🟢 CUMPLE | `PromptTemplateId` puro Java.                                                                |
| Fail-fast de marcadores sin resolver                                     | 🟢 CUMPLE | `ClasspathPromptTemplateAdapter` valida y lanza `PromptTemplateException` si falta valor.    |
| Sin secretos hardcodeados (S2068)                                        | 🟢 CUMPLE | Cero secretos o credenciales.                                                                |
| Llaves `{}` en todo control de flujo (S1117)                             | 🟢 CUMPLE | Todos los bloques disponen de llaves `{}`.                                                   |
| Sin números mágicos (S109)                                               | 🟢 CUMPLE | Constantes estáticas declaradas.                                                             |
| Tests GIVEN/WHEN/THEN y aserciones limpias                               | 🟢 CUMPLE | Pruebas estructuradas en `ClasspathPromptTemplateAdapterTest` y `PromptTemplateContentTest`. |
| `./gradlew build` / `./gradlew test` verde                               | 🟢 CUMPLE | 391 pruebas aprobadas al 100%.                                                               |

---

## 8. Hallazgos

- La externalización de `program-planning-roadmap.md` desacopla por completo la ingeniería de
  prompts del código Java, permitiendo ajustar directivas de agilidad y formato sin recompilar la
  lógica del negocio.
- La incorporación de validaciones en `PromptTemplateContentTest` asegura que los cambios futuros en
  las plantillas no violen accidentalmente las decisiones corporativas de agilidad (DP-PL-02 y
  DP-PL-04).

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `fases/FASE-06-handler-planner-agent.md`
- **Prompt de siguiente fase generado:** `prompts/PROMPT-FASE-06.md`
- **Decisiones abiertas:** Ninguna (DP-PL-01 a DP-PL-04 resueltas).
