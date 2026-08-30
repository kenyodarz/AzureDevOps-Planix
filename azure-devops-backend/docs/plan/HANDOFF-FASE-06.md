# HANDOFF — Arranque de la Fase 06

> **Uso:** copia el bloque de §1 como **primer mensaje** de una sesión nueva. El resto de este
> documento es lo que esa sesión leerá; no hace falta pegarlo.
> **Generado:** 2026-08-29, al cerrar la Fase 05.

---

## 1. Prompt de arranque (copiar tal cual)

```text
Vas a ejecutar la FASE 06 del plan de refactorización del BFF.

Trabajo en C:\Users\minaj\Work\GitHub\Labs\AzureDevOps (monorepo, Windows, shell PowerShell).
El módulo objetivo es azure-devops-backend.

ANTES DE TOCAR NADA, lee en este orden y NADA MÁS:
  1. azure-devops-backend/docs/plan/HANDOFF-FASE-06.md   ← empieza aquí, es tu guía
  2. azure-devops-backend/docs/fases/fase-06.md
  3. azure-devops-backend/docs/resultados/RESULTADO-FASE-05.md  (solo §3, §6 y §8)

El HANDOFF trae los hechos ya verificados en código (rutas, líneas, números, trampas
conocidas). Está escrito para que NO tengas que redescubrirlos. Respétalo: cada
verificación que repitas sin necesidad es contexto que no te sobrará después.

La Fase 06 está BLOQUEADA por DP-04 y por tres bloqueantes menores (B-05, B-06, B-07).
Tu primer mensaje debe ser pedírmelos, resumidos en no más de 15 líneas y con tu
recomendación. No escribas una sola línea de código hasta que yo responda.

IMPORTANTE: hay trabajo SIN CONFIRMAR en git desde la Fase 02. No hagas commit de
nada salvo que yo te lo pida expresamente.

Cumple sin excepción las reglas de §5 del HANDOFF y las de
.github/copilot-instructions.md (Política de No-Asunción y COMMIT_RULES.md).
```

---

## 2. Dónde estamos

|                         |                                                                   |
|-------------------------|-------------------------------------------------------------------|
| **Build backend**       | 🟢 verde — **219 pruebas, 0 fallos**                              |
| **Build MCP**           | 🟢 verde                                                          |
| **Última fase cerrada** | 05 → [`RESULTADO-FASE-05.md`](../resultados/RESULTADO-FASE-05.md) |
| **Fase a ejecutar**     | 06 — El flujo SSE y la salida de los reportes                     |
| **Deudas objetivo**     | D-05, D-10, D-11, D-31 + DP-04                                    |
| **Estado**              | 🔴 **BLOQUEADA** por DP-04, B-05, B-06, B-07                      |

Lo que dejó la Fase 05:

- El tablero **ya tiene dominio**: `BacklogAudit`, `BacklogMetrics`, `StoryQuality`, `StoryUpdate`,
  todos `record` inmutables en `domain/model/.../model/dashboard/`.
- `Handler` **ya no piensa**: 0 reglas de negocio, 0 mutaciones in-place, 0 `sharedData`. 382
  líneas.
- El fallo del año está cerrado en el BFF **y** en el MCP.
- `DashboardResponse` y compañía viven ahora en
  `reactive-web/.../api/dto/dashboard/`, con `DashboardDtoMapper` como frontera.
- `audit.batch-size` (10) y `audit.concurrency` (2) son configurables, inyectados **por
  constructor** en `Handler`.
- Cobertura: `domain/model` 94,8 % · `domain/usecase` 97,6 %.

---

## 3. Hechos ya verificados — **no los vuelvas a investigar**

### 3.1 ⚠️ Hay trabajo sin confirmar desde la Fase 02

`git status` muestra decenas de ficheros modificados, añadidos y borrados que **no son tuyos**:
son las Fases 02, 03, 04 y 05. El usuario decidió expresamente **arreglar primero los issues de
SonarQube y confirmar después, por fases**.

> **No hagas commit de nada** salvo petición explícita. Y si la hay, sigue `COMMIT_RULES.md`:
> `tipo(scope_snake_case): descripción en español en minúscula`.

### 3.2 El 500 mudo del stream (D-31) está caracterizado

`DashboardRoutesCharacterizationTest.givenInitialFailsWithMcpEnabled_whenGetStream_then500AndTaskIsFailed`
(línea ~372) **fija el comportamiento actual**: con MCP habilitado, un fallo al obtener los datos
iniciales produce un **500 con cuerpo JSON**, no un stream SSE. El propio javadoc del test explica
por qué: WebFlux no llega a escribir la cabecera `text/event-stream` porque el error ocurre antes
del primer elemento.

> **Si B-06 se aprueba, esa prueba tiene que cambiar.** Es la única modificación que la Fase 06
> necesita del test de caracterización, y debe autorizarse por escrito, igual que se hizo con S-2 en
> la Fase 05.

### 3.3 El test de caracterización ya lleva una excepción registrada

`DashboardRoutesCharacterizationTest` sigue siendo **intocable**, pero su javadoc documenta la
**excepción S-2** de la Fase 05: se cambiaron dos líneas de *tipos* (un import y un `any(...)`), sin
tocar ninguna aserción. Ese es el precedente y el formato a seguir si hace falta otra.

### 3.4 Dónde vive cada cosa

| Qué                 | Ruta (desde `azure-devops-backend/`)                                                                         |
|---------------------|--------------------------------------------------------------------------------------------------------------|
| Entry-point         | `infrastructure/entry-points/reactive-web/src/main/java/co/com/bancolombia/api/Handler.java` (382 líneas)    |
| DTOs + mapper       | `.../reactive-web/src/main/java/co/com/bancolombia/api/dto/dashboard/`                                       |
| Eventos SSE         | `.../reactive-web/src/main/java/co/com/bancolombia/api/dto/event/`                                           |
| Dominio del tablero | `domain/model/src/main/java/co/com/bancolombia/model/dashboard/`                                             |
| Caso de uso         | `domain/usecase/src/main/java/co/com/bancolombia/usecase/dashboard/DevOpsDashboardUseCase.java` (205 líneas) |
| Reporte Markdown    | `domain/usecase/.../dashboard/DashboardMarkdownReport.java` (package-private)                                |
| Cableado            | `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`                       |
| Configuración       | `applications/app-service/src/main/resources/application.yaml`                                               |

### 3.5 Los cuatro puntos de repliegue a mock (D-11)

| Dónde                                                  | Qué hace                            |
|--------------------------------------------------------|-------------------------------------|
| `DevOpsDashboardUseCase.fallbackOrPropagate`           | Devuelve el mock si `!isMcpEnabled` |
| `getDashboardData` → `onErrorResume`                   | Llama al anterior                   |
| `getDashboardInitialData` → `onErrorResume`            | Llama al anterior                   |
| `Handler`, dos `onErrorResume` + el `catch` del parseo | Llaman a `getMockDashboardStream()` |

`isMcpEnabled()` es público en el caso de uso **solo** para que el `Handler` pueda consultarlo. Ese
es el olor: un adaptador HTTP no debería saber si el canal de IA está habilitado.

### 3.6 La escritura de ficheros (D-10)

`DevOpsDashboardUseCase.saveDashboardReport(BacklogAudit, String, String)` es `void`, síncrono, hace
`new File(...)`, `mkdirs()` y `Files.writeString(...)`, y se invoca desde un `Mono.defer` dentro del
`concatWith` del `Handler`. Bloquea el event loop y se traga las excepciones en un `catch` que solo
loguea.

`DashboardMarkdownReport` ya está separado y es dominio puro: es el proveedor de contenido natural
para el futuro `ReportStoragePort`.

---

## 4. ⚠️ Trampas conocidas — te ahorran un ciclo de build cada una

1. **`DashboardRoutesCharacterizationTest` es INTOCABLE**, salvo lo que B-06 autorice expresamente y
   por escrito. Son 12 pruebas y son la definición operativa de que no rompiste nada.
2. **Esa prueba mockea `devOpsDashboardUseCase`** con `@MockitoBean`. Si cambias la firma de
   cualquier método público del caso de uso, **no compila**.
3. **`Handler` se construye por constructor con 8 parámetros**, dos de ellos `@Value`. El test usa
   `@WebFluxTest` + `@ContextConfiguration`, así que los valores por defecto (`:10` y `:2`) se
   aplican solos. Si añades otro parámetro, no hace falta tocar el test.
4. **La consola de PowerShell destroza los acentos.** Verás `Mtrica` en vez de `Métrica`. Los
   ficheros están bien, en UTF-8. **No “corrijas” nada basándote en la salida del terminal.**
5. **ArchUnit emite warnings preexistentes**: *Rule_2.7 violated (6 times)* en el backend y
   *Rule_2.2 violated (1 time)* en el MCP. **No los introdujiste tú.** *Rule_2.2* en el backend está
   a **0** desde la Fase 05: si vuelve a aparecer, es tuyo.
6. **Ojo con `Rule_2.7`:** si añades un campo `@Value` **no final** a un bean, la regla sube. Usa
   siempre inyección por constructor. Pasó en la Fase 05 y costó un ciclo.
7. **`app-service` tiene `processResources.dependsOn copyFrontendToStatic`**: compilar ese módulo
   dispara un build del frontend con `pnpm`. Para iterar rápido, lanza pruebas por módulo:
   `.\gradlew :usecase:test`, `.\gradlew :model:test`, `.\gradlew :reactive-web:test`.
8. **`jacocoTestReport` depende de `pitest`.** No lo pidas en cada iteración, solo al cerrar.
9. **Prohibido `block()` y `subscribe()` manual** en `src/main` (DP-08). Hoy hay **cero**; que siga
   así. Es especialmente relevante en esta fase, que toca I/O de ficheros.

### Comandos que funcionan (PowerShell)

```powershell
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-backend

# Suite completa
.\gradlew test --console=plain 2>&1 | Select-String -Pattern '^BUILD|FAILED|violated'

# Contar pruebas y fallos  (el resumen de Gradle no los da)
$t=0;$f=0;$e=0
Get-ChildItem -Recurse -Filter TEST-*.xml | Where-Object { $_.FullName -match 'test-results\\test' } |
  ForEach-Object { [xml]$x=Get-Content $_.FullName; $t+=[int]$x.testsuite.tests
                   $f+=[int]$x.testsuite.failures; $e+=[int]$x.testsuite.errors }
"TESTS=$t FAIL=$f ERR=$e"      # referencia al cerrar la Fase 05: TESTS=219 FAIL=0 ERR=0

# Cobertura  (OJO: --no-configuration-cache es obligatorio)
.\gradlew jacocoMergedReport --no-configuration-cache --console=plain
[xml]$r = Get-Content build\reports\jacocoMergedReport\jacocoMergedReport.xml
foreach ($p in @('co/com/bancolombia/model','co/com/bancolombia/usecase')) {
  $cov=0;$mis=0
  $r.report.package | Where-Object { $_.name -like "$p*" } | ForEach-Object {
    $c = $_.counter | Where-Object { $_.type -eq 'INSTRUCTION' }
    $cov+=[int]$c.covered; $mis+=[int]$c.missed }
  "{0,-32} {1,6:N1} %" -f $p, (100*$cov/($cov+$mis)) }
# Referencia Fase 05:  model 94,8 %  ·  usecase 97,6 %

# Comprobar que no se coló I/O bloqueante
Get-ChildItem -Recurse -Include *.java -Path .\domain,.\infrastructure,.\applications |
  Where-Object { $_.FullName -match '\\src\\main\\' } |
  Select-String -Pattern '\.block\(|\.subscribe\('
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
   y pregunta. No inventes. En esta fase aplica muy en concreto al **bucket S3 de DP-04**: no
   supongas que existe, que es accesible ni con qué credenciales.
8. **Dominio puro**: `domain/model` y `domain/usecase` sin anotaciones de Spring ni dependencias
   técnicas (`rules/spring-rules.md`). Esto incluye Jackson: si un tipo necesita deserializarse, es
   un DTO y va en `reactive-web`. La Fase 05 tropezó justo con eso.
9. **Si dos instrucciones de la fase se contradicen, no elijas en silencio.** Dilo, propón la salida
   y déjala escrita. Pasó en la Fase 04 (T-04 vs T-07) y en la Fase 05 (B-03 vs T-07 → S-2).
10. **Verde tras cada paso.** Nada de acumular seis cambios y compilar al final.
11. **No hagas commit** salvo petición expresa; ver §3.1.
12. **Cero secretos** en código, configuración o documentación. Si DP-04 se resuelve con S3, las
    credenciales van por variable de entorno o gestor de secretos, **nunca** en `application.yaml`.

---

## 6. Qué tiene que hacer la Fase 06

### Paso 0 — desbloquear (antes de cualquier código)

Pedir al usuario **DP-04**, **B-05**, **B-06** y **B-07**, con recomendación. Están razonados en
[`fase-06.md`](../fases/fase-06.md) §2:

| #         | Pregunta                                                           | Recomendación                                                                               |
|-----------|--------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| **DP-04** | ¿Dónde acaban los reportes `.md`: disco, S3 o ambos?               | **A — disco tras un `ReportStoragePort`**, salvo que se confirme que el bucket es accesible |
| **B-05**  | ¿Un fallo al guardar el reporte debe tumbar el stream?             | **No, solo registrarse**                                                                    |
| **B-06**  | ¿El stream debe emitir un evento SSE de error en vez del 500 mudo? | **Sí** — implica cambiar 1 prueba de caracterización y coordinar con el front               |
| **B-07**  | ¿Sigue existiendo el repliegue a mock en producción?               | **No** — unificarlo en un punto y gobernarlo por configuración                              |

### Pasos 1..n — según lo decidido

Borrador en `fase-06.md` §3: T-01 a T-06.

### Criterios de cierre

Los de `fase-06.md` §3. Los dos que más se olvidan: **cero I/O síncrona en `domain/usecase`** y **
`handleDevOpsDashboardStream` por debajo de 30 líneas**.

---

## 7. Lo que NO entra en la Fase 06

- Unificar las tres fachadas de logging → **Fase 08** (D-12).
- El módulo `mcp-client` casi vacío → **Fase 08** (D-32).
- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Medir el coste real del lote y la concurrencia → **D-38**, requiere entorno con datos.
- Pruebas de integración del adaptador REST del MCP → **D-36**.
- Cambiar el proveedor de LLM o el protocolo MCP → fuera del plan.

