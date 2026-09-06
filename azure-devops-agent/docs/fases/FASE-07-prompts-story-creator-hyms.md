# FASE 07 — Especialización de Prompts Story Creator con Regla HyMS

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 05, FASE 06 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(agent): especializar prompts de story creator con reglas de negocio HyMS y QA (DP-PL-02)`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* La **Fase 05** externalizó la plantilla de planeación macro `program-planning-roadmap.md`,
  consagrando formalmente la regla **DP-PL-02** (HU hasta QA vs HA con HyMS en Producción).
* La **Fase 06** implementó `ProgramPlanningUseCase` y `ProgramPlanningFlowHandler`, que orquestan
  la generación y almacenamiento del roadmap trimestral asegurando la separación estricta de HUs y
  HAs.
* Sin embargo, la plantilla downstream responsable de generar el detalle de historias estructuradas
  (`fase2-historia-estructurada.md` asociada a `PromptTemplateId.STRUCTURED_STORY`) no cuenta aún
  con las directivas especializadas de **DP-PL-02** ni con la inyección contextual de frentes
  (**DP-PL-03**).

### 1.2 Problema que resuelve esta fase

* Cuando el usuario o un agente orquestador solicita crear el detalle de una Historia de Usuario o
  Historia Habilitadora a partir del roadmap o de una idea, el Story Creator actual puede mezclar
  criterios de desarrollo con despliegue a producción, o no incluir los artefactos mandatorios de
  HyMS (Runbook, observabilidad, mesas de control) en las HAs.
* Es necesario especializar las directivas de generación en las plantillas de historias
  (`fase2-historia-estructurada.md` y/o fragmentos de criterios de aceptación):
    1. Si la actividad es **HU (USER_STORY)**: Criterios de aceptación orientados a diseño,
       desarrollo, pruebas unitarias/integradas y aprobación en ambiente **QA**, prohibiendo
       expresamente actividades productivas.
    2. Si la actividad es **HA (ENABLER)**: Estructuración de tareas técnicas previas o el checklist
       formal de **Paso a Producción bajo marco corporativo HyMS** (ciberseguridad, observabilidad,
       Runbook de operación y soporte post-producción).
    3. Soporte para inyección de especificación técnica del frente (`specsContexto` o contexto del
       frente según **DP-PL-03**).

### 1.3 Estado esperado al terminar

* Plantilla `fase2-historia-estructurada.md` (y/o fragmentos asociados) enriquecida con las reglas
  taxativas de **DP-PL-02**.
* Verificación de la presencia de directivas HyMS y QA en `PromptTemplateContentTest`.
* Compatibilidad con `PromptTemplatePort` garantizada sin alterar firmas públicas de dominio.
* 100% de pruebas pasando en todo el proyecto (mínimo 403 pruebas) y cero regresiones.

### 1.4 Archivos involucrados

| Ruta                                                                                              |  Acción   | Propósito                                                                               |
|:--------------------------------------------------------------------------------------------------|:---------:|:----------------------------------------------------------------------------------------|
| `applications/app-service/src/main/resources/prompts/fase2-historia-estructurada.md`              | MODIFICAR | Incorporar directivas normativas de DP-PL-02 (alcance QA para HU vs HyMS para HA).      |
| `applications/app-service/src/test/java/co/com/bancolombia/prompt/PromptTemplateContentTest.java` | MODIFICAR | Añadir pruebas normativas verificando directivas DP-PL-02 en la plantilla estructurada. |
| `docs/plan/ESTADO.md`                                                                             | MODIFICAR | Actualizar tablero y bitácora del plan.                                                 |
| `docs/resultados/RESULTADO-FASE-07.md`                                                            |   CREAR   | Documento de cierre con métricas de la Fase 07.                                         |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §4 (Calidad de Código): Formato limpio, sin caracteres especiales o variables residuales.
    - §5 (Testing): Pruebas con aserciones exactas y desacopladas de frameworks externos.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. Actualizar `fase2-historia-estructurada.md`:
    - Añadir sección de directivas DP-PL-02 delimitando el alcance de HUs (construcción y QA) y HAs
      (HyMS, setups, Runbook).
2. Actualizar `PromptTemplateContentTest`:
    - Validar que la plantilla estructurada contenga las cláusulas obligatorias de DP-PL-02 y no
      contenga marcadores rotos.
3. Ejecutar `./gradlew test` y `./gradlew validateStructure` para certificar la estabilidad completa
   del proyecto.
4. Generar el artefacto de resultado `RESULTADO-FASE-07.md` y preparar la Fase 08
   (`Orquestación y Despacho del Harness`).

### 2.2 Qué NO hacer

* No alterar firmas ni contratos de puertos de dominio existentes.
* No relajar las pruebas existentes de `PromptTemplateContentTest`.

### 2.3 Criterios de Aceptación

- [ ] `fase2-historia-estructurada.md` actualizada con directivas DP-PL-02.
- [ ] Pruebas en `PromptTemplateContentTest` verificando la especialización de HU/HA.
- [ ] 100% de pruebas pasando en todo el proyecto.
- [ ] `validateStructure` pasa sin advertencias.
