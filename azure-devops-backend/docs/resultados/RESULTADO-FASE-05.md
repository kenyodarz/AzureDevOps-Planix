# RESULTADO — Fase 05

> **Estado:** ✅ **CERRADA** · **Fecha:** 2026-08-29
> **Fase anterior:** [`RESULTADO-FASE-04.md`](RESULTADO-FASE-04.md) · **Siguiente:** [
`fase-06.md`](../fases/fase-06.md)
> **Bloqueantes:** [`BLOQUEANTES-FASE-05.md`](../plan/BLOQUEANTES-FASE-05.md) — B-01 a B-04, más
> **S-2** surgido en ejecución
> **Deudas saldadas:** **D-06, D-07, D-09, D-13, D-19**

---

## 1. Objetivo

Dar dominio al tablero. Al empezar, el flujo de auditoría del backlog no tenía modelo: tenía un caso
de uso que hablaba con el agente y un entry-point HTTP que **pensaba**. Partía en lotes, calculaba
métricas, mutaba los modelos in situ dentro de una cadena reactiva y guardaba el estado en un array
de un elemento.

---

## 2. Decisiones

### 2.1 Bloqueantes de arranque (B-01 a B-04)

Aprobados los cuatro el 2026-08-29 tal y como se recomendaban:

| #        | Decisión              | Resuelta como                                                |
|----------|-----------------------|--------------------------------------------------------------|
| **B-04** | Alcance del MCP       | **Levantada de forma acotada y nominal** (texto en §2.2)     |
| **B-01** | Origen del año        | **C — se le pregunta a Azure DevOps**                        |
| **B-02** | Lote y concurrencia   | **Configurable, 10 por defecto, más tope de concurrencia 2** |
| **B-03** | Sufijos `...Response` | **A por pasos** — dominio inmutable + DTOs en `reactive-web` |

### 2.2 Excepción autorizada a `plan-maestro.md` §10

> *Se autoriza modificar `azure-devops-mcp` **exclusivamente** para añadir la resolución de
> `IterationPath` por consulta a Azure DevOps, simétrica a la que ya existe para `AreaPath`,
> conservando la concatenación actual como fallback. Cualquier otro cambio en ese repositorio sigue
> fuera de alcance.*

### 2.3 S-2 — contradicción detectada en ejecución, resuelta por el usuario

`DashboardRoutesCharacterizationTest` es intocable (T-07) **y a la vez anclaba el nombre y el
paquete** de los tipos de dominio. B-03 (A) exigía moverlos, con lo que ese test no compilaba: dos
instrucciones de la misma fase se contradecían. Conforme a la regla §5.9 del HANDOFF se detuvo el
trabajo y se consultó.

**Resolución: S-2, autorizada por escrito.** Se cambiaron **dos líneas de tipos**:

| Línea | Antes                                                          | Después                                                          |
|-------|----------------------------------------------------------------|------------------------------------------------------------------|
| 17    | `import co.com.bancolombia.model.dashboard.DashboardResponse;` | `import co.com.bancolombia.api.dto.dashboard.DashboardResponse;` |
| 333   | `any(DashboardResponse.class)`                                 | `any(BacklogAudit.class)`                                        |

**Ninguna aserción, ningún dato de entrada y ningún comportamiento esperado se tocaron.** La
autorización queda registrada también en el javadoc del propio test.

> **Por qué era inevitable.** `Handler` hace `jsonMapper.readValue(..., DashboardResponse.class)`.
> Sin setters, Jackson necesita `@JsonDeserialize` o `@JsonCreator` **dentro de `domain/model`**, lo
> que rompe la pureza del dominio. Mientras esos tipos vivieran allí no podían ser inmutables, y sin
> inmutabilidad **D-06 no se podía saldar**. El anclaje además condenaba a D-13 a no cerrarse nunca,
> ni en esta fase ni en ninguna posterior.

---

## 3. Qué se construyó

### T-01 · Value Objects (D-09)

`domain/model/.../model/dashboard/`

| Clase        | Qué garantiza                                                          |
|--------------|------------------------------------------------------------------------|
| `TeamName`   | `record` inmutable y autovalidado: rechaza el blanco, recorta espacios |
| `SprintName` | Igual, más `isNumbered()` para la convención `"Sprint N"`              |

> **Desviación respecto al borrador de `fase-05.md` §3.** El borrador pedía `AreaPath` e
> `IterationPath`. Al resolverse B-01 como C, el BFF **deja de manejar rutas de Azure DevOps**, así
> que un VO llamado `AreaPath` dentro del BFF no representaría nada real. Los VO correctos son los
> **nombres**, que es lo único que el BFF conoce y valida. Es consecuencia directa de la decisión:
> `BLOQUEANTES-FASE-05.md` §2 ya lo enunciaba.

### T-02 · El año deja de calcularse (D-19) — en **los dos** repositorios

**BFF:** se eliminaron `resolveCell`, `resolveSprint`, `prefixed`, `PATH_SEPARATOR` y
`SPRINT_NUMBER_PATTERN`. Con ellos se fue el `LocalDate.now().getYear()`.

**MCP** (al amparo de B-04), completando la simetría que llevaba meses a medias:

| Fichero                                                     | Novedad                                                                         |
|-------------------------------------------------------------|---------------------------------------------------------------------------------|
| `model/iteration/TeamIteration.java`                        | Modelo con `id`, `name` y **`path`**: el `IterationPath` real                   |
| `model/iteration/gateways/GetTeamIterationsRepository.java` | Puerto, gemelo de `GetTeamFieldValuesRepository`                                |
| `usecase/iteration/GetTeamIterationsUseCase.java`           | `resolveIterationPath(...)`, con comparación laxa por nombre                    |
| `consumer/TeamIterationsDTO`, `TeamIterationDTO`            | DTOs de `_apis/work/teamsettings/iterations`                                    |
| `consumer/RestConsumer.java`                                | Implementa el nuevo puerto                                                      |
| `config/UseCasesConfig.java`                                | Cablea el caso de uso                                                           |
| `mcp/tools/AzureDevOpsTools.java`                           | `resolveIteration(...)`: pregunta primero, concatena solo en el `onErrorResume` |

**Por qué el MCP era obligatorio, no opcional.** Al quitar la resolución del BFF, el MCP pasa a
recibir `"Sprint 247"` en vez de la ruta montada. Su `resolveIterationPath` — **el mismo código
carácter por carácter** que el del BFF, incluido el `LocalDate.now().getYear()`— habría vuelto a
fabricar el año. Cerrar solo el lado BFF no habría arreglado nada: habría movido el fallo de sitio.

El repliegue por concatenación se conserva intacto: si Azure DevOps no responde, el comportamiento
es el de antes. La diferencia es que ahora **deja rastro en el log** en vez de fallar en silencio.

### T-03 y T-04 · Las reglas de negocio bajan al dominio (D-07)

| Antes, en `Handler`                                   | Ahora, en `domain/model`                |
|-------------------------------------------------------|-----------------------------------------|
| `partitionItems`, con el 10 escrito a mano            | `BacklogAudit.partition(int)`           |
| `recalculateMetrics`                                  | `BacklogMetrics.recalculatedFrom(List)` |
| `updateOriginalItems`                                 | `BacklogAudit.withUpdates(List)`        |
| Los setters que fabricaban el estado inicial del mock | `BacklogAudit.unaudited()`              |

Se podían probar únicamente levantando un servidor web; por eso las únicas pruebas que las cubrían
estaban en el test de caracterización de las rutas. Ahora se ejercitan con objetos planos.

**Además (B-02):**

- `audit.batch-size` configurable, **10 por defecto** — el valor observable no cambia.
- `audit.concurrency` con **tope 2**. Antes no había ninguno: `flatMapSequential` ordena la salida
  pero ejecuta en paralelo con concurrencia 256, así que un sprint de 100 historias lanzaba **diez
  llamadas simultáneas** al agente, cada una con 120 s de timeout. **El riesgo nunca fue el tamaño
  del lote, sino esto.**

### T-05 · Fin de las mutaciones in-place (D-06)

`BacklogAudit`, `BacklogMetrics`, `StoryQuality` y `StoryUpdate` son `record`s inmutables. Aplicar
una auditoría no modifica nada: devuelve una auditoría nueva.

El `final DashboardResponse[] sharedData = new DashboardResponse[1]` —una variable global disfrazada
de array— pasa a ser un `AtomicReference<BacklogAudit>`. La diferencia de fondo no es el tipo: es
que ahí **nunca se modifica una auditoría, se reemplaza la referencia**. Con varios lotes
resolviéndose a la vez, `updateAndGet` garantiza además que ninguna actualización se pierda, cosa
que la mutación anterior no aseguraba.

**Corrección de comportamiento heredada:** el código anterior anidaba dos bucles —cada actualización
contra cada historia—, de modo que ante identificadores repetidos ganaba la última. Ahora se indexa
por id y **gana la primera**, que es lo que el usuario espera cuando el agente responde dos veces
por el mismo ítem. Queda cubierto por prueba.

### T-06 · Sufijos técnicos fuera del dominio (D-13)

`DashboardResponse`, `DashboardMetricsResponse` y `DashboardStoryItemResponse` se mudan a
`infrastructure/entry-points/reactive-web/.../api/dto/dashboard/`. Allí el sufijo `Response` no
sobra: describe exactamente lo que son. Conservan setters porque **Jackson los necesita**, y ese es
justo el motivo por el que no podían vivir en el dominio.

`DashboardDtoMapper` es la frontera nueva. La Fase 06 la reducirá a un único punto al rediseñar el
flujo SSE.

---

## 4. Métricas

| Métrica                                             |       Fase 04 |                                                                    **Fase 05** |
|-----------------------------------------------------|--------------:|-------------------------------------------------------------------------------:|
| Pruebas backend / fallos                            |       170 / 0 |                                                                    **219 / 0** |
| Pruebas MCP                                         |             — | **verde** (+2 en `AzureDevOpsToolsTest`, +9 en `GetTeamIterationsUseCaseTest`) |
| Reglas de negocio en `entry-points`                 |             3 |                                                                          **0** |
| Mutaciones in-place en cadenas reactivas            |             3 |                                                                          **0** |
| Cálculos del año por calendario en la ruta caliente | 2 (BFF + MCP) |                                                                          **0** |
| Tope de llamadas concurrentes al agente             | ninguno (256) |                                                                          **2** |
| ArchUnit *Rule_2.2* (sufijos en el dominio)         |             3 |                                                                          **0** |
| ArchUnit *Rule_2.7*                                 |             6 |                                                          **6** (sin regresión) |
| `block()` / `subscribe()` manual en `src/main`      |             0 |                                                                          **0** |
| Cobertura `domain/model`                            |        82,2 % |                                                                     **94,8 %** |
| Cobertura `domain/usecase`                          |        96,8 % |                                                                     **97,6 %** |
| Cobertura `model/dashboard` · `api/dto/dashboard`   |             — |                                                              **100 % · 100 %** |
| `Handler` (líneas)                                  |           422 |                                                                        **382** |
| `DevOpsDashboardUseCase` (líneas)                   |           225 |                                                                        **205** |

---

## 5. Comandos

```powershell
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-backend
.\gradlew test --console=plain 2>&1 | Select-String -Pattern '^BUILD|FAILED|violated'

# Conteo de pruebas
$t=0;$f=0;$e=0
Get-ChildItem -Recurse -Filter TEST-*.xml | Where-Object { $_.FullName -match 'test-results\\test' } |
  ForEach-Object { [xml]$x=Get-Content $_.FullName; $t+=[int]$x.testsuite.tests
                   $f+=[int]$x.testsuite.failures; $e+=[int]$x.testsuite.errors }
"TESTS=$t FAIL=$f ERR=$e"     # → TESTS=219 FAIL=0 ERR=0

# Cobertura  (--no-configuration-cache es obligatorio)
.\gradlew jacocoMergedReport --no-configuration-cache --console=plain
[xml]$r = Get-Content build\reports\jacocoMergedReport\jacocoMergedReport.xml
foreach ($p in @('co/com/bancolombia/model','co/com/bancolombia/usecase')) {
  $cov=0;$mis=0
  $r.report.package | Where-Object { $_.name -like "$p*" } | ForEach-Object {
    $c = $_.counter | Where-Object { $_.type -eq 'INSTRUCTION' }
    $cov+=[int]$c.covered; $mis+=[int]$c.missed }
  "{0,-32} {1,6:N1} %" -f $p, (100*$cov/($cov+$mis)) }

cd ..\azure-devops-mcp
.\gradlew test --console=plain 2>&1 | Select-String -Pattern '^BUILD|FAILED'
```

---

## 6. Desviaciones respecto al plan

1. **T-01 cambió de objeto.** `AreaPath`/`IterationPath` → `TeamName`/`SprintName`. Razonado en §3.
2. **Se modificó el test «intocable»**, en dos líneas de tipos y con autorización expresa (S-2,
   §2.3).
3. **Se tocó `azure-devops-mcp`**, al amparo de la excepción nominal de B-04 (§2.2). El alcance real
   coincide exactamente con el autorizado.
4. **Se corrigió `Rule_2.7` sobre la marcha.** Las dos propiedades nuevas se introdujeron primero
   como campos `@Value` no finales, lo que subió la regla de 6 a 8 violaciones. Se pasaron a
   inyección por constructor y volvió a 6.

---

## 7. Hallazgos

- **H-1.** El fallo del año **no se podía cerrar sin tocar el MCP**, ni siquiera parcialmente.
  Confirma que B-04 era la llave, tal como anticipaba el análisis.
- **H-2.** Los prompts **nunca pidieron rutas**: dicen *«la célula/equipo»* y *«el
  sprint/iteración»*. Enviar el nombre corto es **más** coherente con su texto que enviar la ruta.
  No hubo que tocar ni una coma y `PromptTemplateEquivalenceTest` siguió verde sin intervención.
- **H-3.** `AzureDevOpsToolsTest` no tenía **ningún** caso para el `IterationPath` dinámico —solo
  para el `AreaPath`—, lo que explica que la asimetría pasara inadvertida tanto tiempo.
- **H-4.** El MCP arrastra una violación **preexistente** de *Rule_2.2* (1 vez), ajena a esta fase.
- **H-5.** El doble bucle de `updateOriginalItems` no solo era cuadrático: ante ids repetidos
  aplicaba la **última** coincidencia. Nadie lo había advertido porque no había prueba que lo
  cubriera.

---

## 8. Deudas nuevas

| #        | Deuda                                                                                                                                                                                | Dónde                  |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| **D-36** | `RestConsumerTest` no cubre `getTeamFieldValues` **ni** `getTeamIterations`: las dos consultas de resolución del MCP no tienen prueba de integración                                 | `azure-devops-mcp`     |
| **D-37** | El repliegue por concatenación del MCP sigue produciendo una ruta con el año del calendario. Correcto como red de seguridad, pero convendría **medir con qué frecuencia se dispara** | `azure-devops-mcp`     |
| **D-38** | El `10` del lote y el `2` de concurrencia siguen sin respaldo empírico. Ahora se ajustan sin recompilar, pero **falta medir** el coste real de un lote                               | `azure-devops-backend` |
| **D-39** | La violación *Rule_2.2* preexistente del MCP (1 vez) sigue sin atender                                                                                                               | `azure-devops-mcp`     |

---

## 9. Estado del control de versiones

⚠️ **Nada se ha confirmado, por decisión expresa del usuario.** El árbol acumula el trabajo de las
Fases 02 a 05. El plan acordado es **arreglar primero los issues de SonarQube y confirmar después,
por fases**. Los mensajes deberán seguir `COMMIT_RULES.md`.

---

## 10. Checklist de cierre

- [x] `gradlew test` verde, ≥ 170 pruebas, 0 fallos → **219 / 0**
- [x] Las 12 pruebas de `DashboardRoutesCharacterizationTest`, verdes; solo 2 líneas de **tipos**
  modificadas, con autorización expresa (S-2)
- [x] Cero `block()` / `subscribe()` manual en `src/main`
- [x] Cobertura `domain/usecase` ≥ 90 % → **97,6 %**
- [x] Cobertura `domain/model` no baja de 82,2 % → **94,8 %**
- [x] Reglas de negocio en `entry-points`: 3 → **0**
- [x] Mutaciones in-place en cadenas reactivas: 3 → **0**
- [x] ArchUnit *Rule_2.2* sin violaciones en el backend
- [x] `RESULTADO-FASE-05.md` con la estructura acordada
- [x] `plan-maestro.md` actualizado
- [x] `docs/fases/fase-06.md` generada, con sus bloqueantes
- [x] `docs/plan/HANDOFF-FASE-06.md` generado

