# FASE 03 — Migración de `PlanningDraftFlowHandler` a `SpecStoragePort`

> **Estado:** 🟡 PENDIENTE · **Depende de:** FASE 01, FASE 02 · **Riesgo:** Bajo  
> **Commit al cerrar:**
>
`feat(agent): migrar PlanningDraftFlowHandler para consumir SpecStoragePort e inyectar specs completos`

---

## 1. CONTEXTO

### 1.1 De dónde venimos

* La **Fase 01** creó el modelo inmutable `SpecDocument`, la excepción `SpecNotFoundException` y el
  puerto reactivo `SpecStoragePort` en `domain/model`.
* La **Fase 02** implementó el adaptador de infraestructura `FileSystemSpecAdapter` en el módulo
  `:spec-storage`, con lectura y escritura no bloqueante sobre `Schedulers.boundedElastic()`,
  validación anti Path Traversal y 18 pruebas unitarias aprobadas.

### 1.2 Problema que resuelve esta fase

* Actualmente, `PlanningDraftFlowHandler` en `domain/usecase` depende de `PlanningVectorStorePort`
  (PostgreSQL/pgvector con fragmentación RAG de 3 chunks).
* Conforme a la decisión acordada **DP-PL-01**, los documentos de especificación deben inyectarse
  íntegros en los prompts de refinamiento para no descontextualizar la planeación.
* Conforme a la decisión **DP-PL-03**, si el texto del usuario incluye un prefijo de frente de
  trabajo (ej. `[Aegis Engine]`), el handler debe cargar el archivo de especificación
  correspondiente (`aegis_engine.md`). Si no lo incluye, debe aplicar un fallback al documento
  principal (`ideas_planning_q3.md` o el spec por defecto) o continuar amigablemente sin interrumpir
  el flujo si el archivo no existe.

### 1.3 Estado esperado al terminar

* `PlanningDraftFlowHandler` inyecta `SpecStoragePort` en lugar de `PlanningVectorStorePort`.
* Lógica de resolución de frente:
    - Extracción de prefijo `[Nombre Frente]` en el mensaje del usuario (o fallback al frente
      predeterminado).
    - Consulta reactiva mediante `specStoragePort.getSpec(specFileName)`.
    - Fallback resiliente con `.onErrorResume()` que registra advertencia y suministra contexto
      vacío si el spec no existe, sin romper la generación del borrador.
* Variable de prompt inyectada con el contenido completo del documento Markdown.
* Eliminación de la dependencia de `PlanningVectorStorePort` en `PlanningDraftFlowHandler`.
* Actualización del wiring en
  `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`.
* Suite completa de pruebas unitarias en `PlanningDraftFlowHandlerTest` (y pruebas de integración /
  use case) adaptadas a `SpecStoragePort` con 100% de éxito.

### 1.4 Archivos involucrados

| Ruta                                                                                                 |  Acción   | Propósito                                                                            |
|:-----------------------------------------------------------------------------------------------------|:---------:|:-------------------------------------------------------------------------------------|
| `domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/PlanningDraftFlowHandler.java` | MODIFICAR | Reemplazar `PlanningVectorStorePort` por `SpecStoragePort` e inyectar spec completo. |
| `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`               | MODIFICAR | Actualizar el `@Bean planningDraftFlowHandler` para inyectar `SpecStoragePort`.      |
| `domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/handler/ChatFlowHandlerTest.java`      | MODIFICAR | Adaptar mocks y verificaciones al nuevo contrato `SpecStoragePort`.                  |
| `applications/app-service/src/test/java/co/com/bancolombia/config/UseCasesConfigWiringTest.java`     | MODIFICAR | Verificar el wiring del contexto de Spring con el nuevo bean.                        |

### 1.5 Reglas aplicables

* `rules/spring-rules.md`:
    - §1 (`domain/usecase`): Casos de uso puros sin dependencias de Spring.
    - §3 (SOLID): Dependency Inversion mediante `SpecStoragePort`.
    - §4 (Calidad de Código): Complejidad cognitiva $\le 15$, llaves obligatorias (S1117).
    - §5 (Testing): Patrón GIVEN/WHEN/THEN con Mockito y `StepVerifier`.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Modificar `PlanningDraftFlowHandler.java`:**
    - Inyectar `SpecStoragePort` como campo final en el constructor.
    - Retirar `PlanningVectorStorePort`.
    - Implementar método auxiliar de resolución de spec a partir del texto del usuario:
        - Detectar patrón regex `\\[([^\\]]+)\\]` para capturar el nombre del frente.
        - Si existe, normalizar a nombre de archivo (ej. `[Aegis Engine]` -> `aegis_engine.md`).
        - Si no existe, usar valor por defecto: `ideas_planning_q3.md`.
    - Invocar `specStoragePort.getSpec(targetSpecFile)`.
    - Manejar error con `.onErrorResume(error -> ...)` retornando un `SpecDocument` con contenido
      vacío o fallback amigable.
    - Renderizar la plantilla con la variable de contexto de planeación y delegar a
      `chatGateway.sendMessage(...)`.

2. **Actualizar `UseCasesConfig.java`:**
    - Reemplazar el parámetro `PlanningVectorStorePort` por `SpecStoragePort` en el método
      `@Bean public PlanningDraftFlowHandler planningDraftFlowHandler(...)`.

3. **Actualizar y enriquecer pruebas unitarias:**
    - En `ChatFlowHandlerTest.java` (o tests dedicados de `PlanningDraftFlowHandler`):
        - Mock de `SpecStoragePort`.
        - Test GIVEN idea con prefijo `[Aegis Engine]` WHEN handle THEN carga `aegis_engine.md` y
          envía prompt renderizado.
        - Test GIVEN idea sin prefijo WHEN handle THEN carga `ideas_planning_q3.md` por defecto.
        - Test GIVEN spec inexistente (`SpecNotFoundException`) WHEN handle THEN continúa sin fallar
          y genera propuesta.

4. **Validación:**
    - Ejecutar `.\gradlew.bat :usecase:test`, `.\gradlew.bat :app-service:test` y
      `.\gradlew.bat test`.
    - Verificar que no haya regresiones y que la arquitectura continúe verde.

### 2.2 Qué NO hacer

* No introducir lógica de base de datos ni Spring dentro de `domain/usecase`.
* No romper los otros handlers de chat existentes.
* No bloquear el hilo reactivo con llamadas síncronas.

### 2.3 Criterios de Aceptación

- [ ] `PlanningDraftFlowHandler` libre de acoplamiento a `PlanningVectorStorePort`.
- [ ] Detección de frente por prefijo con fallback a `ideas_planning_q3.md` operando correctamente.
- [ ] 100% de pruebas del proyecto pasando (`.\gradlew.bat test`).
- [ ] `validateStructure` pasa sin observaciones.
