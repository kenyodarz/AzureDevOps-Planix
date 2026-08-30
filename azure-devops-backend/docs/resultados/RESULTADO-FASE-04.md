# RESULTADO — FASE 04: Prompts y datos fuera del dominio

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Instrucciones:** `docs/fases/fase-04.md`
> **Estado:** 🟢 **COMPLETADA** — 2026-08-29
> **Deudas atacadas:** D-08 (saldada), D-19 (parcial)
> **Deudas nuevas:** D-35

---

## 1. Objetivo de la fase

Sacar del dominio lo que nunca fue lógica de negocio: **3 prompts y 66 líneas de JSON simulado**,
184 de las 430 líneas de `DevOpsDashboardUseCase`. Un prompt es la interfaz con un proveedor de IA
concreto; un mock es un artefacto de pruebas. `rules/spring-rules.md` es explícito: el dominio es
puro.

Objetivo secundario: ponerles nombre a los números mágicos del `Handler` (D-19).

---

## 2. Qué se construyó

### `domain/model` — los dos puertos nuevos

| Archivo                                                | Qué hace                                                                                                                                                                                                     |
|--------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `model/prompt/gateways/PromptTemplatePort.java`        | `render(name, variables)`. El dominio conoce el **nombre lógico** de la plantilla, nunca su ubicación. Esa indirección es lo que permitirá que D-35 cambie el origen sin volver a tocar los casos de uso.    |
| `model/prompt/exceptions/PromptTemplateException.java` | Excepción **de dominio** con dos factorías (`notFound`, `missingVariable`). El caso de uso no se entera de que hoy las plantillas son ficheros: las implementaciones envuelven aquí cualquier `IOException`. |
| `model/dashboard/gateways/DashboardFallbackPort.java`  | `String mockDashboard()`. Síncrono a propósito (§6, desviación 1).                                                                                                                                           |

### `domain/usecase` — el adelgazamiento

- `DevOpsDashboardUseCase`: **430 → 225 líneas**. Desaparecen `DASHBOARD_PROMPT`,
  `DASHBOARD_INITIAL_PROMPT`, `BATCH_AUDIT_PROMPT` y `MOCK_DASHBOARD_DATA`.
- La resolución de `AreaPath`/`IterationPath`, que estaba **duplicada literalmente** en
  `getDashboardData` y `getDashboardInitialData`, pasa a dos métodos privados (`resolveCell`,
  `resolveSprint`). El cálculo del año **no se tocó**: DP-02.c está aplazada a la Fase 05. Esto **no
  salda D-09**, que pide Value Objects, pero deja de duplicar el código.
- `DashboardMarkdownReport` (nueva, package-private): compone el reporte. Ver §6, desviación 2.

### `applications/app-service` — los adaptadores y los recursos

| Archivo                                                                         | Qué hace                                                                                                                                                                                        |
|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `config/prompt/ClasspathPromptTemplateAdapter.java`                             | Lee `classpath:prompts/{name}.txt`, **cachea** por nombre, normaliza saltos de línea, descarta las líneas `#~` y sustituye `${variable}`. Una variable sin valor es **error**, no cadena vacía. |
| `config/dashboard/ClasspathDashboardFallbackAdapter.java`                       | Lee `classpath:mock/dashboard.json` **una sola vez, al construirse**. Si el recurso falta, el contexto no arranca.                                                                              |
| `resources/prompts/dashboard.txt` · `dashboard-initial.txt` · `batch-audit.txt` | Copia **literal**. Cada uno abre con una cabecera `#~` que reparte sus bloques por dueño para D-35.                                                                                             |
| `resources/mock/dashboard.json`                                                 | Copia literal del mock.                                                                                                                                                                         |

### `infrastructure/entry-points/reactive-web` — D-19

```java
private static final int AUDIT_BATCH_SIZE = 10;                 // 2 usos literales sustituidos
private static final Duration MOCK_STREAM_DELAY = ofMillis(800); // 1 uso literal sustituido
```

Los **valores no cambiaron**. `AUDIT_BATCH_SIZE` se ratifica en DP-03 (Fase 05), que además muda la
regla al dominio (D-07).

---

## 3. Decisiones aplicadas

| Decisión                                          | Resolución                                                                                                        | Dónde                 |
|---------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|-----------------------|
| **DP-02.a** — dónde viven las plantillas          | Destino final el MCP/agente, **ejecutado en dos tiempos**: ahora ficheros tras `PromptTemplatePort`; después D-35 | `resources/prompts/`  |
| **DP-02.b** — qué se hace con el mock             | **Opción B**: a `resources/mock/dashboard.json` tras `DashboardFallbackPort`                                      | `app-service`         |
| **DP-02.c** — el año, ¿se calcula o se configura? | **Aplazada a la Fase 05**, junto a DP-03 y D-09/D-13                                                              | sin cambios           |
| **DP-08** — nada bloqueante                       | Respetado: cero `block()` / `subscribe()` en `src/main`                                                           | verificado por `grep` |

### Sobre el mock por defecto (lo que en el borrador iba a ser D-36)

El borrador de la fase marcaba en rojo que `spring.ai.mcp.client.enabled` está en `false` en el
`application.yaml` y concluía que «el dashboard sirve JSON inventado como si fuera real».

**El usuario aclaró que ese `false` es deliberado y exclusivo de la POC de desarrollo**: en
producción esa línea no existe y el despliegue va contra el MCP de forma obligatoria, sin ruta
alternativa. Por tanto **no se registró D-36** y el mock **no se eliminó**: sigue siendo el modo de
trabajo local. Lo único que cambió es dónde vive.

> Queda anotado, sin número de deuda, que el *default* del `@Value` en `UseCasesConfig` (`true`) y
> el del `application.yaml` (`false`) no coinciden. No se cambió: alteraría el comportamiento local
> y `UseCasesConfigTest` fija ambos valores.

---

## 4. Métricas obtenidas

| Métrica                                 | Antes (Fase 03) | Después (Fase 04) |        Objetivo |
|-----------------------------------------|----------------:|------------------:|----------------:|
| Tests totales                           |             153 |           **170** |           ≥ 153 |
| Tests fallando                          |               0 |             **0** |               0 |
| Líneas de `DevOpsDashboardUseCase`      |             430 |           **225** |        ≤ 250 ✅ |
| Líneas de `Handler`                     |             405 |               422 | ≤ 150 (Fase 06) |
| Prompts/mock en `domain/`               |      184 líneas |             **0** |            0 ✅ |
| Cobertura `domain/usecase`              |          96,4 % |        **96,8 %** |       ≥ 90 % ✅ |
| Cobertura `app-service`                 |               — |        **82,1 %** |       ≥ 70 % ✅ |
| Cobertura `reactive-web`                |          89,8 % |        **89,7 %** |               — |
| `block()` / `subscribe()` en `src/main` |               0 |             **0** |            0 ✅ |

`Handler` creció 17 líneas: son las dos constantes de D-19 con su javadoc. Bajarlo a ≤150 es de la
Fase 06.

### Pruebas nuevas

| Suite                                    | Qué fija                                                                                                                                                                                                                              |
|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ClasspathPromptTemplateAdapterTest` (9) | Sustitución, **variable sin valor → error**, mapa nulo, plantilla inexistente, nombre vacío, descarte de `#~`, normalización CRLF, **la caché lee el recurso una sola vez**, y que las tres plantillas reales existen en el classpath |
| `PromptTemplateEquivalenceTest` (4)      | Que el texto renderizado desde `resources/` es **idéntico** al de las constantes originales, conservadas como *golden copy* dentro de la propia prueba. Cubre también `mock/dashboard.json`                                           |
| `PromptTemplateExceptionTest` (3)        | Que la excepción nombra plantilla y variable, y conserva la causa                                                                                                                                                                     |

### Pruebas tocadas

| Suite                                      | Qué cambió                                                                                                                                          |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `DashboardRoutesCharacterizationTest` (12) | **Ni una línea.** Es el criterio de que la refactorización no cambió comportamiento                                                                 |
| `DevOpsDashboardUseCaseTest` (18)          | Los dos puertos sustituidos por dobles; 3 aserciones de *texto de prompt* reemplazadas por aserciones de *plantilla y variables* (§6, desviación 3) |
| `UseCasesConfigTest` (5)                   | Los dos adaptadores **reales** añadidos al contexto de prueba                                                                                       |

---

## 5. Comandos ejecutados

```powershell
.\gradlew test                                          # BUILD SUCCESSFUL — 170 / 0 fallos
.\gradlew jacocoMergedReport --no-configuration-cache   # BUILD SUCCESSFUL

# DASHBOARD_PROMPT | MOCK_DASHBOARD_DATA | BATCH_AUDIT_PROMPT en domain/
#   → cero referencias
# .block( | .subscribe( en domain/ y applications/
#   → cero referencias  (DP-08)
# new DevOpsDashboardUseCase | getMockDashboardData
#   → solo en los puntos esperados (UseCasesConfig, Handler, sus dos tests)
```

---

## 6. Desviaciones respecto a las instrucciones

### 1. `DashboardFallbackPort` es síncrono, no `Mono<String>`; `getMockDashboardData()` se conserva

**Lo que pedía T-04:** `Mono<String> mockDashboard()`, que `getMockDashboardData()` desapareciera
del caso de uso y que `Handler` dependiera del puerto.

**Por qué no se hizo así:** `DashboardRoutesCharacterizationTest:409` hace
`when(devOpsDashboardUseCase.getMockDashboardData()).thenReturn(...)`. Quitar el método **no
compila** y obliga a tocar la prueba que T-07 declara intocable. Las dos instrucciones se
contradicen entre sí.

**Qué se hizo:** el puerto devuelve `String`; el adaptador resuelve el recurso **una sola vez al
construirse**, así que en tiempo de petición no queda E/S que bloquear y DP-08 se mantiene.
`getMockDashboardData()` conserva su firma pero delega en el puerto. `Handler` no cambia.

**Resultado:** las 66 líneas salen del dominio —que es el objetivo real de T-04— sin romper la
definición operativa de la refactorización. Registrado en `fase-04.md` §T-04.

### 2. La generación del Markdown se extrajo a `DashboardMarkdownReport`

**Lo que pedía T-03:** dejar el reporte donde estaba (→ Fase 06) y bajar de 430 a **≤ 250 líneas**.

**Por qué no cabían las dos cosas:** con los prompts fuera, la clase quedó en **300 líneas**. Las
~95 de composición del Markdown eran lo único que impedía cumplir el umbral, y comprimir javadoc
para llegar a 250 habría sido cosmética, no arquitectura.

**Qué se hizo:** las funciones de composición pasaron a una clase hermana **package-private y de
dominio puro**, sin cambiar el texto generado ni una coma. La escritura en disco sigue en el caso de
uso, tal como pedía la fase; sacarla es de la Fase 06 (DP-04), que ahora encuentra el trabajo
preparado.

**Verificación:** las tres pruebas de caracterización del reporte pasan sin modificación.

### 3. Tres aserciones de `DevOpsDashboardUseCaseTest` cambiaron de objeto

Las que buscaban frases **dentro del prompt** (`"NO analices la calidad"`, `"IDs a analizar: …"`,
`proyecto "…"`) verificaban algo que ya no es del dominio. Pasaron a verificar **qué plantilla** se
pide y **con qué variables**. El texto literal quedó cubierto —y mejor— por
`PromptTemplateEquivalenceTest`. Las aserciones sobre resolución de rutas **no se tocaron**.

### 4. Convención `#~` para los metadatos de las plantillas

T-02 pedía que cada `.txt` abriera con un comentario de procedencia. Un comentario dentro del
fichero se enviaría al LLM y rompería la equivalencia. Se adoptó una sintaxis explícita —`#~` al
principio de línea— que el adaptador descarta antes de renderizar. Es la única regla especial del
formato y está documentada en el propio `PromptTemplatePort`.

---

## 7. Checklist de calidad

- [x] `gradlew test` en verde: **170 / 0 fallos** (≥ 153).
- [x] `grep` de `DASHBOARD_PROMPT` / `MOCK_DASHBOARD_DATA` / `BATCH_AUDIT_PROMPT` en `domain/` →
  **0**.
- [x] `DevOpsDashboardUseCase` ≤ 250 líneas: **225**.
- [x] Las 12 pruebas de `DashboardRoutesCharacterizationTest`, verdes **y sin modificar**.
- [x] Ninguna cadena reactiva con `block()` ni `subscribe()` manual (DP-08).
- [x] Los prompts renderizados son **idénticos** a los de antes (`PromptTemplateEquivalenceTest`).
- [x] Cobertura `domain/usecase` ≥ 90 % (**96,8 %**) y `app-service` ≥ 70 % (**82,1 %**).
- [x] Dominio sin anotaciones de Spring ni dependencias técnicas.
- [x] Cero secretos introducidos.

---

## 8. Hallazgos

### H-1 · Los prompts no son una unidad: son tres materias con tres dueños

Analizando `DASHBOARD_PROMPT` línea a línea aparecen tres bloques con dueño distinto:

| Bloque                                       | Dueño real                                               | Destino      |
|----------------------------------------------|----------------------------------------------------------|--------------|
| Reglas WIQL (`CONTAINS`, `@project`, UUID)   | **MCP** — quien define la herramienta define cómo se usa | D-35         |
| Definición de «calidad», `qualityScore`, DoD | **Agente** — el cerebro vive allí (DP-01)                | D-35         |
| Esquema JSON de salida                       | **BFF** — es su contrato de deserialización              | **se queda** |

El tercero **no puede migrar**: si el agente lo cambiara por su cuenta, el BFF fallaría al
deserializar, en silencio y en producción. Este reparto quedó escrito en la cabecera `#~` de cada
`.txt` para que D-35 no tenga que volver a deducirlo.

### H-2 · El BFF no puede leer del MCP en tiempo de ejecución

Verificado, no supuesto. La cadena es `BFF → agente → MCP`. En `src/main` no existe ningún uso de
`ChatClient` ni de cliente MCP: `OAuth2WebClientConfig.chatClientBuilder()` es un `@Bean` que
**nadie inyecta**, residuo del rol de agente que la Fase 02 retiró (D-32). Un `.txt` alojado en el
repositorio del MCP sería inalcanzable desde aquí. Esto es lo que obliga a que D-35 cambie el
**contrato** —enviar intenciones en vez de prompts— y no solo la ubicación de un fichero.

### H-3 · La resolución de rutas estaba duplicada carácter a carácter

`getDashboardData` y `getDashboardInitialData` contenían el mismo bloque de 10 líneas, incluido el
cálculo del año. Es D-09, que la Fase 05 salda con Value Objects; aquí se dejó en un solo sitio para
que esa fase tenga un único punto que cambiar.

### H-4 · `getBatchAudit` invierte proyecto y organización

El prompt de auditoría por lotes recibe `(batchSize, project, org, ids)`, mientras los otros dos
reciben `(org, project, …)`. No es un fallo —los rótulos del texto son correctos— pero es una trampa
para quien edite la plantilla. Quedó anotado dentro de `batch-audit.txt`.

### H-5 · La cobertura de `domain/model` se lee de dos sitios distintos, y no dicen lo mismo

El `jacoco.xml` **del módulo** `domain/model` da 38,4 %, porque ese módulo solo tiene dos suites
propias. El **reporte mezclado** da 82,2 %, que es la cifra comparable con el 88,3 % que registra el
plan maestro: allí las clases del modelo aparecen ejercitadas por las pruebas de los demás módulos.

La caída de 88,3 % a 82,2 % es real y tiene explicación: entran tres tipos nuevos —dos puertos y una
excepción— y solo la excepción tiene prueba propia. Las interfaces no aportan líneas ejecutables,
así que el denominador crece sin que el numerador lo acompañe. Conviene **no comparar la cifra por
módulo con la mezclada**; son medidas distintas.

---

## 9. Estado al cerrar

|               |                                                                                                         |
|---------------|---------------------------------------------------------------------------------------------------------|
| **Build**     | 🟢 verde — 170 pruebas, 0 fallos                                                                        |
| **D-08**      | ✅ **saldada**                                                                                          |
| **D-19**      | 🟡 parcial — lote y retardo nombrados; `maxResults 3` ya tenía nombre; el **año** queda para la Fase 05 |
| **D-09**      | 🟡 duplicación eliminada, Value Objects pendientes (Fase 05)                                            |
| **D-35**      | 🆕 registrada — migración de prompts al MCP/agente, requiere plan propio                                |
| **Siguiente** | `docs/fases/fase-05.md` — dominio del dashboard, bloqueada por **DP-03**                                |

