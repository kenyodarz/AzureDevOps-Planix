# FASE 05 — Dominio del dashboard

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** ✅ **CERRADA** (2026-08-29)
> **Deudas saldadas:** **D-06, D-07, D-09, D-13, D-19**
> **Resultado:** [`RESULTADO-FASE-05.md`](../resultados/RESULTADO-FASE-05.md)
> **Bloqueantes:** [`BLOQUEANTES-FASE-05.md`](../plan/BLOQUEANTES-FASE-05.md) — B-01 a B-04
> **cerrados** · **S-2** resuelto
> **Fase anterior:** [`fase-04.md`](fase-04.md) → [
`RESULTADO-FASE-04.md`](../resultados/RESULTADO-FASE-04.md)
> **Fase siguiente:** [`fase-06.md`](fase-06.md) · relevo en [
`HANDOFF-FASE-06.md`](../plan/HANDOFF-FASE-06.md)

---

## 1. Contexto

### 1.1 De dónde vienes

La Fase 04 está cerrada. Al arrancar esta fase existe:

- **Build en verde: 170 pruebas, 0 fallos.**
- `DevOpsDashboardUseCase` en **225 líneas**: sin prompts ni mock dentro (D-08 saldada).
- `Handler` con `AUDIT_BATCH_SIZE` y `MOCK_STREAM_DELAY` nombrados, **sin cambiar sus valores**.
- La resolución de `AreaPath`/`IterationPath` ya **no está duplicada**, pero sigue siendo
  concatenación de `String`.
- Cobertura: `domain/usecase` 96,8 % · `app-service` 82,1 % · `reactive-web` 89,7 %.

### 1.2 El problema

El tablero **no tiene dominio**. Tiene un caso de uso que habla con el agente y un entry-point que
piensa. Verificado en código al cerrar la Fase 04:

| Deuda    | Dónde                                                  | Qué pasa                                                                                                                                                                                                                                             |
|----------|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-07** | `Handler.partitionItems`, `Handler.recalculateMetrics` | La partición en lotes y el cálculo de `avgQualityScore` / `undocumentedCount` son **reglas de negocio dentro de un adaptador HTTP**. Están probadas en `DashboardRoutesCharacterizationTest` porque es el único sitio desde el que se pueden probar. |
| **D-06** | `Handler.updateOriginalItems`, `Handler:196`           | Se **mutan los modelos in-place** dentro de la cadena reactiva, y el estado viaja en `final DashboardResponse[] sharedData = new DashboardResponse[1]`. Un array de un elemento es una variable global con disfraz.                                  |
| **D-09** | `DevOpsDashboardUseCase.resolveCell/resolveSprint`     | La ruta se construye concatenando `String` y barras invertidas. Nadie valida nada; un `IterationPath` mal formado solo se descubre cuando Azure DevOps devuelve 400.                                                                                 |
| **D-13** | `model/dashboard/*Response.java`                       | El dominio es anémico, mutable y con sufijos técnicos. ArchUnit ya lo señala: *Rule_2.2 violated (3 times)*.                                                                                                                                         |
| **D-19** | `resolveSprint`                                        | El año se calcula con `LocalDate.now()`. Es lo que queda de la deuda.                                                                                                                                                                                |

### 1.3 🔴 El problema del año, con nombre y apellido

Es la propuesta que quedó pendiente de DP-02.c, y conviene enunciarla bien porque **no es un fallo
de cálculo**:

> Un sprint que va de diciembre de 2025 a enero de 2026 **es de 2025**, porque así se llama su
> `IterationPath`. No porque el calendario lo diga.

`LocalDate.now().getYear()` lo busca en el año en curso. En enero de 2026 no lo encuentra, Azure
DevOps devuelve una iteración vacía y el tablero sale sin ítems: **sin error, sin aviso**. Es el
peor modo de fallo posible.

**El error de fondo no es calcular mal el año: es calcularlo.** La salida es dejar de construir la
ruta por concatenación y **preguntarle a Azure DevOps qué iteración corresponde al sprint**. Eso es
lo que convierte a `IterationPath` en un Value Object con una resolución detrás, y por eso encaja
exactamente aquí y no en la Fase 04.

---

## 2. ✅ DP-03 — resuelta el 2026-08-29

> **El análisis completo, con hechos verificados en código, opciones y recomendación, está en
> [`BLOQUEANTES-FASE-05.md`](../plan/BLOQUEANTES-FASE-05.md).** Esta sección solo resume.

**Hallazgo que reordenó la discusión (2026-08-29):** `AzureDevOpsTools.resolveIterationPath` del
**MCP** (líneas 168-179) era **el mismo código, carácter por carácter**, que `resolveSprint` del
BFF, con el mismo fallo del año. La lógica estaba **triplicada** —BFF, MCP y el propio prompt— y la
fuente del fallo estaba **en el MCP**, no aquí. Arreglarlo solo en el BFF no arreglaba nada.

| Bloqueante | Pregunta                                                   | **Resuelto como**                                  |
|------------|------------------------------------------------------------|----------------------------------------------------|
| **B-04**   | ¿Se levanta §10 acotada al `resolveIterationPath` del MCP? | ✅ **Sí, opción B — nominal y acotada**            |
| **B-01**   | ¿De dónde sale el año del `IterationPath`?                 | ✅ **C — se le pregunta a Azure DevOps**           |
| **B-02**   | ¿Tamaño de lote y concurrencia?                            | ✅ **B — configurable, 10 por defecto + tope 2**   |
| **B-03**   | ¿Qué pasa con los sufijos `...Response`?                   | ✅ **A por pasos** *(ejecución bloqueada por S-2)* |

### Excepción autorizada a `plan-maestro.md` §10

> *Se autoriza modificar `azure-devops-mcp` **exclusivamente** para añadir la resolución de
> `IterationPath` por consulta a Azure DevOps, simétrica a la que ya existe para `AreaPath`,
> conservando la concatenación actual como fallback. Cualquier otro cambio en ese repositorio sigue
> fuera de alcance.*

---

## 2.bis 🔴 S-2 — bloqueante nuevo, detiene T-03 a T-06

`DashboardRoutesCharacterizationTest` es intocable (T-07) **y a la vez ancla el nombre y el
paquete**
de `DashboardResponse` en sus líneas 17, 286 y 333. B-03 (A) exige moverlo a `reactive-web`, con lo
que ese test **no compilaría**. Las dos instrucciones se contradicen y, conforme a la regla §5.9 del
HANDOFF, la salida no se elige en silencio.

Opciones y recomendación: [`RESULTADO-FASE-05.md`](../resultados/RESULTADO-FASE-05.md) §6.
**Recomendación: S-2** — autorizar por escrito un cambio de 3 líneas de *tipos* (sin tocar ninguna
aserción) en el test.

---

## 3. Lo que esta fase hará

- **T-01** · ✅ **Hecha.** `TeamName` y `SprintName` como Value Objects inmutables y autovalidados
  (D-09).
  > **Desviación respecto al borrador.** Decía «`AreaPath` e `IterationPath`». Al resolverse B-01
  > como C, el BFF **deja de manejar rutas de Azure DevOps**, así que esos dos VO no representarían
  > nada dentro del BFF. Los VO correctos son los **nombres**, que es lo único que el BFF conoce.
  > Queda escrito aquí por la regla §5.9 del HANDOFF.
- **T-02** · ✅ **Hecha, en los dos repositorios.** El año deja de calcularse: el BFF ya no construye
  rutas y el MCP resuelve el `IterationPath` preguntando a Azure DevOps, con la concatenación como
  fallback (D-19).
- **T-03** · ⏸️ La partición en lotes se muda al dominio, con `audit.batch-size` configurable (10
  por defecto) y tope de concurrencia 2 (D-07, B-02).
- **T-04** · ⏸️ El recálculo de `avgQualityScore` y `undocumentedCount` se muda al dominio (D-07).
- **T-05** · ⏸️ `BacklogAudit` inmutable: se acaban las mutaciones in-place y el array `sharedData`
  (D-06). **Bloqueada por S-2.**
- **T-06** · ⏸️ Sufijos técnicos fuera del dominio (D-13). **Bloqueada por S-2.**
- **T-07** · ✅ `DashboardRoutesCharacterizationTest` verde **sin modificar** — y así sigue.

---

## 4. Lo que NO entra en esta fase

- Sacar la escritura de ficheros del caso de uso → **Fase 06** (DP-04).
- Rediseñar el flujo SSE y el fallback único → **Fase 06** (D-05, D-10, D-11, D-31).
- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Unificar las tres fachadas de logging → **Fase 08** (D-12).
- El módulo `mcp-client` casi vacío → **Fase 08** (D-32).

---

## 5. Resultado

> **Detalle completo:** [`RESULTADO-FASE-05.md`](../resultados/RESULTADO-FASE-05.md)

- **Fecha de cierre:** 2026-08-29
- **DP-03 resuelta como:** a → **B-01 = C** (se pregunta a Azure DevOps) · b → **B-02 = B**
  (configurable, 10 por defecto, + tope de concurrencia 2) · c → **B-03 = A por pasos**
  (dominio inmutable + DTOs en `reactive-web`) · **B-04 = B** (excepción nominal al MCP)
- **Bloqueante surgido en ejecución:** **S-2**, resuelto autorizando 2 líneas de *tipos* en el test
  de caracterización. Sin aserciones tocadas.
- **Tests totales / fallidos:** **219 / 0** (desde 170)
- **Reglas de negocio en `entry-points`:** 3 → **0**
- **Mutaciones in-place en cadenas reactivas:** 3 → **0**
- **Cálculos del año por calendario:** 2 → **0**
- **ArchUnit Rule_2.2:** 3 → **0**
- **Cobertura `domain/model` / `domain/usecase`:** **94,8 % / 97,6 %**
- **Hallazgos no previstos:** el fallo del año no se podía cerrar sin tocar el MCP · los prompts
  nunca pidieron rutas · `updateOriginalItems` aplicaba la última coincidencia ante ids repetidos
- **Deudas nuevas detectadas:** **D-36** (adaptador REST del MCP sin pruebas) · **D-37** (medir el
  repliegue) · **D-38** (medir lote y concurrencia) · **D-39** (Rule_2.2 preexistente del MCP)


