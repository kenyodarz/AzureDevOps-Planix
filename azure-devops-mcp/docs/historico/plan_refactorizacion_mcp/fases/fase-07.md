# FASE 07 — Dominio rico y cobertura ≥ 90 %

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🟢 **COMPLETADA**
> (2026-08-30)
> **Deudas que ataca:** D-16 *(**saldada**)* · D-21 *(enunciado **reescrito**, B-11)*
> **Decisión bloqueante:** ✅ **DP-07 (RESUELTA)**
> **Riesgo:** 🟢 Bajo · **Depende de:** Fases 01 a 06 (cerradas) · **Habilita:** Fase 08
> **Regla de oro de esta fase:** **el `@Setter` no se retira porque quede feo, se retira porque un
> modelo que cualquiera puede mutar a mitad de un flujo reactivo no tiene invariantes.** Y el
> retorno de cuatro de esas clases **se serializaba hacia el cliente MCP**, así que tocarlas sin
> cuidado **sí cambiaba el contrato público**.
> ✅ **Y no cambió:** **0 nombres de campo, orden, nulos u orden de claves del JSON alterados**,
> verificado contra **literales** escritos antes de tocar una sola clase.

---

## 0. ⚠️ PASO BLOQUEANTE — DP-07

**No se escribe código antes de que el propietario responda.** No porque el objetivo sea ambiguo
—D-16 está clarísima— sino porque **el camino tiene una trampa que las seis fases anteriores
esquivaron y ésta no puede**.

### 0.1 La trampa: cinco de las 21 clases **son** el contrato de salida

`CONTRATO-MCP.md` §3.4 lo dejó escrito al cerrar la Fase 03, y sigue siendo cierto:

> *En el **retorno**, `WorkItem`, `WorkItemRelation`, `WorkItemReference`, `WiqlResult` y
> `TeamFieldValues` **siguen serializándose desde el dominio hacia el cliente MCP**. Esa mitad no
> entraba en el alcance de la Fase 03 —que ataca la frontera con Azure DevOps— y queda anotada para
> cuando el dominio se convierta en Value Objects (D-16, **Fase 07**).*

La Fase 03 cerró la frontera de **entrada** y la de **salida hacia Azure DevOps**. La frontera de
**salida hacia el cliente MCP nunca se cerró**: el dominio se serializa tal cual. Por tanto:

| Cambio                             | ¿Rompe el contrato?                                                                                                |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| Quitar `@Setter` de `WorkItem`     | ❌ No: Jackson serializa por *getters*                                                                             |
| Convertir `WorkItem` en `record`   | ⚠️ **Depende**: cambian los nombres de accesor (`getId()` → `id()`), y con ellos **los nombres de campo del JSON** |
| Renombrar un campo de `WiqlResult` | 🔴 **Sí, y en silencio**                                                                                           |

**`WiqlResult` es el retorno de `listWorkItemsByTeamAndSprint`, la tool más usada del sistema.**

### 0.2 Las preguntas

| ID      | Cuestión                                                                                                                                                                           | Por qué no se puede asumir                                                                                                                                                                                                                                                         |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **(a)** | ¿Se cierra también la **frontera de salida hacia el cliente MCP** —DTOs de respuesta + mappers, como hizo la Fase 03 en las otras dos— o el dominio sigue serializándose tal cual? | Cerrarla es lo coherente con DP-03 y lo que deja el dominio verdaderamente libre. Pero son **5 DTOs y 5 mappers nuevos** y toca el contrato de las 6 tools. No cerrarla obliga a que el dominio siga siendo **serializable**, lo que limita cuánto puede enriquecerse              |
| **(b)** | ¿Las 21 clases pasan a **`record`** o se quedan como clases con `@Getter`/`@Builder` **sin `@Setter`**?                                                                            | Un `record` cambia los nombres de accesor. Si (a) dice que el dominio sigue serializándose, **eso cambia el JSON**. Si (a) cierra la frontera, es gratis                                                                                                                           |
| **(c)** | ¿Qué **invariantes** debe imponer cada agregado? `spring-rules.md` prohíbe el modelo anémico, pero **cuáles son las reglas** es negocio, no refactor                               | ⚠️ **Hay precedente explícito en contra de endurecer a ciegas:** el javadoc de `TeamScope` dice que **solo se rechazan los nulos y no las cadenas en blanco** porque «rechazar aquí las cadenas en blanco convertiría ese tablero vacío en un error», el cambio que DP-04 descartó |
| **(d)** | ¿Se toca `WorkItem.fields`, que hoy es un `Map<String,Object>` abierto?                                                                                                            | Es lo más anémico que hay en el repositorio, pero también lo que permite que el cliente pida cualquier campo de Azure DevOps sin cambiar el servidor                                                                                                                               |

### 0.3 Sub-decisiones

| ID       | Cuestión                                                                                                                                                                                                                                                                                                                                                    |
|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-11** | D-21 quedó marcada como saldada en la Fase 01, pero **su enunciado en §3 del plan sigue con el texto viejo** («`RestConsumerTest` (127 líneas) no cubre…»), y esa clase **ya no existe**: se dividió en tres en la Fase 05. ¿Se **reescribe el enunciado** de D-21 o se **tacha** como saldada de hecho?                                                    |
| **B-12** | `AzureDevOpsTools` lleva **173 líneas** frente al objetivo de ≤ 120, y `WorkItemQueryAdapter` **143** frente a ≤ 120. Las dos son deuda **heredada y documentada**, no nueva. ¿Se parten (dos beans de tools, dos adaptadores de consulta) o se **acepta formalmente** que el objetivo de líneas era orientativo y lo que importa es «0 reglas de negocio»? |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-07 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 08**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 06 (2026-08-30), no estimado:

| Dato                                         | Valor                                                                                                 |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------|
| Build                                        | 🟢 **VERDE** — **184 pruebas, 0 fallos**                                                              |
| Cobertura `domain/model`                     | **95,8 %**                                                                                            |
| Cobertura `domain/usecase`                   | **97,8 %**                                                                                            |
| Cobertura `mcp-server`                       | **83,3 %**                                                                                            |
| Cobertura `rest-consumer`                    | **92,3 %**                                                                                            |
| Cobertura `app-service`                      | **58,2 %**                                                                                            |
| Adaptadores                                  | **3**: `WorkItemQueryAdapter` **143** · `WorkItemCommandAdapter` **106** · `TeamScopeAdapter` **112** |
| Puertos de dominio                           | **3** + 1 de observabilidad · **0 huérfanos**                                                         |
| Paquetes de `domain/model`                   | **3** (`workitem/`, `team/`, **`exception/`**)                                                        |
| Líneas de `AzureDevOpsTools`                 | **173** *(0 reglas de negocio)*                                                                       |
| **Excepciones de dominio**                   | **3** *(+1 base abstracta)* — `AZDO_NOT_FOUND`, `AZDO_UNAUTHORIZED`, `AZDO_UNAVAILABLE`               |
| **Errores técnicos crudos hacia el cliente** | **0** *(7 de 7 puntos traducidos, con 1 solo traductor)*                                              |
| **Cortacircuitos**                           | **3**, los tres **declarados y configurados** (50 % / 10 / 10 s)                                      |
| **Timeouts por operación**                   | **4** *(10 s · 30 s lote · 10 s · 5 s)*, más los de Netty intactos                                    |
| Versiones de API                             | **2 en `@ConfigurationProperties`** *(mismos literales)* + **2 literales en ruta** *(B-10)*           |
| Clases de `domain/model` con `@Setter`       | **21** ← **el objetivo de esta fase**                                                                 |
| Value Objects inmutables                     | **6** *(+ los 4 tipos de excepción, que nacen inmutables)*                                            |
| Sentencia WIQL                               | **congelada carácter a carácter**, 8 ramas                                                            |
| Cuerpo HTTP de salida                        | **congelado carácter a carácter**, 4 cuerpos                                                          |
| ArchUnit `Rule_2.2`                          | ✅ **0 violaciones reales** *(verificado en el log; D-25 sigue viva)*                                 |

### 1.2 Lo que dejaron hechas las fases previas y afecta a ésta

**De la Fase 06:**

- ✅ **D-06, D-13 y D-24 saldadas; D-18 saldada en su parte configurable.**
- 🆕 **`CONTRATO-MCP.md` §6 es contrato de errores público.** Los cuatro códigos —`AZDO_NOT_FOUND`,
  `AZDO_UNAUTHORIZED`, `AZDO_UNAVAILABLE`, `MCP_INTERNAL_ERROR`— **no se tocan**.
- 🆕 **`domain/model` tiene un tercer paquete: `exception/`.** Sus 4 tipos **ya son inmutables**: no
  entran en D-16, pero sí cuentan para la cobertura.
- 🔸 **El repliegue de DP-04 sigue vivo y ahora está fijado por prueba** también contra excepciones
  de dominio (`ResolveTeamScopeUseCaseTest`). **No lo rompas.**
- 🔸 **Los 3 adaptadores tienen dos constructores**: el de Spring (`@Autowired`, con
  `AzureDevOpsAdapterProperties`) y uno de conveniencia con valores por defecto **que existe para
  que las pruebas heredadas no se toquen**.

**De las Fases 02 a 05, intocable:**

- La postura de seguridad es **configuración** (`mcp.security.mode`, `PERMISSIVE` por defecto).
- La **frontera de DTOs** de entrada y de salida-hacia-Azure-DevOps: 1 mapper de entrada, 5 de
  salida. **Ningún modelo de dominio puede volver a exponerse como `@McpToolParam` ni ser
  serializado por el `WebClient`.**
- Los **nombres de campo del cable** (`op`, `path`, `value`, `from`, `ids`, `fields`, `expand`,
  `errorPolicy`) son intocables, con prueba por reflexión.
- Los **3 nombres de cortacircuito** son definitivos.

### 1.3 El problema que esta fase resuelve

```
HOY                                          OBJETIVO DE ESTA FASE

model  @Setter × 21, 0 invariantes           model  inmutable, con invariantes de negocio
       (cualquiera muta un WorkItem                 (un WorkItem válido no puede dejar de serlo)
        a mitad de un flujo reactivo)
       5 clases serializadas hacia MCP              ¿frontera cerrada? ← DP-07 (a)
```

`spring-rules.md` §1 es explícito: **«Clases ricas en comportamiento e invariantes de negocio.
Prohibido el modelo anémico»**, y limita Lombok a un uso «controlado». 21 clases con `@Setter` y
cero invariantes no es un uso controlado: es el modelo anémico literal.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No cambia el contrato de errores** de la Fase 06: los cuatro códigos son públicos y estables.
- **No toca** la seguridad (Fase 02), la frontera de entrada (Fase 03), el flujo compuesto (Fase
  04),
  el reparto de adaptadores (Fase 05) ni la traducción de errores (Fase 06).
- **No retira el repliegue** de DP-04, ni endurece invariantes que lo conviertan en un error.
- **No arregla D-25** (informe de ArchUnit vacío), ni D-05, D-19, D-20, D-22, D-23, D-27 → **Fase
  08**.
- **No pasa ArchUnit de `warning` a `error`** → **Fase 08**.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-07**.

### T-01 · Cerrar (o no) la frontera de salida hacia el cliente MCP

Según **DP-07 §0.2 (a)**. Si se cierra: DTOs de respuesta en `mcp-server/.../mcp/dto/` + mappers,
con **los mismos nombres de campo en el JSON**, congelados por una prueba de caracterización
**escrita
antes** de mover nada.

### T-02 · Retirar el `@Setter` y dar invariantes (D-16)

**Ficheros:** las **21** clases de `domain/model`

1. Forma según **DP-07 §0.2 (b)** (`record` vs `@Getter`/`@Builder`).
2. Invariantes según **DP-07 §0.2 (c)**, y **solo las que el propietario declare**.
3. ⚠️ **Respetar el precedente de `TeamScope`**: no convertir una cadena en blanco en un error sin
   autorización, porque eso reabre DP-04.

### T-03 · Cobertura ≥ 90 % y mutaciones

`domain/model` y `domain/usecase` **ya están** por encima del 90 %. El objetivo real aquí es que
**no bajen** al reescribir las clases, y subir las mutaciones eliminadas de Pitest hacia el ≥ 60 %
del plan.

### T-04 · Resolver B-11 y B-12

Enunciado de D-21 y política de líneas de `AzureDevOpsTools` / `WorkItemQueryAdapter`.

### T-05 · Actualizar documentación y generar la Fase 08

1. `CONTRATO-MCP.md`: §3.4 **debe dejar de decir «queda anotada para la Fase 07»**.
2. Plan maestro: §3 (D-16, D-21), §6 (métricas), §7 (DP-07), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-08.md`** y **`PROMPT-FASE-08.md`**.
5. Commit: `refactor(domain_model): convertir el modelo en objetos de valor inmutables`

---

## 3. Orden de Ejecución

- [x] **0.** ⚠️ **Plantear DP-07 (y B-11, B-12) al propietario y esperar respuesta.**
- [x] **1.** Releer `CONTRATO-MCP.md` §3.4 y §6: qué se serializa hacia el cliente y qué es estable.
- [x] **2.** Inventariar las 21 clases: cuáles se serializan hacia MCP y cuáles no.
- [x] **3.** **Escribir la prueba de caracterización del JSON de salida ANTES de tocar nada.**
- [x] **4.** Aplicar lo que decida DP-07 §0.2 (a) sobre la frontera de salida. **(T-01)**
- [x] **5.** Retirar `@Setter` y aplicar invariantes. **(T-02)**
- [x] **6.** `.\gradlew.bat build` verde: **ni un nombre de campo del JSON alterado**.
- [x] **7.** Medir cobertura y mutaciones. **(T-03)**
- [x] **8.** Resolver B-11 y B-12. **(T-04)**
- [x] **9.** Comprobar en el **log** del `ArchitectureTest` que sigue a **0** violaciones.
- [x] **10.** `.\gradlew.bat build` verde, con **≥ 184** pruebas y **0** fallos.
- [x] **11.** Actualizar `CONTRATO-MCP.md` y el plan maestro. **(T-05)**
- [x] **12.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [x] **13.** **Generar `fase-08.md` y `PROMPT-FASE-08.md`.**
- [x] **14.** Commit: `refactor(domain_model): convertir el modelo en objetos de valor inmutables`

---

## 4. Resultado

> Cerrada el **2026-08-30**. Build 🟢 **VERDE**: **205 pruebas, 0 fallos**.

| Métrica                                                     |                        Antes |                                                                                                                         Después |
|-------------------------------------------------------------|-----------------------------:|--------------------------------------------------------------------------------------------------------------------------------:|
| Clases de `domain/model` con `@Setter` o `@Data`            |   **9** *(no 21 — ver §4.3)* |                                                                                                                        ✅ **0** |
| Value Objects inmutables                                    |                        **6** |                                                                                                                    ✅ **15** ▲9 |
| Invariantes de negocio declaradas                           |         **las de los 6 VOs** |                                                                                 ✅ **≥ 1 por agregado**, todas de DP-07 §0.2(c) |
| Modelos de dominio serializados hacia el cliente MCP        |    **4** *(no 5 — ver §4.3)* |                                                                                                                        ✅ **0** |
| DTOs de respuesta + mappers en la frontera de salida MCP    |                        **0** |                                                                                                        ✅ **4 DTOs + 1 mapper** |
| Nombres de campo del JSON de salida alterados               |                          n/a |                                                                                 ✅ **0** *(congelados por prueba de literales)* |
| Orden de campos / emisión de nulos alterados                |                          n/a |                                                                                                                        ✅ **0** |
| URLs, cuerpos, parámetros de consulta o cabeceras alterados |                          n/a |                                                                                                                        ✅ **0** |
| Contrato de errores de la Fase 06 alterado                  |                          n/a |                                                                                             ✅ **0** *(los 4 códigos intactos)* |
| Pruebas totales                                             |                      **184** |                                                                                                                  ✅ **205** ▲21 |
| Pruebas heredadas **modificadas**                           |                          n/a | 🔸 **7 clases** *(6 por renombrado de accesor + 1 por el cierre de la frontera)* — **autorizadas**, sin alterar ni un escenario |
| Cobertura `domain/model`                                    |                   **95,8 %** |                                                                                                              ✅ **96,2 %** ▲0,4 |
| Cobertura `domain/usecase`                                  |                   **97,8 %** |                                                                                                                 ✅ **97,8 %** = |
| Cobertura `mcp-server`                                      |                   **83,3 %** |                                                                                                              ✅ **84,3 %** ▲1,0 |
| Cobertura `rest-consumer` / `app-service`                   |          **92,3 % / 58,2 %** |                                                                                                           **92,3 % / 58,2 %** = |
| Mutaciones eliminadas (Pitest)                              | **84 %** *(`rest-consumer`)* |                                                                        ✅ **40/41 = 98 %** `model` · **38/39 = 97 %** `usecase` |
| ArchUnit `Rule_2.2` (violaciones reales)                    |                        **0** |                                                                              ✅ **0** *(verificado en el log; D-25 sigue viva)* |
| Líneas de `AzureDevOpsTools`                                |                      **173** |                                                                                       🔸 **188** *(B-12: aceptado formalmente)* |

**Decisión DP-07:** ✅ resuelta el 2026-08-30, en el primer paso, **antes de escribir una línea**.

| Sub-pregunta                   | Respuesta                                                                                                                                                                                                                                                                                                                     |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **§0.2(a) Frontera de salida** | **SÍ se cierra** *(opción a1)*. `WorkItemResponse`, `WorkItemRelationResponse`, `WorkItemReferenceResponse` y `WiqlResultResponse` + `McpResponseMapper`, en `mcp-server/.../mcp/dto/`, simétricos al `McpToolDtoMapper` de entrada                                                                                           |
| **§0.2(b) Forma**              | **`record` para todas.** Solo era seguro *después* de (a)                                                                                                                                                                                                                                                                     |
| **§0.2(c) Invariantes**        | **Las mínimas que no cambian nada observable:** copia defensiva e inmodificable de todas las colecciones **preservando orden y nulos**; `WiqlQuery` con sentencia no vacía; `JsonPatchOperation` con `op` y `path`. **Rechazadas expresamente** `id` no nulo, `ids` no vacío y la normalización de nulos a colecciones vacías |
| **§0.2(d) `WorkItem.fields`**  | **No se toca.** Sigue siendo `Map<String,Object>` abierto; lo único que gana es que **ya no se puede mutar**                                                                                                                                                                                                                  |
| **B-11**                       | **Enunciado de D-21 reescrito**, no tachado: nombraba `RestConsumerTest`, clase que ya no existe                                                                                                                                                                                                                              |
| **B-12**                       | **Aceptado formalmente.** Lo que importa es «0 reglas de negocio», no el recuento de líneas                                                                                                                                                                                                                                   |

**Cambios en el contrato MCP público:** **ninguno.** Ni un nombre de tool, ni de parámetro, ni de
campo, ni el orden, ni la emisión de nulos, ni la forma de un resultado. A diferencia de la Fase 06
—que sí cambió la forma del error, con autorización—, **esta fase no cambia nada observable**, y eso
es precisamente lo que la prueba de caracterización demuestra.

**Bloqueos encontrados:** ninguno. DP-07 se respondió en el paso 0 y la cuestión colateral de las
pruebas heredadas se arbitró antes de tocar código.

**Fase siguiente generada:** ☑ `docs/fases/fase-08.md` · ☑ `docs/fases/PROMPT-FASE-08.md`

### 4.1 Qué se construyó

| Capa           | Antes                                                               | Después                                                                                                               |
|----------------|---------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `domain/model` | 3 paquetes · **9 clases mutables** · 0 invariantes fuera de los VOs | **4 paquetes** (+`common/`) · **0 clases mutables** · **15 objetos de valor** · invariantes **declaradas y probadas** |
| `mcp-server`   | frontera de entrada cerrada, **salida abierta**                     | **frontera de salida cerrada**: `mcp/dto/` estrena **4 DTOs de respuesta** y **`McpResponseMapper`**                  |
| Pruebas        | 184                                                                 | **205**: `McpResponsePayloadCharacterizationTest` (5) y `DomainInvariantsTest` (16)                                   |

**La secuencia importó tanto como el resultado.** El paso 3 del checklist —escribir la
caracterización **antes** de tocar nada— no era burocracia: los cuatro literales JSON se redactaron
contra el modelo de dominio **todavía mutable**, y siguen intactos. Si el orden hubiera sido el
inverso, la prueba habría congelado lo que produjera el código nuevo, y habría certificado como
correcto cualquier cambio silencioso.

### 4.2 Por qué (a) tenía que ir antes que (b)

```
SI SE HUBIERA HECHO AL REVÉS                 LO QUE SE HIZO

WiqlResult → record                          1. frontera cerrada (DTOs + mapper)
  getQueryType() → queryType()               2. WiqlResult → record
  JSON: "queryType" → ¿?                        el JSON lo produce el DTO, no el dominio
  ↳ el retorno de la tool más usada,          ↳ 0 nombres de campo alterados,
    roto en silencio                            verificado contra literales
```

### 4.3 Dos cifras del plan que la medición corrigió

| Cifra heredada                                                             | Realidad medida                                                                                                                                                                                                                                                                                                |
|----------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| «`@Setter` en las **21** clases de `domain/model`» (D-16)                  | **21 era el número de tipos del módulo**, no el de clases mutables. Las mutables eran **9** —7 con `@Setter` y **2 con `@Data`**, que es peor: genera `equals`/`hashCode` sobre campos mutables—. Además **ningún setter se invocaba en todo el repositorio**: los cinco mappers ya construían con `builder()` |
| «**5** clases se serializan hacia el cliente MCP» (`CONTRATO-MCP.md` §3.4) | Eran **4**. `TeamFieldValues` **nunca** se devolvió a ninguna tool: es interna a `ResolveTeamScopeUseCase`                                                                                                                                                                                                     |

Ninguna de las dos cambia lo que había que hacer, pero las dos cambian **el tamaño declarado del
problema**, y por eso quedan escritas: un plan que arrastra cifras sin verificar acaba justificando
decisiones con datos que nadie midió.

### 4.4 Hallazgo: la red de seguridad de la Fase 03 es autorreferencial

`OutboundPayloadCharacterizationTest` compara el cuerpo emitido contra
`MAPPER.writeValueAsString(modeloDeDominio)`, es decir, **usa el propio dominio como oráculo**.
Sirve
para demostrar que un mapper *reproduce* al dominio, pero **no detecta un cambio que afecte a los
dos
a la vez** — que es exactamente el riesgo de esta fase. Por eso la caracterización nueva compara
contra **cadenas literales**: un literal no cambia solo. Se anota como observación, **no se tocó esa
clase**.

### 4.5 Desviaciones

| #     | Desviación                                                                                                                                                                                                                                                                       | Justificación                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|-------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1** | **7 clases de prueba heredadas adaptadas** *(`WiqlCharacterizationTest`, `ListWorkItemsByTeamAndSprintUseCaseTest`, `DelegatingUseCasesTest`, `McpToolDtoMapperTest`, `WorkItemQueryAdapterTest`, `WorkItemCommandAdapterTest`, `TeamScopeAdapterTest`, `AzureDevOpsToolsTest`)* | **Se midió primero y se preguntó antes de escribir código**, conforme a la regla innegociable nº 5 y a `spring-rules.md` §6. El propietario autorizó el **renombrado mecánico de accesor**. **Ni una aserción, ni un literal WIQL, ni un escenario alterado.** En `DelegatingUseCasesTest` se añadió además un `path` a dos parches de relleno, exigido por la invariante nueva: la prueba verifica **que el caso de uso delega**, no el contenido del parche |
| **2** | **`AzureDevOpsTools` pasa de 173 a 188 líneas**                                                                                                                                                                                                                                  | 6 líneas de `.map(McpResponseMapper::...)` más javadoc. **B-12 lo aceptó formalmente**: la clase está por encima de ≤ 120 desde la Fase 04 —deuda heredada y documentada— y lo que exige `spring-rules.md` es **0 reglas de negocio**, que se mantiene. Partirla habría cambiado el wiring de las tools y tocado `McpToolsAuthorizationTest`, con riesgo sobre el contrato MCP a cambio de una cifra                                                          |
| **3** | **`domain/model` estrena un cuarto paquete: `common/`**                                                                                                                                                                                                                          | `DomainCollections` centraliza la copia defensiva que necesitan **6 de los 9 records**. La alternativa era repetir el mismo par de líneas en cada uno. Es dominio puro y no viola `Rule_2.2`                                                                                                                                                                                                                                                                  |
| **4** | **D-25 sigue viva**                                                                                                                                                                                                                                                              | Los `issues.json` siguen saliendo `{"issues":[],"rules":[]}`. Coincide con la realidad (**0 violaciones reales, verificado en el log**), pero el informe sigue sin ser prueba de nada. Fuera de alcance: **Fase 08**                                                                                                                                                                                                                                          |
