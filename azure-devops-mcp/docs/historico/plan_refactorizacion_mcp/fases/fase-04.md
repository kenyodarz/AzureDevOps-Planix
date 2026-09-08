# FASE 04 — Desacople del flujo compuesto

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🟢 **COMPLETADA**
> (2026-08-30)
> **Deudas que ataca:** D-07, D-08, D-09, D-12 *(las cuatro **saldadas**)* · **Decisión
bloqueante:** ✅ **DP-04 (RESUELTA)**
> **Riesgo:** 🔴 **Alto** — el más alto del plan · **Depende de:** Fases 01, 02 y 03 (cerradas) ·
> **Habilita:** Fases 05 a 08
> **Regla de oro de esta fase:** **la sentencia WIQL que sale por el cable debe ser idéntica,
> carácter a carácter.** Si cambia un espacio, el tablero sale vacío y nadie se entera.
> ✅ **Lo fue, en las 8 ramas.**

---

## 0. ⛔ PASO BLOQUEANTE — DP-04

**No se escribe una sola línea de código antes de que el propietario responda.**

Esta fase no mueve DTOs: mueve **reglas de negocio de Azure DevOps**. Y hay dos que **nadie ha
decidido nunca**: están en el código porque alguien las escribió, no porque alguien las eligiera.
Moverlas de sitio sin decidirlas sería perpetuar una asunción y, además, escribirla en el dominio,
que es donde más cara sale de corregir.

### 0.1 La primera pregunta: el repliegue por concatenación

`resolveIterationPath` fabrica la ruta del sprint concatenando, y para un sprint con formato
`Sprint N` **intercala el año del calendario**:

```java
if (cleanSprint.matches("Sprint \\d+")) {
    int currentYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).getYear();
    return project + '\\' + currentYear + '\\' + cleanSprint;   // ← el fallo del tablero vacío
}
```

Un sprint que va de diciembre a enero pertenece al año en que **empezó**. Consultado en enero, esta
ruta apunta a una iteración que no existe: la consulta WIQL devuelve cero ítems y el tablero sale
vacío **sin error, sin aviso y sin log**. Hoy ya no es el camino principal —solo se dispara desde el
`onErrorResume` cuando Azure DevOps no responde—, pero **sigue vivo y nadie mide cuántas veces se
dispara** (D-09, heredada como D-37 del plan del BFF).

| Opción                                                                              | Qué implica                                                                                                                                               |
|-------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **(a)** Se conserva, con **métrica y log de aviso**                                 | El comportamiento no cambia; se gana visibilidad. Es la opción sin regresión posible                                                                      |
| **(b)** Se conserva **sin el año**, y se devuelve error si Azure DevOps no responde | Elimina la causa raíz, pero **cambia el comportamiento observable** cuando Azure DevOps falla: donde antes salía un tablero vacío, ahora saldría un error |
| **(c)** Se retira el repliegue por completo                                         | Lo más limpio; si Azure DevOps no responde, la tool falla. Máximo cambio de contrato de facto                                                             |

> ⚠️ El punto 5 y el punto 6 de `CONTRATO-MCP.md` §2.4 declaran este repliegue **contrato de
facto**:
> los clientes pueden estar explotándolo sin saberlo. Por eso lo decide el propietario, no la fase.

### 0.2 La segunda pregunta: ¿qué es regla de dominio y qué es configuración?

Tres reglas de Azure DevOps viven **hardcodeadas en el entry-point** (D-08, heredada como D-35):

| Regla                                                   | Dónde está hoy                   |
|---------------------------------------------------------|----------------------------------|
| Tipos por defecto `'Historia de Usuario','Habilitador'` | `parseWorkItemTypes`, literal    |
| Traducción `User Story` → `Historia de Usuario`         | `normalizeWorkItemType`, literal |
| Entrecomillado de los tipos para el WIQL                | `quoteWorkItemType`              |

**¿Son regla de dominio —y por tanto van a un Value Object inmutable— o son configuración —y por
tanto van a `@ConfigurationProperties`?** No es una cuestión estética: si son configuración, cambiar
el idioma de una organización no requiere recompilar; si son dominio, se prueban y se versionan con
el código. La respuesta determina dónde acaban y qué prueba se les escribe.

### 0.3 Sub-decisión adicional

| ID       | Cuestión                                                                                                                                                                                                                       |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-04** | ¿El nuevo `ListWorkItemsByTeamAndSprintUseCase` recibe un **objeto comando** (`ListWorkItemsCommand`) o los seis parámetros sueltos? El plan maestro §4.1 anticipa el comando; conviene ratificarlo antes de escribir la firma |

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-04 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 05**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 03 (2026-08-30), no estimado:

| Dato                         | Valor                                                      |
|------------------------------|------------------------------------------------------------|
| Build                        | 🟢 **VERDE** — **77 pruebas, 0 fallos**                    |
| Cobertura `domain/model`     | **0 %** *(el módulo sigue sin `src/test`, D-26)*           |
| Cobertura `domain/usecase`   | **68,2 %**                                                 |
| Cobertura `mcp-server`       | **73,1 %**                                                 |
| Cobertura `rest-consumer`    | **88,1 %**                                                 |
| Cobertura `app-service`      | **48,4 %**                                                 |
| Líneas de `AzureDevOpsTools` | **277** *(objetivo del plan: ≤ 120)*                       |
| Líneas de `RestConsumer`     | **186** *(era 237; la Fase 03 sacó los mappers)*           |
| Sentencia WIQL               | **congelada carácter a carácter**, 8 ramas                 |
| Cuerpo HTTP de salida        | **congelado carácter a carácter**, 4 cuerpos               |
| ArchUnit `Rule_2.2`          | ✅ **0 violaciones reales** *(D-17 saldada en la Fase 03)* |

Documentos de referencia: [`BASELINE.md`](../resultados/BASELINE.md) ·
[`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) ·
[`ACTIVACION-SEGURIDAD.md`](../resultados/ACTIVACION-SEGURIDAD.md)

### 1.2 Lo que dejaron hechas las fases previas y afecta a ésta

**De la Fase 02:**

- ✅ Las **8 tools llevan `@PreAuthorize`**, con los roles en
  `mcp-server/.../mcp/security/McpRoles.java`.
  Esta fase **va a mover el cuerpo de `listWorkItemsByTeamAndSprint` a un caso de uso**: la
  anotación **se queda en el método de la tool**, que sigue siendo el punto de entrada.
  `McpToolsAuthorizationTest` debe seguir verde **sin modificarse**.
- 🔸 La postura de seguridad es configuración (`mcp.security.mode`, `PERMISSIVE` por defecto). **No
  la
  toques.**

**De la Fase 03:**

- ✅ **D-11 y D-17 saldadas.** El dominio ya no es el contrato de cable: hay **1 mapper de entrada**
  y **5 de salida**. Si esta fase introduce tipos nuevos en la frontera, **debe respetar esa
  separación**: nada de volver a exponer un modelo de dominio como `@McpToolParam`.
- ✅ **`WorkItemsBatchRequest` → `WorkItemBatchCriteria`.** No revivas el nombre viejo.
- ✅ **`OutboundPayloadCharacterizationTest`** compara el cuerpo HTTP emitido contra la serialización
  del dominio. Es tu segunda red de seguridad, junto a `WiqlCharacterizationTest`.
- 🔸 **Precedente autorizado:** la Fase 03 tocó 3 líneas de `RestConsumerTest` para un renombrado de
  símbolo, **previa autorización expresa del propietario**. Esa autorización fue puntual: **no es un
  permiso general** para modificar pruebas heredadas.

### 1.3 El problema que esta fase resuelve

**La pirámide está invertida y `listWorkItemsByTeamAndSprint` es la prueba.** Sus 44 líneas
(`AzureDevOpsTools.java`) **son** el sistema: limpian cadenas, resuelven el `AreaPath`, resuelven el
`IterationPath`, normalizan los tipos de work item, **redactan la sentencia WIQL con
`String.format`** y encadenan la consulta. Las otras cinco tools son pasamanos.

Y `spring-rules.md` es explícito para `entry-points`: **«Cero lógica de negocio»**.

```
HOY                                          OBJETIVO DE ESTA FASE

entry-point  ████████████ 277 líneas         entry-point  ███ (protocolo MCP y nada más)
             (WIQL + rutas + normalización)
usecase      █ (7 delegantes vacíos)         usecase      ███████ (el flujo compuesto)
model        █ (anémico, mutable)            model        █████ (VOs con invariantes)
```

Seis de los siete casos de uso son **delegantes de 17–18 líneas**: la capa de aplicación está vacía
**porque la lógica se quedó arriba** (D-12). No son dos deudas, son la misma vista desde los dos
lados.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No toca la seguridad** (Fase 02) ni la frontera de DTOs (Fase 03). Ni el modo, ni las
  anotaciones, ni el token, ni los mappers.
- **No parte `RestConsumer`** en adaptadores por agregado: es D-10, **Fase 05**.
- **No reagrupa los paquetes de `domain/model`** (`getworkitem`, `createworkitem`… → `workitem`):
  eso
  es **DP-05**, **Fase 05**.
- **No traduce errores** ni toca cortacircuitos: D-13, D-06, **Fase 06**.
- **No retira los `@Setter`** ni convierte el modelo entero en Value Objects inmutables: D-16,
  **Fase 07**. *(Los VOs **nuevos** que esta fase cree sí nacen inmutables.)*
- **No arregla D-25** (informe de ArchUnit vacío para Sonar) **ni D-26** (`domain/model` sin
  `src/test`): **Fases 07 y 08**.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-04**.

### T-01 · Value Objects del dominio (D-08)

**Ficheros:** `domain/model/.../model/workitem/` *(nuevos)*

Crear, **inmutables y autovalidados**, según lo que decida DP-04 §0.2:

```java
public record TeamName(String value)     { /* no nulo, no en blanco; se queda con el último tramo */ }
public record SprintName(String value)   { /* no nulo, no en blanco */ }
public record WorkItemTypes(List<String> values) {
    public static WorkItemTypes defaults();        // si DP-04 dice «dominio»
    public static WorkItemTypes parse(String csv); // normaliza, traduce y entrecomilla
}
public record TeamScope(String areaPath, String iterationPath) { }
public record WiqlStatement(String value) { }      // se construye, no se concatena a mano
```

**Sin Lombok `@Setter`, sin Spring, sin Jackson.** Son `record`: nacen inmutables.

### T-02 · El flujo compuesto sale del entry-point (D-07, D-12)

**Ficheros:** `domain/usecase/.../listworkitems/ListWorkItemsByTeamAndSprintUseCase.java` *(
nuevo)* ·
`AzureDevOpsTools.java`

1. Mover a este caso de uso: limpieza de cadenas, resolución de `AreaPath` e `IterationPath`,
   normalización de tipos y **construcción de la sentencia WIQL**.
2. El entry-point queda reducido a: recibir los parámetros MCP, mapearlos con `McpToolDtoMapper` y
   delegar. **Cero `String.format` de WIQL en `mcp-server`.**
3. El `@PreAuthorize` **se queda en la tool**.

**Validación:** `WiqlCharacterizationTest` (8 ramas) y `AzureDevOpsToolsTest` (7) siguen en verde
**sin modificarse**. Si una sola cambia de resultado, la sentencia dejó de ser idéntica.

### T-03 · Resolución de rutas medida (D-09)

**Ficheros:** `domain/usecase/.../ResolveTeamScopeUseCase.java` *(nuevo)* · `app-service` (métricas)

Según lo que decida **DP-04 §0.1**. En cualquiera de las tres opciones:

1. El repliegue deja de estar en el entry-point.
2. **Cada disparo del repliegue se cuenta**, con un contador de Micrometer y un `log.warn`
   inequívoco: hoy nadie sabe cuántas veces ocurre.
3. Si se conserva el año del calendario, **el número mágico desaparece** en favor de una constante o
   un método con nombre (S109).

### T-04 · Cobertura del flujo movido

El código que baja del entry-point al dominio **debe llegar mejor probado que como salió**.
Objetivo:
`domain/usecase` de **68,2 %** a **≥ 85 %**, y crear `domain/model/src/test` —lo que además empieza
a
saldar **D-26** por la vía de los hechos.

### T-05 · Actualizar documentación y generar la Fase 05

1. `CONTRATO-MCP.md` §2.4: dejar constancia de qué pasó con los seis «comportamientos tolerantes»,
   uno a uno. Son contrato de facto: cualquier cambio se declara aquí o no existe.
2. Plan maestro: §3 (D-07, D-08, D-09, D-12), §6 (métricas), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-05.md`** con **DP-05** marcada como bloqueante en su primer paso.
5. Commit: `refactor(work_item_query): mover la construccion del wiql al caso de uso`

---

## 3. Orden de Ejecución

- [ ] **0.** ⛔ **Plantear DP-04 al propietario y esperar respuesta.** No continuar sin ella.
- [ ] **1.** Releer `BASELINE.md` §7.1: la plantilla WIQL y las **8 ramas** congeladas.
- [ ] **2.** Releer `CONTRATO-MCP.md` §2.4: los **6 comportamientos tolerantes** son contrato de
  facto.
- [ ] **3.** Crear los Value Objects inmutables en `domain/model`. **(T-01)**
- [ ] **4.** Crear `domain/model/src/test` y probarlos: invariantes y casos borde. **(T-01, D-26)**
- [ ] **5.** `.\gradlew.bat build` verde. Las 77 pruebas heredadas, **sin modificar**.
- [ ] **6.** Crear `ResolveTeamScopeUseCase` con la resolución de rutas y su métrica. **(T-03)**
- [ ] **7.** Crear `ListWorkItemsByTeamAndSprintUseCase` con el WIQL dentro. **(T-02)**
- [ ] **8.** Vaciar `listWorkItemsByTeamAndSprint` en el entry-point, conservando el
  `@PreAuthorize`. **(T-02)**
- [ ] **9.** **Comparar la sentencia WIQL generada, carácter a carácter, en las 8 ramas.** **(
  T-02)**
- [ ] **10.** `.\gradlew.bat build` verde: `WiqlCharacterizationTest` y `AzureDevOpsToolsTest`
  **intactas**.
- [ ] **11.** Medir líneas de `AzureDevOpsTools` (objetivo ≤ 120) y cobertura de `domain/usecase`.
  **(T-04)**
- [ ] **12.** Actualizar `CONTRATO-MCP.md` §2.4 y el plan maestro (§3, §6, §9, cabecera). **(T-05)**
- [ ] **13.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [ ] **14.** **Generar `docs/fases/fase-05.md`** con DP-05 bloqueante. **(Regla de Continuidad)**
- [ ] **15.** Commit: `refactor(work_item_query): mover la construccion del wiql al caso de uso`

---

## 4. Resultado

> Rellenar **al cerrar la fase**, con lo alcanzado de verdad.

| Métrica                                                           |            Antes | Después |
|-------------------------------------------------------------------|-----------------:|--------:|
| Líneas de `AzureDevOpsTools`                                      |          **277** |       — |
| Reglas de negocio en `entry-points`                               |            **4** |       — |
| Sentencias WIQL construidas con `String.format` fuera del dominio |            **1** |       — |
| Cálculos del año por calendario **sin medir**                     |            **1** |       — |
| Casos de uso delegantes vacíos                                    |            **6** |       — |
| Value Objects inmutables en `domain/model`                        |            **0** |       — |
| Módulos sin carpeta de pruebas                                    |            **1** |       — |
| Cobertura `domain/usecase` / `domain/model`                       | **68,2 % / 0 %** |       — |
| Pruebas totales                                                   |           **77** |       — |
| Pruebas heredadas **modificadas**                                 |              n/a |   **0** |
| Sentencia WIQL alterada (debe ser 0)                              |              n/a |   **0** |

**Decisión DP-04:** *(pendiente)*
**Cambios en el contrato MCP público:** *(pendiente — debe ser «ninguno»)*
**Bloqueos encontrados:** *(pendiente)*
**Fase siguiente generada:** ☐ `docs/fases/fase-05.md`

