# HANDOFF — Arranque de la Fase 08

> **Uso:** copia el bloque de §1 como **primer mensaje** de una sesión nueva. El resto de este
> documento es lo que esa sesión leerá; no hace falta pegarlo.
> **Generado:** 2026-08-29, al cerrar la Fase 07.

---

## 1. Prompt de arranque (copiar tal cual)

```text
Vas a ejecutar la FASE 08 del plan de refactorización del BFF. Es la ÚLTIMA: cierra el plan.

Trabajo en C:\Users\minaj\Work\GitHub\Labs\AzureDevOps (monorepo, Windows, shell PowerShell).
El módulo objetivo es azure-devops-backend.

ANTES DE TOCAR NADA, lee en este orden y NADA MÁS:
  1. azure-devops-backend/docs/plan/HANDOFF-FASE-08.md   ← empieza aquí, es tu guía
  2. azure-devops-backend/docs/fases/fase-08.md
  3. azure-devops-backend/docs/resultados/RESULTADO-FASE-07.md  (solo §3, §6 y §8)

El HANDOFF trae los hechos ya verificados en código (rutas, líneas, números, trampas
conocidas) y DESMIENTE tres cosas que el plan maestro da por ciertas. Respétalo: cada
verificación que repitas sin necesidad es contexto que no te sobrará después.

La Fase 08 está BLOQUEADA por B-11, B-12 y B-13, y por una comprobación previa sobre
D-27 y D-04. Tu primer mensaje debe ser pedírmelos, resumidos en no más de 15 líneas y
con tu recomendación. No escribas una sola línea de código hasta que yo responda.

IMPORTANTE: hay trabajo SIN CONFIRMAR en git desde la Fase 02 (128 ficheros). No hagas
commit de nada salvo que yo te lo pida expresamente.

Al ser la última fase, además del RESULTADO-FASE-08.md tienes que dejar el INFORME DE
CIERRE del plan completo: recorrido de las 8 fases, estado final de las 45 deudas y qué
queda vivo, con dueño.

Cumple sin excepción las reglas de §5 del HANDOFF y las de
.github/copilot-instructions.md (Política de No-Asunción y COMMIT_RULES.md).
```

---

## 2. Dónde estamos

|                         |                                                                   |
|-------------------------|-------------------------------------------------------------------|
| **Build backend**       | 🟢 verde — **238 pruebas, 0 fallos**                              |
| **Build frontend**      | 🟢 verde — **29 pruebas, 0 fallos**                               |
| **Última fase cerrada** | 07 → [`RESULTADO-FASE-07.md`](../resultados/RESULTADO-FASE-07.md) |
| **Fase a ejecutar**     | 08 — Configuración, cierre y verificación **(última)**            |
| **Deudas objetivo**     | D-04, D-12, D-32, D-42 (+ D-27, ver §3.1)                         |
| **Estado**              | 🟠 **BLOQUEADA** por B-11, B-12, B-13                             |

Lo que dejó la Fase 07:

- **Un solo criterio de traducción de errores** (`ApiErrorTranslator`): 404/400/503/502/504/500.
  Antes convivían dos en el mismo entry-point.
- `PlanningChunk` tiene **entidad de persistencia** y **DTO de respuesta**: el dominio ya no viaja
  ni a la base de datos ni por HTTP. **Sin migración**: el JSON almacenado es idéntico.
- `task-store-inmemory` → **`task-store-postgres`**. Ningún módulo miente ya sobre su contenido.
- El frontend **escucha el evento `ERROR`** del stream: D-31 cerrada por completo.
- Deudas nuevas: **D-43**, **D-44**, **D-45** (las dos últimas, atadas a D-29/D-34).

---

## 3. Hechos ya verificados — **no los vuelvas a investigar**

### 3.1 ⚠️ El plan maestro se equivoca en tres puntos. Están comprobados en código

| Lo que dice el plan                                                                                       | Lo que hay de verdad                                                                                                                                                                                                                                                        |
|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-27**: «`agent.url` **no existe** en `application.yaml`»                                               | **Existe**, línea 4: `url: "${AGENT_URL:http://localhost:8082}"`, con `agent.timeout` justo debajo. Alguna fase lo añadió sin marcar la deuda. **D-27 está saldada**: verifícalo y márcala, no la «arregles»                                                                |
| **D-04**: «residuos `a2a.*`, `agent.system-prompt`, `adapters.aws.s3` **y** `adapter.aws.s3`, consola H2» | `a2a.*` y `agent.system-prompt` **ya no están**. Quedan **solo dos** residuos reales: la **consola H2** (líneas 59-62, con un datasource que es PostgreSQL) y el **bloque S3 duplicado y mal escrito**: `adapters.aws.s3` (102-107) y `adapter.aws.s3` (108-113), idénticos |
| **D-32**: «`mcp-client` con `NoOpAgentResponseAdapter` y tres clases de configuración»                    | `NoOpAgentResponseAdapter` **se eliminó en la Fase 03** (D-33). Quedan **tres clases, todas de configuración**: `SecurityConfig`, `OAuth2WebClientConfig`, `McpConnectionHeadersProperties`                                                                                 |

> Y una salvedad sobre D-32: el plan dice «el BFF no habla MCP con nadie». **El `application.yaml`
> sí configura un cliente MCP de Spring AI** (`spring.ai.mcp.client`, líneas 51-58, apagado por
> defecto) y las cabeceras `mcp.connection-headers` (94-97). **No asumas que el módulo es
> prescindible sin comprobar quién consume esa configuración.**

### 3.2 La tercera fachada de logging vive en el DOMINIO, y por un motivo

Recuento exacto en `src/main`:

| Fachada             | Veces | Dónde                                                                                                                   |
|---------------------|------:|-------------------------------------------------------------------------------------------------------------------------|
| `@Log4j2`           | **4** | Todas en `reactive-web`: `Handler`, `TaskHandler`, `ApiErrorTranslator`, `DashboardStreamOrchestrator`                  |
| `@Slf4j`            | **5** | Todos los driven-adapters: `pgvector-store`, `task-store-postgres`, `agent-client`, `mcp-client`, `report-storage-file` |
| `java.util.logging` | **1** | `domain/usecase/.../DevOpsDashboardUseCase.java`, líneas 14-15                                                          |

**Ese JUL no es un descuido: es la única fachada del JDK.** Meter SLF4J en `domain/usecase` es meter
una dependencia técnica en el dominio, justo lo que `rules/spring-rules.md` prohíbe y lo que D-14
acaba de arreglar en la frontera de al lado. **B-12 tiene que decidir eso explícitamente**, no
«unificar en SLF4J» sin mirar dónde.

### 3.3 Dónde vive cada cosa

| Qué                            | Ruta (desde `azure-devops-backend/`)                                                                                     |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| Configuración                  | `applications/app-service/src/main/resources/application.yaml` (114 líneas)                                              |
| Cableado                       | `applications/app-service/src/main/java/co/com/bancolombia/config/UseCasesConfig.java`                                   |
| Lista de rutas a mano          | `applications/app-service/src/test/java/co/com/bancolombia/ArchitectureTest.java`, método `exportIssues()`, líneas 61-75 |
| Entry-point                    | `infrastructure/entry-points/reactive-web/.../api/Handler.java` (186 líneas)                                             |
| Traductor de errores           | `.../api/error/ApiErrorTranslator.java` — **el único** criterio (Fase 07)                                                |
| Colaboradores con `new` (D-42) | `Handler`, constructor: `new DashboardStreamOrchestrator(...)` y `new DashboardTaskTracker(...)`                         |
| Módulo a decidir               | `infrastructure/driven-adapters/mcp-client/` — 3 clases, todas de configuración                                          |
| Único log del dominio          | `domain/usecase/.../dashboard/DevOpsDashboardUseCase.java`, líneas 14-15                                                 |

### 3.4 El test de caracterización ya lleva cuatro excepciones registradas

`DashboardRoutesCharacterizationTest` y `PlanningRoutesCharacterizationTest` documentan en su
javadoc **S-2** (Fase 05), **S-3** y **S-4** (Fase 06) y **S-5** (Fase 07). Ese es el formato exacto
a seguir si B-13 autoriza la quinta: qué se cambió, por qué era inevitable y qué **no** se tocó.

### 3.5 La clase más larga

`A2APayloadMapper` (**351 líneas**) es la **única** clase productiva por encima de 300. Traduce el
contrato `a2a`, así que tocarla roza D-29/D-34, que están fuera del plan. **Mídela antes de prometer
nada.**

---

## 4. ⚠️ Trampas conocidas — te ahorran un ciclo de build cada una

1. **`ApiErrorTranslator` es estático y sin inyección a propósito.** Si lo conviertes en bean para
   «hacerlo más Spring», rompes el `@ContextConfiguration` de las dos pruebas de caracterización y
   te comes una excepción `S-n` que no hacía falta.
2. **`Rule_2.7` se cuenta por CAMPO, no por clase.** Pasar `@Value` a `@ConfigurationProperties` con
   *setters* **sube** el contador. Usa `record` o campos `final`.
3. **`app-service` tiene `processResources.dependsOn copyFrontendToStatic`**: compilar o probar ese
   módulo dispara un build del frontend con `pnpm`. Para iterar rápido, por módulo:
   `.\gradlew :usecase:test`, `.\gradlew :reactive-web:test`, `.\gradlew :pgvector-store:test`.
4. **`jacocoTestReport` depende de `pitest`.** No lo pidas en cada iteración, solo al cerrar.
5. **Prohibido `block()` y `subscribe()` manual** en `src/main` (DP-08). Hoy hay **cero**.
6. **Antes de borrar una clave del `yaml`, demuestra que nadie la lee**: `@Value`,
   `@ConfigurationProperties`, `Environment`, y también los `build.gradle` y los `deployment/`. Una
   clave sin lector aparente puede tenerlo en un perfil o en un manifiesto de despliegue.
7. **Renombrar o eliminar un módulo obliga a tocar tres sitios**: `settings.gradle`,
   `app-service/build.gradle` y la lista codificada de `ArchitectureTest`. Lo aprendió la Fase 07.
8. **La consola de PowerShell destroza los acentos.** Verás `Mtrica` en vez de `Métrica`. Los
   ficheros están bien, en UTF-8. **No “corrijas” nada basándote en la salida del terminal.**
9. **PowerShell a veces devuelve vacío** si encadenas `ForEach-Object` con `Sort-Object` y formato
   en la misma línea. Si un comando no imprime nada, pártelo; no supongas que el resultado es cero.

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
"TESTS=$t FAIL=$f ERR=$e"      # referencia al cerrar la Fase 07: TESTS=238 FAIL=0 ERR=0

# Cobertura  (OJO: --no-configuration-cache es obligatorio)
.\gradlew jacocoMergedReport --no-configuration-cache --console=plain
[xml]$r = Get-Content build\reports\jacocoMergedReport\jacocoMergedReport.xml
foreach ($p in @('co/com/bancolombia/model','co/com/bancolombia/usecase')) {
  $cov=0;$mis=0
  $r.report.package | Where-Object { $_.name -like "$p*" } | ForEach-Object {
    $c = $_.counter | Where-Object { $_.type -eq 'INSTRUCTION' }
    $cov+=[int]$c.covered; $mis+=[int]$c.missed }
  "{0,-32} {1,6:N1} %" -f $p, (100*$cov/($cov+$mis)) }
# Referencia Fase 07:  model 94,8 %  ·  usecase 98,9 %

# Frontend
cd ..\azure-devops-frontend
pnpm test --watch=false
pnpm exec tsc -p tsconfig.app.json --noEmit
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

7. **Política de No-Asunción.** En esta fase aplica muy en concreto a **borrar configuración**: una
   clave que aquí no lee nadie puede leerse en un despliegue. Demuéstralo antes de quitarla.
8. **Dominio puro**: `domain/model` y `domain/usecase` sin anotaciones de Spring ni dependencias
   técnicas, Jackson y SLF4J incluidos. Ver §3.2: es exactamente el nudo de B-12.
9. **Si dos instrucciones se contradicen, no elijas en silencio.** Dilo, propón la salida y déjala
   escrita. Pasó en la 04, la 05, la 06 y la 07 (criterio de cierre vs B-10 → D-44 y D-45).
10. **Verde tras cada paso.** Nada de acumular seis cambios y compilar al final.
11. **No hagas commit** salvo petición expresa: hay 128 ficheros sin confirmar desde la Fase 02.
12. **Cero secretos** en código, configuración o documentación. Ojo al tocar el `yaml`: hay
    `client-secret` y credenciales de datasource **como placeholders con variable de entorno**.
    Déjalos así; no los sustituyas por valores.

---

## 6. Qué tiene que hacer la Fase 08

### Paso 0 — desbloquear (antes de cualquier código)

Pedir al usuario **B-11**, **B-12** y **B-13**, con recomendación:

| #        | Pregunta                                                                                              | Recomendación                                                                                                                                                                                                                           |
|----------|-------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **B-11** | `mcp-client` (3 clases de configuración): ¿se absorbe y se elimina el módulo, o solo se renombra?     | **Comprobar primero quién usa `spring.ai.mcp.client`**; si nadie, absorber en `app-service` y eliminar. Si alguien, **renombrar** a lo que de verdad es                                                                                 |
| **B-12** | ¿Qué fachada de logging se queda, y qué hace el dominio? (§3.2)                                       | **SLF4J en infraestructura** (4 `@Log4j2` → `@Slf4j`) y **el dominio se queda con JUL**, que no es una dependencia externa. Alternativa si se prefiere pureza total: un puerto de logging, pero eso es una deuda nueva, no una limpieza |
| **B-13** | ¿Se autoriza tocar el `@ContextConfiguration` de las dos pruebas de caracterización para cerrar D-42? | **Sí**, con el formato de S-2…S-5. Es la última vez que hará falta                                                                                                                                                                      |

Y una **comprobación previa**, no una decisión: **D-27 ya está saldada** (§3.1). Verificarlo y
marcarlo en el plan antes de planificar nada sobre ella.

### Pasos 1..n — según lo decidido

Borrador en `fase-08.md` §3: T-01 a T-06.

### Criterios de cierre

Los de `fase-08.md` §3. Los dos que más se olvidan: **ninguna clave borrada sin su `grep`** y el
**informe de cierre del plan**, que no es lo mismo que el `RESULTADO-FASE-08.md`.

---

## 7. Lo que NO entra en la Fase 08

- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Contrato `a2a` compartido y versionado → **D-29/D-34**, y con ellos **D-44** y **D-45**.
- La fuga del esquema JSONB en `GET /api/planning/initiatives` → **D-43**.
- Llevar los reportes a S3 → **D-41**, requiere confirmar acceso al bucket.
- Medir el coste real del lote y la concurrencia → **D-38**, requiere entorno con datos.
- Las deudas del MCP (**D-36**, **D-37**, **D-39**) → otro repositorio.
- Cambiar el proveedor de LLM o el protocolo MCP.

