# FASE 05 — Entry-Points Reactivos y DTOs (`infrastructure/entry-points/reactive-web`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 01, Fase 02, Fase 03, Fase 04 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(backend): exponer endpoints reactivos y DTOs para gestion documental y planeacion`  
> **Siguiente fase:** `fases/FASE-06-cableado-spring-validacion-e2e.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- En la **Fase 01** y **Fase 02** se crearon los modelos puros y puertos en `domain/model/spec` y
  `domain/model/planning`.
- En la **Fase 03** se implementó el adaptador reactivo `FileSystemSpecAdapter` en
  `infrastructure/driven-adapters/spec-storage`.
- En la **Fase 04** se implementaron y probaron al 100% los casos de uso:
    - `GetSpecDocumentUseCase`
    - `ListAvailableSpecsUseCase`
    - `TriggerProgramPlanningUseCase`

### 1.2 Problema que resuelve esta fase

Los casos de uso de negocio existen pero no están expuestos al exterior vía HTTP. Se requiere
construir los adaptadores de entrada reactivos en `infrastructure/entry-points/reactive-web`:

1. DTOs inmutables para desacoplar el contrato HTTP de las entidades de dominio.
2. Handlers reactivos (`SpecHandler`, `ProgramPlanningHandler`) que validen el payload, deleguen en
   los casos de uso y emitan respuestas consistentes (200 OK, 202 ACCEPTED, 400 Bad Request, 404 Not
   Found).
3. Configuración de rutas en `RouterRest.java` garantizando la decisión **DP-BFF-02** (coexistencia
   con las rutas legadas de `pgvector`).
4. Pruebas unitarias de slice web (`WebTestClient` o Mockito con ServerRequest simulado) con
   cobertura $\ge 90\%$.

### 1.3 Estado esperado al terminar

Existirán en `infrastructure/entry-points/reactive-web`:

1. **DTOs en `co.com.bancolombia.api.dto`:**
    - `ProgramPlanRequestDTO.java`
    - `ProgramPlanResponseDTO.java`
    - `SpecDocumentDTO.java`
    - `SpecListDTO.java`
2. **Handlers en `co.com.bancolombia.api`:**
    - `SpecHandler.java`: métodos `getSpec` y `listSpecs`.
    - `ProgramPlanningHandler.java`: método `triggerProgramPlanning`.
3. **Rutas en `RouterRest.java`:**
    - `GET /api/planning/specs` $\to$ `SpecHandler::listSpecs`
    - `GET /api/planning/specs/{specName}` $\to$ `SpecHandler::getSpec`
    - `POST /api/planning/program` $\to$ `ProgramPlanningHandler::triggerProgramPlanning`
4. Pruebas unitarias completas en `src/test/java/co/com/bancolombia/api/` con cobertura $\ge 90\%$.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Tareas a Ejecutar

#### T-01 · Diseñar e Implementar DTOs

**Ubicación:** `infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/dto/`

- Records o clases inmutables con validación de entrada o mapping limpio hacia los modelos de
  dominio (`ProgramPlanRequest`, `SpecDocument`).

#### T-02 · Implementar Handlers Reactivos

**Ubicación:** `infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/`

- `SpecHandler.java`
- `ProgramPlanningHandler.java`

#### T-03 · Registrar Rutas en RouterRest

**Ubicación:**
`infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/RouterRest.java`

- Añadir las nuevas rutas sin alterar las rutas legadas de iniciativas y búsqueda (`DP-BFF-02`).

#### T-04 · Pruebas Unitarias de Capa Web

**Ubicación:** `infrastructure/entry-points/reactive-web/src/test/java/co/com/bancolombia/api/`

- `SpecHandlerTest.java`
- `ProgramPlanningHandlerTest.java`

---

## 3. CRITERIOS DE ACEPTACIÓN

- [ ] Cero lógica de negocio en handlers; delegación inmediata a casos de uso.
- [ ] Rutas legadas `/api/planning/ingest`, `/api/planning/initiatives` y `/api/planning/search`
  intactas.
- [ ] `./gradlew :reactive-web:test` pasa al 100% con cobertura $\ge 90\%$.
- [ ] `./gradlew test` mantiene todas las pruebas pasando sin regresiones.
- [ ] `validateStructure` aprueba sin advertencias.
- [ ] Generación de `RESULTADO-FASE-05.md` y actualización de `ESTADO.md`.
