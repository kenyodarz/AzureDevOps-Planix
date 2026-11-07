# ADR-0005: Almacenamiento Documental Desacoplado vía SpecStoragePort (DP-PL-01)

* **Fecha:** 2026-09-05
* **Estado:** Aceptado
* **Autor(es):** Architecture & Storage Team
* **Aprobadores:** Lead Architect

---

## 1. Contexto y Planteamiento del Problema

Las especificaciones de arquitectura generadas por el agente (Macro-Specs, roadmaps de planeación,
desgloses por frentes) deben persistirse de forma legible, versionable y auditable. Si se guardan
únicamente como registros binarios o tablas relacionales en base de datos, se pierde la capacidad de
lectura directa por desarrolladores y herramientas de CI/CD. Si se acopla el dominio a
`java.io.File` o `java.nio.file.Path`, se viola Clean Architecture y se dificulta la migración
futura a almacenamiento en la nube (AWS S3, Azure Blob Storage).

---

## 2. Opciones Evaluadas

| Opción                                                                                                 | Ventajas                                                                                                                                                                 | Desventajas                                                                                              |
|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| **Opción A:** Almacenar especificaciones en PostgreSQL como JSONB                                      | Consultas SQL directas sobre metadatos.                                                                                                                                  | No visible directamente en el sistema de archivos ni versionable por Git; requiere base de datos activa. |
| **Opción B:** Manipulación directa de archivos en el caso de uso                                       | Implementación inmediata.                                                                                                                                                | Violación flagrante de Clean Architecture; pruebas unitarias dependientes de disco.                      |
| **Opción C:** Puerto de Dominio `SpecStoragePort` con adaptador `FileSystemSpecAdapter` (**DP-PL-01**) | Dominio desacoplado con modelos inmutables (`SpecDocument`); persistencia en Markdown en disco; testeable con stubs en memoria; migrable a S3/Blob sin tocar el dominio. | Requiere mantener un módulo dedicado `:spec-storage`.                                                    |

---

## 3. Decisión Adoptada

**Adoptamos la Opción C (DP-PL-01):**

* Se crea en `domain/model` la abstracción `SpecStoragePort` y la entidad inmutable `SpecDocument`
  (nombre, contenido Markdown, frente, metadata).
* La persistencia por defecto se implementa en el submódulo `:spec-storage` mediante
  `FileSystemSpecAdapter`, almacenando los documentos en rutas relativas configurables
  (`docs/sdd/specs/`).
* Si se requiere almacenar en la nube, solo se desarrolla un nuevo adaptador (ej. `S3SpecAdapter`)
  que implemente el mismo puerto.

---

## 4. Consecuencias y Compromisos

### Positivas:

* Cobertura de 92% de líneas y 95.2% de fuerza de mutación en pruebas PIT para `:spec-storage`.
* Los documentos generados por el agente (`ideas_planning_QX.md`, etc.) quedan disponibles de
  inmediato en formato Markdown legible en el repositorio.
* Pruebas del caso de uso 100% aisladas del disco mediante mocks del puerto.

### Negativas / Riesgos Aceptados:

* En entornos multi-instancia en contenedores efímeros sin volúmenes persistentes, los archivos
  locales no se comparten entre réplicas hasta configurar almacenamiento compartido.

---

## 5. Referencias y Enlaces Relacionados

* `azure-devops-agent/docs/fases/FASE-01-modelos-y-puerto-spec-storage.md`
* `azure-devops-agent/docs/fases/FASE-02-adaptador-filesystem-spec.md`
