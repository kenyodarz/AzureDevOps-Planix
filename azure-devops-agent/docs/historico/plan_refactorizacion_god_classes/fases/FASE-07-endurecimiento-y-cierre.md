# FASE 07 — Endurecimiento y Cierre

> **Estado:** 🟢 COMPLETADA (2026-08-29) · **Depende de:** FASE 06 (🟢 completada) · **Riesgo:** Bajo
> **Commit al cerrar:** `refactor(agent): endurecer arquitectura y cerrar deudas del plan`
> **Resultado:** `docs/resultados/RESULTADO-FASE-07.md` · **Cierre:** `docs/resultados/CIERRE-DEL-PLAN.md`

> ✅ **DP-05 resuelta por el usuario el 2026-08-29: Opción A** (conservar `chat()` como punto de
> extensión A2A/Kafka). El bloqueo quedó levantado y la fase se ejecutó completa.

---

## 1. CONTEXTO

### 1.1 De dónde venimos

Seis fases dejaron la arquitectura en su sitio:

- `AgentChatUseCase`: 644 → **89 líneas**, constructor de 4 argumentos tipados.
- Seis `ChatFlowHandler` + `ChatFlowDispatcher`, con **un único** `onErrorResume`.
- `IntentResolver`, `AzureDevOpsScope` y `CorporateKnowledge` al **100%** de cobertura.
- `Handler`: 495 → **112 líneas**; `JsonRpcPayloadMapper`, `JsonRpcResponseFactory`,
  `JsonRpcDispatcher` y `AgentCardProvider` al **100%**.
- **Cero clases de más de 300 líneas.** La más larga es `PgVectorPlanningAdapter`, con 240.
- **251 tests** en verde. `domain/model` 96,1%, `domain/usecase` 95,4%.

No queda ninguna refactorización estructural pendiente. **Esta es la fase de cierre**: se saldan
las deudas acumuladas y se resuelve la última decisión abierta.

### 1.2 Problema que resuelve esta fase

| ID / Hallazgo | Detalle | Detectado en |
|---|---|---|
| **DP-05** | `chat()` asíncrono publica en `AgentResponseGateway`, cuya única implementación (`NoOpAgentResponseAdapter`) devuelve `Mono.empty()`. Camino muerto en producción (D-13) | Fase 00 |
| **`Rule_2.7` ×5** | Beans con campos no finales. El plugin avisa: *«This will cause a build error in future»* | Fase 00 |
| **`Rule_2.2` ×1** | Clase de dominio con sufijo tecnológico | Fase 00 |
| **`UseCasesConfigTest`** | Captura y descarta `UnsatisfiedDependencyException`: **un cableado roto pasaría como prueba verde**. Mitigado con `UseCasesConfigWiringTest`, pero la señal falsa sigue ahí | Fase 05 |
| **Modelos A2A sin pruebas** | ~15 clases de `domain/model` lastran la cobertura del módulo | Fase 02 |
| **`AgentChatUseCase` al 75,9%** | Lo no cubierto es exactamente el camino `chat()` asíncrono | Fase 04 |
| **`jacocoMergedReport`** | Incompatible con la configuration cache por el plugin `pitest`. Workaround en uso: `--no-configuration-cache` | Fase 00 |
| **`Handler` al 86,1%** | Sin cubrir: el camino de error de `handleListTasks` y el `onErrorResume` del endpoint legacy | Fase 06 |

### 1.3 Estado esperado al terminar

1. DP-05 **resuelta por el usuario** y aplicada; `DECISIONES_PENDIENTES.md` sin entradas ABIERTAS.
2. `ArchitectureTest` sin advertencias: cero violaciones de `Rule_2.7` y `Rule_2.2`.
3. `UseCasesConfigTest` deja de dar una señal falsa.
4. Cobertura de `domain/model` y `domain/usecase` ≥ 90% **sin excepciones por clase**.
5. `README.md` del módulo describe la arquitectura real resultante.
6. Existe un **informe de cierre del plan** con las métricas baseline → final.

### 1.4 Archivos involucrados

| Ruta | Acción |
|---|---|
| `docs/plan/DECISIONES_PENDIENTES.md` | MODIFICAR (registrar la resolución de DP-05) |
| `domain/usecase/.../chat/AgentChatUseCase.java` | MODIFICAR **solo si** DP-05 lo exige |
| `domain/model/.../gateways/AgentResponseGateway.java` | MODIFICAR / ELIMINAR según DP-05 |
| `infrastructure/.../NoOpAgentResponseAdapter.java` | MODIFICAR / ELIMINAR según DP-05 |
| Las 5 clases que violan `Rule_2.7` | MODIFICAR (campos a `final`) |
| La clase que viola `Rule_2.2` | MODIFICAR (renombrar) |
| `applications/app-service/src/test/java/.../UseCasesConfigTest.java` | MODIFICAR o SUSTITUIR |
| `domain/model/src/test/java/co/com/bancolombia/model/a2a/**` | CREAR (pruebas de los ~15 modelos) |
| `azure-devops-agent/README.md` | MODIFICAR |
| `docs/resultados/RESULTADO-FASE-07.md` | CREAR |
| `docs/resultados/CIERRE-DEL-PLAN.md` | CREAR |
| `applications/app-service/src/test/java/co/com/bancolombia/ArchitectureTest.java` | **NO TOCAR** (dice literalmente *«Please do not modify this file»*) |
| Las 4 suites del entry-point | **NO TOCAR** |

> ⚠️ Antes de tocar `Rule_2.7`, **identifica las 5 clases reales** ejecutando
> `.\gradlew.bat :app-service:test --tests "*ArchitectureTest*" --info` y leyendo el mensaje de la
> advertencia, o `build/issues.json`. **No las adivines.**

### 1.5 Reglas aplicables

De `rules/spring-rules.md`:

- **§5:** cobertura ≥ 90% en `domain/model` y `domain/usecase`.
- **§6 No-Asunción:** DP-05 la decide el usuario, nadie más.
- **§7:** el checklist completo se revisa **entero** en esta fase, no por muestreo.
- **Rule_2.7 / Rule_2.2** de ArchUnit: hoy advertencia, mañana error de build.

### 1.6 Decisiones pendientes que bloquean

| ID | Estado | Acción si sigue ABIERTA |
|---|---|---|
| **DP-05** | 🔴 ABIERTA | **DETENERSE y preguntar al usuario antes de escribir una sola línea** |

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

1. **Resolver DP-05 preguntando al usuario.** Plantear las tres opciones **tal como están
   registradas**, sin recomendar ninguna como si ya estuviera decidida:
   - **Opción A:** conservar `chat()` y documentarlo como punto de extensión para Kafka.
   - **Opción B:** eliminar `chat()`, `AgentResponseGateway` y `NoOpAgentResponseAdapter`.
   - **Opción C:** conservarlo tras un *feature flag* explícito en `application.yaml`.

   Registrar la respuesta literal en `DECISIONES_PENDIENTES.md` con fecha, y solo entonces aplicarla.

2. **Corregir las 5 violaciones de `Rule_2.7`** (campos no finales en beans) y la de `Rule_2.2`.
   Identificarlas primero con evidencia ejecutable. Si la de `Rule_2.2` obliga a renombrar una
   clase con sufijo tecnológico, verificar que no forma parte de un contrato serializado.

3. **Sustituir o corregir `UseCasesConfigTest`.** Hoy contiene:

   ```java
   } catch (org.springframework.beans.factory.UnsatisfiedDependencyException e) {
       assertTrue(true, "Unsatisfied dependencies are expected...");
   }
   ```

   Un cableado roto pasa como verde. Es andamiaje de Bancolombia: si se decide conservarlo, dejar
   documentado por qué; si se sustituye, `UseCasesConfigWiringTest` ya cubre el caso real.

4. **Dar pruebas a los ~15 modelos A2A de `domain/model`.** Son POJOs con Lombok: probar
   construcción, `toBuilder()` y la serialización de los enums (`TaskState` viaja como
   `"completed"` en minúsculas, según verifica `RouterRestTest`).

5. **Cubrir el camino `chat()` asíncrono de `AgentChatUseCase`** (hoy 75,9%), **según lo que
   decida DP-05**. Si la decisión es la Opción B, este punto desaparece por eliminación.

6. **Cubrir los caminos de error restantes de `Handler`** (86,1%): fallo de `handleListTasks` y
   `onErrorResume` del endpoint legacy. Añadir las pruebas en una suite **nueva**; las 4 del
   contrato siguen intocables.

7. **Investigar el workaround `--no-configuration-cache`** de `jacocoMergedReport`. Si la
   incompatibilidad del plugin `pitest` no tiene arreglo, documentarla en el `README.md` en vez de
   dejarla como conocimiento tácito.

8. **Revisión final contra el checklist completo de `spring-rules.md` §7**, criterio por criterio,
   con evidencia ejecutable por cada uno —como se hizo en las Fases 05 y 06—.

9. **Actualizar el `README.md`** del módulo con la arquitectura resultante y **escribir
   `docs/resultados/CIERRE-DEL-PLAN.md`** con la tabla baseline → final de las métricas de éxito
   del plan maestro §5.

### 2.2 Qué NO hacer

- ❌ **No decidir DP-05 por cuenta propia**, ni siquiera si la Opción A parece la más conservadora.
- ❌ No iniciar refactorizaciones estructurales nuevas: es una fase de cierre.
- ❌ No modificar `ArchitectureTest.java` (el archivo lo prohíbe expresamente); corregir el código
   que viola las reglas, no la regla.
- ❌ No tocar `RouterRestTest`, `JsonRpcErrorContractTest`, `RouterRestLegacyToggleTest` ni
   `RouterRestLegacySunsetTest`.
- ❌ No cambiar el contrato JSON-RPC ni el esquema de base de datos.
- ❌ No dar por cumplido un criterio por inspección visual: cada uno con su comando.

### 2.3 Criterios de aceptación

- [ ] DP-05 resuelta **por el usuario**, registrada con fecha y aplicada en el código.
- [ ] `ArchitectureTest` sin advertencias: 0 violaciones de `Rule_2.7` y `Rule_2.2`.
- [ ] `UseCasesConfigTest` ya no descarta `UnsatisfiedDependencyException` (o se documenta por qué).
- [ ] Cobertura `domain/model` ≥ 90% **y** ningún modelo A2A al 0%.
- [ ] Cobertura `domain/usecase` ≥ 90%; `AgentChatUseCase` coherente con lo decidido en DP-05.
- [ ] Cobertura `Handler` ≥ 90%.
- [ ] Las 4 suites del entry-point verdes y sin modificar (`git diff` vacío).
- [ ] `.\gradlew.bat build` en `BUILD SUCCESSFUL`.
- [ ] `README.md` del módulo actualizado.
- [ ] `docs/resultados/CIERRE-DEL-PLAN.md` escrito, con métricas reales baseline → final.

### 2.4 Checklist de calidad (obligatoria, de `spring-rules.md` §7)

- [ ] `domain/model` sin dependencias de Spring, persistencia ni serialización
- [ ] `domain/usecase` sin anotaciones de Spring
- [ ] Los mappers modelo↔entidad viven en `driven-adapters`
- [ ] Sin secretos hardcodeados (S2068)
- [ ] `Optional<T>` en retornos opcionales (S2259)
- [ ] Complejidad cognitiva ≤ 15
- [ ] Llaves `{}` en todo control de flujo (S1117)
- [ ] Sin números mágicos (S109)
- [ ] Campos finales en los beans (Rule_2.7)
- [ ] Tests GIVEN/WHEN/THEN con Mockito
- [ ] `.\gradlew.bat build` verde (incluye ArchUnit y BlockHound)
- [ ] Commit en formato `tipo(scope): descripcion` en español

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción | Resultado esperado |
|---:|---|---|
| 1 | Leer `ESTADO.md`, `DECISIONES_PENDIENTES.md` y `RESULTADO-FASE-06.md` | Contexto al día |
| 2 | **Preguntar DP-05 al usuario y ESPERAR su respuesta** | Decisión registrada; sin ella no se continúa |
| 3 | Ejecutar `.\gradlew.bat build` **antes de tocar nada** | Línea base verde registrada |
| 4 | Aplicar la decisión de DP-05 | Camino muerto resuelto |
| 5 | Identificar con evidencia las 5 clases de `Rule_2.7` y la de `Rule_2.2` | Lista real, no supuesta |
| 6 | Corregirlas | `ArchitectureTest` sin advertencias |
| 7 | Corregir o sustituir `UseCasesConfigTest` | Fin de la señal falsa |
| 8 | Escribir las pruebas de los ~15 modelos A2A | `domain/model` ≥ 90% sin huecos |
| 9 | Cubrir lo que falte de `AgentChatUseCase` y `Handler` | Ambos ≥ 90% |
| 10 | Investigar y documentar el workaround de `jacocoMergedReport` | Conocimiento explícito |
| 11 | Recorrer el checklist §7 completo, con un comando por criterio | Evidencia ejecutable |
| 12 | Ejecutar `.\gradlew.bat build` y `jacocoMergedReport` | `BUILD SUCCESSFUL` + métricas finales |
| 13 | `git diff` sobre las 4 suites del entry-point | **Salida vacía** |
| 14 | Actualizar el `README.md` del módulo | Arquitectura documentada |
| 15 | **Escribir `docs/resultados/RESULTADO-FASE-07.md`** con `_PLANTILLA_RESULTADO.md` | Trazabilidad |
| 16 | **Escribir `docs/resultados/CIERRE-DEL-PLAN.md`** | Baseline → final del plan maestro §5 |
| 17 | Actualizar `docs/plan/ESTADO.md`: 🟢 COMPLETADA + métricas + bitácora | Tablero al día |
| 18 | Commit: `refactor(agent): endurecer arquitectura y cerrar deudas del plan` | Cambios versionados |

> ⚠️ **El paso 2 es innegociable y bloqueante.** Aplicar DP-05 sin respuesta del usuario invalida
> la fase completa, por muy verde que quede el build.
>
> ⚠️ **Los pasos 15 y 16 son innegociables.** Sin ellos el plan no queda cerrado, solo interrumpido.

### 3.1 Contenido mínimo del informe de cierre

`CIERRE-DEL-PLAN.md` debe contener, con **datos medidos**:

1. Tabla de las métricas de éxito de `PLAN_MAESTRO.md` §5: baseline → objetivo → **final real**.
2. Estado de cada deuda **D-01 a D-14**: saldada, parcialmente saldada o viva, con su justificación.
3. Estado de cada decisión **DP-01 a DP-07**, con fecha de resolución.
4. Las dos ratificaciones pendientes del usuario (Fases 01 y 03), con su procedimiento de reversión.
5. Deudas que el plan **no** atacó y por qué (ver `PLAN_MAESTRO.md` §9, Fuera de alcance).

> Al ser esta la última fase del plan, **no se genera un MD de FASE-08**. En su lugar, el informe de
> cierre debe listar el trabajo que quedaría para un plan futuro.

