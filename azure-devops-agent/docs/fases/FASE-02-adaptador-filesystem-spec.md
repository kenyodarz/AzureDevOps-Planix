# FASE 02 — Adaptador FileSystemSpecAdapter (`infrastructure`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01 · **Riesgo:** Bajo  
> **Commit al cerrar:**
> `feat(agent): implementar adaptador FileSystemSpecAdapter para almacenamiento de specs`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 01 creó el núcleo de dominio para especificaciones en `domain/model`:

* La entidad inmutable `SpecDocument(name, content, path)`.
* La excepción de dominio `SpecNotFoundException`.
* La interfaz reactiva de puerto `SpecStoragePort` en `co.com.bancolombia.model.spec.gateways`.

### 1.2 Problema que resuelve esta fase

* Los documentos de planeación (`ideas_planning_q3.md`, specs por frente de trabajo) residen
  físicamente en el sistema de archivos local o workspace del agente.
* Para materializar la decisión **DP-PL-01** y desacoplar definitivamente el almacenamiento
  documental de `PlanningVectorStorePort` (PostgreSQL/pgvector), se requiere un adaptador de
  infraestructura reactivo que lea y persista archivos Markdown en disco respetando las invariantes
  de dominio y el estándar reactivo no bloqueante.

### 1.3 Estado esperado al terminar

* Nuevo submódulo driven-adapter `:spec-storage` en `infrastructure/driven-adapters/spec-storage`
  registrado en `settings.gradle` y en `applications/app-service/build.gradle`.
* Clase `FileSystemSpecAdapter` en el paquete `co.com.bancolombia.spec.storage` implementando
  `SpecStoragePort`.
* Lectura y escritura asíncrona no bloqueante apoyada en `Schedulers.boundedElastic()` para
  operaciones de Java NIO `Path`/`Files`.
* Si un archivo solicitado no existe, retornar reactivamente
  `Mono.error(new SpecNotFoundException(specName))`.
* Propiedad configurable de ruta base para specs (ej. `agent.specs.base-path` o fallback al
  directorio de trabajo).
* Suite de pruebas unitarias exhaustiva con JUnit 5, `@TempDir` y `StepVerifier` de `reactor-test`
  con cobertura $\ge 90\%$.

### 1.4 Archivos involucrados

| Ruta                                                                                                                       |  Acción   | Propósito                                                               |
|:---------------------------------------------------------------------------------------------------------------------------|:---------:|:------------------------------------------------------------------------|
| `settings.gradle`                                                                                                          | MODIFICAR | Registrar el nuevo módulo `:spec-storage`.                              |
| `applications/app-service/build.gradle`                                                                                    | MODIFICAR | Añadir dependencia `implementation project(':spec-storage')`.           |
| `infrastructure/driven-adapters/spec-storage/build.gradle`                                                                 |   CREAR   | Configuración de dependencias de Gradle del adaptador.                  |
| `infrastructure/driven-adapters/spec-storage/src/main/java/co/com/bancolombia/spec/storage/FileSystemSpecAdapter.java`     |   CREAR   | Adaptador concreto de infraestructura que implementa `SpecStoragePort`. |
| `infrastructure/driven-adapters/spec-storage/src/test/java/co/com/bancolombia/spec/storage/FileSystemSpecAdapterTest.java` |   CREAR   | Pruebas unitarias reactivas con `@TempDir` y `StepVerifier`.            |

### 1.5 Reglas aplicables

* Referencias literales a `rules/spring-rules.md`:
    * §1 (`infrastructure/driven-adapters`): Adaptador de salida que implementa el Gateway del
      dominio sin propagar excepciones técnicas crudas al exterior.
    * §3 (SOLID): Principio de Inversión de Dependencias (D) y Sustitución de Liskov (L).
    * §4 (Calidad de Código): Sin credenciales hardcodeadas (S2068), complejidad cognitiva $\le 15$,
      llaves explícitas en todos los bloques (S1117).
    * §5 (Testing): Estructura GIVEN/WHEN/THEN y uso de `StepVerifier` de Project Reactor para
      aserciones de flujos reactivos.

### 1.6 Decisiones pendientes que bloquean

|      ID      |     Estado      | Acción si sigue ABIERTA                                                              |
|:------------:|:---------------:|:-------------------------------------------------------------------------------------|
| **DP-PL-01** | 🟢 **RESUELTA** | Implementar almacenamiento en archivos Markdown locales vía `FileSystemSpecAdapter`. |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Configurar el módulo Gradle `:spec-storage`:**
    - En `settings.gradle`:
      ```groovy
      include ':spec-storage'
      project(':spec-storage').projectDir = file('./infrastructure/driven-adapters/spec-storage')
      ```
    - Crear `infrastructure/driven-adapters/spec-storage/build.gradle`:
      ```groovy
      dependencies {
          implementation project(':model')
          implementation 'org.springframework.boot:spring-boot-starter'
      }
      ```
    - En `applications/app-service/build.gradle`: agregar `implementation project(':spec-storage')`.

2. **Crear `FileSystemSpecAdapter`:**
    - En
      `infrastructure/driven-adapters/spec-storage/src/main/java/co/com/bancolombia/spec/storage/FileSystemSpecAdapter.java`:
        - Anotar con `@Repository` o `@Component`.
        - Inyectar la ruta base configurable mediante `@Value("${agent.specs.base-path:specs}")`.
        - Implementar `getSpec(String specName)`:
            - Validar el nombre del spec evitando ataques de Path Traversal
              (`specName.contains("..")`).
            - Ejecutar la lectura de `Files.readString(path)` en `Schedulers.boundedElastic()`.
            - Si el archivo no existe, emitir `Mono.error(new SpecNotFoundException(specName))`.
            - Mapear al record inmutable `SpecDocument(specName, content, path.toString())`.
        - Implementar `listAvailableSpecs()`:
            - Escanear la carpeta de specs en `Schedulers.boundedElastic()`.
            - Filtrar archivos terminados en `.md`.
            - Emitir los nombres de archivo mediante `Flux.fromIterable()`.
        - Implementar `saveSpec(String specName, String markdownContent)`:
            - Crear directorios padre si no existen (`Files.createDirectories`).
            - Escribir contenido (`Files.writeString`) en `Schedulers.boundedElastic()`.
            - Retornar `Mono.empty()`.

3. **Crear las Pruebas Unitarias Reactivas:**
    - En
      `infrastructure/driven-adapters/spec-storage/src/test/java/co/com/bancolombia/spec/storage/FileSystemSpecAdapterTest.java`:
        - Usar `@TempDir Path tempDir` para aislamiento de pruebas de filesystem.
        - Test: `givenExistingSpec_whenGetSpec_thenReturnsSpecDocument` usando `StepVerifier`.
        - Test: `givenNonExistingSpec_whenGetSpec_thenEmitsSpecNotFoundException` con
          `StepVerifier`.
        - Test: `givenMaliciousPath_whenGetSpec_thenEmitsIllegalArgumentException`.
        - Test: `givenSpecDirectoryWithFiles_whenListAvailableSpecs_thenListsAllMarkdownFiles`.
        - Test: `givenNewContent_whenSaveSpec_thenPersistsAndCanBeRetrieved`.

### 2.2 Qué NO hacer

* No exponer excepciones de I/O de bajo nivel (`IOException`, `NoSuchFileException`) sin atraparlas
  o traducirlas a `SpecNotFoundException` o `RuntimeException`.
* No realizar lecturas bloqueantes directas en hilos de Reactor sin derivar a
  `Schedulers.boundedElastic()`.
* No modificar la lógica de los handlers (`PlanningDraftFlowHandler`) en esta fase (eso corresponde
  a la Fase 03).
* No alterar los modelos ni el puerto de `domain/model` (establecido en Fase 01).

### 2.3 Criterios de Aceptación

- [ ] Submódulo `:spec-storage` compila y se integra en el build multi-proyecto sin errores.
- [ ] `./gradlew :spec-storage:test` aprueba el 100% de las pruebas con cobertura $\ge 90\%$.
- [ ] La validación del plugin `cleanArchitecture` (`validateStructure`) se mantiene verde sin
  infracciones.
- [ ] Cero vulnerabilidades de Path Traversal al resolver archivos.
- [ ] Se cumplen todas las restricciones de `spring-rules.md` §7.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización.
- [ ] `domain/usecase` sin anotaciones de Spring.
- [ ] Sin secretos hardcodeados (S2068).
- [ ] `Optional<T>` en retornos opcionales (S2259) o `Mono<T>`/`Flux<T>` en contratos reactivos.
- [ ] Complejidad cognitiva $\le 15$.
- [ ] Llaves `{}` en todo control de flujo (S1117).
- [ ] Sin números mágicos (S109).
- [ ] Tests GIVEN/WHEN/THEN con `StepVerifier` y Mockito.
- [ ] `./gradlew build` verde.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                                   | Resultado esperado                                  |
|-----:|:---------------------------------------------------------------------------------------------------------|:----------------------------------------------------|
|    1 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                                        | Confirmar que la Fase 02 está activa y no bloqueada |
|    2 | Configurar módulo `:spec-storage` en `settings.gradle` y `app-service/build.gradle`                      | Gradle sincronizado                                 |
|    3 | Implementar `FileSystemSpecAdapter.java` en `spec-storage`                                               | Código compila                                      |
|    4 | Implementar `FileSystemSpecAdapterTest.java`                                                             | Tests unitarios reactivos escritos                  |
|    5 | Ejecutar `.\gradlew.bat :spec-storage:test` y `.\gradlew.bat test`                                       | 100% pruebas aprobadas, 0 violaciones               |
|    6 | Escribir `docs/resultados/RESULTADO-FASE-02.md` con métricas reales                                      | Trazabilidad completa                               |
|    7 | Actualizar `docs/plan/ESTADO.md` a 🟢 COMPLETADA                                                         | Tablero actualizado                                 |
|    8 | Generar `docs/fases/FASE-03-migracion-draft-handler.md` y `docs/prompts/PROMPT-FASE-03.md`               | Checkpoint de continuidad                           |
|    9 | Commit sugerido: `feat(agent): implementar adaptador FileSystemSpecAdapter para almacenamiento de specs` | Control de versiones                                |

---

### 3.1 Contenido mínimo del MD de la siguiente fase

- **Objetivo de la Fase 03:** Migrar `PlanningDraftFlowHandler` para consumir `SpecStoragePort` en
  lugar de `PlanningVectorStorePort`, inyectando el contenido completo del spec del frente
  seleccionado en el prompt de refinamiento y diseño de tareas.
- **Archivos:** `PlanningDraftFlowHandler.java`, `PlanningDraftFlowHandlerTest.java`, wiring en
  `app-service`.
