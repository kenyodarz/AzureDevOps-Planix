# FASE 07 — Adaptadores y contrato HTTP

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** 🟢 **CERRADA** (2026-08-29)
> **Deudas objetivo:** **D-14, D-15, D-16, D-20** *(las cuatro saldadas)* · más **D-40**
> **Resultado:** [`RESULTADO-FASE-07.md`](../resultados/RESULTADO-FASE-07.md)
> **Arranque en sesión nueva:** [`HANDOFF-FASE-07.md`](../plan/HANDOFF-FASE-07.md)
> **Fase anterior:** [`fase-06.md`](fase-06.md) → [
`RESULTADO-FASE-06.md`](../resultados/RESULTADO-FASE-06.md)
> **Fase siguiente:** [`fase-08.md`](fase-08.md)

---

## 1. Contexto

### 1.1 De dónde vienes

La Fase 06 está cerrada. Al arrancar esta fase existe:

- **Build en verde: 227 pruebas, 0 fallos.**
- El flujo SSE **ya no miente**: un fallo del agente llega al cliente como evento `ERROR`, no como
  un 500 sin cabecera. `Handler` bajó de 382 a 192 líneas y su método del stream, de ~90 a 15.
- `ReportStoragePort` existe y el dominio **no toca el disco**.
- El repliegue a datos simulados vive en **un solo punto**, apagado por defecto.
- Cobertura: `domain/model` 94,8 % · `domain/usecase` 98,9 %.

### 1.2 El problema

Lo que queda del BFF que no es dominio está mal traducido en sus dos fronteras: la de salida (base
de datos) y la de entrada (HTTP).

| Deuda    | Dónde                                | Qué pasa                                                                                                                                                                                                                                                                                                                     |
|----------|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-16** | `Handler`, 6 bloques `onErrorResume` | **Todo** error se traduce a HTTP 400, sea lo que sea: un fallo del agente, una validación o un timeout. El cliente no puede distinguirlos y el BFF miente sobre de quién es la culpa. `TaskHandler` ya hace lo contrario —cinco excepciones, cinco códigos—, así que hoy **conviven dos criterios** en el mismo entry-point. |
| **D-14** | `PgVectorPlanningAdapter`            | El **modelo de dominio se serializa con Jackson dentro del adaptador**: no hay entidad de persistencia ni mapper. Cualquier renombrado en el dominio cambia en silencio el formato almacenado.                                                                                                                               |
| **D-15** | `Handler`                            | El entry-point devuelve **modelos de dominio** como respuesta HTTP. La Fase 05 hizo esto mismo con el tablero (`DashboardDtoMapper`); el resto de rutas sigue sin frontera.                                                                                                                                                  |
| **D-20** | `PostgresTaskStoreAdapter`           | Vive en un módulo llamado **`task-store-inmemory`**, y no tiene nada de *in-memory*: escribe en PostgreSQL. El nombre lleva mintiendo desde antes del plan.                                                                                                                                                                  |
| **D-42** | `Handler`                            | Los colaboradores del stream se instancian con `new` para no alterar el `@ContextConfiguration` del test de caracterización *(heredada de la Fase 06)*.                                                                                                                                                                      |

### 1.3 Por qué D-16 es la más importante

Es la misma familia de fallo que las dos fases anteriores acaban de cerrar: **información que se
pierde por el camino**. La Fase 05 mató un tablero vacío sin aviso; la Fase 06, un 500 sin cabecera.
D-16 es la tercera versión: un 400 que dice «te equivocaste tú» cuando el que se cayó fue el agente.
Un BFF cuya misión declarada es validar y traducir no puede tener **dos criterios de traducción de
errores** en el mismo fichero.

---

## 2. 🟠 Decisiones pendientes — bloquean la fase

### DP-05

**Preguntas:**

1. **Nombre del módulo** que hoy se llama `task-store-inmemory` y contiene un adaptador de
   PostgreSQL (D-20). Propuesta: `task-store-postgres`. Renombrar un módulo Gradle toca
   `settings.gradle`, `app-service/build.gradle` y la lista de rutas de `ArchitectureTest`, que
   además está marcado como *«Please do not modify this file»*: **hay que decidir qué hacer con esa
   lista antes de renombrar**.
2. **¿`planning_chunks` admite columnas nuevas?** De ello depende si D-14 se cierra con una entidad
   de persistencia real o solo con un mapper sobre el esquema actual. **No se asume nada** sobre el
   esquema desplegado ni sobre quién más lo lee.

### DP-06

**Pregunta:** el mapa de excepción de negocio → código HTTP.

Hoy conviven dos criterios en el mismo entry-point: `TaskHandler` traduce cinco excepciones a cinco
códigos; `Handler` devuelve **400 para todo**. Hace falta el mapa acordado antes de centralizar la
traducción, porque **cambiar un código de respuesta es un cambio de contrato** con el frontend.

### Bloqueantes menores a confirmar junto con DP-05 y DP-06

| #        | Pregunta                                                                                                                 | Recomendación                                                                                   |
|----------|--------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| **B-08** | ¿Se puede tocar `azure-devops-frontend` en esta fase, para D-40 (evento `ERROR`) y para los códigos HTTP nuevos de D-16? | **Sí, en una tarea propia y explícita.** D-31 está hoy cerrada solo a medias.                   |
| **B-09** | ¿Se autoriza reescribir `DashboardRoutesCharacterizationTest` cuando D-16 cambie los códigos de respuesta del tablero?   | **Sí**, con el mismo formato de S-2/S-3/S-4 y solo para las pruebas cuyo comportamiento cambie. |
| **B-10** | ¿D-29/D-34 (contrato `a2a` compartido y versionado) entran aquí o esperan?                                               | **Esperan.** Tocan tres repositorios; encajan mejor con D-35, que ya tiene plan propio.         |

---

## 3. Lo que esta fase hará (borrador, sujeto a DP-05 y DP-06)

- **T-01** · Traducción de errores centralizada y **un solo criterio** para todo el entry-point,
  según el mapa de DP-06 (D-16).
- **T-02** · DTOs de respuesta para las rutas que hoy devuelven dominio, con su mapper, siguiendo el
  precedente de `DashboardDtoMapper` (D-15).
- **T-03** · Entidad de persistencia y mapper para `PlanningChunk`; el dominio deja de serializarse
  con Jackson dentro del adaptador (D-14).
- **T-04** · Renombrado del módulo `task-store-inmemory` según DP-05 (D-20).
- **T-05** · El frontend consume el evento `ERROR` del stream, si B-08 lo autoriza (D-40).
- **T-06** · Los colaboradores del stream pasan a beans, si el test de caracterización lo permite
  (D-42).

### Criterios de cierre propuestos

- [ ] `gradlew test` verde, **≥ 227 pruebas**, 0 fallos.
- [ ] **Un solo criterio** de traducción de errores en `entry-points`; cero `onErrorResume` que
  devuelvan 400 indiscriminadamente.
- [ ] Cero modelos de dominio devueltos como respuesta HTTP.
- [ ] Cero serialización del dominio con Jackson dentro de adaptadores.
- [ ] Ningún módulo con nombre que contradiga su contenido.
- [ ] Cobertura `domain/usecase` ≥ 90 %; `domain/model` no baja de 94,8 %.
- [ ] ArchUnit: *Rule_2.2* sigue en 0 y *Rule_2.7* no sube de 3.
- [ ] `RESULTADO-FASE-07.md` con la estructura habitual.
- [ ] `plan-maestro.md` y `fase-08.md` actualizados.

---

## 4. Lo que NO entra en esta fase

- Unificar las tres fachadas de logging → **Fase 08** (D-12).
- El módulo `mcp-client` casi vacío → **Fase 08** (D-32).
- Limpiar `application.yaml` → **Fase 08** (D-04).
- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Contrato `a2a` compartido y versionado → **D-29/D-34**, ver B-10.
- Llevar los reportes a S3 → **D-41**, requiere confirmar acceso al bucket.
- Medir el coste real del lote y la concurrencia → **D-38**, requiere entorno con datos.

---

## 5. Resultado

> Detalle completo en [`RESULTADO-FASE-07.md`](../resultados/RESULTADO-FASE-07.md).

- **Fecha de cierre:** 2026-08-29
- **DP-05 resuelta como:** módulo renombrado a **`task-store-postgres`** (autorizado actualizar la
  ruta codificada de `ArchitectureTest`); **`planning_chunks` sin columnas nuevas ni consumidores
  adicionales** → D-14 se cierra con entidad + mapper sobre el esquema actual, sin migración
- **DP-06 resuelta como:** el mapa de `TaskHandler` extendido a todas las rutas, más
  `TimeoutException → 504`. Cuerpo de respuesta sin cambios
- **B-08 / B-09 / B-10:** sí / sí / no
- **Tests totales / fallidos:** backend **238 / 0** · frontend **29 / 0**
- **Criterios de traducción de errores:** 2 → **1**
- **Cobertura `domain/model` / `domain/usecase`:** 94,8 % / 98,9 % (sin cambios)
- **Hallazgos no previstos:** la caracterización de la Fase 01 ya había marcado en su javadoc las
  dos pruebas que D-16 rompería; el mapa «nuevo» ya existía desde la Fase 03 dentro de
  `TaskHandler`; D-40 se cerró con doce líneas porque el `error:` del frontend ya estaba escrito,
  solo faltaba que alguien lo llamara
- **Deudas nuevas detectadas:** **D-43** (el esquema JSONB se publica tal cual en
  `GET /api/planning/initiatives`), **D-44** y **D-45** (la frontera del contrato `a2a` sigue sin
  existir, en persistencia y en HTTP; van con D-29/D-34)
- **No ejecutado:** **T-06 / D-42** — requiere tocar el `@ContextConfiguration` de dos pruebas de
  caracterización, y B-09 solo cubría las afectadas por D-16. Pasa a la Fase 08

