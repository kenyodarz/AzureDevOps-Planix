# FASE 06 — Errores, resiliencia y contrato de fallos

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🟢 **COMPLETADA**
> (2026-08-30)
> **Deudas que ataca:** D-06, D-13, D-18, D-24 *(D-06, D-13 y D-24 **saldadas**; D-18 **saldada en
su parte configurable**)*
> **Decisión bloqueante:** ✅ **DP-06 (RESUELTA)**
> **Riesgo:** 🟠 Medio · **Depende de:** Fases 01 a 05 (cerradas) · **Habilita:** Fases 07 y 08
> **Regla de oro de esta fase:** **esta fase SÍ cambia comportamiento observable, y por eso lo
> decide el propietario antes de escribirlo.** Es la primera del plan que no puede prometer «cero
> cambios»: traducir un error **es** cambiar lo que ve el cliente.
> ✅ **Y el cambio fue exactamente el autorizado:** solo la forma del error. **0 URLs, cuerpos,
> parámetros de consulta o cabeceras alterados**, verificado con diff literal contra `HEAD`.

---

## 0. ⛔ PASO BLOQUEANTE — DP-06

**No se escribe una sola línea de código antes de que el propietario responda.**

### 0.1 La pregunta: ¿qué debe ver el cliente MCP cuando Azure DevOps falla?

Hoy **no hay una sola excepción de dominio en el repositorio** (D-13). Un `401`, un `404` o un
`TF401232` de Azure DevOps viaja **crudo** hasta el cliente MCP como `WebClientResponseException`
—una excepción de Spring— a través de un contrato que debería ser de dominio. El único tratamiento
que existe es un `doOnError` en `queryByWiql` que **registra y no traduce**.

Dos pruebas lo dejan **congelado a propósito** en `TeamScopeAdapterTest`, con este aviso escrito
dentro:

> ⚠️ *Las dos pruebas siguientes NO validan un buen comportamiento: fijan el ACTUAL. La FASE 06
> introducirá excepciones de dominio y estas aserciones deberán cambiar de tipo esperado. Cuando eso
> ocurra será un cambio DELIBERADO y visible, que es justo lo que estas pruebas existen para
> garantizar.*

**Ese momento es ahora.** Y hace falta saber exactamente qué poner en su lugar:

| Pregunta                                                                                             | Por qué no se puede asumir                                                                                                                                                                                      |
|------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **(a)** ¿Qué **forma** tiene el error que llega al cliente? ¿Mensaje libre, código estable, o ambos? | El agente y el BFF son clientes reales. Si alguno hace `catch` por tipo o por texto, cambiarlo lo rompe en silencio                                                                                             |
| **(b)** ¿Se **propaga el cuerpo original** de Azure DevOps, se resume, o se oculta?                  | Propagarlo filtra nomenclatura de Azure DevOps aguas arriba, justo lo que §1 del plan maestro prohíbe. Ocultarlo puede dejar sin diagnóstico a quien opera                                                      |
| **(c)** ¿Se distingue **404 de 401 de 5xx**? ¿Cuántas excepciones de dominio hay?                    | El plan fija «**≥ 3**» como objetivo, pero los nombres y la granularidad son contrato                                                                                                                           |
| **(d)** ¿Un fallo de Azure DevOps debe **romper la tool** o devolver un resultado vacío?             | **Esto ya está medio decidido y no se puede contradecir:** DP-04 conservó el repliegue de rutas precisamente para no convertir un tablero vacío en un error. Hay que declarar si esa política vale también aquí |

### 0.2 La segunda pregunta: los valores de resiliencia

**D-06 lleva vivo desde el baseline y la Fase 05 lo dejó listo, no resuelto.** Los cortacircuitos
son hoy **tres** —`workItemQuery`, `workItemCommand`, `teamScope`— y **ninguno está declarado en
`application.yaml`**, que sigue declarando `testGet` y `testPost`, dos nombres del scaffold que **no
usa nadie**. Los tres corren con la configuración **por defecto** de Resilience4j sin que nadie la
haya elegido: no es un fallo visible, es un parámetro operativo fantasma.

Hacen falta **cifras, no criterio del refactor**:

| Parámetro                                                                            | Para qué                                                                                                                       |
|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `failureRateThreshold`, `slidingWindowSize`, `waitDurationInOpenState` por instancia | Abrir el circuito ni antes ni después de lo que el negocio tolera                                                              |
| **Timeout por operación** (D-24)                                                     | Hoy solo hay timeouts de Netty de **5 s compartidos** por todas las llamadas, **incluida la de lote**, que es la que más tarda |
| ¿Se **elimina** `testGet` / `testPost` del YAML?                                     | Son residuo del scaffold, pero borrarlos es tocar configuración de entornos                                                    |

> ⚠️ **Aviso heredado de la Fase 05.** Los tres nombres de instancia son **definitivos**: se
> renombraron en la Fase 05 (B-05) precisamente para que esta fase configurara los correctos. **No
> los vuelvas a cambiar.**

### 0.3 Sub-decisiones

| ID       | Cuestión                                                                                                                                                                                                                                                                                                                                                            |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-08** | ¿Dónde se traduce el error: en **el adaptador** (`rest-consumer`), en **el entry-point** (`McpErrorTranslator`), o en ambos con responsabilidades distintas? §4 del plan maestro dibuja las dos piezas; conviene ratificar el reparto antes de escribirlas                                                                                                          |
| **B-09** | D-18: ¿las versiones de API pasan a **`@ConfigurationProperties`** o a un **objeto de valor de dominio** (`ApiVersion`)? DP-04 sentó el precedente de que las reglas de Azure DevOps son dominio, pero una versión de API se parece más a configuración. Hoy están centralizadas en `consumer/ApiVersions` (Fase 05, B-07), **con los mismos literales de siempre** |
| **B-10** | Las **dos** consultas de ámbito de equipo llevan `api-version=7.0` **escrito literalmente en la ruta**, sin parámetro que lo sobreescriba. ¿Se parametrizan —lo que **cambia una llamada HTTP**— o se dejan?                                                                                                                                                        |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-06 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 07**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 05 (2026-08-30), no estimado:

| Dato                         | Valor                                                                                                               |
|------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Build                        | 🟢 **VERDE** — **148 pruebas, 0 fallos**                                                                            |
| Cobertura `domain/model`     | **94,7 %**                                                                                                          |
| Cobertura `domain/usecase`   | **97,8 %**                                                                                                          |
| Cobertura `mcp-server`       | **80,7 %**                                                                                                          |
| Cobertura `rest-consumer`    | **88,4 %**                                                                                                          |
| Cobertura `app-service`      | **58,2 %**                                                                                                          |
| Adaptadores de Azure DevOps  | **3**: `WorkItemQueryAdapter` **115** · `WorkItemCommandAdapter` **80** · `TeamScopeAdapter` **78** *(todos ≤ 120)* |
| Puertos de dominio           | **3** (`WorkItemQueryPort`, `WorkItemCommandPort`, `TeamScopePort`) + 1 de observabilidad · **0 huérfanos**         |
| Paquetes de `domain/model`   | **2** (`workitem/`, `team/`)                                                                                        |
| Líneas de `AzureDevOpsTools` | **157** *(sin ninguna regla de negocio)*                                                                            |
| Cortacircuitos               | **3** (`workItemQuery`, `workItemCommand`, `teamScope`), **ninguno declarado en el YAML**                           |
| Excepciones de dominio       | **0**                                                                                                               |
| Sentencia WIQL               | **congelada carácter a carácter**, 8 ramas, en `domain/usecase`                                                     |
| Cuerpo HTTP de salida        | **congelado carácter a carácter**, 4 cuerpos                                                                        |
| ArchUnit `Rule_2.2`          | ✅ **0 violaciones reales** *(verificado en el log; D-25 sigue viva)*                                               |

> **Cobertura medida por líneas** (JaCoCo `LINE`), que es el criterio del baseline. Por
> instrucciones sería 95,8 % · 98,6 % · 76,6 % · 91,1 % · 53,8 %.

### 1.2 Lo que dejaron hechas las fases previas y afecta a ésta

**De la Fase 05:**

- ✅ **D-10, D-14 y D-15 saldadas.** Hay **tres adaptadores** en `rest-consumer`, no uno. **La
  traducción de errores debe repartirse entre los tres sin duplicarse**, igual que se repartieron
  los mappers: si acaba copiada tres veces, la fase ha fallado.
- ✅ **Los 3 nombres de cortacircuito son definitivos** (B-05). Configúralos; no los renombres.
- ✅ **`consumer/ApiVersions`** centraliza el ternario de versión con **los literales originales**.
  Es el único punto que tocar para D-18.
- 🔸 **`RestConsumerConfig` no se tocó en la Fase 05.** Ahí viven el `WebClient` compartido, la
  validación del token (Fase 02) y los timeouts de Netty de 5 s que D-24 debe sustituir por timeouts
  por operación.
- 🔸 **Precedente autorizado:** la Fase 05 adaptó **5 clases de prueba heredadas** y dividió
  `RestConsumerTest` en tres, **con autorización expresa**. Fue puntual: **no es un permiso
  general**.

**De las Fases 02, 03 y 04, intocable:**

- La postura de seguridad es **configuración** (`mcp.security.mode`, `PERMISSIVE` por defecto).
- La **frontera de DTOs** existe: 1 mapper de entrada, 5 de salida. **Ningún modelo de dominio
  puede volver a exponerse como `@McpToolParam` ni ser serializado por el `WebClient`.**
- El **repliegue por concatenación se conserva y se mide** (DP-04, opción a). Si esta fase traduce
  errores, debe **respetar** que un fallo al resolver rutas siga cayendo al repliegue y **no** se
  convierta en un error para el cliente, salvo que DP-06 §0.1 (d) diga lo contrario.

### 1.3 El problema que esta fase resuelve

```
HOY                                          OBJETIVO DE ESTA FASE

cliente MCP  ◄── WebClientResponseException  cliente MCP  ◄── excepción de dominio traducida
             (una excepción de Spring)                     (contrato estable y descrito)

cortacircuitos  3 declarados / 0 configurados   3 declarados / 3 configurados
timeouts        1 de Netty, 5 s, compartido     1 por operación, elegido
```

`spring-rules.md` es explícito para `driven-adapters`: **«Capturan excepciones técnicas y las
traducen a excepciones tipadas del dominio»**. Hoy no se traduce ninguna. Y §1 del plan maestro pide
que «versiones, formatos, códigos de error y nomenclatura **no se filtren** aguas arriba»: hoy se
filtran todos.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No retira los `@Setter`** del modelo preexistente ni convierte el dominio en Value Objects:
  D-16 → **Fase 07**.
- **No toca la seguridad** (Fase 02), ni la frontera de DTOs (Fase 03), ni el flujo compuesto (Fase
  04), ni el reparto de adaptadores (Fase 05).
- **No arregla D-25** (informe de ArchUnit vacío), ni D-05, D-19, D-20, D-23, D-27 → **Fase 08**.
- **No cambia el contrato MCP público** —nombres de tool, de parámetro, de campo— salvo en lo que
  DP-06 declare expresamente sobre la **forma del error**.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-06**.

### T-01 · Excepciones de dominio (D-13)

**Ficheros:** `domain/model/.../model/exception/` *(nuevo)*

Crear las excepciones que decida **DP-06 §0.1 (c)**, extendiendo de una base de dominio. **Sin
Spring, sin HTTP, sin Jackson.** El plan maestro §4 anticipa `AzureDevOpsUnavailableException`,
`WorkItemNotFoundException` e `IterationNotFoundException`, pero los nombres los ratifica DP-06.

### T-02 · Traducción única en el adaptador (D-13)

**Ficheros:** `rest-consumer/.../consumer/AzureDevOpsErrorTranslator.java` *(nuevo)* · los **3**
adaptadores

1. Un solo traductor, **compartido por los tres adaptadores**. Si acaba duplicado, la fase falla.
2. Los tres adaptadores lo enchufan **sin cambiar ninguna URL, cuerpo ni cabecera**.
3. Según **B-08**, decidir si el entry-point necesita además un `McpErrorTranslator`.

**Validación:** las dos pruebas de caracterización de D-13 en `TeamScopeAdapterTest` **cambian de
tipo esperado**. Es el único cambio de aserción autorizado por esta fase, y solo si DP-06 lo dice.

### T-03 · Cortacircuitos y timeouts reales (D-06, D-24)

**Ficheros:** `applications/app-service/src/main/resources/application.yaml` · `RestConsumerConfig`

1. Declarar las **tres** instancias con los valores de **DP-06 §0.2**.
2. Retirar `testGet` y `testPost` si DP-06 lo autoriza.
3. Timeout **por operación**, no un Netty de 5 s compartido; el de lote es el caso crítico.

### T-04 · Versiones de API (D-18)

Según **B-09** y **B-10**. Punto de partida: `consumer/ApiVersions`, ya centralizado. **Si B-10 dice
que no se parametrizan, las dos URLs de equipo se quedan como están.**

### T-05 · Actualizar documentación y generar la Fase 07

1. `CONTRATO-MCP.md`: **sección nueva sobre el contrato de errores.** Es contrato público desde el
   momento en que se traduce el primero.
2. Plan maestro: §3 (D-06, D-13, D-18, D-24), §6 (métricas), §7 (DP-06), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-07.md`** y **`PROMPT-FASE-07.md`**.
5. Commit: `refactor(error_handling): traducir los fallos de azure devops a excepciones de dominio`

---

## 3. Orden de Ejecución

- [x] **0.** ⛔ **Plantear DP-06 (y B-08, B-09, B-10) al propietario y esperar respuesta.**
- [x] **1.** Releer `CONTRATO-MCP.md` §2 y §3: qué es contrato público y qué no.
- [x] **2.** Inventariar cada punto donde hoy escapa un error técnico, y las 2 pruebas que lo
  congelan.
- [x] **3.** Crear las excepciones de dominio. **(T-01)**
- [x] **4.** Crear el traductor único y enchufarlo en los **3** adaptadores. **(T-02)**
- [x] **5.** `.\gradlew.bat build` verde: **ni una URL ni un cuerpo alterados**. **(T-02)**
- [x] **6.** Declarar los **3** cortacircuitos y los timeouts por operación. **(T-03)**
- [x] **7.** Aplicar B-09 y B-10 sobre las versiones de API. **(T-04)**
- [x] **8.** Comprobar en el **log** del `ArchitectureTest` que sigue a **0** violaciones.
- [x] **9.** `.\gradlew.bat build` verde, con **≥ 148** pruebas y **0** fallos.
- [x] **10.** Actualizar `CONTRATO-MCP.md` y el plan maestro (§3, §6, §7, §9, cabecera). **(T-05)**
- [x] **11.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [x] **12.** **Generar `fase-07.md` y `PROMPT-FASE-07.md`.**
- [x] **13.** Commit:
  `refactor(error_handling): traducir los fallos de azure devops a excepciones de dominio`

---

## 4. Resultado

> Cerrada el **2026-08-30**. Build 🟢 **VERDE**: **184 pruebas, 0 fallos**.

| Métrica                                                     |                           Antes |                                                                    Después |
|-------------------------------------------------------------|--------------------------------:|---------------------------------------------------------------------------:|
| Excepciones de dominio                                      |                           **0** |     ✅ **3** *(+1 base abstracta)*, en `domain/model/.../model/exception/` |
| Errores técnicos que llegan crudos al cliente MCP           |                       **todos** |                                      ✅ **0** *(7 de 7 puntos traducidos)* |
| Traductores de errores en el adaptador                      |                           **0** |                          ✅ **1**, compartido por los 3 · **0 duplicados** |
| Cortacircuitos declarados **sin** configuración             |                           **3** |                                  ✅ **0** *(3 de 3 con umbrales elegidos)* |
| Cortacircuitos declarados en el YAML **que no usa nadie**   | **2** *(`testGet`, `testPost`)* |                                                                   ✅ **0** |
| Timeouts por operación                                      | **0** *(1 de Netty compartido)* |                             ✅ **4** *(10 s · **30 s lote** · 10 s · 5 s)* |
| Versiones de API hardcodeadas                               |            **7** *(en 1 sitio)* | 🔸 **2** *(las de B-10, en la ruta)* · **2 en `@ConfigurationProperties`** |
| URLs, cuerpos, parámetros de consulta o cabeceras alterados |                             n/a |                                    ✅ **0** *(diff literal contra `HEAD`)* |
| Pruebas totales                                             |                         **148** |                                                             ✅ **184** ▲36 |
| Pruebas heredadas **modificadas**                           |                             n/a |   **1 clase, 2 aserciones** *(las de D-13, cambio anunciado y autorizado)* |
| Cobertura `rest-consumer`                                   |                      **88,4 %** |                                                         ✅ **92,3 %** ▲3,9 |
| Cobertura `mcp-server`                                      |                      **80,7 %** |                                                         ✅ **83,3 %** ▲2,6 |
| Cobertura `domain/model`                                    |                      **94,7 %** |                                                         ✅ **95,8 %** ▲1,1 |
| Cobertura `domain/usecase`                                  |                      **97,8 %** |                                                               **97,8 %** = |
| Cobertura `app-service`                                     |                      **58,2 %** |                                                               **58,2 %** = |
| ArchUnit `Rule_2.2` (violaciones reales)                    |                           **0** |                         ✅ **0** *(verificado en el log; D-25 sigue viva)* |

**Decisión DP-06:** ✅ resuelta el 2026-08-30, en el primer paso.

| Sub-pregunta                  | Respuesta                                                                                                                                                                                                                                                             |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **§0.1(a) Forma**             | **Código estable + mensaje neutro**: `CODIGO: mensaje en español`. El código existe para que el cliente ramifique sin hacer `catch` por texto                                                                                                                         |
| **§0.1(b) Cuerpo original**   | **Se oculta al cliente y se registra íntegro en `ERROR`** en el log del MCP. Cumple §1 del plan maestro sin dejar sin diagnóstico a quien opera                                                                                                                       |
| **§0.1(c) Granularidad**      | **4 excepciones**: `AzureDevOpsException` (base) + `WorkItemNotFoundException` (404) + `AzureDevOpsUnauthorizedException` (401/403) + `AzureDevOpsUnavailableException` (5xx, otros 4xx, timeout, circuito abierto, red)                                              |
| **§0.1(d) Romper o vaciar**   | **La tool falla con excepción de dominio, PERO el repliegue de rutas se respeta intacto.** No contradice DP-04: donde hoy sale un tablero vacío, sigue saliendo un tablero vacío                                                                                      |
| **§0.2 Cortacircuitos**       | **Los mismos valores del scaffold aplicados a las tres instancias reales**: `failureRateThreshold: 50`, `slidingWindowSize: 10`, `waitDurationInOpenState: 10s`. No introduce criterio nuevo: lo que cambia es que por fin se aplica a los cortacircuitos que existen |
| **§0.2 Timeouts (D-24)**      | consulta **10 s** · **lote 30 s** · comando **10 s** · ámbito de equipo **5 s**                                                                                                                                                                                       |
| **§0.2 `testGet`/`testPost`** | **Retirados.** Ningún `@CircuitBreaker` los nombraba                                                                                                                                                                                                                  |

**Sub-decisiones:**

| ID       | Respuesta                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-08** | **En ambas capas, con responsabilidades distintas.** `AzureDevOpsErrorTranslator` (rest-consumer) sabe qué es un 404, un timeout y un cortacircuito abierto, y traduce **técnico → dominio**. `McpErrorTranslator` (mcp-server) sabe qué forma tiene un error en MCP, y traduce **dominio → mensaje con código estable**. Concentrarlo todo en el adaptador le habría exigido conocer el protocolo MCP —dependencia que la arquitectura prohíbe—; concentrarlo todo en el entry-point habría dejado al adaptador incumpliendo `spring-rules.md`. **Ninguna duplica a la otra** |
| **B-09** | **`@ConfigurationProperties`**, no objeto de valor de dominio. DP-04 declaró dominio las *reglas* de Azure DevOps, pero una versión de API no es una regla de negocio: es un detalle del proveedor que cambia por motivos ajenos al negocio y que un operador debe poder mover sin recompilar. Vive en `AzureDevOpsAdapterProperties`, **con los mismos dos literales por defecto**: un YAML que no los mencione produce las llamadas de siempre                                                                                                                               |
| **B-10** | **NO se parametrizan.** Las dos consultas de ámbito de equipo conservan `api-version=7.0` **literal en la ruta**, porque cambiarlas habría alterado dos llamadas HTTP y la regla innegociable nº 2 de esta fase es que no cambia ninguna                                                                                                                                                                                                                                                                                                                                       |

**Cambios en el contrato MCP público:** **solo la forma del error**, tal y como DP-06 autorizó
expresamente. Documentado en `CONTRATO-MCP.md` **§6** *(sección nueva)*. **Ni un nombre de tool, ni
de parámetro, ni de campo, ni la forma de un resultado de éxito ha cambiado.**

**Bloqueos encontrados:** ninguno. DP-06 se respondió en el paso 0.

**Fase siguiente generada:** ☑ `docs/fases/fase-07.md` · ☑ `docs/fases/PROMPT-FASE-07.md`

### 4.1 Qué se construyó

| Capa               | Antes                                                      | Después                                                                                                                                                         |
|--------------------|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/model`     | 2 paquetes, **0 excepciones**                              | **3 paquetes** (`workitem/`, `team/`, **`exception/`**) · **4 tipos de excepción**, sin Spring, sin HTTP, sin Jackson                                           |
| `rest-consumer`    | 3 adaptadores · **0 traductores** · versiones en el código | **`AzureDevOpsErrorTranslator`** (144 líneas, **uno solo**) · **`AzureDevOpsAdapterProperties`** (versiones + timeouts) · `ApiVersions` reducido a la condición |
| `mcp-server`       | **0 tratamiento de errores**                               | **`McpErrorTranslator`** (78) + **`McpToolExecutionException`** en `mcp/error/`                                                                                 |
| `application.yaml` | 2 cortacircuitos fantasma · 1 timeout compartido           | **3 cortacircuitos reales configurados** · **4 timeouts por operación** · **2 versiones configurables**                                                         |

Los siete puntos por los que escapaba un error crudo —3 en `WorkItemQueryAdapter`, 2 en
`WorkItemCommandAdapter`, 2 en `TeamScopeAdapter`— se cierran con **una sola línea cada uno**
(`.onErrorMap(AzureDevOpsErrorTranslator.forOperation(...))`), apuntando **todos al mismo
traductor**. Es el mismo criterio con el que la Fase 05 repartió los cinco mappers: **cero lógica
duplicada**.

De paso desaparece el `doOnError` suelto de `queryByWiql` —el **único** tratamiento de errores que
existía en el repositorio, que **registraba el cuerpo y no traducía nada**, y solo en uno de los
siete puntos—. Ahora los siete registran el cuerpo de forma uniforme.

### 4.2 Desviaciones

| #     | Desviación                                                                   | Justificación                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|-------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1** | **2 aserciones de `TeamScopeAdapterTest` cambiadas de tipo esperado**        | **Es el único cambio de aserción que la fase se ha permitido, y estaba anunciado por escrito desde la Fase 01** dentro del propio javadoc de la clase: «*la FASE 06 introducirá excepciones de dominio y estas aserciones deberán cambiar de tipo esperado; cuando eso ocurra será un cambio DELIBERADO y visible, que es justo lo que estas pruebas existen para garantizar*». DP-06 lo autorizó. Se añadió además un tercer escenario (404) que demuestra que la traducción **distingue** los tres casos en vez de fundirlos |
| **2** | **Los 3 adaptadores estrenan un segundo constructor de conveniencia**        | Enchufar los timeouts y las versiones configurables cambia la firma del constructor, lo que habría dejado **3 clases de prueba heredadas sin compilar**. En vez de pedir otra autorización para tocarlas, se añadió un constructor con los valores por defecto: **las 3 clases de prueba de la Fase 05 no se han tocado ni una línea**. El constructor de Spring está marcado con `@Autowired`, así que no hay ambigüedad de beans                                                                                             |
| **3** | **`AzureDevOpsTools` pasa de 157 a 173 líneas**                              | Son 6 líneas de `.onErrorMap(...)` más javadoc. La clase ya estaba por encima del objetivo de ≤ 120 desde la Fase 04 —**desviación heredada y documentada**—, y bajar de ahí exige partirla en dos beans de herramientas, decisión que sigue sin tomarse por cuenta propia. **0 reglas de negocio**, que es lo que sí exige `spring-rules.md`                                                                                                                                                                                  |
| **4** | **`WorkItemQueryAdapter` pasa de 115 a 143 líneas** *(el mayor de los tres)* | Excede el objetivo de ≤ 120 de la Fase 05, pero **28 de las 28 líneas nuevas son javadoc y 6 son `.timeout(...)` / `.onErrorMap(...)`**: el cuerpo ejecutable no crece en complejidad. Se anota; partirlo más sería deshacer el reparto de DP-05                                                                                                                                                                                                                                                                               |
| **5** | **D-25 sigue viva**                                                          | Los seis `issues.json` siguen saliendo `{"issues":[],"rules":[]}`. Coincide con la realidad (0 violaciones reales, **verificado en el log**), pero el informe sigue sin ser prueba de nada. Fuera de alcance: **Fase 08**                                                                                                                                                                                                                                                                                                      |
| **6** | **El build falló una vez por un artefacto de Gradle**, no por el código      | `:app-service:test` reventó con `NoSuchFileException: ...in-progress-results-generic.bin`, un resto corrupto de una ejecución anterior combinado con la caché de configuración. Se resolvió borrando `build/test-results` y ejecutando con `--no-configuration-cache`. **Ni un solo fallo de prueba ni de compilación** en ninguna de las ejecuciones                                                                                                                                                                          |


