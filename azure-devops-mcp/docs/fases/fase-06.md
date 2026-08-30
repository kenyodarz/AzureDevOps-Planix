# FASE 06 — Errores, resiliencia y contrato de fallos

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** ⚪ PENDIENTE
> **Deudas que ataca:** D-06, D-13, D-18, D-24 · **Decisión bloqueante:** ⛔ **DP-06 (ABIERTA)**
> **Riesgo:** 🟠 Medio · **Depende de:** Fases 01 a 05 (cerradas) · **Habilita:** Fases 07 y 08
> **Regla de oro de esta fase:** **esta fase SÍ cambia comportamiento observable, y por eso lo
> decide el propietario antes de escribirlo.** Es la primera del plan que no puede prometer «cero
> cambios»: traducir un error **es** cambiar lo que ve el cliente.

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

| Pregunta | Por qué no se puede asumir |
|----------|----------------------------|
| **(a)** ¿Qué **forma** tiene el error que llega al cliente? ¿Mensaje libre, código estable, o ambos? | El agente y el BFF son clientes reales. Si alguno hace `catch` por tipo o por texto, cambiarlo lo rompe en silencio |
| **(b)** ¿Se **propaga el cuerpo original** de Azure DevOps, se resume, o se oculta? | Propagarlo filtra nomenclatura de Azure DevOps aguas arriba, justo lo que §1 del plan maestro prohíbe. Ocultarlo puede dejar sin diagnóstico a quien opera |
| **(c)** ¿Se distingue **404 de 401 de 5xx**? ¿Cuántas excepciones de dominio hay? | El plan fija «**≥ 3**» como objetivo, pero los nombres y la granularidad son contrato |
| **(d)** ¿Un fallo de Azure DevOps debe **romper la tool** o devolver un resultado vacío? | **Esto ya está medio decidido y no se puede contradecir:** DP-04 conservó el repliegue de rutas precisamente para no convertir un tablero vacío en un error. Hay que declarar si esa política vale también aquí |

### 0.2 La segunda pregunta: los valores de resiliencia

**D-06 lleva vivo desde el baseline y la Fase 05 lo dejó listo, no resuelto.** Los cortacircuitos
son hoy **tres** —`workItemQuery`, `workItemCommand`, `teamScope`— y **ninguno está declarado en
`application.yaml`**, que sigue declarando `testGet` y `testPost`, dos nombres del scaffold que **no
usa nadie**. Los tres corren con la configuración **por defecto** de Resilience4j sin que nadie la
haya elegido: no es un fallo visible, es un parámetro operativo fantasma.

Hacen falta **cifras, no criterio del refactor**:

| Parámetro | Para qué |
|-----------|----------|
| `failureRateThreshold`, `slidingWindowSize`, `waitDurationInOpenState` por instancia | Abrir el circuito ni antes ni después de lo que el negocio tolera |
| **Timeout por operación** (D-24) | Hoy solo hay timeouts de Netty de **5 s compartidos** por todas las llamadas, **incluida la de lote**, que es la que más tarda |
| ¿Se **elimina** `testGet` / `testPost` del YAML? | Son residuo del scaffold, pero borrarlos es tocar configuración de entornos |

> ⚠️ **Aviso heredado de la Fase 05.** Los tres nombres de instancia son **definitivos**: se
> renombraron en la Fase 05 (B-05) precisamente para que esta fase configurara los correctos. **No
> los vuelvas a cambiar.**

### 0.3 Sub-decisiones

| ID | Cuestión |
|----|----------|
| **B-08** | ¿Dónde se traduce el error: en **el adaptador** (`rest-consumer`), en **el entry-point** (`McpErrorTranslator`), o en ambos con responsabilidades distintas? §4 del plan maestro dibuja las dos piezas; conviene ratificar el reparto antes de escribirlas |
| **B-09** | D-18: ¿las versiones de API pasan a **`@ConfigurationProperties`** o a un **objeto de valor de dominio** (`ApiVersion`)? DP-04 sentó el precedente de que las reglas de Azure DevOps son dominio, pero una versión de API se parece más a configuración. Hoy están centralizadas en `consumer/ApiVersions` (Fase 05, B-07), **con los mismos literales de siempre** |
| **B-10** | Las **dos** consultas de ámbito de equipo llevan `api-version=7.0` **escrito literalmente en la ruta**, sin parámetro que lo sobreescriba. ¿Se parametrizan —lo que **cambia una llamada HTTP**— o se dejan? |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-06 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 07**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 05 (2026-08-30), no estimado:

| Dato | Valor |
|------|-------|
| Build | 🟢 **VERDE** — **148 pruebas, 0 fallos** |
| Cobertura `domain/model` | **94,7 %** |
| Cobertura `domain/usecase` | **97,8 %** |
| Cobertura `mcp-server` | **80,7 %** |
| Cobertura `rest-consumer` | **88,4 %** |
| Cobertura `app-service` | **58,2 %** |
| Adaptadores de Azure DevOps | **3**: `WorkItemQueryAdapter` **115** · `WorkItemCommandAdapter` **80** · `TeamScopeAdapter` **78** *(todos ≤ 120)* |
| Puertos de dominio | **3** (`WorkItemQueryPort`, `WorkItemCommandPort`, `TeamScopePort`) + 1 de observabilidad · **0 huérfanos** |
| Paquetes de `domain/model` | **2** (`workitem/`, `team/`) |
| Líneas de `AzureDevOpsTools` | **157** *(sin ninguna regla de negocio)* |
| Cortacircuitos | **3** (`workItemQuery`, `workItemCommand`, `teamScope`), **ninguno declarado en el YAML** |
| Excepciones de dominio | **0** |
| Sentencia WIQL | **congelada carácter a carácter**, 8 ramas, en `domain/usecase` |
| Cuerpo HTTP de salida | **congelado carácter a carácter**, 4 cuerpos |
| ArchUnit `Rule_2.2` | ✅ **0 violaciones reales** *(verificado en el log; D-25 sigue viva)* |

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
  convierta en un error para el cliente, salvo que DP-06 §0.1(d) diga lo contrario.

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
- **No toca la seguridad** (Fase 02), ni la frontera de DTOs (Fase 03), ni el flujo compuesto
  (Fase 04), ni el reparto de adaptadores (Fase 05).
- **No arregla D-25** (informe de ArchUnit vacío), ni D-05, D-19, D-20, D-23, D-27 → **Fase 08**.
- **No cambia el contrato MCP público** —nombres de tool, de parámetro, de campo— salvo en lo que
  DP-06 declare expresamente sobre la **forma del error**.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-06**.

### T-01 · Excepciones de dominio (D-13)

**Ficheros:** `domain/model/.../model/exception/` *(nuevo)*

Crear las excepciones que decida **DP-06 §0.1(c)**, extendiendo de una base de dominio. **Sin
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

Según **B-09** y **B-10**. Punto de partida: `consumer/ApiVersions`, ya centralizado.
**Si B-10 dice que no se parametrizan, las dos URLs de equipo se quedan como están.**

### T-05 · Actualizar documentación y generar la Fase 07

1. `CONTRATO-MCP.md`: **sección nueva sobre el contrato de errores.** Es contrato público desde el
   momento en que se traduce el primero.
2. Plan maestro: §3 (D-06, D-13, D-18, D-24), §6 (métricas), §7 (DP-06), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-07.md`** y **`PROMPT-FASE-07.md`**.
5. Commit: `refactor(error_handling): traducir los fallos de azure devops a excepciones de dominio`

---

## 3. Orden de Ejecución

- [ ] **0.** ⛔ **Plantear DP-06 (y B-08, B-09, B-10) al propietario y esperar respuesta.**
- [ ] **1.** Releer `CONTRATO-MCP.md` §2 y §3: qué es contrato público y qué no.
- [ ] **2.** Inventariar cada punto donde hoy escapa un error técnico, y las 2 pruebas que lo congelan.
- [ ] **3.** Crear las excepciones de dominio. **(T-01)**
- [ ] **4.** Crear el traductor único y enchufarlo en los **3** adaptadores. **(T-02)**
- [ ] **5.** `.\gradlew.bat build` verde: **ni una URL ni un cuerpo alterados**. **(T-02)**
- [ ] **6.** Declarar los **3** cortacircuitos y los timeouts por operación. **(T-03)**
- [ ] **7.** Aplicar B-09 y B-10 sobre las versiones de API. **(T-04)**
- [ ] **8.** Comprobar en el **log** del `ArchitectureTest` que sigue a **0** violaciones.
- [ ] **9.** `.\gradlew.bat build` verde, con **≥ 148** pruebas y **0** fallos.
- [ ] **10.** Actualizar `CONTRATO-MCP.md` y el plan maestro (§3, §6, §7, §9, cabecera). **(T-05)**
- [ ] **11.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [ ] **12.** **Generar `fase-07.md` y `PROMPT-FASE-07.md`.**
- [ ] **13.** Commit: `refactor(error_handling): traducir los fallos de azure devops a excepciones de dominio`

---

## 4. Resultado

> Rellenar **al cerrar la fase**, con lo alcanzado de verdad.

| Métrica | Antes | Después |
|---------|------:|--------:|
| Excepciones de dominio | **0** | — |
| Errores técnicos que llegan crudos al cliente MCP | **todos** | — |
| Cortacircuitos declarados **sin** configuración | **3** | — |
| Timeouts por operación | **0** *(1 de Netty compartido)* | — |
| Versiones de API hardcodeadas | **7** *(en 1 sitio)* | — |
| URLs o cuerpos HTTP alterados | n/a | — |
| Pruebas totales | **148** | — |
| Pruebas heredadas **modificadas** | n/a | — |
| Cobertura `rest-consumer` | **88,4 %** | — |

**Decisión DP-06:** *(pendiente)*
**Cambios en el contrato MCP público:** *(pendiente — solo la forma del error, si DP-06 lo dice)*
**Bloqueos encontrados:** *(pendiente)*
**Fase siguiente generada:** ☐ `docs/fases/fase-07.md` · ☐ `docs/fases/PROMPT-FASE-07.md`

