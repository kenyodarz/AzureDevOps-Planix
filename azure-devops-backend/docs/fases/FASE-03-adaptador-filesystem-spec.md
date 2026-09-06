# FASE 03 — Adaptador Concreto FileSystemSpecAdapter (`infrastructure/driven-adapters`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Fase 01, Fase 02 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(backend): implementar adaptador reactivo FileSystemSpecAdapter para spec storage`  
> **Siguiente fase:** `fases/FASE-04-casos-de-uso-planeacion.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

En la **Fase 01** se definió el puerto de dominio `SpecStoragePort` y las entidades `SpecDocument` y
`SpecNotFoundException` en `co.com.bancolombia.model.spec`.  
En la **Fase 02** se implementaron los modelos de planeación `ActivityType`, `SprintAllocation`,
`ProgramPlanRequest`, `ProgramPlanResult` y `ProgramPlanningCommand` en
`co.com.bancolombia.model.planning`.

### 1.2 Problema que resuelve esta fase

Para que el BFF (`azure-devops-backend`) pueda leer y persistir especificaciones funcionales
(`ideas_planning_QX.md`, `frente_*.md`) compartidas con el agente sin dependencias de red ni
servicios externos (resolviendo la decisión **DP-BFF-01**), se requiere un adaptador de
infraestructura concreto que implemente `SpecStoragePort` sobre el sistema de archivos local de
forma puramente reactiva, asíncrona y segura.

### 1.3 Estado esperado al terminar

Existirá en `infrastructure/driven-adapters/spec-storage`:

1. El submódulo Gradle `:spec-storage` registrado en `settings.gradle` y configurado con
   dependencias de `:model`, Spring Context y Reactor.
2. La clase `@Repository` / `@Component` `FileSystemSpecAdapter` que implementa `SpecStoragePort`
   con:
    - Configuración de ruta base inyectada vía `@Value("${specs.storage.path:specs}")`.
    - Protección estricta contra Path Traversal / Directory Traversal.
    - Operaciones I/O no bloqueantes delegadas a `Schedulers.boundedElastic()`.
    - Manejo de `SpecNotFoundException` ante archivos inexistentes.
3. Suite de pruebas unitarias reactivas `FileSystemSpecAdapterTest` con JUnit 5, `@TempDir`, AssertJ
   y `StepVerifier` con cobertura $\ge 90\%$.

---

## 2. INSTRUCCIONES TÉCNICAS

### 2.1 Tareas a Ejecutar

#### T-01 · Configuración del Submódulo Gradle `:spec-storage`

1. Registrar `:spec-storage` en `azure-devops-backend/settings.gradle`:
   ```groovy
   include ':spec-storage'
   project(':spec-storage').projectDir = file('./infrastructure/driven-adapters/spec-storage')
   ```
2. Crear `infrastructure/driven-adapters/spec-storage/build.gradle`:
   ```groovy
   dependencies {
       implementation project(':model')
       implementation 'org.springframework.boot:spring-boot-starter'
   }
   ```

#### T-02 · Crear el Adaptador `FileSystemSpecAdapter`

**Ubicación:**
`infrastructure/driven-adapters/spec-storage/src/main/java/co/com/bancolombia/spec/storage/FileSystemSpecAdapter.java`

- Implementa `co.com.bancolombia.model.spec.gateways.SpecStoragePort`.
- Anotado con `@Repository`.
- Constructores:
    - `FileSystemSpecAdapter(@Value("${specs.storage.path:specs}") String basePath)`
    - `FileSystemSpecAdapter(Path basePath)` (para inyección directa y pruebas unitarias).
- Métodos del contrato:
    - `Mono<SpecDocument> getSpec(String specName)`
    - `Flux<String> listAvailableSpecs()`
    - `Mono<Void> saveSpec(String specName, String markdownContent)`
- Reglas de seguridad:
    - Sanitización de `specName`: validar que no contenga `..`, `/` ni `\`. Si contiene caracteres
      maliciosos, retornar `Mono.error(new IllegalArgumentException(...))`.
    - Asegurar creación automática de directorios si no existen.
    - Ejecutar operaciones I/O en `Schedulers.boundedElastic()`.

#### T-03 · Pruebas Unitarias Reactivas con `@TempDir`

**Ubicación:**
`infrastructure/driven-adapters/spec-storage/src/test/java/co/com/bancolombia/spec/storage/FileSystemSpecAdapterTest.java`

- Casos obligatorios:
    - Guardar spec exitosamente y recuperarlo con `getSpec`.
    - Lanzar `SpecNotFoundException` cuando el archivo solicitado no existe.
    - Prevenir Path Traversal en `getSpec` y `saveSpec` (ej. `../secret.txt`, `sub/file.md`).
    - Listar specs disponibles ordenadas y filtradas por extensión `.md`.
    - Manejo de directorios vacíos o no inicializados.
    - Inyección por String y por `Path`.

---

## 3. CRITERIOS DE ACEPTACIÓN

- [ ] Submódulo `:spec-storage` compila y se integra en la estructura limpia de Gradle.
- [ ] Implementación no bloqueante y compliant con `BlockHound`.
- [ ] Suite de pruebas pasando al 100% (`./gradlew :spec-storage:test` y `./gradlew test`).
- [ ] Cobertura en `:spec-storage` $\ge 90\%$ y cero violaciones en `validateStructure`.
- [ ] Generación de `RESULTADO-FASE-03.md` y actualización de `ESTADO.md`.
