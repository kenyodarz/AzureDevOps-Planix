# FASE 08 — Configuración, cierre y verificación

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** 🟢 **COMPLETADA** (2026-08-29)
> **Deudas objetivo:** **D-04, D-12, D-32, D-42** · **D-27 ya saldada** (ver §1.2)
> **Arranque en sesión nueva:** [`HANDOFF-FASE-08.md`](../plan/HANDOFF-FASE-08.md)
> **Fase anterior:** [`fase-07.md`](fase-07.md) → [
`RESULTADO-FASE-07.md`](../resultados/RESULTADO-FASE-07.md)
> **Fase siguiente:** ninguna. Ésta cierra el plan.

---

## 1. Contexto

### 1.1 De dónde vienes

Al arrancar esta fase existe:

- **Backend en verde: 238 pruebas, 0 fallos.** Frontend: 29 pruebas, 0 fallos.
- **Un solo criterio de traducción de errores** en todo el entry-point (`ApiErrorTranslator`).
- Los fragmentos de planificación tienen **entidad de persistencia** y **DTO de respuesta**: el
  dominio ya no viaja ni a la base de datos ni por HTTP.
- Ningún módulo miente sobre su contenido: `task-store-postgres` se llama como lo que es.
- El frontend **escucha el evento `ERROR`** del stream: D-31 queda cerrada por completo.
- Cobertura: `domain/model` 94,8 % · `domain/usecase` 98,9 %. ArchUnit *Rule_2.7* en 3, *Rule_2.2*
  en 0.

### 1.2 El problema que queda

Lo que sobrevive no son fallos de diseño del flujo, sino **residuos**: configuración que nadie lee,
tres formas distintas de escribir un log, un módulo casi vacío y dos colaboradores que se construyen
con `new` para no despeinar una prueba.

> ⚠️ **Tres afirmaciones del plan maestro no se sostienen al mirar el código.** Verificado el
> 2026-08-29 al generar esta fase; detalle en [`HANDOFF-FASE-08.md`](../plan/HANDOFF-FASE-08.md)
> §3.1.

| Deuda    | Dónde                           | Qué pasa **de verdad**                                                                                                                                                                                                                                                                                                                                                                  |
|----------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-04** | `application.yaml` (114 líneas) | `a2a.*` y `agent.system-prompt` **ya no están**. Quedan **dos** residuos reales: la **consola H2** (59-62) sobre un datasource que es PostgreSQL, y el bloque **S3 duplicado y mal escrito**: `adapters.aws.s3` (102-107) y `adapter.aws.s3` (108-113), idénticos                                                                                                                       |
| **D-12** | Todo el backend                 | Tres fachadas: **`@Log4j2` ×4** (todas en `reactive-web`), **`@Slf4j` ×5** (todos los driven-adapters) y **`java.util.logging` ×1**, en `DevOpsDashboardUseCase`. **Ese JUL es el único log del dominio y es deliberado**: es la única fachada del JDK, y meter SLF4J en `domain/usecase` sería meter una dependencia técnica en el dominio                                             |
| **D-27** | `application.yaml`              | **Ya saldada.** `agent.url` **existe**, línea 4, con `agent.timeout` debajo. Alguna fase lo añadió sin marcar la deuda: hay que **verificarlo y marcarlo**, no arreglarlo                                                                                                                                                                                                               |
| **D-32** | `mcp-client`                    | `NoOpAgentResponseAdapter` **se eliminó en la Fase 03**. Quedan **tres clases, todas de configuración**: `SecurityConfig`, `OAuth2WebClientConfig`, `McpConnectionHeadersProperties`. **Pero el `yaml` sí configura un cliente MCP de Spring AI** (`spring.ai.mcp.client`, 51-58) y `mcp.connection-headers` (94-97): antes de dar el módulo por prescindible hay que ver quién lee eso |
| **D-42** | `Handler`                       | `DashboardStreamOrchestrator` y `DashboardTaskTracker` se instancian con `new` para no alterar el `@ContextConfiguration` de dos pruebas de caracterización                                                                                                                                                                                                                             |

### 1.3 Por qué D-42 va aquí y no fue en la 07

La Fase 07 tenía autorización (B-09) para reescribir las pruebas de caracterización **afectadas por
D-16**, no cualquiera. Convertir esos dos colaboradores en beans obliga a tocar el
`@ContextConfiguration` de dos pruebas por un motivo **distinto**, así que se dejó. Esta fase es el
sitio: es la que jubila andamios.

---

## 2. 🟠 Decisiones pendientes

Ninguna heredada. Pero esta fase **sí** necesita respuesta a tres cosas antes de tocar código:

| #        | Pregunta                                                                                                 | Recomendación                                                                                                                                                                                                                          |
|----------|----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-11** | `mcp-client` (3 clases de configuración): ¿se absorbe y se elimina el módulo, o solo se renombra? (D-32) | **Comprobar primero quién consume `spring.ai.mcp.client`**; si nadie, absorber en `app-service` y eliminar el módulo. Si alguien, **renombrarlo** a lo que de verdad es                                                                |
| **B-12** | ¿Qué fachada de logging se queda, y **qué hace el dominio**? (D-12)                                      | **SLF4J en infraestructura** (los 4 `@Log4j2` → `@Slf4j`) y **el dominio se queda con `java.util.logging`**, que es JDK y no una dependencia externa. La alternativa «pura» —un puerto de logging— es una deuda nueva, no una limpieza |
| **B-13** | ¿Se autoriza tocar el `@ContextConfiguration` de las dos pruebas de caracterización para cerrar D-42?    | **Sí**, con el formato de S-2…S-5. Es la última vez que hará falta                                                                                                                                                                     |

Y una **verificación previa obligatoria**, no una decisión: antes de borrar una sola clave de
`application.yaml`, confirmar con `grep` que **nadie** la lee (`@Value`, `@ConfigurationProperties`,
`Environment`, `build.gradle`, `deployment/`). Política de No-Asunción: una clave sin lector
aparente puede tenerlo en un perfil o en un manifiesto de despliegue.

---

## 3. Lo que esta fase hará (borrador)

- **T-01** · Limpiar `application.yaml`: retirar la consola H2 y **uno** de los dos bloques S3
  duplicados (D-04). Cada borrado, justificado con su `grep`. **Verificar y marcar D-27 como
  saldada**, en vez de «arreglarla».
- **T-02** · Una sola fachada de logging en infraestructura, según B-12 (D-12).
- **T-03** · Resolver `mcp-client` según B-11, después de comprobar quién lee su configuración
  (D-32).
- **T-04** · `DashboardStreamOrchestrator` y `DashboardTaskTracker` pasan a beans, si B-13 lo
  autoriza (D-42).
- **T-05** · Configuración tipada donde haya más de dos `@Value` en la misma clase, vigilando que
  **no suba *Rule_2.7*** (se cuenta por campo: usar `record` o campos `final`).
- **T-06** · **Informe de cierre del plan**: recorrido de las 8 fases, estado final de las 45 deudas
  y lo que queda vivo, con dueño. No es lo mismo que el `RESULTADO-FASE-08.md`.

### Criterios de cierre propuestos

- [ ] `gradlew test` verde, **≥ 238 pruebas**, 0 fallos.
- [ ] Frontend verde, ≥ 29 pruebas.
- [ ] **Una sola** fachada de logging en la infraestructura; la del dominio, justificada por
  escrito.
- [ ] **Cero claves borradas sin su `grep`** que demuestre que nadie las lee.
- [ ] Cero bloques de configuración duplicados.
- [ ] Cero módulos cuyo nombre no corresponda a su contenido.
- [ ] ArchUnit: *Rule_2.2* en 0 y *Rule_2.7* **no sube de 3** (idealmente baja).
- [ ] Cobertura `domain/usecase` ≥ 90 %; `domain/model` no baja de 94,8 %.
- [ ] `RESULTADO-FASE-08.md` **y** el informe de cierre del plan.
- [ ] `plan-maestro.md` cerrado: bitácora completa y §3 con el estado final de cada deuda.

---

## 4. Lo que NO entra en esta fase

- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Contrato `a2a` compartido y versionado → **D-29/D-34**, y con ellos **D-44** y **D-45**.
- La fuga del esquema JSONB en `GET /api/planning/initiatives` → **D-43**, requiere conocer a todos
  los consumidores de esas claves.
- Llevar los reportes a S3 → **D-41**, requiere confirmar acceso al bucket.
- Medir el coste real del lote y la concurrencia → **D-38**, requiere entorno con datos.
- Las deudas del MCP (**D-36**, **D-37**, **D-39**) → otro repositorio.
- Cambiar el proveedor de LLM o el protocolo MCP.

---

## 5. Resultado

> 🟢 **COMPLETADA.** Detalle en [`RESULTADO-FASE-08.md`](../resultados/RESULTADO-FASE-08.md) y cierre
> del plan en [`CIERRE-DEL-PLAN.md`](../resultados/CIERRE-DEL-PLAN.md).

- **Fecha de cierre:** 2026-08-29
- **B-11 / B-12 / B-13:** eliminar `mcp-client` / SLF4J en infraestructura + JUL en el dominio, y
  una sola forma de declarar el log (anotación de Lombok) / autorizada
- **Tests totales / fallidos:** 238 / 0 (frontend: 29 / 0)
- **Fachadas de logging:** 3 → **2**, una por capa; declaraciones a mano 1 → **0**
- **Claves retiradas de `application.yaml`:** 6 bloques, **21 líneas** (114 → 93), cada una con su
  `grep`. Se **añadió** `dashboard.mock-fallback.enabled`, contrapartida de eliminar la bandera MCP
- **Cobertura `domain/model` / `domain/usecase`:** 94,8 % / 98,9 % (sin variación)
- **Hallazgos no previstos:** `mcp-client` era el portador del starter de OpenAI para
  `pgvector-store` y quien fijaba el buffer de 10 MB del cliente del agente; su bandera
  `spring.ai.mcp.client.enabled` gobernaba el tablero simulado. Apareció un módulo `s3-repository`
  no documentado, que es quien lee `adapters.aws.s3`. **D-03 estaba saldada sin marcar**, igual que
  D-27
- **Deudas nuevas detectadas:** **D-46** (clave de embeddings posiblemente muerta), **D-47**
  (starter de OpenAI probablemente innecesario) y **D-48** (agente y BFF vectorizan sobre la misma
  tabla `planning_chunks` con modelos que nadie garantiza que coincidan; la búsqueda por similitud
  degradaría **sin lanzar error**). El usuario decidió **no tocarlo en esta fase**
- **Deudas vivas al cerrar el plan:** **15** — D-18, D-29, D-34, D-35, D-36, D-37, D-38, D-39, D-41,
  D-43, D-44, D-45, D-46, D-47, D-48. Dueño de cada una en `CIERRE-DEL-PLAN.md` §3.2 y §3.3
- **ArchUnit:** *Rule_2.2* 0 → **0** · *Rule_2.7* 3 → **0**

