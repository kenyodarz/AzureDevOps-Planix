# FASE 01 — Baseline, caracterización y verificación del wiring

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🟢 **COMPLETADA** (2026-08-30)
> **Deudas que ataca:** D-05, D-06, D-21 · **Deudas que congela:** D-07, D-08, D-09, D-11
> **Riesgo:** Nulo · **Depende de:** — · **Habilita:** Fases 02, 03 y 04
> **Regla de oro de esta fase:** **cero cambios en `src/main`.** ✅ **Cumplida.**

---

## 1. Contexto

### 1.1 Estado actual del sistema

`azure-devops-mcp` es un servidor MCP reactivo (Spring Boot 4.1.0, Spring AI 2.0.0-RC1, protocolo
`STATELESS`, tipo `ASYNC`, Java 25) que expone Azure DevOps al agente y al BFF. Cinco módulos Gradle:
`:app-service`, `:model`, `:usecase`, `:mcp-server`, `:rest-consumer`.

Lo que hay hoy, medido:

| Artefacto                                             | Líneas | Papel real                                                     |
|-------------------------------------------------------|-------:|-----------------------------------------------------------------|
| `mcp-server/.../tools/AzureDevOpsTools.java`          |    267 | 6 tools + **el único flujo compuesto del sistema**              |
| `rest-consumer/.../consumer/RestConsumer.java`        |    237 | **7 gateways** + 8 mappers privados                             |
| `app-service/.../config/McpSecurityConfig.java`       |   ~180 | Termina en `.anyExchange().permitAll()`                         |
| `domain/usecase/**` (7 clases)                        |   ~175 | 6 delegantes de 17–18 líneas + `GetTeamIterationsUseCase` (68)  |
| `mcp-server/.../audit/McpAuditAspect.java`            |   ~190 | Auditoría AOP de las llamadas MCP                               |

Pruebas existentes (9 ficheros, incluido `Utils.java` que es utilería del `ArchitectureTest`):

| Test                            | Líneas | Módulo         |
|---------------------------------|-------:|----------------|
| `AzureDevOpsToolsTest`          |    328 | `mcp-server`   |
| `ArchitectureTest` + `Utils`    | 239+483| `app-service`  |
| `GetTeamIterationsUseCaseTest`  |    168 | `usecase`      |
| `RestConsumerTest`              |    127 | `rest-consumer`|
| `McpAuditAspectTest`            |     94 | `mcp-server`   |
| `McpSecurityConfigTest`         |     53 | `app-service`  |
| `UseCasesConfigTest`            |     46 | `app-service`  |
| `HealthToolTest`                |     38 | `mcp-server`   |

### 1.2 Dependencias resueltas de fases previas

Ninguna: **esta es la primera fase**. No hay estado heredado dentro de este repositorio.

Sí hay estado heredado **de otro plan**: el de `azure-devops-backend` cerró trasladando cuatro deudas
a este repositorio —**D-35, D-36, D-37 y D-39**—, reidentificadas aquí como **D-08, D-21, D-09** y
**D-17**. Esta fase toca dos de ellas: cubre D-21 (cobertura ausente de los dos gateways que resuelven
las rutas) e **instrumenta** la medición de D-09 (frecuencia del repliegue por año).

**Decisiones ya resueltas que condicionan esta fase (2026-08-30):**

- **DP-01** — La seguridad laxa es **deliberada**: el sistema es una POC y **no existe todavía el
  Service Principal**, por eso ni el agente ni el BFF envían token. La Fase 02 **no cerrará** el
  acceso; lo convertirá en configuración (`permissive` / `enforced`) para que el binario de
  producción sea **copia 1 a 1** del probado. **Consecuencia para esta fase:** el inventario de
  T-06 debe registrar el estado de autorización de cada tool **sin juzgarlo**, y las pruebas de
  caracterización deben escribirse **en modo abierto**, que es el vigente.
- **DP-02** — El PAT vendrá de variable de entorno o secreto; el usuario de servicio aún no existe.
  **Consecuencia para esta fase:** ninguna prueba puede depender de un token real, y `BASELINE.md`
  **no debe contener** ningún valor de token, ni siquiera de ejemplo.

### 1.3 Alcance específico de esta fase

Esta fase **no mejora nada**. Hace dos cosas y solo dos:

1. **Medir.** Producir cifras reales —no estimadas— de cobertura, número de pruebas, complejidad
   cognitiva y violaciones de ArchUnit, para poder demostrar después que el refactor mejoró algo.
2. **Congelar.** Blindar con pruebas de caracterización el comportamiento que las Fases 03 y 04 van a
   mover de sitio. En concreto, **la sentencia WIQL debe quedar fijada carácter a carácter**: es el
   artefacto más frágil del sistema y el que, si cambia, deja el tablero vacío **sin error y sin
   log** —exactamente el fallo que el plan del BFF tardó cinco fases en cerrar—.

Además resuelve por **inspección y prueba, no por suposición**, dos dudas del diagnóstico que no
pueden dejarse abiertas:

- **D-05** — ¿el `@ComponentScan` de `UseCasesConfig` registra realmente los casos de uso, además de
  los siete `@Bean` manuales? Con `useDefaultFilters=false` y un `includeFilter` por **regex sobre el
  nombre**, Spring puede registrar clases **sin anotación estereotipo**. Si lo hace, hay dos
  definiciones por bean. Hay que **verificarlo con una prueba**, no razonarlo.
- **D-06** — ¿qué configuración usan de verdad los siete `@CircuitBreaker`, si `application.yaml`
  solo declara `testGet` y `testPost`?

### 1.4 Lo que esta fase deliberadamente NO hace

- No toca **ningún** fichero bajo `src/main`. Ni un import, ni un espacio.
- No corrige D-01 a D-04 (seguridad): es la Fase 02, ya desbloqueada por **DP-01** y **DP-02**, pero
  con dos sub-decisiones vivas (**B-01**, el formato del PAT, y **B-02**, propiedad frente a perfil).
- No mueve el WIQL de sitio: es la Fase 04.
- No parte `RestConsumer`: es la Fase 05.
- No convierte el `ArchitectureTest` de *warning* a *error*: es la Fase 08. Aquí solo se **registra**
  el número de violaciones.

---

## 2. Instrucciones

### T-01 · Medir el baseline y dejarlo escrito

**Objetivo:** que ninguna cifra del plan maestro siga diciendo *«por medir»*.

1. Ejecutar la batería completa y los informes:

   ```
   cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
   .\gradlew.bat clean build jacocoMergedReport --no-daemon
   ```

2. Recoger de los informes generados:
   - `build/reports/jacoco*.xml` → cobertura **por módulo**, con `domain/model` y `domain/usecase`
     separados.
   - `build/reports/tests/test/index.html` de cada módulo → **total de pruebas y fallos**.
   - `build/issues.json` de cada módulo (lo escribe `ArchitectureTest`) → **violaciones de ArchUnit
     por regla**. Confirmar que `Rule_2.2` aparece **una vez** y que la clase señalada es
     `WorkItemsBatchRequest` (D-17).
3. Crear **`docs/resultados/BASELINE.md`** con: tabla de cobertura por módulo, número de pruebas,
   fallos, violaciones de ArchUnit por regla, líneas por clase de las cinco clases de §1.1 y la
   complejidad cognitiva máxima observada.
4. Volcar esas cifras en §6 del plan maestro, sustituyendo cada *«por medir (Fase 01)»*.

**Validación:** `BASELINE.md` existe y §6 del plan maestro no contiene la cadena `por medir`.

> Si el build sale **rojo**, se documenta el fallo en el bloque **Resultado** de esta fase y se
> continúa: la 01 es la única fase autorizada a heredar rojo (§8.4 del plan maestro).

---

### T-02 · Congelar la sentencia WIQL carácter a carácter

**Objetivo:** hacer imposible que la Fase 04 cambie la consulta sin que el build se ponga rojo.
**Es la tarea más importante de la fase.**

**Fichero nuevo:**
`infrastructure/entry-points/mcp-server/src/test/java/co/com/bancolombia/mcp/tools/WiqlCharacterizationTest.java`

Diseño:

- `@ExtendWith(MockitoExtension.class)`, `@Mock` de los siete casos de uso, `@InjectMocks` de
  `AzureDevOpsTools`.
- Capturar el `WiqlQuery` que recibe `queryByWiqlUseCase` con un `ArgumentCaptor<WiqlQuery>` y
  **comparar `getQuery()` contra la cadena literal esperada**, no contra un `contains(...)`.
- Javadoc de clase obligatorio explicando que **esta prueba existe para ser movida, no para ser
  borrada**: cuando la Fase 04 traslade la construcción del WIQL al caso de uso, esta prueba debe
  seguir verde apuntando al nuevo sitio.

Casos a cubrir (uno por rama real del código, `AzureDevOpsTools.java:120-163`):

| # | Escenario                                          | Qué congela                                              |
|---|-----------------------------------------------------|-----------------------------------------------------------|
| 1 | Camino feliz: `AreaPath` e `IterationPath` resueltos | La plantilla completa del `String.format` y el `ORDER BY` |
| 2 | `workItemTypes` nulo o en blanco                    | El literal por defecto `'Historia de Usuario','Habilitador'` |
| 3 | `workItemTypes = "User Story, Bug"`                 | El mapeo `User Story → Historia de Usuario` y el entrecomillado |
| 4 | `workItemTypes = "'Task'"` (ya entrecomillado)      | Que **no** se duplican las comillas                       |
| 5 | `teamName` con ruta y **dobles** barras             | La limpieza `\\\\` → `\` y el corte por el último `\`      |
| 6 | `getTeamFieldValues` falla                          | El repliegue a `resolveAreaPath` (concatenación)          |
| 7 | `resolveIterationPath` falla y el sprint es `Sprint N` | El repliegue con **el año del calendario** (D-09)       |
| 8 | `resolveIterationPath` falla y el sprint **no** es `Sprint N` | El repliegue sin año                            |

> **Aviso obligatorio en el javadoc del caso 7:** ese caso fija un comportamiento **que el plan
> considera defectuoso** y que **DP-04** puede decidir eliminar. Está aquí para que su desaparición
> sea una decisión visible y no un efecto colateral.

**Validación:** los 8 casos en verde. Si alguno no puede escribirse sin tocar `src/main`, se documenta
en **Resultado** en lugar de tocar `src/main`.

---

### T-03 · Cubrir los dos gateways que resuelven las rutas (D-21 ← D-36)

**Objetivo:** saldar la deuda que el plan del BFF dejó explícitamente a este repositorio.

**Fichero a ampliar:**
`infrastructure/driven-adapters/rest-consumer/src/test/java/co/com/bancolombia/consumer/RestConsumerTest.java`

Añadir, con `MockWebServer` (o el doble ya usado por las pruebas existentes: **inspeccionar primero y
seguir el patrón vigente**, no introducir uno nuevo):

1. `getTeamFieldValues` — camino feliz: verificar la **URI exacta** solicitada
   (`/{org}/{project}/{team}/_apis/work/teamsettings/teamfieldvalues?api-version=7.0`) y el mapeo
   `TeamFieldValuesDTO → TeamFieldValues`, incluido el caso `values == null → List.of()`.
2. `getTeamIterations` — camino feliz: URI exacta
   (`/{org}/{project}/{team}/_apis/work/teamsettings/iterations?api-version=7.0`) y el mapeo
   `TeamIterationsDTO → List<TeamIteration>`, incluido `value == null → List.of()`.
3. Ambos con **respuesta de error** (401 y 500): dejar constancia con una aserción de **qué excepción
   sale hoy** (`WebClientResponseException`). Esa aserción es la que la **Fase 06** vendrá a cambiar
   cuando exista la excepción de dominio; debe llevarlo escrito en su javadoc.

**Validación:** cobertura de `RestConsumer` medida antes y después en `BASELINE.md`; los dos métodos
dejan de estar a 0 %.

---

### T-04 · Resolver la duda del doble wiring (D-05)

**Objetivo:** convertir una sospecha en un hecho documentado, **sin arreglar nada todavía**.

**Fichero a ampliar:**
`applications/app-service/src/test/java/co/com/bancolombia/config/UseCasesConfigTest.java`

1. **Antes de escribir nada, leer el test actual.** El precedente del BFF (deuda D-17 de aquel plan)
   encontró un `UseCasesConfigTest` que **mentía**: capturaba `UnsatisfiedDependencyException` y daba
   verde. Verificar si aquí ocurre lo mismo y **dejarlo escrito en Resultado**.
2. Añadir una prueba que levante el contexto con `ApplicationContextRunner` (o el mecanismo que ya use
   el test) y **cuente las definiciones de bean por tipo**:
   `context.getBeanNamesForType(GetWorkItemUseCase.class).length` y equivalentes para los siete.
3. La aserción debe reflejar **lo que ocurre hoy**, sea 1 o 2. Si es 2, se documenta como
   confirmación de D-05 y se deja para la Fase 08. Si es 1, se documenta que el `@ComponentScan` es
   **inerte** y que la deuda D-05 se reduce a *código muerto que confunde*.

**Validación:** §3 del plan maestro se actualiza con el veredicto real de D-05.

---

### T-05 · Resolver la duda de los cortacircuitos (D-06)

**Objetivo:** documentar qué configuración usan de verdad los siete `@CircuitBreaker`.

1. Enumerar los nombres declarados en `RestConsumer` (`getWorkItem`, `createWorkItem`,
   `updateWorkItem`, `queryByWiql`, `getWorkItemsBatch`, `getTeamFieldValues`, `getTeamIterations`) y
   contrastarlos con los declarados en `application.yaml` (`testGet`, `testPost`).
2. Levantar el contexto en una prueba y consultar el `CircuitBreakerRegistry`: registrar en
   `BASELINE.md` los **valores efectivos** de `failureRateThreshold`, `slidingWindowSize`,
   `minimumNumberOfCalls` y `waitDurationInOpenState` para uno de los siete.
3. Dejar constancia de si `testGet` y `testPost` los usa **alguien** (`grep` en todo el repositorio,
   excluyendo `build/` y `build-cache/`).

**Validación:** `BASELINE.md` contiene la tabla «nombre declarado → configuración efectiva» de los
siete, y §3 confirma o corrige D-06.

---

### T-06 · Inventariar el contrato MCP público

**Objetivo:** que las Fases 03 y 04 no puedan romper a los clientes sin enterarse.

Crear **`docs/resultados/CONTRATO-MCP.md`** con, para **cada** tool expuesta:

- Nombre exacto de la tool (`name` del `@McpTool`).
- Nombre, tipo y obligatoriedad de **cada** parámetro (`@McpToolParam`).
- Tipo de retorno y forma del payload resultante.
- Autorización declarada, o **la ausencia de ella** (esto alimenta D-02 y la Fase 02).
- Marca explícita de los parámetros cuyo tipo es un **modelo de dominio** —`List<JsonPatchOperation>`
  en `createWorkItem` y `updateWorkItem`— porque son los que la Fase 03 va a sustituir por un DTO y
  los que **DP-03** debe decidir si conservan el nombre en el cable.

Incluir además la anomalía **D-19**: `queryByWiql` tiene el `@McpTool` **comentado** pero el método
sigue siendo público y su caso de uso y gateway existen. Registrar que **no forma parte del contrato
público** hoy.

**Validación:** el documento cubre las 8 tools/métodos anotados (6 activos + `queryByWiql` comentado
+ `checkHealth` y `getServerInfo` de `HealthTool`).

---

### T-07 · Actualizar el plan y generar la Fase 02

**Objetivo:** cumplir la **Regla de Continuidad** de §8 del plan maestro.

1. Marcar el checklist de §3 de este documento.
2. Rellenar el bloque **Resultado** de §4 con lo realmente alcanzado.
3. Actualizar en el plan maestro: §6 (cifras reales), §9 (Bitácora) y la cabecera (**Fase activa** →
   `fase-02.md`, **Completadas** → `01`).
4. **Generar `docs/fases/fase-02.md`** con las tres secciones obligatorias, tomando como **Contexto**
   el estado real medido en esta fase, y con **DP-01** y **DP-02** marcadas como bloqueantes en su
   primer paso.
5. Commit: `test(mcp_baseline): agregar pruebas de caracterizacion de las herramientas mcp`

---

## 3. Orden de Ejecución

Secuencial. **No adelantar pasos**: T-02 depende de conocer el estado real que produce T-01.

- [x] **1.** Ejecutar `.\gradlew.bat clean build --no-daemon` y anotar el resultado **antes de tocar
      nada**. → 🟢 **VERDE**, **33 pruebas, 0 fallos**. No hay rojo heredado.
- [x] **2.** Recoger la cobertura por módulo. → `model` **0 % (sin `src/test`)** · `usecase`
      **68,2 %** · `mcp-server` **70,5 %** · `rest-consumer` **51,1 %** · `app-service` **17,0 %**.
- [x] **3.** Leer los `build/issues.json`; contar violaciones por regla. → **los seis están vacíos**,
      pero el log del test sí declara `Rule_2.2 ... violated (1 times)`. **D-17 confirmada** y
      **D-25 detectada**: el informe para Sonar descarta las incidencias en silencio.
- [x] **4.** Crear [`docs/resultados/BASELINE.md`](../resultados/BASELINE.md) con todas las cifras. **(T-01)**
- [x] **5.** Leer `AzureDevOpsToolsTest.java` completo. → Sus 7 pruebas usan **solo `contains(...)`**:
      nunca fijan la sentencia completa. El test nuevo **no duplica**, aporta precisión.
- [x] **6.** Crear `WiqlCharacterizationTest` con los casos **1 a 4**. **(T-02)**
- [x] **7.** Añadir los casos **5 a 8**. **(T-02)** → **8 casos en verde.**
- [x] **8.** `:mcp-server` verde; sentencia WIQL literal anotada en `BASELINE.md` §7.1.
- [x] **9.** Leer `RestConsumerTest.java`. → Patrón vigente: `MockWebServer` **estático y compartido**,
      sin drenar la cola. Se respeta y se añade `drainRecordedRequests()` para poder asertar la URI
      sin desincronizar las pruebas existentes.
- [x] **10.** Pruebas de `getTeamFieldValues` (feliz + `values` nulo + 401). **(T-03)**
- [x] **11.** Pruebas de `getTeamIterations` (feliz + `value` nulo + 500). **(T-03)**
- [x] **12.** `:rest-consumer` verde. Cobertura **51,1 % → 71,5 %** (+20,4 puntos).
- [x] **13.** Leer `UseCasesConfigTest.java`. → **MIENTE por partida doble** (deuda nueva **D-27**):
      registra su propio bean `myUseCase`, que ya satisface la única aserción, **y** captura
      `UnsatisfiedDependencyException` con `assertTrue(true)`. **(T-04)**
- [x] **14.** Crear `UseCasesConfigWiringTest`. → **D-05: el `@ComponentScan` es INERTE.** Cada caso
      de uso resuelve a **exactamente un bean** y el contexto arranca. **(T-04)**
- [x] **15.** Contrastar los nombres de `@CircuitBreaker` con `application.yaml`. → **D-06
      CONFIRMADA**: las 7 instancias reales no están configuradas; las 2 configuradas no las usa nadie.
- [x] **16.** `grep` de `testGet` y `testPost`. → Solo aparecen en `application.yaml:53` y `:62`.
      **Cero usos.** Los umbrales escritos allí **no aplican a nada**. **(T-05)**
- [x] **17.** Crear [`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) con las 8 tools. **(T-06)**
- [x] **18.** `.\gradlew.bat build` → 🟢 **BUILD SUCCESSFUL**, **48 pruebas, 0 fallos**.
- [x] **19.** `git status -- "*/src/main/*"` → **vacío**. Regla de oro cumplida.
- [x] **20.** Cifras reales en §6 del plan maestro; veredictos de D-05 y D-06 en §3.
- [x] **21.** Bloque **Resultado** de §4 relleno y checklist marcado.
- [x] **22.** §9 (Bitácora) y cabecera del plan maestro actualizadas.
- [x] **23.** **[`docs/fases/fase-02.md`](fase-02.md) generado.** **(Regla de Continuidad)**
- [ ] **24.** Commit: `test(mcp_baseline): agregar pruebas de caracterizacion de las herramientas mcp`

---

## 4. Resultado

> **Estado: 🟢 COMPLETADA (2026-08-30).** Cifras completas en
> [`BASELINE.md`](../resultados/BASELINE.md) · Contrato en
> [`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md).

| Métrica                                          |            Antes |          Después |
|--------------------------------------------------|-----------------:|-----------------:|
| Tests totales                                    |           **33** |     **48** ▲15   |
| Tests fallando                                   |            **0** |       **0** ✅  |
| Cobertura `domain/model`                         | **0 %** *(sin `src/test`)* | **0 %** *(D-26)* |
| Cobertura `domain/usecase`                       |       **68,2 %** |     **68,2 %** = |
| Cobertura `rest-consumer`                        |       **51,1 %** | **71,5 %** ▲20,4 |
| Cobertura `mcp-server`                           |       **70,5 %** |  **71,3 %** ▲0,8 |
| Cobertura `app-service`                          |       **17,0 %** |  **23,6 %** ▲6,6 |
| Violaciones ArchUnit reales / exportadas         |        **1 / 0** |      **1 / 0**   |
| Ficheros modificados bajo `src/main`             |              n/a |       **0** ✅  |

**Veredicto D-05 (doble wiring): 🔽 DEGRADADA de 🔴 a 🟡.** `UseCasesConfigWiringTest` levanta el
contexto con los siete gateways como dobles: **cada caso de uso resuelve a exactamente un bean** y el
arranque no produce conflictos. El `@ComponentScan` con filtro por expresión regular **no aporta
ninguna definición observable** — o no registra nada, o lo que registra queda sustituido por el
`@Bean` homónimo. Deja de ser un riesgo de arranque y pasa a ser **código muerto que induce a error**.
Se retira en la Fase 08.

**Veredicto D-06 (cortacircuitos): ✅ CONFIRMADA.** Las siete instancias declaradas en
`RestConsumer` **no existen** en `application.yaml`; las dos que sí existen —`testGet` y `testPost`,
líneas 53 y 62— **no las usa nadie**. Los siete cortacircuitos reales corren con la configuración por
defecto de Resilience4j, y los umbrales escritos en el YAML **no aplican a nada**.

**Veredicto D-17 (`Rule_2.2`): ✅ CONFIRMADA.** El log declara
`Rule 'Rule_2.2 ...' was violated (1 times)`. Confirma **D-39** del plan del BFF.

**Veredicto D-21 (cobertura de rutas): ✅ SALDADA.** Seis pruebas nuevas cubren `getTeamFieldValues`
y `getTeamIterations` —camino feliz, colección nula y error técnico— con aserción sobre la **URI
exacta**. Cierra **D-36** del plan del BFF.

**Sentencia WIQL congelada: ✅** 8 ramas fijadas **carácter a carácter** en
`WiqlCharacterizationTest`. Literal en `BASELINE.md` §7.1. Incluye, con aviso explícito en su
javadoc, la rama que **intercala el año del calendario** (D-09), para que su eventual desaparición
sea una decisión visible de **DP-04** y no un efecto colateral.

**Deudas nuevas detectadas:**

- **D-25** 🟠 — El informe de ArchUnit para Sonar **sale vacío** pese a existir la violación.
- **D-26** 🟠 — `domain/model` **no tiene `src/test`**: 21 clases, cero pruebas.
- **D-27** 🟠 — `UseCasesConfigTest` **miente por partida doble**: registra su propio bean
  `myUseCase`, que satisface la única aserción por sí solo, **y** captura
  `UnsatisfiedDependencyException` con `assertTrue(true)`. Como no registra ningún gateway, el
  contexto no puede arrancar y **el camino que toma siempre es el `catch`**.

**Bloqueos encontrados:** ninguno. El build estaba y sigue verde, así que **no se consume** la
excepción de rojo heredado del §8.4 del plan maestro.

**Fase siguiente generada:** ☑ [`docs/fases/fase-02.md`](fase-02.md)

