# FASE 01 — Modelos y Puerto de Almacenamiento Documental (`domain/model`)

> **Estado:** 🟡 PENDIENTE · **Depende de:** Plan Maestro 1.0 · **Riesgo:** Muy Bajo  
> **Commit al cerrar:**
> `feat(agent): agregar modelos y puerto de almacenamiento documental de specs`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

En `azure-devops-agent`, el acceso a información de planeación se realizaba a través de
`PlanningVectorStorePort`, el cual dependía de fragmentar documentos en chunks y persistirlos en
PostgreSQL con la extensión `pgvector`.

### 1.2 Problema que resuelve esta fase

* Los documentos de planeación de un proyecto (`ideas_planning_q3.md`, `aegis_engine.md`,
  `exodia_ia_catalog.md`) son archivos Markdown compactos (5 a 20 KB).
* Fragmentarlos en pedazos de 500 tokens destruye la coherencia de la arquitectura y genera
  respuestas descontextualizadas.
* La decisión **DP-PL-01** determinó desacoplar el vector store y modelar un puerto documental que
  trate el documento de especificación como una unidad íntegra.

### 1.3 Estado esperado al terminar

* Paquete `co.com.bancolombia.model.spec` existente en `domain/model`.
* Record inmutable `SpecDocument` creado con validaciones de invariantes (nombre no vacío, contenido
  no nulo).
* Interface `SpecStoragePort` definida en `co.com.bancolombia.model.spec.gateways` con contratos
  reactivos (`Mono`/`Flux`).
* Excepción de dominio `SpecNotFoundException`.
* Suite de pruebas unitarias cubriendo el 100% de los nuevos tipos en `domain/model`.

### 1.4 Archivos involucrados

| Ruta                                                                                     | Acción | Propósito                                                      |
|:-----------------------------------------------------------------------------------------|:------:|:---------------------------------------------------------------|
| `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecDocument.java`             | CREAR  | Entidad de dominio inmutable que representa la especificación. |
| `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecNotFoundException.java`    | CREAR  | Excepción de dominio para specs no encontrados.                |
| `domain/model/src/main/java/co/com/bancolombia/model/spec/gateways/SpecStoragePort.java` | CREAR  | Puerto que desacopla la persistencia de especificaciones.      |
| `domain/model/src/test/java/co/com/bancolombia/model/spec/SpecDocumentTest.java`         | CREAR  | Pruebas unitarias de invariantes y contratos.                  |

### 1.5 Reglas aplicables

* `domain/model` 100% puro: **cero anotaciones de Spring, Jackson o frameworks externos**.
* Uso de tipos reactivos estándar de Project Reactor (`Mono`, `Flux`).
* Cobertura de pruebas unitarias $\ge 90\%$.

### 1.6 Decisiones pendientes que bloquean

|      ID      |     Estado      | Acción si sigue ABIERTA                                  |
|:------------:|:---------------:|:---------------------------------------------------------|
| **DP-PL-01** | 🟢 **RESUELTA** | Utilizar interfaz de almacenamiento documental reactiva. |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Crear `SpecDocument`:**
   En `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecDocument.java`:
   ```java
   package co.com.bancolombia.model.spec;

   public record SpecDocument(String name, String content, String path) {
       public SpecDocument {
           if (name == null || name.isBlank()) {
               throw new IllegalArgumentException("El nombre del spec no puede ser nulo ni vacío");
           }
           if (content == null) {
               content = "";
           }
       }
   }
   ```

2. **Crear `SpecNotFoundException`:**
   En `domain/model/src/main/java/co/com/bancolombia/model/spec/SpecNotFoundException.java`:
   ```java
   package co.com.bancolombia.model.spec;

   public class SpecNotFoundException extends RuntimeException {
       public SpecNotFoundException(String specName) {
           super("No se encontró el documento de especificación: " + specName);
       }
   }
   ```

3. **Crear `SpecStoragePort`:**
   En `domain/model/src/main/java/co/com/bancolombia/model/spec/gateways/SpecStoragePort.java`:
   ```java
   package co.com.bancolombia.model.spec.gateways;

   import co.com.bancolombia.model.spec.SpecDocument;
   import reactor.core.publisher.Flux;
   import reactor.core.publisher.Mono;

   public interface SpecStoragePort {
       Mono<SpecDocument> getSpec(String specName);
       Flux<String> listAvailableSpecs();
       Mono<Void> saveSpec(String specName, String markdownContent);
   }
   ```

4. **Crear las Pruebas Unitarias:**
   En `domain/model/src/test/java/co/com/bancolombia/model/spec/SpecDocumentTest.java`:
    * Test: crear `SpecDocument` válido con datos completos.
    * Test: validar que nombre nulo o vacío lance `IllegalArgumentException`.
    * Test: validar que contenido nulo se normalice a cadena vacía.
    * Test: instanciar `SpecNotFoundException` y verificar su mensaje.

### 2.2 Qué NO hacer

* No agregar anotaciones `@Component`, `@Service`, `@Repository` ni `@Data`.
* No tocar ninguna clase de `infrastructure` ni `applications` en esta fase (eso corresponde a la
  Fase 02).
* No alterar los tests existentes en `domain/model`.

### 2.3 Criterios de Aceptación

- [ ] `SpecDocument` valida sus invariantes correctamente.
- [ ] `SpecStoragePort` define el contrato reactivo limpio.
- [ ] `./gradlew :agent-model:test` pasa al 100%.
- [ ] Cobertura en el nuevo paquete `model.spec` es del 100%.
- [ ] Cero violaciones de ArchUnit.

---

## 3. ORDEN DE EJECUCIÓN

1. Escribir `SpecDocument.java` y `SpecNotFoundException.java`.
2. Escribir la interfaz `SpecStoragePort.java`.
3. Escribir `SpecDocumentTest.java`.
4. Ejecutar pruebas unitarias de `domain/model`.
5. Redactar `resultados/RESULTADO-FASE-01.md`.
6. Generar la especificación de la siguiente fase: `fases/FASE-02-adaptador-filesystem-spec.md`.
7. Actualizar `plan/ESTADO.md` a `COMPLETADA`.
