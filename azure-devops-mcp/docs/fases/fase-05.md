# FASE 05 — Segregación de puertos y adaptadores

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** ⚪ PENDIENTE
> **Deudas que ataca:** D-10, D-14, D-15 · **Decisión bloqueante:** ⛔ **DP-05 (ABIERTA)**
> **Riesgo:** 🟠 Medio · **Depende de:** Fases 01 a 04 (cerradas) · **Habilita:** Fases 06 a 08
> **Regla de oro de esta fase:** **ni una URL, ni un parámetro de consulta, ni un cuerpo JSON
> cambian.** Esta fase mueve código de sitio; no cambia una sola llamada HTTP.

---

## 0. ⛔ PASO BLOQUEANTE — DP-05

**No se escribe una sola línea de código antes de que el propietario responda.**

### 0.1 La pregunta

Los paquetes de `domain/model` están organizados **por operación CRUD**, no por agregado:

```
model/getworkitem/       GetWorkItem        + gateways/GetWorkItemRepository
model/createworkitem/    CreateWorkItem     + gateways/CreateWorkItemRepository
model/updateworkitem/    UpdateWorkItem     + gateways/UpdateWorkItemRepository
model/getworkitemsbatch/ GetWorkItemsBatch  + gateways/GetWorkItemsBatchRepository
model/querybywiql/       QueryByWiql        + gateways/QueryByWiqlRepository
model/team/              TeamFieldValues    + gateways/GetTeamFieldValuesRepository
model/iteration/         TeamIteration      + gateways/GetTeamIterationsRepository
model/workitem/          ← el agregado real, donde la Fase 04 ya puso los 6 Value Objects
```

**Siete paquetes y siete puertos para un solo agregado** (D-14). El plan maestro §4.1 propone
reagruparlos en `workitem/` y `team/`, con **tres puertos segregados por responsabilidad**
(`WorkItemQueryPort`, `WorkItemCommandPort`, `TeamScopePort`) en lugar de siete por operación.

> ⚠️ **Por qué lo decide el propietario y no la fase.** Reagrupar cambia **rutas de importación en
> los cinco módulos** y, sobre todo, toca el `ArchitectureTest`, que lleva escrito
> `// Please do not modify this file` **y rutas absolutas de máquina** en su método `exportIssues`.
> El precedente del BFF (DP-05a) autorizó tocarlo con justificación explícita, pero aquí no se
> asume: se pregunta.

| Opción | Qué implica |
|--------|-------------|
| **(a)** Reagrupar **paquetes y puertos** (`workitem/` + `team/`, 7 puertos → 3) | Lo que propone §4.1. Máximo beneficio estructural y máximo número de importaciones tocadas |
| **(b)** Reagrupar **solo los paquetes**, conservando los 7 puertos tal cual | Cambio mecánico y de bajo riesgo; deja D-14 a medias |
| **(c)** **No reagrupar.** Partir únicamente `RestConsumer` (D-10) y retirar el puerto huérfano (D-15) | Cierra la deuda más cara sin tocar ni una importación del dominio |

### 0.2 Sub-decisiones

| ID | Cuestión |
|----|----------|
| **B-05** | Si se parte `RestConsumer`, **¿cómo se reparten los siete `@CircuitBreaker`?** Hoy sus instancias **no existen** en `application.yaml` (D-06) y corren con la configuración por defecto. ¿Se conservan los nombres literales al mover los métodos —lo que preserva el comportamiento actual, sea el que sea— o se renombran ya por adaptador, anticipando la Fase 06? |
| **B-06** | El puerto huérfano `workitem/gateways/WorkItemRepository` **no lo implementa ni lo usa nadie** (D-15). ¿Se **borra**, o se **conserva** como la interfaz agregadora hacia la que converger? |
| **B-07** | ¿Los adaptadores resultantes comparten un único `WebClient` y un único helper de versión de API, o cada uno construye el suyo? De la respuesta depende si D-18 (7 versiones hardcodeadas) se alivia aquí o espera a la Fase 06. |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-05 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 06**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 04 (2026-08-30), no estimado:

| Dato | Valor |
|------|-------|
| Build | 🟢 **VERDE** — **148 pruebas, 0 fallos** |
| Cobertura `domain/model` | **94,7 %** *(el módulo estrenó `src/test` en la Fase 04)* |
| Cobertura `domain/usecase` | **97,8 %** |
| Cobertura `mcp-server` | **80,7 %** |
| Cobertura `rest-consumer` | **88,2 %** |
| Cobertura `app-service` | **58,2 %** |
| Líneas de `AzureDevOpsTools` | **157** *(era 278; ya no contiene ninguna regla de negocio)* |
| Líneas de `RestConsumer` | **191** *(objetivo: ≤ 120 **por adaptador resultante**)* |
| Sentencia WIQL | **congelada carácter a carácter**, 8 ramas, ahora en `domain/usecase` |
| Cuerpo HTTP de salida | **congelado carácter a carácter**, 4 cuerpos |
| ArchUnit `Rule_2.2` | ✅ **0 violaciones reales** *(verificado en el log; D-25 sigue viva)* |

### 1.2 Lo que dejaron hechas las fases previas y afecta a ésta

**De la Fase 03:** hay **1 mapper de entrada** y **5 de salida**; los DTOs del adaptador viven en
`consumer/dto/` y los mappers en `consumer/mapper/`. **Al partir `RestConsumer`, los mappers no se
duplican**: se reparten. `OutboundPayloadCharacterizationTest` congela los cuerpos emitidos y es la
red de seguridad de esta fase.

**De la Fase 04:**

- ✅ **D-07, D-08, D-09 y D-12 saldadas.** El dominio ya contiene el flujo compuesto, 6 Value
  Objects inmutables en `model/workitem/`, el servicio de dominio `TeamPathFallback` y el puerto
  `TeamScopeFallbackMetrics`. **Si esta fase reagrupa paquetes, ese contenido se mueve con ellos.**
- 🔸 **`domain/usecase` no admite ninguna dependencia nueva.** La tarea `validateStructure` del
  plugin de Clean Architecture rechaza el módulo si se le añade cualquier cosa más allá de
  `:model` —se comprobó con `slf4j-api` y el build falló con «Use case module is invalid»—.
  **No lo intentes de nuevo.**
- 🔸 **Precedente autorizado:** la Fase 04 **reubicó y adaptó 3 clases de prueba heredadas** con
  autorización expresa del propietario, bajo la condición de **no alterar ningún escenario**. Esa
  autorización fue puntual: **no es un permiso general**.
- 🔸 **Deuda anotada, no cerrada:** `AzureDevOpsTools` quedó en **157 líneas** y no en ≤ 120. Lo que
  resta es declaración de protocolo (30 `@McpToolParam`). Bajar de 120 exigiría **partir la clase
  en dos beans de herramientas**, decisión que **no se tomó por cuenta propia**.

### 1.3 El problema que esta fase resuelve

`RestConsumer` implementa **siete gateways** en una sola clase. La consecuencia práctica es que **no
hay forma de probar un flujo sin arrastrar los otros seis**: cualquier prueba del adaptador de
consulta carga también el de escritura, el de lote y los de equipo. Y `spring-rules.md` §2 pide
justamente lo contrario —separar lecturas de escrituras (CQS)— y §3 lo repite en SOLID-I.

```
HOY                                     OBJETIVO DE ESTA FASE

adapter  ████████ (7 gateways juntos)   adapter  ██ ██ ██ (uno por agregado y responsabilidad)
model    7 paquetes CRUD, 7 puertos     model    workitem/ + team/, ≤ 3 puertos
         + 1 puerto huérfano                     0 puertos huérfanos
```

### 1.4 Lo que esta fase deliberadamente NO hace

- **No cambia ninguna llamada HTTP**: ni URL, ni parámetro de consulta, ni cuerpo, ni cabecera.
- **No toca la seguridad** (Fase 02), ni la frontera de DTOs (Fase 03), ni el flujo compuesto
  (Fase 04).
- **No traduce errores** ni **configura cortacircuitos**: D-06, D-13, D-18, D-24 → **Fase 06**.
- **No retira los `@Setter`** del modelo preexistente: D-16 → **Fase 07**.
- **No arregla D-25** (informe de ArchUnit vacío) ni D-19/D-20/D-23/D-05 → **Fase 08**.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-05**.

### T-01 · Partir `RestConsumer` por agregado y responsabilidad (D-10)

**Ficheros:** `rest-consumer/.../consumer/` *(nuevos adaptadores)* · `RestConsumer.java`

1. Un adaptador por responsabilidad: consulta de work items, mutación de work items y ámbito de
   equipo.
2. Los mappers de la Fase 03 **se reparten, no se duplican**.
3. **Ni una URL ni un cuerpo cambian.** `RestConsumerTest` y
   `OutboundPayloadCharacterizationTest` son la red de seguridad.

**Validación:** las 148 pruebas siguen en verde. Si `RestConsumerTest` exige adaptación por el
cambio de tipo, **se pregunta antes** (precedente de las Fases 03 y 04).

### T-02 · Reagrupar paquetes y segregar puertos (D-14)

Según lo que decida **DP-05 §0.1**. Si se autoriza la opción (a), los siete puertos por operación se
funden en `WorkItemQueryPort`, `WorkItemCommandPort` y `TeamScopePort`.

### T-03 · Retirar el puerto huérfano (D-15)

Según **B-06**: borrar `workitem/gateways/WorkItemRepository` o convertirlo en la interfaz
agregadora. Hoy **no lo implementa ni lo usa nadie**.

### T-04 · Cobertura y líneas

Cada adaptador resultante **≤ 120 líneas**. La cobertura de `rest-consumer` **no debe bajar** de
88,2 %.

### T-05 · Actualizar documentación y generar la Fase 06

1. `CONTRATO-MCP.md`: si algún puerto cambia de nombre, dejarlo escrito.
2. Plan maestro: §3 (D-10, D-14, D-15), §6 (métricas), §7 (DP-05), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-06.md`** y **`PROMPT-FASE-06.md`** con **DP-06** bloqueante.
5. Commit: `refactor(rest_consumer): segregar los adaptadores de azure devops por agregado`

---

## 3. Orden de Ejecución

- [ ] **0.** ⛔ **Plantear DP-05 (y B-05, B-06, B-07) al propietario y esperar respuesta.**
- [ ] **1.** Releer `CONTRATO-MCP.md` §3 y el §4 de `fase-03.md`: qué mapper vive dónde y por qué.
- [ ] **2.** Inventariar los 7 gateways, sus 7 URLs y sus 7 nombres de cortacircuito.
- [ ] **3.** Partir `RestConsumer` en adaptadores, repartiendo los mappers. **(T-01)**
- [ ] **4.** `.\gradlew.bat build` verde: **ni una URL ni un cuerpo alterados**. **(T-01)**
- [ ] **5.** Aplicar lo que decida DP-05 sobre paquetes y puertos. **(T-02)**
- [ ] **6.** Aplicar lo que decida B-06 sobre el puerto huérfano. **(T-03)**
- [ ] **7.** Comprobar en el **log** del `ArchitectureTest` que sigue a **0** violaciones.
- [ ] **8.** Medir líneas por adaptador (objetivo ≤ 120) y cobertura. **(T-04)**
- [ ] **9.** `.\gradlew.bat build` verde de nuevo, con **≥ 148** pruebas y **0** fallos.
- [ ] **10.** Actualizar `CONTRATO-MCP.md` y el plan maestro (§3, §6, §7, §9, cabecera). **(T-05)**
- [ ] **11.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [ ] **12.** **Generar `fase-06.md` y `PROMPT-FASE-06.md`** con DP-06 bloqueante.
- [ ] **13.** Commit: `refactor(rest_consumer): segregar los adaptadores de azure devops por agregado`

---

## 4. Resultado

> Rellenar **al cerrar la fase**, con lo alcanzado de verdad.

| Métrica | Antes | Después |
|---------|------:|--------:|
| Gateways implementados por una sola clase | **7** | — |
| Líneas de la clase de adaptador mayor | **191** | — |
| Puertos por agregado | **7 / 1** | — |
| Puertos huérfanos | **1** | — |
| URLs o cuerpos HTTP alterados (debe ser 0) | n/a | — |
| Pruebas totales | **148** | — |
| Pruebas heredadas **modificadas** | n/a | — |
| Cobertura `rest-consumer` | **88,2 %** | — |

**Decisión DP-05:** *(pendiente)*
**Cambios en el contrato MCP público:** *(pendiente — debe ser «ninguno»)*
**Bloqueos encontrados:** *(pendiente)*
**Fase siguiente generada:** ☐ `docs/fases/fase-06.md` · ☐ `docs/fases/PROMPT-FASE-06.md`

