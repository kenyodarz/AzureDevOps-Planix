# RESULTADO — FASE 08

> **Fase:** 08 — Configuración, cierre y verificación · **la última del plan**
> **Estado:** 🟢 **COMPLETADA** · **Fecha de cierre:** 2026-08-29
> **Plan:** [`plan-maestro.md`](../plan/plan-maestro.md) v2.0 · **Definición:** [
`fase-08.md`](../fases/fase-08.md)
> **Fase anterior:** [`RESULTADO-FASE-07.md`](RESULTADO-FASE-07.md)
> **Cierre del plan completo:** [`CIERRE-DEL-PLAN.md`](CIERRE-DEL-PLAN.md)

---

## 1. Resumen

|                                         | Antes      | Después                                                        |
|-----------------------------------------|------------|----------------------------------------------------------------|
| Pruebas backend                         | 238 (0 ❌) | **238 (0 ❌)**                                                 |
| Pruebas frontend                        | 29 (0 ❌)  | **29 (0 ❌)** · `tsc --noEmit` limpio                          |
| Fachadas de logging                     | **3**      | **2**, y por capa: SLF4J en infraestructura, JUL en el dominio |
| Declaraciones de log a mano             | 1          | **0** — todas por anotación de Lombok                          |
| Módulos                                 | 11         | **10** (`mcp-client` eliminado)                                |
| Bloques de configuración duplicados     | 2          | **0**                                                          |
| Claves retiradas de `application.yaml`  | —          | **21 líneas**, todas con su `grep`                             |
| ArchUnit *Rule_2.7*                     | 3          | **0**                                                          |
| ArchUnit *Rule_2.2*                     | 0          | **0**                                                          |
| Cobertura `domain/model`                | 94,8 %     | **94,8 %**                                                     |
| Cobertura `domain/usecase`              | 98,9 %     | **98,9 %**                                                     |
| Colaboradores del entry-point con `new` | 2          | **0**                                                          |

**Deudas saldadas en esta fase: D-04, D-12, D-32, D-42.** Verificadas y marcadas como ya saldadas
sin haberlo estado: **D-27** y **D-03**. Deudas nuevas detectadas: **D-46** y **D-47**.

---

## 2. Decisiones de arranque

| #        | Pregunta                                                                         | Respuesta                                                                                                                                                    |
|----------|----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-11** | ¿`mcp-client` se absorbe o se renombra?                                          | **Se elimina.** «El BFF no va al MCP, por ende no debería tener la clase mcp-client»                                                                         |
| **B-12** | ¿Qué fachada de logging queda y qué hace el dominio?                             | **SLF4J en infraestructura**; el dominio **conserva `java.util.logging`**, y todo el proyecto declara el log **de una sola manera: con anotación de Lombok** |
| **B-13** | ¿Se autoriza tocar el `@ContextConfiguration` de las pruebas de caracterización? | **Sí** (sin objeciones a la recomendación)                                                                                                                   |

> **Ambigüedad de B-12, resuelta por escrito y no en silencio (regla §5.9 del handoff).** La
> respuesta pedía «SLF4J para los demás» y a la vez «usemos una sola manera en todo el proyecto»,
> dos cosas que no pueden ser ciertas al mismo tiempo si el dominio se queda con JUL. Se interpretó
> «una sola manera» como **la forma de declarar**, no la fachada: en todo el backend el log se
> declara ahora con una anotación de Lombok (`@Slf4j` en infraestructura, `@Log` en el dominio) y
> **en ningún sitio con un `private static final Logger` escrito a mano**. La interpretación se
> comunicó al usuario antes de tocar código.

---

## 3. Qué se construyó

### T-01 · `application.yaml`: 114 → 93 líneas (D-04)

Cada borrado, con la prueba de que nadie lo leía.

| Qué se retiró                              |  Líneas | Evidencia de que nadie lo lee                                                                                                                                                                                                       |
|--------------------------------------------|--------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `spring.h2.console`                        |   59-62 | Cero coincidencias de `spring.h2` fuera del propio `yaml`, y **cero dependencias de `com.h2database`** en ningún `build.gradle`. Configuraba una consola H2 sobre un datasource que es **PostgreSQL**: nunca hubo nada que consolar |
| `adapter.aws.s3`                           | 108-113 | El único lector de S3 es `S3ConnectionProperties`, con `@ConfigurationProperties(prefix = "adapters.aws.s3")` — **en plural**. El bloque en singular era una copia byte a byte con el prefijo mal escrito                           |
| `spring.ai.mcp.client.*`                   |   51-58 | Consumido por la autoconfiguración del starter de Spring AI MCP, que sale del proyecto con `mcp-client` (T-03)                                                                                                                      |
| `mcp.connection-headers`                   |   94-97 | Único lector: `McpConnectionHeadersProperties`, eliminada en T-03                                                                                                                                                                   |
| `spring.security.oauth2.*`                 |   63-76 | Único lector: `OAuth2WebClientConfig`, eliminada en T-03. **Se va con ella el `client-secret`**, que era un placeholder con variable de entorno: el `yaml` queda con una superficie de secretos menor                               |
| `logging.level.org.springframework.ai.mcp` |      81 | Nivel de log de un paquete que ya no está en el classpath                                                                                                                                                                           |

**El bloque S3 que se conserva es el que tiene lector.** No se eligió por gusto ni por orden de
aparición: `adapters` (plural) es el prefijo que `S3ConnectionProperties` declara, y el otro no lo
declaraba nadie.

**Se añadió** una clave, `dashboard.mock-fallback.enabled` — ver T-03, es consecuencia de eliminar
el módulo MCP.

### T-02 · Una fachada por capa, una sola forma de declararla (D-12)

| Dónde                                                                                             | Antes                                                     | Después               |
|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------|-----------------------|
| `reactive-web` ×4 (`Handler`, `TaskHandler`, `ApiErrorTranslator`, `DashboardStreamOrchestrator`) | `@Log4j2`                                                 | **`@Slf4j`**          |
| Driven-adapters ×4                                                                                | `@Slf4j`                                                  | `@Slf4j` (sin cambio) |
| `domain/usecase` (`DevOpsDashboardUseCase`)                                                       | `private static final Logger log = Logger.getLogger(...)` | **`@Log` de Lombok**  |

Resultado: **9 clases con anotación, 0 declaraciones manuales, 0 `@Log4j2`.**

**El dominio se queda con `java.util.logging`, y está justificado en el propio código.** No es un
descuido heredado: es la única fachada que vive en el JDK. Poner SLF4J en `domain/usecase` sería
meter una dependencia técnica en el dominio, exactamente lo que prohíbe `rules/spring-rules.md` y lo
que D-14 acababa de arreglar en la frontera de al lado. Lombok no cuenta: es procesamiento en tiempo
de compilación y no deja rastro en el classpath de ejecución.

### T-03 · `mcp-client` eliminado (D-32)

Se retiraron las tres clases (`SecurityConfig`, `OAuth2WebClientConfig`,
`McpConnectionHeadersProperties`), el módulo y sus **diez** dependencias, y los **tres** puntos que
lo nombraban: `settings.gradle`, `applications/app-service/build.gradle` y la lista de rutas
codificada de `ArchitectureTest`.

**Pero el módulo no era «tres clases de configuración».** Antes de borrarlo aparecieron tres cosas
que el plan no recogía, y cada una habría roto algo distinto:

1. **Era el portador silencioso del modelo de embeddings.** `mcp-client/build.gradle` declaraba
   `spring-ai-starter-model-openai`, y `pgvector-store` **no lo declara**. Un módulo de MCP sostenía
   el classpath del almacén vectorial. La dependencia se **movió a `pgvector-store`**, que es quien
   la usa.
2. **Era quien fijaba el buffer del cliente del agente.** `OAuth2WebClientConfig` publicaba un
   `WebClient.Builder` con `maxInMemorySize` de **10 MB**, y `A2AAgentAdapter` lo inyectaba sin
   saberlo. Borrarlo lo habría dejado en los **256 KB** por defecto de WebFlux: el tablero habría
   empezado a fallar con respuestas grandes, sin relación aparente con el cambio. El ajuste se
   **movió a `A2AAgentAdapter`**, que construye su propio `WebClient`.
3. **Su bandera gobernaba una decisión de negocio.** `UseCasesConfig` leía
   `spring.ai.mcp.client.enabled` para **deducir** el repliegue al tablero simulado
   (`mockFallbackEnabled = !isMcpEnabled`). Borrar la clave sin más habría **invertido** el
   comportamiento: el `yaml` la declaraba `false`, luego el repliegue estaba **encendido**, y el
   valor por defecto de la anotación era `true`, luego habría pasado a **apagado**.

Sobre el punto 3, la salida fue hacer la propiedad **explícita**: `dashboard.mock-fallback.enabled`,
declarada en el `yaml` con `${DASHBOARD_MOCK_FALLBACK_ENABLED:true}` — el mismo valor efectivo que
había — y con **`false` como valor por defecto en código**, que es el seguro: si nadie declara nada,
no se sirve un tablero de mentira. Una decisión sobre qué ve el usuario deja de depender de la
configuración de un protocolo que el BFF ya no habla.

> **`SecurityConfig` desaparece sin dejar hueco.** Era el único `SecurityWebFilterChain` del
> backend, pero existía para desactivar la seguridad que introducía
> `spring-boot-starter-oauth2-client`, declarado en ese mismo módulo. Se van los dos a la vez:
> Spring Security sale del classpath y no hay nada que desactivar.

### T-04 · Se acabaron los andamios en el entry-point (D-42)

`DashboardStreamOrchestrator` y `DashboardTaskTracker` pasan a ser `@Component`. El `Handler` los
**inyecta** en lugar de construirlos con `new`.

El javadoc de `DashboardTaskTracker` decía que no era un bean «a propósito, porque no tiene estado
ni configuración propia». No era exacto: el orquestador **sí** tenía configuración propia —
`audit.batch-size` y `audit.concurrency`—, sólo que la declaraba el `Handler` en su nombre y se la
pasaba al construirlo. El entry-point leía dos propiedades que no usa. Ahora las lee quien las usa,
y el constructor del `Handler` baja de **8 parámetros a 5**: pierde `TaskStoreGateway`, `JsonMapper`
y los dos `@Value`, que sólo estaban ahí para montar a mano las dependencias de sus dependencias.

**Excepción S-6**, registrada en el javadoc de las dos pruebas de caracterización con el formato de
S-2…S-5: se añadieron las dos clases al `@ContextConfiguration` de
`DashboardRoutesCharacterizationTest`, `PlanningRoutesCharacterizationTest` y `TaskRoutesTest`
— **tres**, no dos: `TaskRoutesTest` comparte el `Handler` y el contexto se construye entero o no se
construye—. **Ninguna aserción, ningún código de estado y ningún cuerpo de respuesta se tocaron.**

### T-05 · Configuración tipada, y *Rule_2.7* a cero

Los tres `@Value` **sobre campos** que quedaban en el backend vivían en `pgvector-store`, y eran
exactamente las 3 violaciones de *Rule_2.7*:

| Clase                     | Antes                                                     | Después                              |
|---------------------------|-----------------------------------------------------------|--------------------------------------|
| `PgVectorPlanningAdapter` | `@Value` en 2 campos (`tableName`, `similarityThreshold`) | Campos **`final`** por constructor   |
| `PgVectorConfig`          | `@Value` en 1 campo (`tableName`)                         | Lee el mismo objeto de configuración |

Ambas leían `spring.ai.vectorstore.pgvector.table-name` **por separado**, con su propio valor por
defecto. Dos lecturas independientes de la misma clave es una invitación a que un día digan cosas
distintas: bastaba cambiar el defecto en un sitio para que el adaptador consultase una tabla y el
store escribiese en otra, **sin ningún error visible**. Ahora hay un solo
`PgVectorProperties`.

Es un **`record`**, no una clase con *setters*, precisamente porque *Rule_2.7* se cuenta **por
campo**: la versión con *setters* habría **subido** el contador en lugar de bajarlo.

**Resultado: *Rule_2.7* 3 → 0.** El criterio de cierre pedía «que no suba de 3».

---

## 4. Verificación

```
Backend    .\gradlew test          BUILD SUCCESSFUL   TESTS=238 FAIL=0 ERR=0
ArchUnit   issues.json             {"issues":[],"rules":[]}   (re-ejecutado con --rerun-tasks)
Cobertura  domain/model            94,8 %      domain/usecase   98,9 %
Frontend   pnpm test               Test Files 4 passed · Tests 29 passed
Frontend   tsc -p ... --noEmit     sin errores
```

> El `pnpm test` imprime `Error: Failed` por `stderr`: es una prueba que **lanza ese error a
> propósito** para comprobar el camino de fallo. Filtrar la salida por `FAILED` lo confunde con un
> fallo real. Las 29 pruebas pasan.

### Criterios de cierre de `fase-08.md` §3

- [x] `gradlew test` verde, ≥ 238 pruebas, 0 fallos → **238 / 0**
- [x] Frontend verde, ≥ 29 pruebas → **29 / 0**
- [x] Una sola fachada de logging en la infraestructura; la del dominio, justificada por escrito
- [x] Cero claves borradas sin su `grep` → **6 bloques, 6 evidencias** (§3, T-01)
- [x] Cero bloques de configuración duplicados
- [x] Cero módulos cuyo nombre no corresponda a su contenido
- [x] *Rule_2.2* en 0 y *Rule_2.7* no sube de 3 → **0 y 0**
- [x] Cobertura `domain/usecase` ≥ 90 % y `domain/model` ≥ 94,8 % → **98,9 % y 94,8 %**
- [x] `RESULTADO-FASE-08.md` **y** el informe de cierre del plan → este fichero y
  [`CIERRE-DEL-PLAN.md`](CIERRE-DEL-PLAN.md)
- [x] `plan-maestro.md` cerrado: bitácora completa y §3 con el estado final de cada deuda

---

## 5. Hallazgos no previstos

1. **`mcp-client` no era prescindible «tal cual».** Las tres trampas de §3/T-03. El handoff avisaba
   de que el `yaml` configuraba un cliente MCP y pedía comprobar quién lo consumía; lo que no se
   sabía es que el módulo sostenía el classpath de otros dos.
2. **Existe un módulo `s3-repository` que el handoff no mencionaba**, con cuatro clases y un
   `@ConfigurationProperties` activo. Es lo que convirtió D-04 de «borrar los dos bloques S3
   duplicados» en «borrar el que no tiene lector y conservar el que sí».
3. **D-03 estaba saldada y nadie la había marcado**, igual que D-27. Los DTOs
   `JsonRpcRequest/Response/Error` ya no existen: hoy son `JsonRpcEnvelope`, en `agent-client`.
   **Dos de las 45 deudas estaban resueltas sin registrar**, las dos relacionadas con configuración
   y contratos que otras fases tocaron de paso.
4. **`PgVectorConfig` define su propio `EmbeddingModel` `@Primary`** construido a mano con
   `RestClient`, lo que hace sospechar que el starter de OpenAI ya no es necesario. **No se
   eliminó**: ver D-47.

---

## 6. Desviaciones respecto al plan

1. **Se modificó `ArchitectureTest`**, marcado como *«Please do not modify this file»*, por segunda
   vez en el plan y por el mismo motivo que en la Fase 07: la lista de rutas está codificada a mano
   y eliminar un módulo sin actualizarla deja `issues.json` apuntando a un directorio inexistente.
   **Se retiró una sola cadena.**

2. **La excepción S-6 afecta a tres pruebas, no a dos.** `fase-08.md` y B-13 hablaban de «las dos
   pruebas de caracterización»; `TaskRoutesTest` también declara el `Handler` en su
   `@ContextConfiguration` y el contexto no se construye a medias. Se trató con el mismo criterio y
   se documentó igual.

3. **Se añadió una clave al `application.yaml` en una fase cuyo objetivo era quitarlas.**
   `dashboard.mock-fallback.enabled` es la contrapartida de eliminar `spring.ai.mcp.client.enabled`:
   sin ella, el repliegue al tablero simulado se habría invertido en silencio. El balance sigue
   siendo de **21 líneas menos**.

4. **Se conservó `spring-ai-starter-model-openai` aunque probablemente sobre.** Ver D-47. Preservar
   el classpath exacto es verificable; suponer que una autoconfiguración no hace falta, no. Política
   de No-Asunción.

5. **D-03 no era objetivo de esta fase**, pero se verificó al preparar el informe de cierre porque
   el plan la daba por viva. Se marca como saldada **sin tocar código**.

---

## 7. Lo que no se tocó, y por qué

- **D-43, D-44, D-45** (fugas del esquema JSONB y del modelo `a2a`): atadas a D-29/D-34 y excluidas
  por B-10. Se mantiene el criterio de la Fase 07.
- **D-35** (prompts al MCP), **D-41** (reportes a S3), **D-38** (medir lote y concurrencia),
  **D-36/D-37/D-39** (otro repositorio): fuera del plan por definición.
- **D-18** (cobertura de adaptadores y entry-point): nunca fue objetivo de ninguna fase.
- **`A2APayloadMapper`, 351 líneas**, la única clase productiva por encima de 300: traduce el
  contrato `a2a`, así que tocarla es D-29/D-34. Se midió y se dejó.

---

## 8. Deudas nuevas

| #        | Deuda                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Dónde                                         |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| **D-46** | `application.yaml` declara `spring.ai.openai.embedding.model`, pero el único lector identificado (`PgVectorConfig`) lee `spring.ai.openai.embedding.options.model`, con **`options.` en medio**. Si la clave declarada no alimenta nada, el BFF embebe con el valor por defecto del código sin que nadie lo haya elegido. **No se retiró**: confirmarlo exige saber cómo enlaza esa propiedad la autoconfiguración de Spring AI 2.0.0                                                                                                                                                                                                                                                                                                                         | `azure-devops-backend`                        |
| **D-47** | `pgvector-store` arrastra `spring-ai-starter-model-openai`, heredado de `mcp-client`, mientras `PgVectorConfig` publica su **propio** `EmbeddingModel` anotado `@Primary`. Sobra el **starter**; el `EmbeddingModel` **no**, porque el BFF sí vectoriza (D-48). **Se mantuvo para preservar el classpath exacto**                                                                                                                                                                                                                                                                                                                                                                                                                                             | `azure-devops-backend`                        |
| **D-48** | **Agente y BFF escriben vectores en la misma tabla `planning_chunks`, cada uno con su propio modelo de embeddings y sin nada que garantice que coinciden.** Detectada al comentar D-46/D-47 con el usuario: se creía que el BFF sólo *leía* planificaciones ya vectorizadas por el agente, pero `POST /api/planning/ingest` llega a `vectorStore.add(...)` (`PgVectorPlanningAdapter:76`) y la búsqueda a `similaritySearch(...)` (línea 110); **ambas embeben**. Si los modelos difieren, la búsqueda **no falla: devuelve resultados malos en silencio**. Es D-24 (integración por BD compartida) otra vez, cortada para `agent_tasks` y viva para el almacén vectorial. **Decisión del usuario: no se toca en esta fase**, se diseñará una solución aparte | `azure-devops-backend` + `azure-devops-agent` |

**Siguen vivas:** D-18, D-29, D-34, D-35, D-36, D-37, D-38, D-39, D-41, D-43, D-44, D-45. Detalle y
dueño de cada una en [`CIERRE-DEL-PLAN.md`](CIERRE-DEL-PLAN.md).

---

## 9. Estado de git

**No se hizo ningún commit.** El trabajo sin confirmar desde la Fase 02 sigue sin confirmar, ahora
con los cambios de esta fase encima. Se hará cuando se pida expresamente, y con el formato de
`COMMIT_RULES.md`.

