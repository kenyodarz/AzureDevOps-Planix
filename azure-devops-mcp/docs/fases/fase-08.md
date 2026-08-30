# FASE 08 — Configuración tipada, ArchUnit y cierre del plan

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🔵 **ACTIVA**
> **Deudas que ataca:** D-05, D-19, D-20, D-22 *(residual)*, D-23, D-25, D-27
> **Decisión bloqueante:** ⚠️ **DP-08 (a plantear)** — §5 no le asignaba ninguna, pero **hay
> decisiones de producto que un refactor no puede tomar**
> **Riesgo:** 🟢 Bajo *(técnicamente)* · **Depende de:** Fases 01 a 07 (cerradas) · **Habilita:** el
> cierre del plan
> **Regla de oro de esta fase:** **es la última, así que lo que no se cierre aquí se queda sin
> cerrar.** Y dos de sus deudas —**D-25** y **D-27**— no son código feo: son **mecanismos de control
> que llevan todo el plan diciendo que todo está bien sin haberlo comprobado nunca**.

---

## 0. ⚠️ PASO BLOQUEANTE — DP-08

**No se escribe código antes de que el propietario responda.** El grueso de la fase es
saneamiento y no necesita permiso, pero **tres de las siete deudas son decisiones de producto**, no
de refactor.

### 0.1 Por qué esta fase tiene decisión bloqueante si §5 no se la asignó

Exactamente por lo mismo que le ocurrió a la Fase 07: §5 tampoco le asignaba una, y **apareció**
—la frontera de salida hacia MCP—. El patrón se ha repetido en las siete fases: **cinco se
detuvieron a preguntar y las cinco veces fue correcto**. Aquí las candidatas están identificadas de
antemano.

### 0.2 Las preguntas

| ID | Cuestión | Por qué no se puede asumir |
|----|----------|----------------------------|
| **(a)** | **D-19 — `queryByWiql`: ¿se expone como tool o se retira?** Su `@McpTool` lleva **comentado** desde siempre; el método es `public`, tiene `@PreAuthorize` activo desde la Fase 02, caso de uso, adaptador y pruebas. **No forma parte del contrato público de hoy** (`CONTRATO-MCP.md` §4) | Exponerla **añade** superficie de contrato: una tool que acepta WIQL **crudo** del cliente, justo lo contrario de la misión de §1 del plan («el MCP es el dueño del *cómo*»). Retirarla **borra** código que funciona y está probado. Las dos son decisiones de producto |
| **(b)** | **D-20 — `HealthTool`: ¿se retira, se arregla o se deja?** Duplica el actuator y devuelve `"Server:  v1.0.0 - Package: co.com.bancolombia"`, con la versión **hardcodeada** y un **placeholder sin rellenar** (doble espacio) | `checkHealth` y `getServerInfo` **son contrato público** (`CONTRATO-MCP.md` §2.6) y el agente puede estar llamándolas. Cambiar la cadena es un cambio observable; retirarlas, más |
| **(c)** | **Capacidades anunciadas y no implementadas.** El servidor declara `resource: true` y `prompt: true` con **cero** `@McpResource` y **cero** `@McpPrompt` (`CONTRATO-MCP.md` §1) | Un cliente MCP puede consultar capacidades y decidir en función de ellas. Ponerlas a `false` es el arreglo honesto, **pero es un cambio observable** |
| **(d)** | **¿ArchUnit pasa a `error`?** El objetivo de §6 lo pide, y hoy hay **0 violaciones reales**, así que el cambio **no rompería el build**… mientras siga habiendo 0 | Convertirlo en `error` significa que **cualquier violación futura para el build**. Es exactamente lo que se quiere, pero es una decisión de política de equipo |

### 0.3 Sub-decisiones

| ID | Cuestión |
|----|----------|
| **B-13** | **D-05:** el `@ComponentScan(includeFilters REGEX "^.+UseCase$")` de `UseCasesConfig` convive con **siete `@Bean` manuales del mismo tipo**. La Fase 01 **midió que el escaneo es inerte** (cada caso de uso resuelve a exactamente un bean). ¿Se retira el `@ComponentScan` —dejando solo los `@Bean` explícitos— o se retiran los `@Bean` dejando el escaneo? |
| **B-14** | **D-22 residual:** `spring.devtools.add-properties` se conservó en la Fase 02 **porque el `grep` demostró que `runtimeOnly('spring-boot-devtools')` existe** en `app-service/build.gradle:13`. Retirar la dependencia es una decisión de **empaquetado**: ¿se retira devtools del artefacto o se queda? |
| **B-15** | **D-23 — `McpAuditAspect`:** llama a `joinPoint.proceed()` **antes** de resolver el contexto de seguridad, y **no audita** la rama no reactiva (solo un `warn`). Arreglarlo cambia **qué se escribe en el log de auditoría**. ¿Es auditoría de cumplimiento —y por tanto el orden importa— o es traza de diagnóstico? |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-08 no se resuelve, **la fase hace
solo la parte que no depende de ella** —D-25, D-27 y B-13, que son saneamiento interno sin
comportamiento observable— y **documenta el bloqueo del resto en su §4**.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 07 (2026-08-30), no estimado:

| Dato | Valor |
|------|-------|
| Build | 🟢 **VERDE** — **205 pruebas, 0 fallos** |
| Cobertura `domain/model` | **96,2 %** |
| Cobertura `domain/usecase` | **97,8 %** |
| Cobertura `mcp-server` | **84,3 %** |
| Cobertura `rest-consumer` | **92,3 %** |
| Cobertura `app-service` | **58,2 %** ← **el único por debajo de su objetivo (60 %)** |
| Mutaciones (Pitest) | **98 %** `domain/model` · **97 %** `domain/usecase` · **84 %** `rest-consumer` |
| Adaptadores | **3**: `WorkItemQueryAdapter` **143** · `WorkItemCommandAdapter` **106** · `TeamScopeAdapter` **112** |
| Puertos de dominio | **3** + 1 de observabilidad · **0 huérfanos** |
| Paquetes de `domain/model` | **4** (`workitem/`, `team/`, `exception/`, **`common/`**) |
| Líneas de `AzureDevOpsTools` | **188** *(**0 reglas de negocio**; B-12 lo aceptó formalmente)* |
| Clases de `domain/model` mutables | ✅ **0** *(9 → 0: `record` inmutables con copia defensiva)* |
| Value Objects inmutables | **15** *(+ los 4 tipos de excepción)* |
| **Fronteras de contrato cerradas** | ✅ **las 3**: entrada MCP · salida hacia Azure DevOps · **salida hacia MCP** *(Fase 07)* |
| Mappers | **1** de entrada · **5** hacia Azure DevOps · **1** hacia MCP |
| Excepciones de dominio | **3** *(+1 base)* — `AZDO_NOT_FOUND`, `AZDO_UNAUTHORIZED`, `AZDO_UNAVAILABLE` |
| Errores técnicos crudos hacia el cliente | **0** |
| Cortacircuitos | **3**, los tres configurados (50 % / 10 / 10 s) |
| Timeouts por operación | **4** (10 s · **30 s lote** · 10 s · 5 s) |
| Sentencia WIQL | **congelada carácter a carácter**, 8 ramas |
| Cuerpo HTTP de salida | **congelado carácter a carácter**, 4 cuerpos |
| **JSON de respuesta hacia MCP** | **congelado contra literales**, 4 formas |
| ArchUnit `Rule_2.2` | ✅ **0 violaciones reales** *(verificado **en el log**; D-25 sigue viva)* |
| ArchUnit ejecutado como | **`warning`** ← **objetivo de esta fase (d)** |

### 1.2 Lo que dejaron hechas las fases previas y afecta a ésta

**De la Fase 07:**

- ✅ **D-16 saldada.** `domain/model` es **inmutable**: 9 clases mutables → 0. **No lo deshagas.**
- ✅ **D-21 con enunciado reescrito** (B-11). Ya no menciona `RestConsumerTest`.
- 🆕 **La frontera de salida hacia MCP está cerrada** (DP-07): las 6 tools devuelven **DTOs de
  respuesta**, no modelos de dominio. **Ningún modelo de dominio puede volver a serializarse hacia
  el cliente.**
- 🆕 **`McpResponsePayloadCharacterizationTest` congela el JSON de respuesta contra literales.**
  Es la red de seguridad más estricta del repositorio: **si se pone roja, el contrato cambió**.
- 🆕 **`DomainInvariantsTest` fija las tolerancias deliberadas** —`defaultValue` en blanco, `id`
  nulo, lote vacío, colecciones nulas que **no** se vacían—. **Existen para que nadie las elimine
  por parecer más limpias**: eliminarlas reabre DP-04.
- 🔸 **`AzureDevOpsTools` queda en 188 líneas y NO se parte** (B-12). Está decidido; no se replantea.
- 🔸 **Hallazgo anotado:** `OutboundPayloadCharacterizationTest` es **autorreferencial** (compara el
  cuerpo contra la serialización del propio dominio). Sirve, pero **no detecta un cambio que afecte
  a dominio y DTO a la vez**. Reforzarlo con literales sería una mejora barata *(opcional)*.

**De las Fases 02 a 06, intocable:**

- La postura de seguridad es **configuración** (`mcp.security.mode`, `PERMISSIVE` por defecto, DP-01).
- Los **nombres de campo del cable** y los **4 códigos de error** son contrato público.
- Los **3 nombres de cortacircuito**, los **4 timeouts** y las **2 URLs de equipo sin parametrizar**
  (B-10) están decididos.

### 1.3 El problema que esta fase resuelve

```
HOY                                          OBJETIVO DE ESTA FASE

ArchUnit  warning · informe VACÍO            ArchUnit  error · informe = realidad
          (SonarQube nunca ha visto
           una violación de este repo)

UseCasesConfigTest  siempre entra al catch   una prueba que comprueba algo
                    y hace assertTrue(true)

queryByWiql   tool comentada, código vivo    expuesta o retirada, pero decidida
HealthTool    "Server:  v1.0.0"              decidido
```

**D-25 y D-27 son la misma enfermedad: mecanismos de control que informan de éxito sin comprobarlo.**
El `issues.json` sale `{"issues":[],"rules":[]}` **haya o no violaciones** —lo dijo el log en la
Fase 01, cuando sí había una—, y `UseCasesConfigTest` envuelve todo en un
`catch (UnsatisfiedDependencyException) { assertTrue(true); }` sobre un contexto que **no puede
arrancar**, de modo que su única aserción probablemente **no se ha ejecutado jamás**.

Que hoy ambos «coincidan con la realidad» es una casualidad, no una garantía.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No deshace nada de las Fases 01 a 07.** Las siete decisiones (DP-01 a DP-07) están cerradas.
- **No parte `AzureDevOpsTools`** ni los adaptadores: **B-12** lo resolvió formalmente.
- **No toca** el modelo inmutable, las fronteras de contrato, el contrato de errores ni el repliegue
  de DP-04.
- **No añade tools ni capacidades MCP nuevas** (§12 del plan). (a) y (c) deciden sobre lo que **ya
  existe**, no crean nada.

---

## 2. Instrucciones

> Las de **T-03** y **T-04** son independientes de DP-08 y pueden hacerse aunque quede bloqueada.

### T-01 · Decidir el destino de `queryByWiql` y `HealthTool` (D-19, D-20)

Según **DP-08 §0.2(a)**, **(b)** y **(c)**. Cualquier cambio en `checkHealth` / `getServerInfo` o en
las capacidades anunciadas es **observable**: documentarlo en `CONTRATO-MCP.md` como hizo la Fase 06
con el contrato de errores.

### T-02 · Auditoría y wiring (D-23, D-05)

Según **B-15** y **B-13**. `UseCasesConfig` debe quedar con **un solo mecanismo de wiring**
(métrica «Mecanismos de wiring por bean: **2 → 1**»).

### T-03 · Arreglar el informe de ArchUnit (D-25) — **no depende de DP-08**

**Ficheros:** `applications/app-service/src/test/java/co/com/bancolombia/Utils.java`

`checkWithWarning` descarta en silencio toda incidencia cuyo fichero no resuelva. ⚠️ **El
`ArchitectureTest` lleva un aviso de «Please do not modify this file»** y **DP-05 ya autorizó
tocarlo** si hiciera falta; `Utils` es su acompañante.

**Validación:** introducir **temporalmente** una violación deliberada, comprobar que **aparece en el
`issues.json`**, y retirarla. Sin esa comprobación, arreglar el informe es indistinguible de no
arreglarlo — que es precisamente la deuda.

### T-04 · Arreglar la prueba que miente (D-27) — **no depende de DP-08**

**Ficheros:** `applications/app-service/src/test/java/.../config/UseCasesConfigTest.java`

Registrar los gateways que el contexto necesita —o usar un contexto de prueba que arranque de
verdad—, retirar el `catch` que garantiza el verde y el bean `myUseCase` que satisface la aserción
por sí solo. Debe **fallar** si el wiring se rompe. Es también la vía natural para subir
`app-service` del **58,2 %** al objetivo del **60 %**.

### T-05 · ArchUnit a `error` (§6)

Según **DP-08 §0.2(d)**. **Solo después de T-03**: pasar a `error` con el informe roto significaría
que el build depende de un mecanismo que no se ha verificado nunca.

### T-06 · Informe de cierre y documentación

1. **`docs/resultados/CIERRE-DEL-PLAN.md`**, como en `azure-devops-agent` y `azure-devops-backend`.
2. `CONTRATO-MCP.md`: §1 (capacidades), §2.6 (`HealthTool`) y §4 (`queryByWiql`) según DP-08.
3. Plan maestro: §3, §6, §7, §9, cabecera y **estado del plan a 🟢 CERRADO**.
4. Rellenar §4 de este documento y marcar el checklist.
5. Commit: `chore(mcp_config): tipar la configuracion y activar las reglas de arquitectura`

---

## 3. Orden de Ejecución

- [ ] **0.** ⚠️ **Plantear DP-08 (y B-13, B-14, B-15) al propietario y esperar respuesta.**
- [ ] **1.** Releer `CONTRATO-MCP.md` §1, §2.6 y §4: qué es contrato público de lo que se va a tocar.
- [ ] **2.** Arreglar el informe de ArchUnit **y demostrarlo con una violación deliberada**. **(T-03)**
- [ ] **3.** Arreglar `UseCasesConfigTest` para que **pueda fallar**. **(T-04)**
- [ ] **4.** `.\gradlew.bat build` verde, con **≥ 205** pruebas y **0** fallos.
- [ ] **5.** Aplicar B-13 sobre `UseCasesConfig`: **un solo mecanismo de wiring**. **(T-02)**
- [ ] **6.** Aplicar DP-08 (a), (b) y (c) sobre `queryByWiql`, `HealthTool` y las capacidades. **(T-01)**
- [ ] **7.** Aplicar B-15 sobre `McpAuditAspect` y B-14 sobre devtools. **(T-02)**
- [ ] **8.** Pasar ArchUnit a `error`. **(T-05)**
- [ ] **9.** Verificar que **las 3 caracterizaciones siguen verdes**: WIQL, cuerpo saliente y **JSON
      de respuesta MCP**.
- [ ] **10.** Medir cobertura y mutaciones; `app-service` debe alcanzar el **60 %**.
- [ ] **11.** `.\gradlew.bat build` verde, con **≥ 205** pruebas y **0** fallos.
- [ ] **12.** Escribir **`CIERRE-DEL-PLAN.md`** y actualizar `CONTRATO-MCP.md`. **(T-06)**
- [ ] **13.** Actualizar el plan maestro y **cerrarlo**. **(T-06)**
- [ ] **14.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-06)**
- [ ] **15.** Commit: `chore(mcp_config): tipar la configuracion y activar las reglas de arquitectura`

---

## 4. Resultado

> Rellenar **al cerrar la fase**, con lo alcanzado de verdad.

| Métrica | Antes | Después |
|---------|------:|--------:|
| Mecanismos de wiring por bean | **2** | — |
| ArchUnit ejecutado como | **warning** | — |
| ArchUnit — violaciones exportadas a Sonar | **0 de 0** *(informe roto)* | — |
| Violación deliberada detectada en el `issues.json` | **no probado nunca** | — |
| Pruebas que no pueden fallar | **1** *(`UseCasesConfigTest`)* | — |
| Tools comentadas con código vivo | **1** *(`queryByWiql`)* | — |
| Capacidades MCP anunciadas sin implementación | **2** *(`resource`, `prompt`)* | — |
| Pruebas totales | **205** | — |
| Cobertura `app-service` | **58,2 %** | — |
| Cambios en el contrato MCP público | n/a | — |

**Decisión DP-08:** *(pendiente)*
**Cambios en el contrato MCP público:** *(pendiente)*
**Bloqueos encontrados:** *(pendiente)*
**Plan cerrado:** ☐ `docs/resultados/CIERRE-DEL-PLAN.md`

