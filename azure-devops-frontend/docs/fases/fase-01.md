# FASE 01 — Baseline y red de seguridad

> **Estado:** 🟢 **COMPLETADA** (2026-08-31) · **Depende de:** — · **Riesgo:** Nulo
> **Resultado:** `docs/resultados/RESULTADO-FASE-01.md` · **Baseline:** `docs/resultados/baseline-fase-01.md`
> **Commit al cerrar:** `test(devops_agent): agregar pruebas de caracterizacion de los flujos del frontend`
> **Siguiente fase:** `fase-02.md` (🔴 bloqueada por DP-03, DP-06 y DP-09)
> **Restricción original:** cero líneas en `src/app/**` fuera de `.spec.ts` — **relajada por la
> excepción S-01**, ver §0.

---

## 0. Resultado de la ejecución

| Métrica                 |                     Antes |      Después |
|-------------------------|--------------------------:|-------------:|
| `pnpm build`            | ❌ **fallaba (`TS2729`)** | ✅ 434.21 kB |
| `pnpm test`             |      ❌ **no ejecutable** |   ✅ 188/188 |
| Casos de prueba         |                        29 |      **188** |
| Archivos `.spec.ts`     |                         4 |       **12** |
| Componentes con pruebas |                     0 / 9 |    **9 / 9** |

**Hallazgos:** H-1 el repositorio no compilaba (🔴) · H-2 un componente habla con la red (D-26) · H-3 la tabla de planeaciones no se repinta en modo zoneless (D-27, 🔴) · H-4 tres métricas subestimadas · H-5 deudas menores D-22 a D-25.

**Excepción S-01 aplicada:** reordenamiento mecánico de declaraciones en
`devops-agent-state.service.ts` para restaurar el build. **D-07 saldada.**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

Esta es la fase inicial del plan. El repositorio `azure-devops-frontend` está en su estado de partida: una aplicación Angular 22 con una única *feature* (`devops-agent`), 9 componentes standalone, 2 servicios y 1 pipe.

Estado verificado el 2026-08-31:

| Artefacto                                                                                       | Líneas |
|-------------------------------------------------------------------------------------------------|-------:|
| `src/app/features/devops-agent/components/planning-dashboard/planning-dashboard.component.ts`   |    754 |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`                          |    406 |
| `src/app/features/devops-agent/pages/devops-agent-home.page.ts`                                 |    277 |
| `src/app/features/devops-agent/components/planning-management/planning-management.component.ts` |    260 |
| `src/app/features/devops-agent/components/planning-upload/planning-upload.component.ts`         |    128 |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`                            |    124 |
| `src/app/features/devops-agent/components/chat-messages/chat-messages.component.ts`             |    122 |
| `src/app/features/devops-agent/models/devops-agent.model.ts`                                    |    111 |
| `src/app/features/devops-agent/components/chat-input/chat-input.component.ts`                   |     98 |
| `src/app/shared/pipes/markdown-parser.pipe.ts`                                                  |     73 |

Pruebas existentes (4 archivos):

- `src/app/app.spec.ts`
- `src/app/shared/pipes/markdown-parser.pipe.spec.ts`
- `src/app/features/devops-agent/services/devops-agent-api.service.spec.ts` (219 L)
- `src/app/features/devops-agent/services/devops-agent-state.service.spec.ts` (202 L)

**Cero componentes tienen pruebas.**

### 1.2 Problema que resuelve esta fase

El plan maestro va a partir un servicio de 406 líneas que concentra **cinco flujos de negocio**
(F1 Chat General, F2 Refinamiento, F3 Tablero de Calidad, F4 Gestión de Planeaciones, F5 Tareas del Agente) y un componente de 754 líneas con reglas de negocio embebidas.

**Refactorizar sin una red de verificación es inaceptable.** Esta fase no arregla ninguna deuda de diseño: construye la red que permitirá comprobar, fase a fase, que el comportamiento observable no cambió.

Deuda que ataca: **D-15** (0/9 componentes con pruebas), parcialmente.

Deudas que esta fase **documenta pero no arregla**, dejando prueba de caracterización que fija su comportamiento actual:

| ID   | Comportamiento a congelar                                                   | Evidencia                                           |
|------|-----------------------------------------------------------------------------|-----------------------------------------------------|
| D-16 | Un único `loading$` compartido por los 5 flujos                             | `devops-agent-state.service.ts:27`                  |
| D-18 | `auditStory()` reinicia el polling global vía `triggerImmediatePoll()`      | `devops-agent-state.service.ts:301, :134-137`       |
| D-02 | El temporizador arranca en el constructor y nunca se limpia                 | `devops-agent-state.service.ts:95-98, :316-335`     |
| D-07 | Orden de declaración frágil (`TS2729` documentado en el propio archivo)     | `devops-agent-state.service.ts:23-24, :55-56, :88`  |
| D-13 | Alias público `messages` apunta a `refinementMessages`                      | `devops-agent-state.service.ts:90`                  |
| D-09 | Umbrales de calidad `90/70/50` (etiqueta) frente a `80/50` (color y filtro) | `planning-dashboard.component.ts:627-637, :702-713` |

### 1.3 Estado esperado al terminar

1. Existe una suite de caracterización que cubre los **cinco flujos** del servicio de estado.
2. Existe una prueba de componente para cada uno de los **9 componentes** actuales.
3. Existe `docs/resultados/baseline-fase-01.md` con las métricas M-01 a M-12 **medidas**, no declaradas.
4. Los umbrales de calidad, los intervalos de polling y las rutas de endpoint están **fijados por pruebas**: cambiarlos en una fase futura rompe una prueba y obliga a una decisión consciente.
5. **`git diff --stat -- src/app` no muestra ningún archivo que no termine en `.spec.ts`.**

### 1.4 Archivos involucrados

| Ruta                                                                                                 | Acción                         |
|------------------------------------------------------------------------------------------------------|--------------------------------|
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`                               | **LEER — PROHIBIDO MODIFICAR** |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`                                 | **LEER — PROHIBIDO MODIFICAR** |
| `src/app/features/devops-agent/components/**/*.ts`                                                   | **LEER — PROHIBIDO MODIFICAR** |
| `src/app/features/devops-agent/pages/devops-agent-home.page.ts`                                      | **LEER — PROHIBIDO MODIFICAR** |
| `src/app/features/devops-agent/services/devops-agent-state.service.spec.ts`                          | MODIFICAR (ampliar)            |
| `src/app/features/devops-agent/services/devops-agent-api.service.spec.ts`                            | MODIFICAR (ampliar)            |
| `src/app/features/devops-agent/components/agent-info/agent-info.component.spec.ts`                   | CREAR                          |
| `src/app/features/devops-agent/components/chat-input/chat-input.component.spec.ts`                   | CREAR                          |
| `src/app/features/devops-agent/components/chat-messages/chat-messages.component.spec.ts`             | CREAR                          |
| `src/app/features/devops-agent/components/info-cards/info-cards.component.spec.ts`                   | CREAR                          |
| `src/app/features/devops-agent/components/planning-upload/planning-upload.component.spec.ts`         | CREAR                          |
| `src/app/features/devops-agent/components/planning-management/planning-management.component.spec.ts` | CREAR                          |
| `src/app/features/devops-agent/components/planning-dashboard/planning-dashboard.component.spec.ts`   | CREAR                          |
| `src/app/features/devops-agent/pages/devops-agent-home.page.spec.ts`                                 | CREAR                          |
| `docs/resultados/baseline-fase-01.md`                                                                | CREAR                          |
| `docs/resultados/RESULTADO-FASE-01.md`                                                               | CREAR                          |
| `docs/fases/fase-02.md`                                                                              | CREAR                          |
| `docs/plan/ESTADO.md`                                                                                | MODIFICAR                      |

### 1.5 Reglas aplicables

De `rules/angular-rules.md`:

- **§6 — Convenciones de Testing (Vitest en Angular).** Estructura `describe('GIVEN …')` →
  `describe('WHEN …')` → `it('THEN …')`. Mocks con `TestBed.configureTestingModule` y
  `provideHttpClient()` + `provideHttpClientTesting()` para red.
- **§5 — TypeScript estricto.** Prohibido `any`, **también en las pruebas**. Los mocks deben tiparse contra las interfaces de `devops-agent.model.ts`.
- **§7 — Política de No-Asunción.** Si una prueba revela un comportamiento que parece un defecto, **se documenta como hallazgo y se congela tal cual está**. Está prohibido «arreglarlo» aquí.

De `AGENTS.md` / `COMMIT_RULES.md`:

- Commit en formato `tipo(scope): descripcion en espanol`.

### 1.6 Decisiones pendientes que bloquean

| ID | Estado | Efecto sobre esta fase                                                           |
|----|--------|----------------------------------------------------------------------------------|
| —  | —      | **Ninguna.** Esta fase no modifica código productivo, así que no está bloqueada. |

> Las decisiones DP-02, DP-04, DP-06 y DP-09 se **alimentan** con las evidencias que produzca esta
> fase. Si las pruebas revelan datos nuevos, hay que actualizar
> `docs/plan/DECISIONES_PENDIENTES.md` antes de cerrar.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Registrar el baseline reproducible

Crear `docs/resultados/baseline-fase-01.md` con:

1. Salida de `pnpm test` (total de pruebas, pasadas, fallidas). Si hay fallos previos, **anotar el número exacto**: es la línea base intocable para todas las fases siguientes.
2. Salida de `pnpm build` (tamaño del bundle inicial y si respeta los presupuestos de
   `angular.json`: 500 kB de aviso / 1 MB de error).
3. Conteo de líneas por archivo de `src/app/**/*.ts` ordenado de mayor a menor.
4. Medición real de las métricas M-01 a M-12 del plan maestro §6, con el comando usado para obtener cada una.

Comandos sugeridos (macOS / zsh), a ejecutar desde `azure-devops-frontend/`:

```bash
# M-01, M-03, top de archivos por líneas
find src/app -name '*.ts' -not -name '*.spec.ts' -exec wc -l {} + | sort -rn | head -20

# M-04 — ocurrencias de `any`
grep -rn ': any\|<any>\|any\[\]' src/app --include='*.ts' | grep -v '.spec.ts'

# M-05 — suscripciones manuales
grep -rn '\.subscribe(' src/app --include='*.ts' | grep -v '.spec.ts'
grep -rn 'takeUntilDestroyed' src/app --include='*.ts'

# M-06 — temporizadores
grep -rn 'setTimeout\|setInterval' src/app --include='*.ts' | grep -v '.spec.ts'

# M-07 — OnPush
grep -rn 'ChangeDetectionStrategy' src/app --include='*.ts'

# M-08 / M-09 — cobertura de archivos de prueba
find src/app -name '*.spec.ts' | sort

# M-10 — endpoints literales
grep -rn "http\.\(get\|post\|put\|delete\)" src/app --include='*.ts' | grep -v '.spec.ts'

# M-11 — números mágicos candidatos
grep -rn '[^0-9a-zA-Z_]\(5000\|30000\|13\|50\|70\|80\|90\)[^0-9]' src/app --include='*.ts' | grep -v '.spec.ts'
```

> Los números que arroje esta tarea **sustituyen** a los valores «declarados» de
> `docs/plan/ESTADO.md` §4. Si difieren del plan maestro §2.1, corregir el plan maestro.

---

#### T-02 — Caracterizar F1 y F2: el aislamiento (inexistente) entre los dos chats

Ampliar `devops-agent-state.service.spec.ts`. Casos **obligatorios**:

|  # | GIVEN / WHEN                                     | THEN (comportamiento actual a congelar)                                                                |
|---:|--------------------------------------------------|--------------------------------------------------------------------------------------------------------|
|  1 | El servicio recién construido                    | `generalMessages` emite exactamente 1 mensaje de rol `agent`                                           |
|  2 | El servicio recién construido                    | `refinementMessages` emite exactamente 1 mensaje de rol `agent`                                        |
|  3 | El servicio recién construido                    | El texto de `generalMessages[0]` **difiere** del de `refinementMessages[0]`                            |
|  4 | `sendGeneralMessage('hola')`                     | El mensaje se añade **solo** a `generalMessages`; `refinementMessages` conserva su longitud            |
|  5 | `sendRefinementMessage('hola')`                  | El mensaje se añade **solo** a `refinementMessages`                                                    |
|  6 | `sendGeneralMessage('hola')` en curso            | **`loading` emite `true`** — congela D-16: el `loading` es compartido                                  |
|  7 | `sendGeneralMessage('   ')` (solo espacios)      | No se emite ningún mensaje y **no** se llama a la API                                                  |
|  8 | La API falla al enviar un mensaje general        | Se añade a `generalMessages` un mensaje de `agent` cuyo texto contiene `'Error de red'`                |
|  9 | Dos llamadas consecutivas a `sendGeneralMessage` | Los `contextId` de ambos mensajes son **idénticos** (misma conversación)                               |
| 10 | `clearGeneralChat()` tras enviar mensajes        | `generalMessages` queda con exactamente 1 mensaje y el `contextId` de los envíos siguientes **cambia** |
| 11 | `clearRefinementChat()`                          | `refinementMessages` queda con exactamente 1 mensaje                                                   |
| 12 | `clearGeneralChat()`                             | `refinementMessages` **no** se altera                                                                  |
| 13 | `messages` y `refinementMessages`                | Emiten el mismo valor — congela D-13                                                                   |
| 14 | `sendMessage('x')`                               | Es equivalente a `sendRefinementMessage('x')`, no a `sendGeneralMessage`                               |
| 15 | `refineStoryInChat('123', 'Titulo')`             | Se envía al flujo de **refinamiento** un texto que contiene `'123'` y `'Titulo'`                       |
| 16 | La API responde sin `message.parts[0].text`      | Se añade un mensaje con el texto de reserva actual                                                     |

> El caso 6 es el que documenta la deuda D-16. **Debe escribirse afirmando el comportamiento
> actual**, con un comentario que remita a D-16 y a DP-08. La Fase 05 lo invertirá de forma
> consciente.

---

#### T-03 — Caracterizar F3: Tablero de Calidad y SSE

En `devops-agent-state.service.spec.ts`, con un doble de `DevopsAgentApiService`:

|  # | GIVEN / WHEN                                     | THEN                                                                                    |
|---:|--------------------------------------------------|-----------------------------------------------------------------------------------------|
|  1 | `loadDashboardData('', 'S1')`                    | **No** se abre el stream (validación de argumentos vacíos)                              |
|  2 | `loadDashboardData('CelulaA', '')`               | **No** se abre el stream                                                                |
|  3 | `loadDashboardData('CelulaA', 'S1')`             | Se abre el stream con esos argumentos y `dashboardError` se reinicia a `null`           |
|  4 | Llega un evento `INITIAL` con datos              | `loading` pasa a `false` y `dashboardData` emite los datos                              |
|  5 | Llega un evento `BATCH_UPDATE`                   | `dashboardData` emite los datos actualizados                                            |
|  6 | El observable del stream emite error con mensaje | `dashboardData` emite `null` y `dashboardError` emite **ese mensaje**                   |
|  7 | El observable del stream emite error sin mensaje | `dashboardError` emite el texto de reserva actual del servicio                          |
|  8 | El stream completa                               | `loading` pasa a `false`                                                                |
|  9 | `auditStory('42')`                               | Se llama a la API con un `contextId` que contiene `'42'` y un texto que contiene `'42'` |
| 10 | `auditStory('42')`                               | **Se dispara además una recarga de tareas** — congela D-18 (efecto cruzado F3 → F5)     |

En `devops-agent-api.service.spec.ts`, ampliar la cobertura del `EventSource`:

|  # | GIVEN / WHEN                                     | THEN                                                                                                                  |
|---:|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| 11 | La URL del stream                                | Contiene `cell` y `sprint` **codificados** con `encodeURIComponent` (probar con un valor que contenga espacios y `&`) |
| 12 | Llega un evento con `data` que no es JSON válido | El observable **no** emite ni falla (se ignora silenciosamente)                                                       |
| 13 | Se dispara `onerror` del `EventSource`           | El observable **completa** (no falla) y el `EventSource` se cierra                                                    |
| 14 | Se desuscribe el consumidor                      | Se invoca `close()` sobre el `EventSource`                                                                            |

---

#### T-04 — Caracterizar F4: Gestión de Planeaciones

|  # | GIVEN / WHEN                                   | THEN                                                                                                                              |
|---:|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
|  1 | `uploadPlanning('id', 'titulo', '# md')`       | Se llama a la API con `{ initiativeId, title, markdownContent }`                                                                  |
|  2 | La carga tiene éxito                           | `uploadStatus` emite el texto de éxito actual **y se recargan las iniciativas**                                                   |
|  3 | La carga falla                                 | `uploadStatus` emite un texto que empieza por `'Error: '`                                                                         |
|  4 | En cualquier caso                              | `uploading` termina en `false`                                                                                                    |
|  5 | `clearUploadStatus()`                          | `uploadStatus` emite `null`                                                                                                       |
|  6 | `loadInitiatives()` con éxito                  | `initiatives` emite la lista y `loading` termina en `false`                                                                       |
|  7 | `loadInitiatives()` con error                  | `initiatives` **conserva** su valor previo y `loading` termina en `false`                                                         |
|  8 | `updateInitiativeCell('a', 'Nueva')` con éxito | Solo la iniciativa `'a'` cambia de célula; el resto queda intacto y **la referencia del objeto cambia** (actualización inmutable) |
|  9 | `deleteInitiative('a')` con éxito              | La iniciativa `'a'` desaparece de la lista                                                                                        |
| 10 | `deleteInitiative('a')` con error              | La lista **no** se altera                                                                                                         |

---

#### T-05 — Caracterizar F5: Tareas del Agente y polling

> ⚠️ El servicio arranca el temporizador en el constructor (`:95-98`). Usar los temporizadores
> falsos de Vitest (`vi.useFakeTimers()` en `beforeEach`, `vi.useRealTimers()` en `afterEach`) y
> **verificar en `afterEach` que no quedan temporizadores pendientes**. Esa verificación es la que
> deja constancia de D-02.

|  # | GIVEN / WHEN                                         | THEN                                                                     |
|---:|------------------------------------------------------|--------------------------------------------------------------------------|
|  1 | El servicio recién construido                        | Se solicita la lista de tareas **una vez** de forma inmediata            |
|  2 | Sin tareas activas, avanzan 30 000 ms                | Se vuelve a solicitar la lista de tareas                                 |
|  3 | Sin tareas activas, avanzan 29 999 ms                | **No** se vuelve a solicitar                                             |
|  4 | La respuesta incluye una tarea en estado `working`   | El siguiente sondeo ocurre a los 5 000 ms                                |
|  5 | La respuesta incluye una tarea en estado `submitted` | El siguiente sondeo ocurre a los 5 000 ms                                |
|  6 | Todas las tareas están en `completed`                | El siguiente sondeo vuelve a 30 000 ms                                   |
|  7 | La petición de tareas falla                          | El intervalo se restablece a 30 000 ms y el sondeo **continúa**          |
|  8 | La API devuelve `null`                               | `tasks` emite un array vacío, no `null`                                  |
|  9 | `triggerImmediatePoll()`                             | Se solicita la lista de inmediato y el intervalo pasa a 5 000 ms         |
| 10 | `cancelTask('t1')` con éxito                         | Se llama a la API de cancelación y **se recarga la lista**               |
| 11 | Se destruye el `TestBed`                             | **`vi.getTimerCount()` sigue siendo mayor que 0** — congela la fuga D-02 |

> El caso 11 documenta un defecto vivo. Debe llevar un comentario que remita a **D-02** y anotar
> que la Fase 06 lo invertirá.

---

#### T-06 — Pruebas de los 7 componentes tontos

Crear un `.spec.ts` por componente. Alcance mínimo por archivo: **el componente se construye y renderiza sin errores**, más los casos indicados:

| Componente            | Casos mínimos                                                                                                                                                                                                         |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `agent-info`          | Renderiza `name`, `version` y `description` del `@Input`; se comporta sin romperse con `card = null`                                                                                                                  |
| `chat-input`          | Emite `sendMessage` con el texto escrito; **no** emite con texto vacío o solo espacios; limpia el campo tras emitir; queda deshabilitado con `loading = true`; **fija la lista de sugerencias actual** (congela D-21) |
| `chat-messages`       | Renderiza N mensajes; distingue visualmente `user` de `agent`; muestra el indicador con `loading = true`; soporta `messages = null`                                                                                   |
| `info-cards`          | **Fija la lista de etiquetas de tecnología actual** (congela D-21)                                                                                                                                                    |
| `planning-upload`     | Emite `upload` con `{ initiativeId, title, content }`; no emite con campos obligatorios vacíos; muestra `uploadStatus`; se deshabilita con `uploading = true`                                                         |
| `planning-management` | Renderiza la tabla de iniciativas; abre y cierra el modal; dispara la edición de célula y el borrado                                                                                                                  |
| `planning-dashboard`  | Ver T-07                                                                                                                                                                                                              |

---

#### T-07 — Congelar las reglas de negocio del Tablero de Calidad

En `planning-dashboard.component.spec.ts`, **fijar los umbrales exactamente como están hoy**:

|  # | Entrada                                                                                     | THEN (comportamiento actual)                                                                                 |
|---:|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
|  1 | `getQualityLabel(90)`                                                                       | `'Excelente'`                                                                                                |
|  2 | `getQualityLabel(89)`                                                                       | `'Buena (Suficiente)'`                                                                                       |
|  3 | `getQualityLabel(70)`                                                                       | `'Buena (Suficiente)'`                                                                                       |
|  4 | `getQualityLabel(69)`                                                                       | `'Regular'`                                                                                                  |
|  5 | `getQualityLabel(50)`                                                                       | `'Regular'`                                                                                                  |
|  6 | `getQualityLabel(49)`                                                                       | `'Deficiente (Requiere Refinar)'`                                                                            |
|  7 | `getQualityBgClass(80)`                                                                     | La clase verde actual                                                                                        |
|  8 | `getQualityBgClass(79)`                                                                     | La clase ámbar actual                                                                                        |
|  9 | `getQualityBgClass(49)`                                                                     | La clase roja actual                                                                                         |
| 10 | Filtro `'critical'`                                                                         | Deja pasar solo los ítems con `qualityScore < 50`                                                            |
| 11 | Filtro `'regular'`                                                                          | Deja pasar solo `50 ≤ qualityScore < 80`                                                                     |
| 12 | Filtro `'good'`                                                                             | Deja pasar solo `qualityScore ≥ 80`                                                                          |
| 13 | Ítem con `qualityScore = 75`                                                                | Etiqueta `'Buena (Suficiente)'` **pero** cae en el filtro `'regular'` — congela la **discrepancia** de DP-02 |
| 14 | `getLargeStoriesCount()` con puntos `[12, 13, 21]`                                          | Devuelve `2` (umbral `>= 13`)                                                                                |
| 15 | `getStateClass('Done')` / `'closed'` / `'Committed'` / `'active'` / `'approved'` / `'otro'` | Las clases actuales, una prueba por estado                                                                   |
| 16 | Filtros de miembro y de estado combinados                                                   | Se aplican en conjunción (AND)                                                                               |
| 17 | Llega nueva `dashboardData`                                                                 | Los tres filtros se reinician y las filas expandidas se colapsan                                             |
| 18 | `generateAnalysis()` con célula o sprint en blanco                                          | **No** se dispara la carga                                                                                   |
| 19 | `refineStory(item)`                                                                         | Emite `refineRequested` y delega en el estado                                                                |
| 20 | `runDetailedAudit('42')` con éxito y con error                                              | Se refleja el contenido o el mensaje de error actual en `detailedAudits['42']`                               |

> El caso 13 es la evidencia dura que debe anexarse a **DP-02**. Anotarla en
> `docs/plan/DECISIONES_PENDIENTES.md`.

---

#### T-08 — Prueba de la página principal

En `devops-agent-home.page.spec.ts`, con un doble del servicio de estado:

| # | GIVEN / WHEN                               | THEN                                                                                                        |
|--:|--------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| 1 | Carga inicial                              | La pestaña activa es `'general'`                                                                            |
| 2 | Se pulsa cada pestaña                      | Se renderiza el contenido correspondiente (chat general, refinamiento, tablero, gestión)                    |
| 3 | Pestaña `'general'` o `'refinement'`       | El panel de carga de planeación **solo** aparece en `'refinement'` y `'management'`                         |
| 4 | El estado emite tareas en `working`        | `workingTasksCount` refleja el número correcto                                                              |
| 5 | Se pulsa el encabezado del panel de tareas | El panel se pliega y se despliega                                                                           |
| 6 | El componente se destruye                  | La suscripción a `tasks` se cierra (`ngOnDestroy` manual actual)                                            |
| 7 | `refineRequested` desde el tablero         | La pestaña activa pasa a `'refinement'`                                                                     |
| 8 | Botones de solo icono de la plantilla      | **Inventariar cuáles tienen `aria-label` y cuáles no**, y registrar el número en el baseline (métrica M-20) |

---

#### T-09 — Validaciones de cierre

1. `pnpm build` termina correctamente.
2. `pnpm test` termina con **el mismo número de fallos que el baseline de T-01, ni uno más**.
3. `git status --porcelain src/app` no lista **ningún** archivo que no termine en `.spec.ts`.
4. `grep -rn ': any' src/app --include='*.spec.ts'` no devuelve nada.

### 2.2 Qué NO hacer

1. **No modificar ni una línea** de ningún archivo de `src/app/` que no termine en `.spec.ts`. Ni una importación, ni un formato, ni un comentario.
2. **No arreglar** ninguno de los defectos que las pruebas revelen. Se documentan como hallazgo y se congelan. Corregirlos es trabajo de las fases 02 a 08.
3. **No unificar** los umbrales de calidad discrepantes (DP-02).
4. **No reescribir** ningún copy de cara al usuario (DP-04).
5. **No introducir** `core/`, `environments/` ni ninguna carpeta nueva bajo `src/app/`.
6. **No añadir dependencias** a `package.json`. Vitest, jsdom y las utilidades de Angular ya están disponibles.
7. **No usar `any`** en las pruebas: tipar los dobles contra las interfaces de
   `devops-agent.model.ts`.
8. **No borrar** pruebas existentes. Si una prueba actual resulta ser falsa (afirma algo que el código no hace), **reescribirla** y anotarlo como hallazgo.
9. **No renombrar** archivos ni símbolos.

### 2.3 Criterios de aceptación

- [ ] Existe `docs/resultados/baseline-fase-01.md` con las métricas M-01 a M-12 **medidas** y el comando usado para cada una.
- [ ] Los cinco flujos (F1–F5) tienen pruebas de caracterización según T-02 a T-05.
- [ ] Existen los **8 archivos `.spec.ts` nuevos** de T-06 y T-08 (7 componentes + 1 página).
- [ ] Los umbrales de calidad de T-07 están fijados por prueba, incluida la discrepancia del caso 13.
- [ ] La fuga del temporizador (D-02) está documentada por la prueba 11 de T-05.
- [ ] El acoplamiento del `loading` (D-16) está documentado por el caso 6 de T-02.
- [ ] El efecto cruzado F3 → F5 (D-18) está documentado por el caso 10 de T-03.
- [ ] `git status --porcelain src/app` solo lista archivos `.spec.ts`.
- [ ] `pnpm build` correcto.
- [ ] `pnpm test` sin fallos nuevos respecto al baseline.

### 2.4 Checklist de calidad (obligatoria, de `rules/angular-rules.md` §8)

- [ ] Pruebas en formato `describe('GIVEN …')` → `describe('WHEN …')` → `it('THEN …')` (§6)
- [ ] Mocks con `TestBed.configureTestingModule` y proveedores simplificados o `spyOn` (§6)
- [ ] `provideHttpClient()` + `provideHttpClientTesting()` en toda prueba con red (§6)
- [ ] `httpMock.verify()` en el `afterEach` de las pruebas de API (§6)
- [ ] Cero `any` en las pruebas (§5)
- [ ] Tipado explícito de los dobles contra `devops-agent.model.ts` (§5)
- [ ] Sin números mágicos en las pruebas: extraer `5000`, `30000` y los umbrales a constantes del propio archivo de prueba (§5)
- [ ] Cero copys inventados: los textos esperados se copian **literalmente** del código productivo (§7)
- [ ] Cero secretos, tokens o credenciales
- [ ] Cero componentes o servicios productivos modificados
- [ ] `pnpm build` correcto
- [ ] `pnpm test` sin fallos nuevos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                                   | Resultado esperado                                    |
|-----:|----------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                                        | Confirmado: ninguna DP bloquea la Fase 01             |
| P-02 | `pnpm install`                                                                                           | Dependencias resueltas                                |
| P-03 | `pnpm test` y anotar el resultado exacto                                                                 | Número de fallos del baseline fijado                  |
| P-04 | `pnpm build` y anotar el tamaño del bundle                                                               | Presupuestos de `angular.json` verificados            |
| P-05 | Ejecutar los comandos de medición de T-01                                                                | Métricas M-01 a M-12 reales obtenidas                 |
| P-06 | Escribir `docs/resultados/baseline-fase-01.md` (T-01)                                                    | Baseline publicado                                    |
| P-07 | Ampliar `devops-agent-state.service.spec.ts` con T-02 (F1 y F2)                                          | 16 casos nuevos verdes                                |
| P-08 | Ampliar `devops-agent-state.service.spec.ts` y `devops-agent-api.service.spec.ts` con T-03 (F3)          | 14 casos nuevos verdes                                |
| P-09 | Ampliar `devops-agent-state.service.spec.ts` con T-04 (F4)                                               | 10 casos nuevos verdes                                |
| P-10 | Ampliar `devops-agent-state.service.spec.ts` con T-05 (F5, temporizadores falsos)                        | 11 casos nuevos verdes                                |
| P-11 | Crear los 6 `.spec.ts` de componentes tontos de T-06                                                     | 6 archivos nuevos verdes                              |
| P-12 | Crear `planning-dashboard.component.spec.ts` con T-07                                                    | 20 casos verdes; discrepancia del caso 13 documentada |
| P-13 | Crear `devops-agent-home.page.spec.ts` con T-08                                                          | 8 casos verdes; inventario de `aria-label` registrado |
| P-14 | `pnpm test`                                                                                              | Sin fallos nuevos respecto a P-03                     |
| P-15 | `pnpm build`                                                                                             | Correcto                                              |
| P-16 | `git status --porcelain src/app`                                                                         | Solo archivos `.spec.ts`                              |
| P-17 | Actualizar `docs/plan/DECISIONES_PENDIENTES.md` con las evidencias halladas (DP-02, DP-04, DP-06, DP-09) | Decisiones enriquecidas con datos reales              |
| P-18 | Corregir el plan maestro §2.1 y §6 si las métricas medidas difieren de las declaradas                    | Plan maestro alineado con la realidad                 |
| P-19 | **Escribir `docs/resultados/RESULTADO-FASE-01.md`** con `_PLANTILLA_RESULTADO.md` y métricas reales      | Trazabilidad de la ejecución                          |
| P-20 | Actualizar `docs/plan/ESTADO.md`: Fase 01 🟢 COMPLETADA, métricas medidas, bitácora                      | Tablero al día                                        |
| P-21 | **Generar `docs/fases/fase-02.md`** con `docs/fases/_PLANTILLA_FASE.md`                                  | Checkpoint de continuidad creado                      |
| P-22 | Commit: `test(devops_agent): agregar pruebas de caracterizacion de los flujos del frontend`              | Cambios versionados                                   |

> ⚠️ **Los pasos P-19 y P-21 son innegociables.** Sin el registro de resultado no hay trazabilidad,
> y sin `fase-02.md` el trabajo no es reanudable tras una desconexión. En cualquiera de los dos
> casos la fase se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la Fase 02

`docs/fases/fase-02.md` debe redactarse con la plantilla obligatoria y contener, como mínimo:

**Nombre.** FASE 02 — Núcleo transversal: `core/`, configuración y erradicación de `any`.

**Riesgo.** Medio. Es la primera fase que toca código productivo.

**Decisiones que la bloquean.** **DP-03** (endpoint de cancelación), **DP-06** (intervalos de polling) y **DP-09** (tipos reales de las respuestas hoy declaradas como `any`). Si alguna sigue 🔴 ABIERTA, **detenerse y preguntar antes de empezar**.

**Deudas que salda.** D-05, D-10, D-14, D-20 (parcial), D-08 (parcial), D-09 (parcial).

**Archivos a crear.**

| Ruta                                                  | Contenido                                                                                                       |
|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `src/environments/environment.ts`                     | Configuración de producción con `apiBaseUrl`                                                                    |
| `src/environments/environment.development.ts`         | Configuración de desarrollo                                                                                     |
| `src/app/core/config/api-endpoints.ts`                | Los 11 endpoints de `devops-agent-api.service.ts:25-121` como constantes tipadas, incluida la decisión de DP-03 |
| `src/app/core/config/app-tuning.ts`                   | `POLLING_INTERVAL_IDLE_MS` y `POLLING_INTERVAL_ACTIVE_MS` con los valores de DP-06                              |
| `src/app/core/interceptors/http-error.interceptor.ts` | Interceptor funcional que normaliza el error HTTP en un tipo de dominio                                         |
| `src/app/core/services/notification.service.ts`       | Canal único de notificación al usuario                                                                          |
| `src/app/core/index.ts`                               | Barrel export                                                                                                   |
| Pruebas `.spec.ts` de cada artefacto nuevo            | GIVEN / WHEN / THEN                                                                                             |

**Archivos a modificar.**

| Ruta                                                                   | Cambio                                                                                                                |
|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `src/app/app.config.ts`                                                | Registrar el interceptor con `provideHttpClient(withInterceptors([...]))`                                             |
| `angular.json`                                                         | Declarar el `fileReplacements` de `environments` en la configuración de producción                                    |
| `src/app/features/devops-agent/models/devops-agent.model.ts`           | Añadir las interfaces que sustituyen a los 5 `any` (según DP-09)                                                      |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`   | Sustituir los literales de ruta por las constantes de `api-endpoints.ts` y los `any` por tipos reales                 |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts` | Sustituir `30000`/`5000` por las constantes y `Observable<any>` por `Observable<SendMessageResponse>` en `auditStory` |

**Criterio de cierre verificable.**

- `grep -rn ': any\|<any>\|any\[\]' src/app --include='*.ts' | grep -v '.spec.ts'` devuelve **cero**
  resultados (M-04).
- `grep -rn "http\.\(get\|post\|put\|delete\)('/" src/app --include='*.ts'` devuelve **cero**
  resultados: no queda ninguna ruta literal (M-10).
- Existe `src/app/core/{config,interceptors,services}` (M-13) y `src/environments/` (M-14).
- **Las 79 pruebas de caracterización de la Fase 01 siguen verdes sin modificarse.** Si alguna falla, el refactor cambió el comportamiento y debe revertirse.

**Prohibición explícita para la Fase 02.**

- No tocar componentes ni la separación de flujos: eso es de las fases 05 a 07.
- No cambiar el valor de ningún intervalo ni de ninguna ruta: solo **mover** literales a constantes.
- No modificar los archivos `.spec.ts` creados en la Fase 01. Son el contrato.

