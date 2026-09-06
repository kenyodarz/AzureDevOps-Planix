# FASE 06 — Cableado en Spring (`app-service`), Validación E2E y Cierre Definitivo

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 01, Fase 02, Fase 03, Fase 04, Fase 05 ·
> **Riesgo:** Bajo  
> **Commit al cerrar:**
>
`feat(backend): configurar wiring en spring para use cases de specs y program planning con validacion e2e`  
> **Siguiente fase:** Cierre Definitivo del Plan Maestro

---

## 1. CONTEXTO

### 1.1 De dónde venimos

- **Fase 01 & Fase 02:** Modelos y contratos puros en `domain/model` (`SpecDocument`,
  `SpecNotFoundException`, `SpecStoragePort`, `ProgramPlanRequest`, `ProgramPlanResult`,
  `ProgramPlanningCommand`).
- **Fase 03:** Implementación del adaptador reactivo en disco `FileSystemSpecAdapter` en
  `infrastructure/driven-adapters/spec-storage`.
- **Fase 04:** Casos de uso de negocio en `domain/usecase` (`GetSpecDocumentUseCase`,
  `ListAvailableSpecsUseCase`, `TriggerProgramPlanningUseCase`).
- **Fase 05:** DTOs inmutables, handlers reactivos (`SpecHandler`, `ProgramPlanningHandler`), rutas
  en `RouterRest.java` (DP-BFF-02) y mapeo en `ApiErrorTranslator` con 100% de éxito en pruebas
  unitarias y 93% de cobertura.

### 1.2 Problema que resuelve esta fase

Los componentes desarrollados a lo largo de las fases 01 a 05 están probados unitariamente de manera
aislada, pero el contenedor de Spring Boot en `applications/app-service` aún no los cablea como
beans para la ejecución en producción:

1. Se deben declarar los `@Bean` de los casos de uso en las clases de configuración de
   `applications/app-service` (`UseCasesConfig.java`).
2. Se debe configurar la ruta de almacenamiento de especificaciones en `application.yaml`
   (`specs.storage.path: docs/specs`).
3. Se debe verificar el arranque del contexto de Spring completo (`MainApplicationTests` /
   `@SpringBootTest`) y realizar pruebas de integración que validen el flujo completo end-to-end.
4. Generar el reporte final de cierre que consolide el cumplimiento de todas las metas del plan
   maestro.

### 1.3 Estado esperado al terminar

1. `UseCasesConfig.java` en `applications/app-service` expone los beans:
    - `getSpecDocumentUseCase(SpecStoragePort specStoragePort)`
    - `listAvailableSpecsUseCase(SpecStoragePort specStoragePort)`
    - `triggerProgramPlanningUseCase(TrackAgentTaskUseCase trackAgentTaskUseCase)`
2. `application.yaml` contiene las propiedades
   `specs.storage.path: "${SPECS_STORAGE_PATH:docs/specs}"`.
3. El contexto de Spring Boot arranca limpiamente en `app-service`.
4. `./gradlew test` y `./gradlew validateStructure` pasan al 100% sin advertencias.
5. `RESULTADO-FASE-06.md` y `REPORTE_FINAL_INTEGRACION_PLANNING_BFF.md` creados.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Tareas a Ejecutar

#### T-01 · Wiring en `applications/app-service`

**Ubicación:**
`applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`

- Registrar los métodos `@Bean` para instanciar los 3 casos de uso nuevos inyectando
  `SpecStoragePort` y `TrackAgentTaskUseCase`.

#### T-02 · Parámetros en `application.yaml`

**Ubicación:** `applications/app-service/src/main/resources/application.yaml`

- Asegurar la propiedad `specs.storage.path: "${SPECS_STORAGE_PATH:docs/specs}"`.

#### T-03 · Pruebas de Integración y Context Load

**Ubicación:** `applications/app-service/src/test/java/co/com/bancolombia/`

- Validar que los tests de configuración (`UseCasesConfigTest`) y el arranque del contexto
  (`MainApplicationTest` o similar) reconozcan los nuevos beans sin conflictos.

#### T-04 · Cierre y Verificación Global

- Ejecutar `./gradlew test` (verificando que los 408+ tests pasen).
- Ejecutar `./gradlew validateStructure`.
- Generar `docs/resultados/RESULTADO-FASE-06.md`.

---

## 3. CRITERIOS DE ACEPTACIÓN

- [ ] Wiring completo en `UseCasesConfig.java` sin anotaciones de Spring en la capa de casos de uso.
- [ ] Configuración parametrizable en `application.yaml`.
- [ ] 100% de pruebas unitarias y de integración pasando en todo el monorepo backend.
- [ ] `validateStructure` aprueba sin advertencias.
- [ ] Cierre definitivo del plan maestro y actualización de `ESTADO.md`.
