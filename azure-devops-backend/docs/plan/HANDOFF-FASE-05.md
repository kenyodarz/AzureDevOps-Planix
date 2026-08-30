# HANDOFF — Arranque de la Fase 05

> **Uso:** copia el bloque de §1 como **primer mensaje** de una sesión nueva. El resto de este
> documento es lo que esa sesión leerá; no hace falta pegarlo.
> **Generado:** 2026-08-29, al cerrar la Fase 04.

---

## 1. Prompt de arranque (copiar tal cual)

```text
Vas a ejecutar la FASE 05 del plan de refactorización del BFF.

Trabajo en C:\Users\minaj\Work\GitHub\Labs\AzureDevOps (monorepo, Windows, shell PowerShell).
El módulo objetivo es azure-devops-backend.

ANTES DE TOCAR NADA, lee en este orden y NADA MÁS:
  1. azure-devops-backend/docs/plan/HANDOFF-FASE-05.md   ← empieza aquí, es tu guía
  2. azure-devops-backend/docs/plan/BLOQUEANTES-FASE-05.md
  3. azure-devops-backend/docs/fases/fase-05.md

El HANDOFF trae los hechos ya verificados en código (rutas, líneas, números, trampas
conocidas). Está escrito para que NO tengas que redescubrirlos. Respétalo: cada
verificación que repitas sin necesidad es contexto que no te sobrará después.

La Fase 05 está BLOQUEADA por cuatro decisiones (B-01 a B-04). Tu primer mensaje
debe ser pedírmelas, resumidas en no más de 15 líneas y con tu recomendación.
No escribas una sola línea de código hasta que yo responda.

Cumple sin excepción las reglas de §5 del HANDOFF y las de
.github/copilot-instructions.md (Política de No-Asunción y COMMIT_RULES.md).
```

---

## 2. Dónde estamos

|                         |                                                                   |
|-------------------------|-------------------------------------------------------------------|
| **Build**               | 🟢 verde — **170 pruebas, 0 fallos**                              |
| **Última fase cerrada** | 04 → [`RESULTADO-FASE-04.md`](../resultados/RESULTADO-FASE-04.md) |
| **Fase a ejecutar**     | 05 — Dominio del dashboard                                        |
| **Deudas objetivo**     | D-06, D-07, D-09, D-13 + el resto de D-19 (el año)                |
| **Estado**              | 🔴 **BLOQUEADA** por B-01, B-02, B-03, B-04                       |

Lo que dejó la Fase 04, por si ayuda a orientarse:

- `DevOpsDashboardUseCase`: 430 → **225 líneas**. Sin prompts ni mock dentro (D-08 saldada).
- Los prompts viven en `applications/app-service/src/main/resources/prompts/*.txt`, tras
  `PromptTemplatePort`. El mock, en `resources/mock/dashboard.json`, tras `DashboardFallbackPort`.
- `DashboardMarkdownReport` (nueva, package-private) compone el reporte. La **escritura** sigue en
  el caso de uso; sacarla es de la Fase 06.
- `Handler` tiene `AUDIT_BATCH_SIZE = 10` y `MOCK_STREAM_DELAY = 800 ms`, **valores sin cambiar**.

---

## 3. Hechos ya verificados — **no los vuelvas a investigar**

Todo lo de esta sección está comprobado en código el 2026-08-29, con ruta y línea. Es el ahorro de
contexto más grande que tienes disponible.

### 3.1 El fallo del año está en el MCP, no en el BFF

`azure-devops-mcp/infrastructure/entry-points/mcp-server/src/main/java/co/com/bancolombia/mcp/tools/AzureDevOpsTools.java:168-179`

`resolveIterationPath` es **el mismo código, carácter por carácter**, que
`DevOpsDashboardUseCase.resolveSprint`, incluido `LocalDate.now().getYear()`. Lo mismo pasa con
`resolveAreaPath` (161-166) frente a `resolveCell`.

La lógica está **triplicada**: BFF + MCP + el propio prompt. Y como el BFF ya envía el path
completo, el MCP hace `startsWith(project)` → verdadero → lo devuelve intacto: **el trabajo del BFF
es redundante**.

> **Consecuencia:** arreglar el año dentro del BFF **no arregla nada**.

### 3.2 El patrón correcto ya existe en el MCP, pero solo para el área

`AzureDevOpsTools.java:135-142` — para el `AreaPath`, el MCP llama a
`getTeamFieldValuesUseCase.getTeamFieldValues(...)` y **solo concatena en el `onErrorResume`**, como
fallback. Para el `IterationPath` nadie hizo lo equivalente.

### 3.3 El frontend no sabe nada de iteraciones

`azure-devops-frontend/src/app/features/devops-agent/components/planning-dashboard/planning-dashboard.component.ts:34-62`
— célula y sprint son **dos inputs de texto libre**. No hay selector, ni lista, ni llamada que
enumere sprints. En el MCP **no existe** ninguna herramienta `listIterations` / `getTeamIterations`.

> Esto **descarta** la opción «que el front mande el `IterationPath` completo».

### 3.4 Herramientas que expone el MCP

`getWorkItem` · `createWorkItem` · `updateWorkItem` · `listWorkItemsByTeamAndSprint` ·
`getWorkItemsBatch` · `checkHealth` · `getServerInfo`. **Ninguna** resuelve o lista iteraciones.

### 3.5 El `10` y la concurrencia

- `Handler.java:46` — `AUDIT_BATCH_SIZE = 10`. **No se encontró ningún límite técnico que lo
  justifique**: ni `maxWorkItems`, ni tamaño de página, ni límite de tokens en agente o MCP.
- `Handler.java:224` — `flatMapSequential(...)` **sin límite de concurrencia**: ordena la salida
  pero ejecuta en paralelo (por defecto 256). Con 100 ítems son **10 llamadas simultáneas** al LLM.
- `application.yaml:5` — `agent.timeout: ${AGENT_TIMEOUT:120s}`, **por llamada**.
- `DashboardRoutesCharacterizationTest:229-250` fija que 25 ítems → lotes de **10, 10 y 5**.

### 3.6 Dónde vive cada cosa

| Qué                 | Ruta (desde `azure-devops-backend/`)                                                                         |
|---------------------|--------------------------------------------------------------------------------------------------------------|
| Caso de uso         | `domain/usecase/src/main/java/co/com/bancolombia/usecase/dashboard/DevOpsDashboardUseCase.java` (225 líneas) |
| Reporte Markdown    | `domain/usecase/.../dashboard/DashboardMarkdownReport.java`                                                  |
| Entry-point         | `infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/Handler.java` (422 líneas)    |
| Modelos del tablero | `domain/model/src/main/java/co/com/bancolombia/model/dashboard/`                                             |
| Cableado            | `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`                       |
| Adaptadores Fase 04 | `applications/app-service/.../config/prompt/` y `.../config/dashboard/`                                      |

### 3.7 Reglas de negocio hoy alojadas en el `Handler` (D-06, D-07)

| Método                | Línea aprox. | Qué hace que no debería                                                                        |
|-----------------------|-------------:|------------------------------------------------------------------------------------------------|
| `partitionItems`      |          295 | Parte en lotes de 10 — regla de negocio en un adaptador HTTP                                   |
| `recalculateMetrics`  |         ~357 | Calcula `avgQualityScore` (media entera) y `undocumentedCount`                                 |
| `updateOriginalItems` |         ~342 | **Muta los modelos in-place** dentro de la cadena reactiva                                     |
| `sharedData[0]`       |          196 | `final DashboardResponse[] sharedData = new DashboardResponse[1]` — variable global disfrazada |

---

## 4. ⚠️ Trampas conocidas — te ahorran un ciclo de build cada una

1. **`DashboardRoutesCharacterizationTest` es INTOCABLE.** 12 pruebas. El propio fichero lo dice:
   *«No modificar estas pruebas para hacerlas pasar.»* Son la definición operativa de que la
   refactorización no cambió comportamiento.
2. **Esa prueba mockea `devOpsDashboardUseCase.getMockDashboardData()`** (línea 409). Si cambias la
   firma de ese método, **no compila**. Ya provocó una contradicción entre T-04 y T-07 en la Fase
    04.
3. **La consola de PowerShell destroza los acentos.** Verás `Mtrica` en vez de `Métrica`. Los
   ficheros están bien, en UTF-8. **No “corrijas” nada basándote en la salida del terminal**, y no
   uses `Select-String` sobre texto con tildes para construir un `replace`.
4. **ArchUnit ya emite warnings preexistentes**, no los introdujiste tú: *Rule_2.2 violated (3
   times)*
   —los tres `...Response` del dominio— y *Rule_2.7 violated (6 times)*. Son `checkWithWarning`, no
   rompen el build. **Rule_2.2 es justo la que B-03 viene a resolver.**
5. **`app-service` tiene `processResources.dependsOn copyFrontendToStatic`**: compilar ese módulo
   dispara un build del frontend con `pnpm`. Es lento y depende de la red. Si vas a iterar rápido,
   lanza pruebas por módulo: `.\gradlew :usecase:test`, `.\gradlew :reactive-web:test`.
6. **`jacocoTestReport` depende de `pitest`.** Pedir cobertura ejecuta análisis de mutación: ~1 min.
   No lo pidas en cada iteración, solo al cerrar.
7. **Prohibido `block()` y `subscribe()` manual** en `src/main` (DP-08). Hoy hay **cero**; que siga
   así.

### Comandos que funcionan (PowerShell)

```powershell
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-backend

# Suite completa
.\gradlew test --console=plain 2>&1 | Select-String -Pattern '^BUILD'

# Contar pruebas y fallos  (el resumen de Gradle no los da)
$t=0;$f=0;$e=0
Get-ChildItem -Recurse -Filter TEST-*.xml | Where-Object { $_.FullName -match 'test-results\\test' } |
  ForEach-Object { [xml]$x=Get-Content $_.FullName; $t+=[int]$x.testsuite.tests
                   $f+=[int]$x.testsuite.failures; $e+=[int]$x.testsuite.errors }
"TESTS=$t FAIL=$f ERR=$e"      # referencia al cerrar la Fase 04: TESTS=170 FAIL=0 ERR=0

# Cobertura mezclada  (OJO: --no-configuration-cache es obligatorio)
.\gradlew jacocoMergedReport --no-configuration-cache --console=plain
# Sale en: build\reports\jacocoMergedReport\jacocoMergedReport.xml
# Referencia Fase 04:  usecase 96,8 %  ·  model 82,2 %
# NO compares esa cifra con la del jacoco.xml por módulo: son medidas distintas (H-5 de la Fase 04)
```

---

## 5. Reglas de trabajo — obligatorias

### Economía de contexto

1. **Para cualquier búsqueda que necesite más de 3 llamadas, usa el subagente `Search`.** Trabaja en
   contexto aislado y te devuelve solo el resultado.
2. **No vuelques ficheros enteros** en el chat. Lee rangos (`offset` + `limit`).
3. **No repitas las verificaciones de §3.** Ya están hechas.
4. Para reescribir un fichero grande: bórralo y créalo. `replace_string_in_file` con bloques enormes
   falla y cuesta dos intentos.
5. **No pegues la salida cruda de Gradle.** Fíltrala con `Select-String`.
6. Si el contexto se te acaba igual: cierra lo que tengas, escribe el resultado parcial en
   `docs/resultados/` y genera un handoff nuevo como éste.

### Ingeniería

7. **Política de No-Asunción.** Ante cualquier duda contractual, visual, de nombres o técnica: para
   y pregunta. No inventes.
8. **Dominio puro**: `domain/model` y `domain/usecase` sin anotaciones de Spring ni dependencias
   técnicas (`rules/spring-rules.md`).
9. **Si dos instrucciones de la fase se contradicen, no elijas en silencio.** Dilo, propón la salida
   y déjala escrita en el documento de la fase. Pasó en la Fase 04 con T-04 vs T-07.
10. **Verde tras cada paso.** Nada de acumular seis cambios y compilar al final.
11. **Commits** según `COMMIT_RULES.md`:
    `tipo(scope_snake_case): descripción en español en minúscula`.
12. **Cero secretos** en código, configuración o documentación.

---

## 6. Qué tiene que hacer la Fase 05

### Paso 0 — desbloquear (antes de cualquier código)

Pedir al usuario **B-01 a B-04**, con recomendación. Están razonados en
[`BLOQUEANTES-FASE-05.md`](BLOQUEANTES-FASE-05.md):

| #        | Pregunta                                                                    | Recomendación del análisis                              |
|----------|-----------------------------------------------------------------------------|---------------------------------------------------------|
| **B-04** | ¿Levantar `plan-maestro.md` §10, acotado al `resolveIterationPath` del MCP? | **Sí, nominal** ← decidir primero                       |
| **B-01** | ¿De dónde sale el año del `IterationPath`?                                  | **C — preguntar a Azure DevOps** (requiere B-04 = sí)   |
| **B-02** | ¿Tamaño de lote y concurrencia?                                             | **Configurable, 10 por defecto + tope de concurrencia** |
| **B-03** | ¿Qué pasa con los sufijos `...Response`?                                    | **Tipos inmutables, por pasos**                         |

**B-04 y B-01 se condicionan entre sí; B-02 y B-03 no bloquean a los demás.**

### Pasos 1..n — según lo decidido

Borrador en `fase-05.md` §3. Resumen:

- **T-01** `AreaPath` / `IterationPath` como Value Objects inmutables y autovalidados (D-09).
- **T-02** El año deja de calcularse, según B-01 (D-19).
- **T-03** La partición en lotes se muda al dominio (D-07).
- **T-04** El recálculo de métricas se muda al dominio (D-07).
- **T-05** `BacklogAudit` inmutable: fin de las mutaciones in-place y del array `sharedData` (D-06).
- **T-06** Sufijos técnicos fuera del dominio, según B-03 (D-13).

### Criterios de cierre

- [ ] `gradlew test` verde, **≥ 170 pruebas**, 0 fallos.
- [ ] Las **12** pruebas de `DashboardRoutesCharacterizationTest`, verdes **y sin modificar** —salvo
  lo que B-02 autorice **expresamente y por escrito**.
- [ ] Cero `block()` / `subscribe()` manual en `src/main`.
- [ ] Cobertura `domain/usecase` ≥ 90 %; `domain/model` **no baja** de 82,2 %.
- [ ] Reglas de negocio en `entry-points`: 3 → 0.
- [ ] Mutaciones in-place en cadenas reactivas: 3 → 0.
- [ ] ArchUnit *Rule_2.2* sin violaciones, si B-03 se resolvió como se recomienda.
- [ ] `docs/resultados/RESULTADO-FASE-05.md` con la misma estructura que el de la Fase 04:
  objetivo · qué se construyó · decisiones · métricas · comandos · **desviaciones** · checklist ·
  hallazgos · estado.
- [ ] `plan-maestro.md` actualizado: deudas, §6 (columna «Actual (Fase 05)»), §9 (bitácora), DP-03 a
  «Resueltas».
- [ ] `docs/fases/fase-06.md` generada, con sus bloqueantes si los tiene.

---

## 7. Lo que NO entra en la Fase 05

- Sacar la escritura de ficheros del caso de uso → **Fase 06** (DP-04).
- Rediseñar el flujo SSE y unificar el fallback → **Fase 06** (D-05, D-10, D-11, D-31).
- Migrar los prompts al MCP / agente → **D-35**, plan propio. **No entra aunque B-04 se levante**:
  la excepción propuesta es nominal y solo cubre `resolveIterationPath`.
- Unificar las tres fachadas de logging → **Fase 08** (D-12).
- El módulo `mcp-client` casi vacío → **Fase 08** (D-32).
- Cambiar el proveedor de LLM o el protocolo MCP → fuera del plan.

