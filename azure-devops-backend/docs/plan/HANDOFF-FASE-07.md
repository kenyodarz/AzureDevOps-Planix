# HANDOFF — Arranque de la Fase 07

> **Uso:** copia el bloque de §1 como **primer mensaje** de una sesión nueva. El resto de este
> documento es lo que esa sesión leerá; no hace falta pegarlo.
> **Generado:** 2026-08-29, al cerrar la Fase 06.

---

## 1. Prompt de arranque (copiar tal cual)

```text
Vas a ejecutar la FASE 07 del plan de refactorización del BFF.

Trabajo en C:\Users\minaj\Work\GitHub\Labs\AzureDevOps (monorepo, Windows, shell PowerShell).
El módulo objetivo es azure-devops-backend.

ANTES DE TOCAR NADA, lee en este orden y NADA MÁS:
  1. azure-devops-backend/docs/plan/HANDOFF-FASE-07.md   ← empieza aquí, es tu guía
  2. azure-devops-backend/docs/fases/fase-07.md
  3. azure-devops-backend/docs/resultados/RESULTADO-FASE-06.md  (solo §3, §6 y §8)

El HANDOFF trae los hechos ya verificados en código (rutas, líneas, números, trampas
conocidas). Está escrito para que NO tengas que redescubrirlos. Respétalo: cada
verificación que repitas sin necesidad es contexto que no te sobrará después.

La Fase 07 está BLOQUEADA por DP-05 y DP-06, y por tres bloqueantes menores
(B-08, B-09, B-10). Tu primer mensaje debe ser pedírmelos, resumidos en no más de
15 líneas y con tu recomendación. No escribas una sola línea de código hasta que
yo responda.

IMPORTANTE: hay trabajo SIN CONFIRMAR en git desde la Fase 02. No hagas commit de
nada salvo que yo te lo pida expresamente.

Cumple sin excepción las reglas de §5 del HANDOFF y las de
.github/copilot-instructions.md (Política de No-Asunción y COMMIT_RULES.md).
```

---

## 2. Dónde estamos

|                         |                                                                   |
|-------------------------|-------------------------------------------------------------------|
| **Build backend**       | 🟢 verde — **227 pruebas, 0 fallos**                              |
| **Última fase cerrada** | 06 → [`RESULTADO-FASE-06.md`](../resultados/RESULTADO-FASE-06.md) |
| **Fase a ejecutar**     | 07 — Adaptadores y contrato HTTP                                  |
| **Deudas objetivo**     | D-14, D-15, D-16, D-20 (+ D-40, D-42)                             |
| **Estado**              | 🟠 **BLOQUEADA** por DP-05, DP-06, B-08, B-09, B-10               |

Lo que dejó la Fase 06:

- **El stream SSE ya no miente**: cualquier fallo sale como evento `ERROR` con `message` y la
  respuesta es 200 con cabecera `text/event-stream`. **El frontend todavía no lo escucha (D-40).**
- `Handler` 382 → **192 líneas**; `handleDevOpsDashboardStream` ~90 → **15**.
- `ReportStoragePort` + módulo `report-storage-file`: el dominio **no toca el disco**.
- Repliegue a mock: 4 puntos → **1**, apagado por defecto (`dashboard.mock-fallback.enabled`).
- ArchUnit *Rule_2.7*: 6 → **3**. *Rule_2.2* sigue en **0**.
- Cobertura: `domain/model` 94,8 % · `domain/usecase` 98,9 %.

---

## 3. Hechos ya verificados — **no los vuelvas a investigar**

### 3.1 ⚠️ Hay trabajo sin confirmar desde la Fase 02

`git status` muestra decenas de ficheros modificados, añadidos y borrados que **no son tuyos**: son
las Fases 02 a 06. El usuario decidió expresamente **arreglar primero los issues de SonarQube y
confirmar después, por fases**.

> **No hagas commit de nada** salvo petición explícita. Y si la hay, sigue `COMMIT_RULES.md`:
> `tipo(scope_snake_case): descripción en español en minúscula`.

### 3.2 El test de caracterización ya lleva tres excepciones registradas

`DashboardRoutesCharacterizationTest` sigue siendo **intocable**, pero su javadoc documenta **S-2**
(Fase 05), **S-3** y **S-4** (Fase 06). Ese es el precedente y el formato exacto a seguir si hace
falta otra: qué se cambió, por qué era inevitable y qué **no** se tocó.

### 3.3 Los dos criterios de error conviven en el mismo entry-point

- `TaskHandler` traduce cinco excepciones a cinco códigos HTTP.
- `Handler` devuelve **400 para todo**, en 6 bloques `onErrorResume` casi idénticos.

Eso es D-16, y por eso DP-06 pide el mapa antes de tocar nada: **cambiar un código es cambiar el
contrato** con el frontend.

### 3.4 Dónde vive cada cosa

| Qué                        | Ruta (desde `azure-devops-backend/`)                                                                      |
|----------------------------|-----------------------------------------------------------------------------------------------------------|
| Entry-point                | `infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/Handler.java` (192 líneas) |
| Entry-point de tareas      | `.../api/TaskHandler.java` — el que **sí** traduce errores                                                |
| Orquestador del stream     | `.../api/dashboard/DashboardStreamOrchestrator.java`                                                      |
| Ciclo de vida de la `Task` | `.../api/dashboard/DashboardTaskTracker.java`                                                             |
| DTOs + mapper del tablero  | `.../api/dto/dashboard/` — el precedente a seguir para D-15                                               |
| Adaptador de PostgreSQL    | `infrastructure/driven-adapters/task-store-inmemory/` ← **el nombre miente** (D-20)                       |
| Adaptador de pgvector      | `infrastructure/driven-adapters/pgvector-store/PgVectorPlanningAdapter.java` (D-14)                       |
| Almacén de reportes        | `infrastructure/driven-adapters/report-storage-file/` — módulo nuevo de la Fase 06                        |
| Cableado                   | `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`                    |
| Configuración              | `applications/app-service/src/main/resources/application.yaml`                                            |

### 3.5 La clase más larga ya no es el `Handler`

`A2APayloadMapper` (324 líneas) es la **única** clase productiva por encima de 300. Es material de
esta fase o de la 08, pero **mídelo antes de prometer nada**.

### 3.6 `ArchitectureTest` tiene una lista de rutas escrita a mano

`ArchitectureTest.exportIssues()` lleva las rutas absolutas de todos los módulos **codificadas en el
fichero**, que además está marcado como *«Please do not modify this file»*. El módulo nuevo de la
Fase 06 no está en esa lista y no pasó nada —solo afecta a dónde se escribe `issues.json`—, pero
**renombrar un módulo (D-20) sí obliga a decidir qué hacer con ella**. Está recogido en DP-05.

---

## 4. ⚠️ Trampas conocidas — te ahorran un ciclo de build cada una

1. **`DashboardRoutesCharacterizationTest` es INTOCABLE**, salvo lo que se autorice por escrito. Son
   12 pruebas y son la definición operativa de que no rompiste nada.
2. **Ese test mockea `devOpsDashboardUseCase` con `@MockitoBean`.** Si cambias la firma de un método
   público del caso de uso, no compila; y si el método devuelve `Mono`, **Mockito retorna `null`** y
   la cadena revienta con un NPE contra un doble. Eso costó la excepción S-3.
3. **Su `@ContextConfiguration` solo declara `RouterRest`, `Handler` y `TaskHandler`.** Añadir un
   colaborador **inyectado** al `Handler` rompe el contexto; por eso hoy se instancian con `new`
   (D-42). Un parámetro `@Value` con valor por defecto, en cambio, no molesta.
4. **Al extraer código reactivo, vigila el momento de la llamada.** Lo que dentro de un `Mono.defer`
   era perezoso, en un método suelto se vuelve **ansioso**: se ejecuta al ensamblar la cadena, no al
   recorrerla. Pasó en la Fase 06 con `taskStoreGateway.save(...)`.
5. **`Rule_2.7` se cuenta por CAMPO, no por clase.** Añadir un `@Value` no final a un bean que ya
   violaba la regla **sí** sube el contador. Usa siempre inyección por constructor.
6. **ArchUnit tiene warnings preexistentes**: *Rule_2.7* (3 veces) en el backend y *Rule_2.2* (1
   vez)
   en el MCP. *Rule_2.2* en el backend está a **0**: si vuelve a aparecer, es tuyo.
7. **`app-service` tiene `processResources.dependsOn copyFrontendToStatic`**: compilar o probar ese
   módulo dispara un build del frontend con `pnpm`. Para iterar rápido, lanza pruebas por módulo:
   `.\gradlew :usecase:test`, `.\gradlew :model:test`, `.\gradlew :reactive-web:test`.
8. **`jacocoTestReport` depende de `pitest`.** No lo pidas en cada iteración, solo al cerrar.
9. **Prohibido `block()` y `subscribe()` manual** en `src/main` (DP-08). Hoy hay **cero**.
10. **La consola de PowerShell destroza los acentos.** Verás `Mtrica` en vez de `Métrica`. Los
    ficheros están bien, en UTF-8. **No “corrijas” nada basándote en la salida del terminal.**

### Comandos que funcionan (PowerShell)

```powershell
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-backend

# Suite completa + recuento de violaciones de ArchUnit
.\gradlew test --console=plain 2>&1 | Select-String -Pattern '^BUILD|FAILED|was violated'

# Contar pruebas y fallos  (el resumen de Gradle no los da)
$t=0;$f=0;$e=0
Get-ChildItem -Recurse -Filter TEST-*.xml | Where-Object { $_.FullName -match 'test-results\\test' } |
  ForEach-Object { [xml]$x=Get-Content $_.FullName; $t+=[int]$x.testsuite.tests
                   $f+=[int]$x.testsuite.failures; $e+=[int]$x.testsuite.errors }
"TESTS=$t FAIL=$f ERR=$e"      # referencia al cerrar la Fase 06: TESTS=227 FAIL=0 ERR=0

# Cobertura  (OJO: --no-configuration-cache es obligatorio)
.\gradlew jacocoMergedReport --no-configuration-cache --console=plain
[xml]$r = Get-Content build\reports\jacocoMergedReport\jacocoMergedReport.xml
foreach ($p in @('co/com/bancolombia/model','co/com/bancolombia/usecase')) {
  $cov=0;$mis=0
  $r.report.package | Where-Object { $_.name -like "$p*" } | ForEach-Object {
    $c = $_.counter | Where-Object { $_.type -eq 'INSTRUCTION' }
    $cov+=[int]$c.covered; $mis+=[int]$c.missed }
  "{0,-32} {1,6:N1} %" -f $p, (100*$cov/($cov+$mis)) }
# Referencia Fase 06:  model 94,8 %  ·  usecase 98,9 %

# Comprobar que no se coló I/O bloqueante
Get-ChildItem -Recurse -Include *.java -Path .\domain,.\infrastructure,.\applications |
  Where-Object { $_.FullName -match '\\src\\main\\' } |
  Select-String -Pattern '\.block\(|\.subscribe\('
```

---

## 5. Reglas de trabajo — obligatorias

### Economía de contexto

1. **Para cualquier búsqueda que necesite más de 3 llamadas, usa el subagente `Search`.**
2. **No vuelques ficheros enteros** en el chat. Lee rangos (`offset` + `limit`).
3. **No repitas las verificaciones de §3.** Ya están hechas.
4. Para reescribir un fichero grande: bórralo y créalo.
5. **No pegues la salida cruda de Gradle.** Fíltrala con `Select-String`.
6. Si el contexto se te acaba: cierra lo que tengas, escribe el resultado parcial en
   `docs/resultados/` y genera un handoff nuevo como éste.

### Ingeniería

7. **Política de No-Asunción.** En esta fase aplica muy en concreto al **esquema de
   `planning_chunks`** (DP-05): no supongas que admite columnas nuevas, ni que nadie más lo lee.
8. **Dominio puro**: `domain/model` y `domain/usecase` sin anotaciones de Spring ni dependencias
   técnicas, **Jackson incluido**. Si un tipo necesita serializarse, es un DTO o una entidad y vive
   en su adaptador. D-14 es exactamente esa regla incumplida.
9. **Si dos instrucciones se contradicen, no elijas en silencio.** Dilo, propón la salida y déjala
   escrita. Pasó en la 04 (T-04 vs T-07), la 05 (B-03 vs T-07 → S-2) y la 06 (B-07 vs el test
   intocable → S-4).
10. **Verde tras cada paso.** Nada de acumular seis cambios y compilar al final.
11. **No hagas commit** salvo petición expresa; ver §3.1.
12. **Cero secretos** en código, configuración o documentación.
13. **Cambiar un código de respuesta HTTP es un cambio de contrato.** No lo hagas sin DP-06 y sin
    saber si el frontend entra en el alcance (B-08).

---

## 6. Qué tiene que hacer la Fase 07

### Paso 0 — desbloquear (antes de cualquier código)

Pedir al usuario **DP-05**, **DP-06**, **B-08**, **B-09** y **B-10**, con recomendación. Están
razonados en [`fase-07.md`](../fases/fase-07.md) §2.

| #         | Pregunta                                                                              | Recomendación                                                    |
|-----------|---------------------------------------------------------------------------------------|------------------------------------------------------------------|
| **DP-05** | Nombre del módulo `task-store-inmemory` y si `planning_chunks` admite columnas nuevas | `task-store-postgres`; **preguntar** por el esquema, no asumirlo |
| **DP-06** | Mapa de excepción → código HTTP                                                       | Partir del que ya usa `TaskHandler` y extenderlo                 |
| **B-08**  | ¿Se puede tocar el frontend (D-40 y códigos nuevos)?                                  | **Sí**, en tarea propia y explícita                              |
| **B-09**  | ¿Se autoriza reescribir pruebas de caracterización afectadas por D-16?                | **Sí**, con el formato de S-2/S-3/S-4                            |
| **B-10**  | ¿D-29/D-34 entran aquí?                                                               | **No**, van con D-35                                             |

### Pasos 1..n — según lo decidido

Borrador en `fase-07.md` §3: T-01 a T-06.

### Criterios de cierre

Los de `fase-07.md` §3. El que más se olvida: **un solo criterio de traducción de errores**, no dos.

---

## 7. Lo que NO entra en la Fase 07

- Unificar las tres fachadas de logging → **Fase 08** (D-12).
- El módulo `mcp-client` casi vacío → **Fase 08** (D-32).
- Limpiar `application.yaml` → **Fase 08** (D-04).
- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Contrato `a2a` compartido y versionado → **D-29/D-34**, ver B-10.
- Llevar los reportes a S3 → **D-41**, requiere confirmar acceso al bucket.
- Medir el coste real del lote y la concurrencia → **D-38**, requiere entorno con datos.
- Cambiar el proveedor de LLM o el protocolo MCP → fuera del plan.

