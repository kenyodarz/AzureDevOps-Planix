# FASE 04 — Prompts y datos fuera del dominio

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** 🟢 **COMPLETADA** — 2026-08-29
> **Deudas objetivo:** D-08 (saldada), D-19 (parcial)
> **Decisión resuelta (2026-08-29):** **DP-02.a → destino final el MCP/agente, ejecución en dos
tiempos** ·
> **DP-02.b → B** · **DP-02.c → aplazada a la Fase 05**
> **Resultado:** [`RESULTADO-FASE-04.md`](../resultados/RESULTADO-FASE-04.md) ·
> **Fase siguiente:** [`fase-05.md`](fase-05.md)

---

## 1. Contexto

### 1.1 De dónde vienes

La Fase 03 está cerrada (`docs/resultados/RESULTADO-FASE-03.md`). Al arrancar esta fase existe:

- **Build en verde: 153 pruebas, 0 fallos.**
- El frontend habla con **un solo servicio**: cinco rutas nuevas bajo `/api/**` en `TaskHandler`.
- La integración por base de datos compartida **cortada**: el BFF ya no escribe tareas ajenas.
- `RequestValidator` en `reactive-web`: el BFF por fin valida la entrada.
- Cobertura: `domain/usecase` 96,4 % · `reactive-web` 89,8 % · `agent-client` 91,4 %.

### 1.2 El problema

`DevOpsDashboardUseCase` tiene **430 líneas, y 184 de ellas no son lógica de negocio**:

|    Líneas | Constante                  | Qué es                            |
|----------:|----------------------------|-----------------------------------|
|   17 – 64 | `DASHBOARD_PROMPT`         | 48 líneas de instrucciones al LLM |
|  66 – 106 | `DASHBOARD_INITIAL_PROMPT` | 41 líneas                         |
| 107 – 133 | `BATCH_AUDIT_PROMPT`       | 27 líneas                         |
| 135 – 200 | `MOCK_DASHBOARD_DATA`      | **66 líneas de JSON de prueba**   |

Esto contradice de frente la regla de oro de `rules/spring-rules.md`: **el dominio es puro**. Un
prompt es la interfaz con un proveedor de IA concreto; un mock es un artefacto de pruebas.

### 1.3 El mock por defecto: decisión consciente de la POC, no un defecto

Verificado el 2026-08-29 en `applications/app-service/src/main/resources/application.yaml:28`:

```yaml
spring.ai.mcp.client.enabled: "${SPRING_AI_MCP_CLIENT_ENABLED:false}"   # ← FALSE por defecto
```

`UseCasesConfig` lo lee (con *default* `true` en la anotación `@Value`) y se lo pasa al caso de uso
como `isMcpEnabled`. Con `false`, `getDashboardData` y `getDashboardInitialData` devuelven
`MOCK_DASHBOARD_DATA` en lugar de propagar el error del agente.

**Aclaración del usuario (2026-08-29):** ese `false` es **deliberado y exclusivo de la POC de
desarrollo**. En producción esa línea no existirá: el despliegue va contra el MCP de forma
obligatoria y no hay ruta alternativa. Por tanto:

- **No es una deuda ni un riesgo de producción.** No se registra D-36.
- El mock **no se elimina**: sigue siendo el modo de trabajo local sin MCP.
- Lo único que cambia en esta fase es **dónde vive**: sale del dominio a `resources/mock/`
  (DP-02.b → B). Es higiene arquitectónica, no una mitigación.

> ⚠️ Lo que sí queda anotado, sin número de deuda, es que el *default* del `@Value` en
> `UseCasesConfig` (`true`) y el del `application.yaml` (`false`) **no coinciden**. Se documenta
> aquí para que no se lea como un descuido; no se cambia en esta fase porque cambiarlo alteraría el
> comportamiento local y `UseCasesConfigTest` fija ambos valores.

### 1.4 Los números mágicos (D-19)

Verificados en código el 2026-08-29:

| Archivo                       | Línea | Literal                        | Significado                  |
|-------------------------------|------:|--------------------------------|------------------------------|
| `Handler.java`                |   287 | `i += 10`                      | Tamaño del lote de auditoría |
| `Handler.java`                |   288 | `Math.min(i + 10, ...)`        | El mismo 10, repetido        |
| `Handler.java`                |   394 | `Duration.ofMillis(800)`       | Retardo del stream simulado  |
| `DevOpsDashboardUseCase.java` |   266 | `LocalDate.now(...).getYear()` | Año actual dentro del prompt |
| `DevOpsDashboardUseCase.java` |   307 | `LocalDate.now(...).getYear()` | El mismo cálculo, duplicado  |

> ⚠️ El tamaño de lote `10` **también** aparece en DP-03 (Fase 05), donde se decide su valor. Aquí
> **solo se le pone nombre y se centraliza**; el valor no se cambia.

---

## 2. DP-02 — RESUELTA (2026-08-29)

### DP-02.a · ¿Dónde viven las plantillas de prompt?

**Decisión del usuario:** *«Deben ir al MCP, si son para contextualizar el agente»* —
`C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`.

**Análisis previo a ejecutar.** Los prompts **no son una unidad**: cada uno mezcla tres materias con
tres dueños distintos. Tomando `DASHBOARD_PROMPT` como referencia:

| Parte |  Líneas | Contenido                                                                                     | Dueño real                                                      |
|-------|--------:|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| **1** | 22 – 24 | Reglas WIQL: no usar `CONTAINS` sobre `IterationPath`, filtrar con `@project` y nunca el UUID | **MCP** — es saber usar sus herramientas                        |
| **2** | 26 – 38 | Qué es «calidad»: criterios de aceptación, DoD, cálculo de `qualityScore`                     | **Agente** — es el cerebro (DP-01)                              |
| **3** | 40 – 63 | El JSON exacto de salida                                                                      | **BFF** — es lo que `Handler` deserializa a `DashboardResponse` |

**Dos restricciones duras, verificadas en código:**

1. **El BFF no puede leer del MCP en tiempo de ejecución.** La cadena es `BFF → agente → MCP`. En
   `src/main` no existe ningún uso de `ChatClient` ni de cliente MCP:
   `OAuth2WebClientConfig.chatClientBuilder()` es un `@Bean` que **nadie inyecta**, residuo del rol
   de agente que la Fase 02 retiró (D-32). Un `.txt` en el repo del MCP es inalcanzable desde aquí.
2. **`plan-maestro.md` §10** declara fuera de alcance modificar `azure-devops-agent` y
   `azure-devops-mcp`.

**Resolución: la decisión se acata, ejecutada en dos tiempos.**

| Tiempo              | Qué                                                                                                  | Dónde                                                  |
|---------------------|------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| **Ahora — Fase 04** | Los prompts salen del dominio a ficheros, detrás de `PromptTemplatePort`                             | `applications/app-service/src/main/resources/prompts/` |
| **Después — D-35**  | Las partes 1 y 2 migran al MCP/agente; el BFF deja de enviar prompts y pasa a enviar **intenciones** | Los tres repositorios                                  |

Justificación del orden: pasar de `String` incrustado en el dominio a fichero en otro repositorio no
es un salto, son dos. **Convertirlos en ficheros es el requisito previo** de cualquier migración
posterior, no toca otros repos y no cambia comportamiento. La parte 3 (el esquema JSON) **se queda
en el BFF** en cualquier escenario: si el agente lo cambiara por su cuenta, el BFF fallaría al
deserializar, en silencio y en producción.

→ La migración completa queda registrada como **D-35**, con su ruta descrita en §5.

### DP-02.b · ¿Qué se hace con `MOCK_DASHBOARD_DATA`?

**Decisión del usuario: opción B.** Se mueve a `resources/mock/dashboard.json` detrás de un
`DashboardFallbackPort`. Sale del dominio, la demo sigue viva y **no se rompe ninguna prueba**.

El mock sigue siendo la ruta local por defecto (§1.3) **por diseño de la POC**: esta fase no cambia
cuándo se usa, solo de dónde se lee.

### DP-02.c · ¿El «año actual» se calcula o se configura?

**Decisión del usuario: aplazada a la Fase 05.**

Motivo declarado: un sprint puede **empezar en un año y terminar en el siguiente**, y pertenece al
año en que empezó. `LocalDate.now().getYear()` lo busca en el año en curso y no lo encuentra. El
problema se manifiesta cada cambio de año.

**Propuesta a desarrollar en la Fase 05** (no se implementa aquí): el error de fondo no es calcular
mal el año, es **calcularlo**. Un sprint «es» de 2025 porque así se llama su `IterationPath`, no
porque lo diga el calendario. La salida es dejar de construir la ruta por concatenación y resolver
la iteración preguntando a Azure DevOps. Encaja con DP-03 y con la conversión de `IterationPath` en
Value Object (D-09, D-13), ambas de la Fase 05.

**En esta fase el cálculo del año se deja intacto**, incluida su duplicación en las líneas 266 y

307.

---

## 3. Instrucciones

### T-01 · `PromptTemplatePort` en el dominio

**Archivo nuevo:**
`domain/model/src/main/java/co/com/bancolombia/model/prompt/gateways/PromptTemplatePort.java`

```java
public interface PromptTemplatePort {

    String render(String name, Map<String, String> variables);
}
```

**Reglas:**

- El dominio conoce el **nombre lógico** de la plantilla, nunca la ruta del fichero. Eso es lo que
  permitirá que D-35 cambie el origen sin tocar el dominio.
- Si la plantilla no existe, lanza excepción **de dominio**, no `IOException`.
- Cero anotaciones de Spring.

**Archivo nuevo:** `domain/model/.../model/prompt/exceptions/PromptTemplateException.java`

### T-02 · Adaptador de plantillas

**Archivo nuevo:**
`applications/app-service/src/main/java/co/com/bancolombia/config/prompt/ClasspathPromptTemplateAdapter.java`

- Carga desde `classpath:prompts/{name}.txt`.
- **Cachea** las plantillas: son inmutables y releer el classpath en cada petición es I/O repetido.
- Sustitución con marcadores `${variable}`. Una variable sin valor es **error**, no cadena vacía: un
  prompt a medio rellenar produce respuestas del LLM que parecen válidas y no lo son.

**Archivos nuevos:** `applications/app-service/src/main/resources/prompts/`
`dashboard.txt`, `dashboard-initial.txt`, `batch-audit.txt`

> ⚠️ Copia **literal**, incluidos saltos de línea y espacios. Un prompt es una interfaz:
> reformatearlo cambia el comportamiento del LLM. Los `%s` / `%d` del `String.formatted(...)` actual
> pasan a marcadores con nombre: `${org}`, `${project}`, `${cell}`, `${sprint}`, `${year}`,
> `${batchSize}`, `${idsCsv}`.

> 📌 **Marcar la procedencia.** Cada `.txt` abre con una cabecera que indica qué bloque migrará al
> MCP y cuál se queda (D-35). Sin eso, la migración futura vuelve a tener que analizarlo línea a
> línea.
>
> 🔧 **Cómo, sin alterar el prompt.** Un comentario dentro del fichero se enviaría al LLM y
> rompería la equivalencia. Se adopta una convención explícita: **toda línea que empiece por `#~`
> es metadato y el adaptador la descarta** antes de sustituir variables. Es la única sintaxis
> especial del formato y queda documentada en el propio `PromptTemplatePort`.

### T-03 · Adelgazar `DevOpsDashboardUseCase`

- Eliminar las tres constantes de prompt y `MOCK_DASHBOARD_DATA`.
- Inyectar `PromptTemplatePort` y `DashboardFallbackPort` por constructor.
- **No tocar** la lógica de `getDashboardData`, `getDashboardInitialData` ni `getBatchAudit` más
  allá de sustituir la construcción del prompt.
- **No tocar** el cálculo del año (DP-02.c aplazada).
- Objetivo: **≤ 250 líneas** (hoy 430). El objetivo final de ≤ 120 es de la Fase 06, que le quitará
  la generación de reportes Markdown.

### T-04 · `DashboardFallbackPort` (DP-02.b → B)

**Archivo nuevo:** `domain/model/.../model/dashboard/gateways/DashboardFallbackPort.java`

```java
public interface DashboardFallbackPort {

    String mockDashboard();
}
```

Implementado en `app-service` leyendo `classpath:mock/dashboard.json`.

> 🔧 **Corrección respecto al borrador de esta fase (2026-08-29).** El borrador proponía
> `Mono<String> mockDashboard()` y que `getMockDashboardData()` **desapareciera** del caso de uso,
> con `Handler` dependiendo del puerto. Verificado en código, eso es **incompatible** con T-07:
> `DashboardRoutesCharacterizationTest:409` hace
> `when(devOpsDashboardUseCase.getMockDashboardData()).thenReturn(...)`, de modo que quitar el
> método no compila y obliga a tocar la prueba intocable.
>
> **Resolución adoptada:**
> - El puerto es **síncrono** (`String mockDashboard()`). El adaptador lee el recurso **una sola vez
    > al construirse** y lo cachea, así que en tiempo de petición no hay E/S: DP-08 se mantiene y no
>   hace falta `block()` en ninguna parte.
> - `getMockDashboardData()` **se conserva** en el caso de uso con la misma firma, pero deja de
>   devolver una constante y pasa a delegar en el puerto.
> - `Handler` **no cambia** en este punto.
>
> Se cumple el objetivo real de T-04 —las 66 líneas de JSON salen del dominio— sin romper la
> definición operativa de la refactorización.

- El JSON se mueve **tal cual**, sin reformatear: la prueba de equivalencia verifica su contenido.

### T-05 · Nombrar los números mágicos (D-19)

**Archivo:** `Handler.java`

```java
/** Tamaño del lote de auditoría. Su valor se ratifica en DP-03 (Fase 05). */
private static final int AUDIT_BATCH_SIZE = 10;

/** Retardo del stream simulado, para que el frontend vea la transición de estados. */
private static final Duration MOCK_STREAM_DELAY = Duration.ofMillis(800);
```

Sustituir los tres usos literales. **No cambiar los valores.**

### T-06 · Pruebas

- **`ClasspathPromptTemplateAdapterTest`** (nueva): plantilla existente, inexistente, sustitución
  correcta, **variable sin valor → error**, descarte de las líneas `#~` y que la caché no relee el
  classpath.
- **Prueba de equivalencia** (`PromptTemplateEquivalenceTest`, nueva, en `app-service`): el texto
  renderizado desde `resources/` debe ser **idéntico** al que producían las constantes, que se
  conservan como *golden copy* dentro de la propia prueba. Cubre también `mock/dashboard.json`. Es
  el seguro de que la copia fue literal.
- **`DevOpsDashboardUseCaseTest`**: los 18 tests verdes con los dos puertos sustituidos por dobles.
  Cambio de alcance, no de intención: las aserciones que hoy verifican **el texto del prompt**
  (`"NO analices la calidad"`, `"IDs a analizar: …"`, `proyecto "…"`) dejan de tener sentido en el
  dominio —ese texto ya no vive aquí— y pasan a verificar **el nombre de plantilla y las variables**
  que el caso de uso entrega. El texto en sí queda cubierto por la prueba de equivalencia. Las
  aserciones sobre resolución de `AreaPath`/`IterationPath` **no se tocan**.
- **`UseCasesConfigTest`**: se le añaden los dos adaptadores reales al contexto de prueba.
- **`DashboardRoutesCharacterizationTest`**: sus 12 pruebas verdes **sin tocar ni una línea**. Es el
  criterio de que la refactorización no cambió comportamiento.
- Cobertura: `domain/usecase` ≥ 90 %, `app-service` ≥ 70 %.

### T-07 · Validaciones de cierre

- `gradlew test` en **verde**, con **153 o más** pruebas.
- `grep -r "DASHBOARD_PROMPT\|MOCK_DASHBOARD_DATA"` en `domain/` → **cero**.
- `DevOpsDashboardUseCase` ≤ 250 líneas.
- Las 12 pruebas de `DashboardRoutesCharacterizationTest` verdes **sin modificar**.
- Ninguna cadena reactiva con `block()` ni `subscribe()` manual (DP-08).
- Los prompts renderizados son idénticos a los de antes.

---

## 4. Orden de ejecución

- [ ] **P-01** — Confirmar `gradlew test` en verde: **153/153**.
- [ ] **P-02** — `PromptTemplatePort` + `PromptTemplateException` en `domain/model` (T-01).
- [ ] **P-03** — Extraer los 3 prompts a `resources/prompts/`, **copia literal**, con la marca de
  procedencia para D-35 (T-02).
- [ ] **P-04** — `ClasspathPromptTemplateAdapter` con caché y variables obligatorias (T-02).
- [ ] **P-05** — `ClasspathPromptTemplateAdapterTest` (T-06).
- [ ] **P-06** — `DashboardFallbackPort` + adaptador + `resources/mock/dashboard.json` (T-04).
- [ ] **P-07** — Adelgazar `DevOpsDashboardUseCase`; registrar los beans en `UseCasesConfig` (T-03).
- [ ] **P-08** — Prueba de equivalencia de los prompts renderizados (T-06).
- [ ] **P-09** — Ajustar `DevOpsDashboardUseCaseTest` a los dos puertos nuevos (T-06).
- [ ] **P-10** — Nombrar los números mágicos de `Handler` (T-05).
- [ ] **P-11** — Verificar que `DashboardRoutesCharacterizationTest` sigue verde **sin tocarlo**.
- [ ] **P-12** — `gradlew test` en **verde** (T-07).
- [ ] **P-13** — `gradlew jacocoMergedReport --no-configuration-cache`; anotar cobertura.
- [ ] **P-14** — `grep` de verificación de T-07.
- [ ] **P-15** — Redactar `docs/resultados/RESULTADO-FASE-04.md` con la plantilla.
- [ ] **P-16** — Registrar **D-35** en el plan maestro; rellenar §6 y actualizar §9.
- [ ] **P-17** — **Generar `docs/fases/fase-05.md`**: dominio del dashboard (D-06, D-07, D-09, D-13)
    + la propuesta del año de DP-02.c, bloqueada por **DP-03**.

---

## 5. D-35 — Ruta de migración de los prompts al MCP / agente

> No se ejecuta en esta fase. Se deja escrita para que la decisión del usuario no se pierda.

**Estado objetivo:** el BFF envía **intenciones**, no prompts.

```
HOY:      BFF ──[prompt de 48 líneas]──► agente ──► MCP
OBJETIVO: BFF ──[intención: auditar célula X, sprint Y]──► agente ──► MCP
                                          └── el agente compone el prompt
                                              con el saber del MCP
```

**Reparto por dueño:**
| Parte del prompt | Destino | Motivo | |---|---|---| | Reglas WIQL (`CONTAINS`, `@project`, UUID) |
**MCP**, junto a sus herramientas | Quien define la herramienta define cómo se usa | | Definición de
«calidad», `qualityScore`, DoD | **Agente** | DP-01: el cerebro vive en el agente | | Esquema JSON
de salida | **Se queda en el BFF** | Es su contrato de deserialización; moverlo lo rompe en
silencio |

**Precondiciones antes de abrirla:**

1. Que exista contrato versionado entre BFF y agente (**D-29**).
2. Que se decida si el esquema JSON se declara en un sitio compartido o se duplica con validación.
3. Levantar la restricción de `plan-maestro.md` §10 para `azure-devops-agent` y `azure-devops-mcp`.

**Coste estimado:** toca los tres repositorios y cambia el contrato `POST /api/chat/messages`.
Requiere su propio plan, no un `T-nn` dentro de una fase.

---

## 6. Referencia rápida

### Estado actual de `DevOpsDashboardUseCase` (430 líneas)

|    Líneas | Contenido                                     | Destino en esta fase                      |
|----------:|-----------------------------------------------|-------------------------------------------|
|        16 | `Logger`                                      | Se queda (D-12, Fase 08)                  |
|  17 – 133 | 3 prompts                                     | → `resources/prompts/`                    |
| 135 – 200 | Mock JSON                                     | → `resources/mock/dashboard.json`         |
| 201 – 251 | Constructor y utilidades                      | Se queda                                  |
| 253 – 329 | `getDashboardData`, `getDashboardInitialData` | Se quedan; cambia cómo obtienen el prompt |
| 331 – 422 | Generación del reporte Markdown               | Se queda (→ Fase 06, `ReportStoragePort`) |
| 424 – 429 | `getBatchAudit`                               | Se queda; cambia cómo obtiene el prompt   |

### Lo que NO entra en esta fase

- Tocar la lógica de auditoría o el cálculo de métricas → **Fase 05**.
- El problema del año en el cruce de ejercicio → **Fase 05** (DP-02.c + DP-03).
- Sacar la escritura de ficheros del caso de uso → **Fase 06** (DP-04).
- Cambiar el valor del lote `10` → **Fase 05** (DP-03).
- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- El módulo `mcp-client` casi vacío (D-32) → **Fase 08**.

---

## 7. Resultado

> Detalle completo en [`docs/resultados/RESULTADO-FASE-04.md`](../resultados/RESULTADO-FASE-04.md).

- **Fecha de cierre:** 2026-08-29
- **DP-02 resuelta como:** a → dos tiempos (ficheros ahora, MCP en D-35) · b → **B** · c → **Fase
  05**
- **Tests totales / fallidos:** **170 / 0**
- **Líneas de `DevOpsDashboardUseCase`:** 430 → **225**
- **Prompts renderizados idénticos a los originales:** **sí**, verificado por
  `PromptTemplateEquivalenceTest` con las constantes originales como *golden copy*
- **Cobertura `domain/usecase` / `app-service`:** **96,8 % / 82,1 %**
- **Hallazgos no previstos:**
    - Los prompts no son una unidad: son tres materias con tres dueños (H-1).
    - `DashboardRoutesCharacterizationTest` mockea `getMockDashboardData()`, lo que hacía
      **contradictorias** las instrucciones T-04 y T-07 (§T-04, resuelto).
    - La resolución de rutas estaba duplicada carácter a carácter (H-3).
    - `getBatchAudit` invierte proyecto y organización respecto a los otros dos prompts (H-4).
    - La cobertura de `domain/model` se mide de dos formas incomparables (H-5).
- **Deudas nuevas detectadas:** **D-35** (migración de prompts al MCP/agente; requiere plan propio).
  **No se registró D-36**: el mock por defecto es una decisión consciente de la POC, no un defecto.
