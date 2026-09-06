# FASE 01 — Modelos y Puerto de Almacenamiento Documental (`domain/model`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Ninguna · **Riesgo:** Bajo  
> **Commit al cerrar:** `feat(backend): crear modelos y puerto de almacenamiento documental spec storage en domain model`  
> **Siguiente fase:** `fases/FASE-02-modelos-dominio-planeacion.md`

---

## 1. CONTEXTO

### 1.1 De dónde venimos
El módulo **`azure-devops-agent`** completó el Plan Maestro del Harness, migrando de la base de datos vectorial `pgvector` hacia el puerto `SpecStoragePort` para persistir y recuperar documentos completos en Markdown (`ideas_planning_QX.md`, `frente_*.md`). El BFF (`azure-devops-backend`) requiere incorporar en su capa pura de dominio las entidades y el contrato del puerto documental para poder exponer estos artefactos hacia `azure-devops-frontend`.

### 1.2 Problema que resuelve esta fase
El módulo `domain/model` del BFF no cuenta con las entidades que representan una especificación documental ni con el puerto que desacopla el almacenamiento de archivos Markdown. Hoy en día, la planeación en el backend solo conoce la entidad legacy `PlanningChunk` asociada a chunks vectoriales en base de datos relacional.

### 1.3 Estado esperado al terminar
Existirán en `domain/model`:
1. El record inmutable `SpecDocument` que encapsula nombre, contenido Markdown y ruta absoluta/relativa.
2. La excepción de negocio tipada `SpecNotFoundException`.
3. La interfaz reactiva del puerto `SpecStoragePort` con operaciones para obtener, listar y guardar especificaciones.
4. Pruebas unitarias exhaustivas en `domain/model` cubriendo constructores, validaciones de no-nulabilidad e inmutabilidad, manteniendo el 100% de pruebas pasando.

### 1.4 Archivos involucrados

| Ruta | Acción | Propósito |
|---|:---:|---|
| `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecDocument.java` | CREAR | Record inmutable para documentos de especificación |
| `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecNotFoundException.java` | CREAR | Excepción de dominio para specs no encontrados |
| `domain/model/src/main/java/co/com/bancolombia/model/spec/gateways/SpecStoragePort.java` | CREAR | Puerto de salida para acceso a almacenamiento documental |
| `domain/model/src/test/java/co/com/bancolombia/model/spec/SpecDocumentTest.java` | CREAR | Pruebas unitarias del modelo y excepciones |

### 1.5 Reglas aplicables
- `rules/spring-rules.md`:
  - §1 (`domain/model`): Pureza absoluta, cero anotaciones de Spring, Jackson o JPA.
  - §4 (Calidad de Código): Inmutabilidad, validación de no-nulabilidad (`Objects.requireNonNull`), complejidad cognitiva $\le 15$.
  - §5 (Testing): Convención GIVEN / WHEN / THEN con aserciones AssertJ.

### 1.6 Decisiones pendientes que bloquean
| ID | Estado | Acción si sigue ABIERTA |
|---|:---:|---|
| **DP-BFF-01** | 🟢 RESUELTA | Almacenamiento documental vía `SpecStoragePort` y `FileSystemSpecAdapter`. |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 · Crear el Record de Dominio `SpecDocument`
**Ubicación:** `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecDocument.java`
- Definir un `record` de Java con campos:
  - `String name`: nombre del archivo (ej. `ideas_planning_q3_2026.md`).
  - `String content`: contenido completo en Markdown.
  - `String path`: ruta física o identificador de recurso.
- Validar en el constructor compacto que `name` no sea nulo ni en blanco.
- Asegurar que `content` nunca sea nulo (asignar `""` si es nulo).

#### T-02 · Crear la Excepción de Dominio `SpecNotFoundException`
**Ubicación:** `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecNotFoundException.java`
- Extender de `RuntimeException` (o excepción base del dominio si aplica).
- Proporcionar constructor con mensaje descriptivo:
  `public SpecNotFoundException(String specName) { super("Documento de especificación no encontrado: " + specName); }`.

#### T-03 · Definir el Puerto Reactivo `SpecStoragePort`
**Ubicación:** `domain/model/src/main/java/co/com/bancolombia/model/spec/gateways/SpecStoragePort.java`
- Declarar la interfaz con las firmas:
  - `Mono<SpecDocument> getSpec(String specName);`
  - `Flux<String> listAvailableSpecs();`
  - `Mono<Void> saveSpec(String specName, String markdownContent);`

#### T-04 · Construir Suite de Pruebas Unitarias
**Ubicación:** `domain/model/src/test/java/co/com/bancolombia/model/spec/SpecDocumentTest.java`
- Validar instanciación válida de `SpecDocument`.
- Validar fallo cuando `name` es nulo o vacío.
- Validar mensaje de `SpecNotFoundException`.
- Verificar que la cobertura en el paquete `spec` sea del 100%.

### 2.2 Qué NO hacer
- NO agregar anotaciones `@Component`, `@Service`, `@Repository` ni `@Autowired`.
- NO usar dependencias de Jackson (`@JsonProperty`) ni JPA (`@Entity`).
- NO tocar módulos fuera de `domain/model`.
- NO romper ninguna de las 238 pruebas existentes.

### 2.3 Criterios de Aceptación
- [ ] `SpecDocument.java` compilando e inmutable en `co.com.bancolombia.model.spec`.
- [ ] `SpecNotFoundException.java` tipada para capturas de 404 en el entry-point.
- [ ] `SpecStoragePort.java` con las 3 operaciones reactivas declaradas.
- [ ] 100% de cobertura en las nuevas clases de dominio.
- [ ] `./gradlew :model:test` y `./gradlew test` verdes al 100%.

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md` | Confirmar que DP-BFF-01 está resuelta |
| P-02 | Crear `SpecDocument.java` y `SpecNotFoundException.java` | Clases de dominio listas |
| P-03 | Crear `SpecStoragePort.java` | Contrato de puerto definido |
| P-04 | Crear `SpecDocumentTest.java` y ejecutar `./gradlew :model:test` | Pruebas unitarias verdes |
| P-05 | Ejecutar `./gradlew test` en todo el backend | 238+ pruebas pasando al 100% |
| P-06 | Escribir `docs/resultados/RESULTADO-FASE-01.md` | Trazabilidad con métricas reales |
| P-07 | Actualizar `docs/plan/ESTADO.md` a `🟢 COMPLETADA` | Tablero al día |
| P-08 | Generar `docs/fases/FASE-02-modelos-dominio-planeacion.md` | Próxima fase lista |
| P-09 | Presentar resumen de cambios al usuario para confirmación de commit | Checkpoint cumplido |
