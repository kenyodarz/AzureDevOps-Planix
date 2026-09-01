# RESULTADO — FASE 01: Baseline y red de seguridad

> **Ejecutada:** 2026-08-31 · **Estado final:** 🟢 COMPLETADA
> **Instrucciones:** `docs/fases/fase-01.md`
> **Baseline medido:** `docs/resultados/baseline-fase-01.md`
> **Código productivo modificado:** 1 archivo, exclusivamente por la excepción autorizada **S-01**

---

## 1. Objetivo de la fase

Construir la red de verificación que permita refactorizar el frontend en las fases 02 a 08 sin romper comportamiento: congelar los cinco flujos de negocio (F1–F5) con pruebas de caracterización, cubrir los nueve componentes que no tenían ninguna prueba y publicar el baseline **medido**, no declarado.

---

## 2. Qué se construyó

### 2.1 Archivos de prueba

| Archivo                                                                |  Antes | Después | Acción                     |
|------------------------------------------------------------------------|-------:|--------:|----------------------------|
| `services/devops-agent-state.service.spec.ts`                          |     10 |  **53** | REESCRITO por flujos F1–F5 |
| `services/devops-agent-api.service.spec.ts`                            |      9 |  **18** | AMPLIADO                   |
| `components/planning-dashboard/planning-dashboard.component.spec.ts`   |      — |  **36** | CREADO                     |
| `pages/devops-agent-home.page.spec.ts`                                 |      — |  **19** | CREADO                     |
| `components/planning-management/planning-management.component.spec.ts` |      — |  **17** | CREADO                     |
| `components/planning-upload/planning-upload.component.spec.ts`         |      — |  **12** | CREADO                     |
| `components/chat-input/chat-input.component.spec.ts`                   |      — |  **11** | CREADO                     |
| `components/chat-messages/chat-messages.component.spec.ts`             |      — |   **7** | CREADO                     |
| `components/agent-info/agent-info.component.spec.ts`                   |      — |   **2** | CREADO                     |
| `components/info-cards/info-cards.component.spec.ts`                   |      — |   **2** | CREADO                     |
| `shared/pipes/markdown-parser.pipe.spec.ts`                            |      8 |       8 | SIN CAMBIOS                |
| `app.spec.ts`                                                          |      2 |       2 | SIN CAMBIOS                |
| **Total**                                                              | **29** | **188** | **+159**                   |

### 2.2 Cobertura por flujo de negocio

| Flujo                                              |   Casos |
|----------------------------------------------------|--------:|
| Arranque del servicio de estado                    |       5 |
| **F1 / F2** — Chat General y Refinamiento          |      16 |
| **F3** — Tablero de Calidad (servicio)             |      10 |
| **F4** — Gestión de Planeaciones (servicio)        |      11 |
| **F5** — Tareas del Agente y sondeo                |      11 |
| Capa de API (REST + SSE)                           |      18 |
| Tablero de Calidad (reglas de negocio en la vista) |      36 |
| Página principal (pestañas, panel de tareas, a11y) |      19 |
| Gestión de Planeaciones (componente)               |      17 |
| Carga de planeación                                |      12 |
| Entrada y render de chat                           |      18 |
| Información del agente y tarjetas                  |       4 |
| Pipe de markdown                                   |       8 |
| Componente raíz                                    |       2 |
| **Total**                                          | **188** |

### 2.3 Documentos publicados

| Archivo                                | Contenido                                                                     |
|----------------------------------------|-------------------------------------------------------------------------------|
| `docs/resultados/baseline-fase-01.md`  | Baseline reproducible, 12 métricas medidas, 3 hallazgos, correcciones al plan |
| `docs/resultados/RESULTADO-FASE-01.md` | Este documento                                                                |
| `docs/fases/fase-02.md`                | Checkpoint de continuidad                                                     |

---

## 3. Deudas congeladas por prueba

Estas pruebas afirman comportamientos que el plan considera deuda. Cuando la fase indicada los invierta, la prueba fallará: esa es la señal de que el cambio fue **consciente**.

| ID        | Comportamiento congelado                                                  | Prueba                                                      | Invierte   |
|-----------|---------------------------------------------------------------------------|-------------------------------------------------------------|------------|
| **D-16**  | El `loading` es único: enviar en el Chat General bloquea el Refinamiento  | «a pending general message ALSO blocks the refinement flow» | Fase 05    |
| **D-18**  | `auditStory()` (F3) reinicia el sondeo global de tareas (F5)              | «auditStory ALSO restarts the global task polling»          | Fase 06    |
| **D-02**  | Destruir el inyector deja el temporizador vivo (`vi.getTimerCount() > 0`) | «destroying the injector leaves the polling timer alive»    | Fase 06    |
| **D-13**  | El alias `messages` refleja el flujo de refinamiento                      | «the legacy `messages` alias mirrors the refinement flow»   | Fase 05    |
| **D-08**  | `cancelTask` hace `POST '/'` con envoltorio JSON-RPC                      | «posts a JSON-RPC envelope to the ROOT path»                | Fase 02/04 |
| **D-21**  | Sugerencias y etiquetas hardcodeadas en los componentes                   | «shows exactly the current hardcoded …»                     | Fase 03    |
| **D-22**  | El auto-scroll agenda un `setTimeout` que nadie cancela                   | «schedules a deferred scroll that is never cancelled»       | Fase 08    |
| **D-23**  | El tablero no declara ni un solo `aria-label`                             | «the template currently declares no aria-label at all»      | Fase 07    |
| **D-04**  | La auditoría detallada se suscribe a la red desde el componente           | «WHEN a detailed audit is run»                              | Fase 07    |
| **D-26**  | `planning-management` llama a la API directamente                         | «calls the API service directly and opens the modal»        | Fase 05    |
| **D-27**  | Publicar iniciativas **no repinta** la tabla en modo zoneless             | «publishing initiatives alone does NOT repaint the table»   | Fase 05    |
| **DP-02** | Un `qualityScore` de 75 se etiqueta *Buena* pero se filtra como *regular* | «a score of 75 is labelled GOOD but filtered as REGULAR»    | Fase 07    |

---

## 4. Métricas obtenidas

|    # | Métrica                                  |                      Antes |      Después | Objetivo |
|-----:|------------------------------------------|---------------------------:|-------------:|---------:|
| M-18 | `pnpm build`                             |    ❌ **fallaba (TS2729)** | ✅ 434.21 kB |       ✅ |
| M-19 | `pnpm test`                              | ❌ **no podía ejecutarse** |   ✅ 188/188 |       ✅ |
| M-08 | Componentes con `.spec.ts`               |                      0 / 9 |    **9 / 9** |    100 % |
| M-09 | Archivos `.spec.ts`                      |                          4 |       **12** |     ≥ 18 |
|    — | Casos de prueba                          |                         29 |      **188** |        — |
| M-01 | Líneas `devops-agent-state.service.ts`   |                        406 |          418 |     ≤ 60 |
| M-03 | Líneas `planning-dashboard.component.ts` |                        753 |          753 |    ≤ 200 |
| M-04 | Ocurrencias de `any` en producción       |                          9 |            9 |        0 |
| M-05 | `.subscribe()` en producción             |                         17 |           17 |        — |
| M-06 | Temporizadores sin limpieza              |                          3 |            3 |        0 |
| M-07 | Componentes con `OnPush`                 |                      0 / 9 |        0 / 9 |    100 % |
| M-11 | Números mágicos                          |                         12 |           12 |        0 |
| M-12 | Señales `loading` por flujo              |                          1 |            1 |        4 |

> Las métricas de deuda **no mejoran en esta fase y no deben hacerlo**: la Fase 01 solo construye la
> red. Su única mejora es de cobertura y de que el proyecto vuelva a compilar.

---

## 5. Comandos ejecutados

```bash
# Preparación del entorno (registro npm interceptado por TLS corporativo)
mkdir -p ~/.ca
security find-certificate -a -p /System/Library/Keychains/SystemRootCertificates.keychain  > ~/.ca/corp-ca.pem
security find-certificate -a -p /Library/Keychains/System.keychain                        >> ~/.ca/corp-ca.pem
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0

corepack pnpm install     # 36.4s
corepack pnpm test        # 188 passed (188) — 12 archivos
corepack pnpm build       # 434.21 kB initial, sin advertencias de presupuesto

# Validaciones de cierre
grep -rn ': any\|<any>\|any\[\]' src/app --include='*.spec.ts'   # sin resultados
git status --porcelain src/app                                    # solo .spec.ts + S-01
```

---

## 6. Hallazgos

### H-1 🔴 — El repositorio no compilaba (bloqueante)

`pnpm build` y `pnpm test` fallaban con `TS2729` en `devops-agent-state.service.ts:24` y `:55`. La deuda **D-07** no era un riesgo latente sino un **fallo activo**. Sin corregirlo era imposible ejecutar una sola prueba. Resuelto mediante la excepción **S-01** (ver §8).

### H-2 🟠 — Un componente de presentación habla con la red

`planning-management.component.ts:190` inyecta `DevopsAgentApiService` directamente y se suscribe en
`:243`. Registrado como **D-26**, asignado a la Fase 05.

### H-3 🔴 — La tabla de planeaciones no se repinta (zoneless)

`app.config.ts` no declara zona, luego la app corre **zoneless**. `editableInitiatives` es un campo plano mutado desde una suscripción: con dos iniciativas cargadas y `loading() === false`, el DOM sigue mostrando *«No hay iniciativas indexadas»*.

Hoy funciona **por accidente**, porque `loadInitiatives()` conmuta el `loading$` compartido y ese signal es el que fuerza el repintado. **La Fase 05 separa el `loading` por flujo (D-16) y eso eliminará el único disparador reactivo**, rompiendo la vista.

> ⚠️ **Restricción heredada por la Fase 05:** migrar el estado de vista a `signal()` **antes** de
> separar el `loading`.

Registrado como **D-27**, severidad 🔴.

### H-4 🟠 — El diagnóstico inicial subestimó tres métricas

| Métrica             | Declarado | Medido |
|---------------------|----------:|-------:|
| M-04 `any`          |         5 |  **9** |
| M-05 `.subscribe()` |         4 | **17** |
| M-06 temporizadores |         1 |  **3** |

Corregido en el plan maestro §3 y en `ESTADO.md` §4.

### H-5 🟡 — Deudas menores no previstas

- **D-22** — `setTimeout` sin cancelar en `chat-input:81` y `chat-messages:114`.
- **D-23** — `planning-dashboard.component.ts`: 753 líneas, **cero** `aria-label`.
- **D-24** — `signal<any[]>` en `planning-management.component.ts:187`.
- **D-25** — No hay verificación de build en integración continua: por eso H-1 llegó a `main`.

---

## 7. Checklist de calidad

| Ítem (`rules/angular-rules.md` §8)                                     | Estado                              |
|------------------------------------------------------------------------|-------------------------------------|
| Pruebas en formato GIVEN / WHEN / THEN                                 | ✅ 12 archivos                      |
| Mocks con `TestBed.configureTestingModule`                             | ✅                                  |
| `provideHttpClient()` + `provideHttpClientTesting()` en pruebas de red | ✅                                  |
| `httpMock.verify()` en `afterEach`                                     | ✅                                  |
| Cero `any` en las pruebas                                              | ✅ verificado por `grep`            |
| Dobles tipados contra `devops-agent.model.ts`                          | ✅                                  |
| Sin números mágicos en las pruebas                                     | ✅ constantes nombradas por archivo |
| Cero copys inventados                                                  | ✅ copiados literalmente del código |
| Cero secretos                                                          | ✅                                  |
| Cero componentes o servicios productivos modificados                   | ⚠️ **1 excepción autorizada: S-01** |
| `pnpm build`                                                           | ✅                                  |
| `pnpm test` sin fallos                                                 | ✅ 188/188                          |

---

## 8. Desviaciones respecto a las instrucciones

### S-01 — Reordenamiento de declaraciones para restaurar el build

| Aspecto          | Detalle                                                                                                                                           |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Motivo**       | H-1: el repositorio no compilaba. La Fase 01 era inejecutable.                                                                                    |
| **Archivo**      | `src/app/features/devops-agent/services/devops-agent-state.service.ts`                                                                            |
| **Naturaleza**   | Puramente mecánica: reordenar campos en 3 bloques con dependencia lineal (constantes de saludo → Subjects privados → Observables públicos).       |
| **No se tocó**   | Ni un carácter de los textos, ninguna firma, ningún nombre, ningún método, ninguna lógica, ningún valor. El alias `messages` se conserva íntegro. |
| **Verificación** | `pnpm build` correcto y las 29 pruebas preexistentes verdes **sin modificarlas**.                                                                 |
| **Efecto**       | **D-07 queda saldada.** La Fase 03 pierde esa responsabilidad y conserva D-06, D-19 y D-21.                                                       |

### S-02 — Reescritura de `devops-agent-state.service.spec.ts`

En lugar de ampliar el archivo, se reorganizó por flujos F1–F5. **Ninguna aserción previa se perdió**: las 10 originales están cubiertas por las 53 nuevas. Motivo: el archivo original mezclaba los cinco flujos en describes planos, exactamente el problema que el plan quiere erradicar.

### S-03 — Preparación del entorno de red

El registro npm estaba interceptado por TLS corporativo. Se resolvió exportando las CA del llavero de macOS a `~/.ca/corp-ca.pem` y usando `NODE_EXTRA_CA_CERTS`. **`strict-ssl` sigue en `true`**, no se añadió ningún `.npmrc` al repositorio ni se desactivó ninguna comprobación.

### S-04 — Ajuste de dos aserciones frente al hallazgo H-3

Las pruebas de `planning-management` no podían escribirse como se preveía porque el componente no repinta. En vez de forzar un `markForCheck` artificial, se optó por **documentar el defecto con una prueba explícita** y reproducir en las demás la secuencia real de producción (rebote del `loading`).

---

## 9. Estado al cerrar

- **Fase 01:** 🟢 COMPLETADA.
- **Siguiente fase generada:** `docs/fases/fase-02.md`.
- **`docs/plan/ESTADO.md`:** actualizado con las métricas medidas.
- **`docs/plan/plan-maestro.md`:** actualizado (D-07 saldada; D-22 a D-27 registradas; métricas corregidas).
- **Deuda saldada en esta fase:** D-07 (vía S-01), D-15 parcialmente (0/9 → 9/9 componentes con pruebas).
- **Deudas nuevas:** D-22, D-23, D-24, D-25, D-26, D-27.
- **Decisiones abiertas:** DP-01 · DP-02 (con evidencia nueva) · DP-03 · DP-04 · DP-05 · DP-06 · DP-08 · DP-09.
- **Restricción crítica heredada:** la Fase 05 debe migrar el estado de vista a signals **antes** de separar el `loading` por flujo (H-3 / D-27).

