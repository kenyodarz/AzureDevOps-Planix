# FASE 07 — Dominio rico y cobertura ≥ 90 %

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🔵 **ACTIVA**
> **Deudas que ataca:** D-16, D-21 *(residual)* · **Decisión bloqueante:** ⚠️ **DP-07 (a plantear)**
> **Riesgo:** 🟢 Bajo · **Depende de:** Fases 01 a 06 (cerradas) · **Habilita:** Fase 08
> **Regla de oro de esta fase:** **el `@Setter` no se retira porque quede feo, se retira porque un
> modelo que cualquiera puede mutar a mitad de un flujo reactivo no tiene invariantes.** Y el
> retorno de cinco de esas clases **se serializa hacia el cliente MCP**, así que tocarlas sin
> cuidado **sí cambia el contrato público**.

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

| Cambio | ¿Rompe el contrato? |
|--------|---------------------|
| Quitar `@Setter` de `WorkItem` | ❌ No: Jackson serializa por *getters* |
| Convertir `WorkItem` en `record` | ⚠️ **Depende**: cambian los nombres de accesor (`getId()` → `id()`), y con ellos **los nombres de campo del JSON** |
| Renombrar un campo de `WiqlResult` | 🔴 **Sí, y en silencio** |

**`WiqlResult` es el retorno de `listWorkItemsByTeamAndSprint`, la tool más usada del sistema.**

### 0.2 Las preguntas

| ID | Cuestión | Por qué no se puede asumir |
|----|----------|----------------------------|
| **(a)** | ¿Se cierra también la **frontera de salida hacia el cliente MCP** —DTOs de respuesta + mappers, como hizo la Fase 03 en las otras dos— o el dominio sigue serializándose tal cual? | Cerrarla es lo coherente con DP-03 y lo que deja el dominio verdaderamente libre. Pero son **5 DTOs y 5 mappers nuevos** y toca el contrato de las 6 tools. No cerrarla obliga a que el dominio siga siendo **serializable**, lo que limita cuánto puede enriquecerse |
| **(b)** | ¿Las 21 clases pasan a **`record`** o se quedan como clases con `@Getter`/`@Builder` **sin `@Setter`**? | Un `record` cambia los nombres de accesor. Si (a) dice que el dominio sigue serializándose, **eso cambia el JSON**. Si (a) cierra la frontera, es gratis |
| **(c)** | ¿Qué **invariantes** debe imponer cada agregado? `spring-rules.md` prohíbe el modelo anémico, pero **cuáles son las reglas** es negocio, no refactor | ⚠️ **Hay precedente explícito en contra de endurecer a ciegas:** el javadoc de `TeamScope` dice que **solo se rechazan los nulos y no las cadenas en blanco** porque «rechazar aquí las cadenas en blanco convertiría ese tablero vacío en un error», el cambio que DP-04 descartó |
| **(d)** | ¿Se toca `WorkItem.fields`, que hoy es un `Map<String,Object>` abierto? | Es lo más anémico que hay en el repositorio, pero también lo que permite que el cliente pida cualquier campo de Azure DevOps sin cambiar el servidor |

### 0.3 Sub-decisiones

| ID | Cuestión |
|----|----------|
| **B-11** | D-21 quedó marcada como saldada en la Fase 01, pero **su enunciado en §3 del plan sigue con el texto viejo** («`RestConsumerTest` (127 líneas) no cubre…»), y esa clase **ya no existe**: se dividió en tres en la Fase 05. ¿Se **reescribe el enunciado** de D-21 o se **tacha** como saldada de hecho? |
| **B-12** | `AzureDevOpsTools` lleva **173 líneas** frente al objetivo de ≤ 120, y `WorkItemQueryAdapter` **143** frente a ≤ 120. Las dos son deuda **heredada y documentada**, no nueva. ¿Se parten (dos beans de tools, dos adaptadores de consulta) o se **acepta formalmente** que el objetivo de líneas era orientativo y lo que importa es «0 reglas de negocio»? |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-07 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 08**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 06 (2026-08-30), no estimado:

| Dato | Valor |
|------|-------|
| Build | 🟢 **VERDE** — **184 pruebas, 0 fallos** |
| Cobertura `domain/model` | **95,8 %** |
| Cobertura `domain/usecase` | **97,8 %** |
| Cobertura `mcp-server` | **83,3 %** |
| Cobertura `rest-consumer` | **92,3 %** |
| Cobertura `app-service` | **58,2 %** |
| Adaptadores | **3**: `WorkItemQueryAdapter` **143** · `WorkItemCommandAdapter` **106** · `TeamScopeAdapter` **112** |
| Puertos de dominio | **3** + 1 de observabilidad · **0 huérfanos** |
| Paquetes de `domain/model` | **3** (`workitem/`, `team/`, **`exception/`**) |
| Líneas de `AzureDevOpsTools` | **173** *(0 reglas de negocio)* |
| **Excepciones de dominio** | **3** *(+1 base abstracta)* — `AZDO_NOT_FOUND`, `AZDO_UNAUTHORIZED`, `AZDO_UNAVAILABLE` |
| **Errores técnicos crudos hacia el cliente** | **0** *(7 de 7 puntos traducidos, con 1 solo traductor)* |
| **Cortacircuitos** | **3**, los tres **declarados y configurados** (50 % / 10 / 10 s) |
| **Timeouts por operación** | **4** *(10 s · 30 s lote · 10 s · 5 s)*, más los de Netty intactos |
| Versiones de API | **2 en `@ConfigurationProperties`** *(mismos literales)* + **2 literales en ruta** *(B-10)* |
| Clases de `domain/model` con `@Setter` | **21** ← **el objetivo de esta fase** |
| Value Objects inmutables | **6** *(+ los 4 tipos de excepción, que nacen inmutables)* |
| Sentencia WIQL | **congelada carácter a carácter**, 8 ramas |
| Cuerpo HTTP de salida | **congelado carácter a carácter**, 4 cuerpos |
| ArchUnit `Rule_2.2` | ✅ **0 violaciones reales** *(verificado en el log; D-25 sigue viva)* |

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
- **No toca** la seguridad (Fase 02), la frontera de entrada (Fase 03), el flujo compuesto (Fase 04),
  el reparto de adaptadores (Fase 05) ni la traducción de errores (Fase 06).
- **No retira el repliegue** de DP-04, ni endurece invariantes que lo conviertan en un error.
- **No arregla D-25** (informe de ArchUnit vacío), ni D-05, D-19, D-20, D-22, D-23, D-27 → **Fase 08**.
- **No pasa ArchUnit de `warning` a `error`** → **Fase 08**.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-07**.

### T-01 · Cerrar (o no) la frontera de salida hacia el cliente MCP

Según **DP-07 §0.2(a)**. Si se cierra: DTOs de respuesta en `mcp-server/.../mcp/dto/` + mappers, con
**los mismos nombres de campo en el JSON**, congelados por una prueba de caracterización **escrita
antes** de mover nada.

### T-02 · Retirar el `@Setter` y dar invariantes (D-16)

**Ficheros:** las **21** clases de `domain/model`

1. Forma según **DP-07 §0.2(b)** (`record` vs `@Getter`/`@Builder`).
2. Invariantes según **DP-07 §0.2(c)**, y **solo las que el propietario declare**.
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

- [ ] **0.** ⚠️ **Plantear DP-07 (y B-11, B-12) al propietario y esperar respuesta.**
- [ ] **1.** Releer `CONTRATO-MCP.md` §3.4 y §6: qué se serializa hacia el cliente y qué es estable.
- [ ] **2.** Inventariar las 21 clases: cuáles se serializan hacia MCP y cuáles no.
- [ ] **3.** **Escribir la prueba de caracterización del JSON de salida ANTES de tocar nada.**
- [ ] **4.** Aplicar lo que decida DP-07 §0.2(a) sobre la frontera de salida. **(T-01)**
- [ ] **5.** Retirar `@Setter` y aplicar invariantes. **(T-02)**
- [ ] **6.** `.\gradlew.bat build` verde: **ni un nombre de campo del JSON alterado**.
- [ ] **7.** Medir cobertura y mutaciones. **(T-03)**
- [ ] **8.** Resolver B-11 y B-12. **(T-04)**
- [ ] **9.** Comprobar en el **log** del `ArchitectureTest` que sigue a **0** violaciones.
- [ ] **10.** `.\gradlew.bat build` verde, con **≥ 184** pruebas y **0** fallos.
- [ ] **11.** Actualizar `CONTRATO-MCP.md` y el plan maestro. **(T-05)**
- [ ] **12.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [ ] **13.** **Generar `fase-08.md` y `PROMPT-FASE-08.md`.**
- [ ] **14.** Commit: `refactor(domain_model): convertir el modelo en objetos de valor inmutables`

---

## 4. Resultado

> Rellenar **al cerrar la fase**, con lo alcanzado de verdad.

| Métrica | Antes | Después |
|---------|------:|--------:|
| Clases de `domain/model` con `@Setter` | **21** | — |
| Value Objects inmutables | **6** | — |
| Invariantes de negocio declaradas | **las de los 6 VOs** | — |
| Modelos de dominio serializados hacia el cliente MCP | **5** | — |
| Nombres de campo del JSON de salida alterados | n/a | — |
| Pruebas totales | **184** | — |
| Pruebas heredadas **modificadas** | n/a | — |
| Cobertura `domain/model` | **95,8 %** | — |
| Cobertura `domain/usecase` | **97,8 %** | — |
| Mutaciones eliminadas (Pitest) | **84 %** *(`rest-consumer`)* | — |

**Decisión DP-07:** *(pendiente)*
**Cambios en el contrato MCP público:** *(pendiente)*
**Bloqueos encontrados:** *(pendiente)*
**Fase siguiente generada:** ☐ `docs/fases/fase-08.md` · ☐ `docs/fases/PROMPT-FASE-08.md`

