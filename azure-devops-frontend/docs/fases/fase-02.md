# FASE 02 — Núcleo transversal: `core/`, configuración y erradicación de `any`

> **Estado:** 🔴 BLOQUEADA (por DP-03, DP-06 y DP-09) · **Depende de:** FASE 01 🟢 · **Riesgo:** Medio
> **Commit al cerrar:** `refactor(core): crear capa core con configuracion, interceptor y tipos sin any`
> **Siguiente fase:** `fase-03.md`
> **Regla de oro:** las **188 pruebas de la Fase 01 deben seguir verdes SIN modificarlas.**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 01 cerró el 2026-08-31 (`docs/resultados/RESULTADO-FASE-01.md`) dejando:

- **188 pruebas verdes en 12 archivos** (antes: 29 en 4). Los 9 componentes tienen `.spec.ts`.
- El proyecto **vuelve a compilar**: se descubrió que estaba roto (`TS2729`) y se corrigió con la excepción **S-01**, que saldó **D-07**.
- Baseline medido en `docs/resultados/baseline-fase-01.md`, que **corrige** el diagnóstico inicial.
- Seis deudas nuevas: **D-22 a D-27**.

**Entorno de ejecución (obligatorio en esta máquina).** El registro npm está interceptado por TLS corporativo. Antes de cualquier comando:

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
# usar siempre: corepack pnpm <comando>
```

Si `~/.ca/corp-ca.pem` no existe, regenerarlo:

```bash
mkdir -p ~/.ca
security find-certificate -a -p /System/Library/Keychains/SystemRootCertificates.keychain  > ~/.ca/corp-ca.pem
security find-certificate -a -p /Library/Keychains/System.keychain                        >> ~/.ca/corp-ca.pem
```

> **Prohibido** añadir `.npmrc` al repositorio o poner `strict-ssl=false`.

### 1.2 Problema que resuelve esta fase

`rules/angular-rules.md` §1 exige una capa `core/{guards,interceptors,services}`. **No existe.**
Las consecuencias medidas en la Fase 01:

| Deuda           |                                  Medición | Detalle                                                  |
|-----------------|------------------------------------------:|----------------------------------------------------------|
| **D-10**        |                                         — | Sin `core/`. Cero interceptores HTTP.                    |
| **D-05 + D-24** |                **9** ocurrencias de `any` | Prohibido sin excepciones por §5                         |
| **D-08**        | **10** llamadas `http.*` con ruta literal | Incluye un `POST '/'` opaco                              |
| **D-09**        |               `30000`, `5000` en 4 puntos | Números mágicos (§5)                                     |
| **D-14**        |                                         — | Sin `environments/`; la red depende de `proxy.conf.json` |
| **D-20**        |                    **10** `console.error` | Fallos silenciosos para el usuario                       |

#### Las 9 ocurrencias de `any` (baseline §4.2)

| Archivo                                                           | Línea | Ocurrencia                                    |
|-------------------------------------------------------------------|------:|-----------------------------------------------|
| `components/planning-management/planning-management.component.ts` |   187 | `signal<any[]>([])`                           |
| `services/devops-agent-state.service.ts`                          |  ~304 | `auditStory(id): Observable<any>`             |
| `services/devops-agent-state.service.ts`                          |  ~415 | `getInitiativeChunks(id): Observable<any[]>`  |
| `services/devops-agent-api.service.ts`                            |    32 | `uploadPlanning(...): Observable<any>`        |
| `services/devops-agent-api.service.ts`                            |    33 | `this.http.post<any>(...)`                    |
| `services/devops-agent-api.service.ts`                            |   104 | `getInitiativeChunks(...): Observable<any[]>` |
| `services/devops-agent-api.service.ts`                            |   105 | `this.http.get<any[]>(...)`                   |
| `services/devops-agent-api.service.ts`                            |   112 | `cancelTask(...): Observable<any>`            |
| `services/devops-agent-api.service.ts`                            |   121 | `this.http.post<any>('/', payload)`           |

#### Los 10 endpoints literales (`devops-agent-api.service.ts`)

| Línea | Método | Ruta                                           |
|------:|--------|------------------------------------------------|
|    25 | GET    | `/.well-known/agent-card.json`                 |
|    29 | POST   | `/message:send`                                |
|    33 | POST   | `/api/planning/ingest`                         |
|    37 | GET    | `/api/planning/initiatives`                    |
|    41 | DELETE | `/api/planning/initiatives/${id}`              |
|    45 | PUT    | `/api/planning/initiatives/${id}/cell`         |
|    50 | GET    | `/api/devops/dashboard`                        |
|    64 | (SSE)  | `/api/devops/dashboard/stream?cell=…&sprint=…` |
|   105 | GET    | `/api/planning/initiatives/${id}/chunks`       |
|   109 | GET    | `/api/tasks`                                   |
|   121 | POST   | `/` ⚠️ **ruta raíz — ver DP-03**               |

### 1.3 Estado esperado al terminar

1. Existe `src/app/core/{config,interceptors,services}` con barrel export.
2. Existe `src/environments/` con reemplazo declarado en `angular.json`.
3. `grep -rn ': any\|<any>\|any\[\]' src/app --include='*.ts' | grep -v spec` devuelve **0**.
4. No queda ninguna ruta literal en llamadas `http.*`.
5. `30000` y `5000` viven en una constante nombrada.
6. Existe un interceptor funcional que normaliza el error HTTP.
7. **Las 188 pruebas de la Fase 01 siguen verdes sin haber tocado un solo `.spec.ts` existente.**

### 1.4 Archivos involucrados

| Ruta                                                                                            | Acción                                             |
|-------------------------------------------------------------------------------------------------|----------------------------------------------------|
| `src/environments/environment.ts`                                                               | CREAR                                              |
| `src/environments/environment.development.ts`                                                   | CREAR                                              |
| `src/app/core/config/api-endpoints.ts`                                                          | CREAR                                              |
| `src/app/core/config/app-tuning.ts`                                                             | CREAR                                              |
| `src/app/core/interceptors/http-error.interceptor.ts`                                           | CREAR                                              |
| `src/app/core/interceptors/http-error.interceptor.spec.ts`                                      | CREAR                                              |
| `src/app/core/services/notification.service.ts`                                                 | CREAR                                              |
| `src/app/core/services/notification.service.spec.ts`                                            | CREAR                                              |
| `src/app/core/index.ts`                                                                         | CREAR                                              |
| `angular.json`                                                                                  | MODIFICAR (`fileReplacements`)                     |
| `src/app/app.config.ts`                                                                         | MODIFICAR (registrar interceptor)                  |
| `src/app/features/devops-agent/models/devops-agent.model.ts`                                    | MODIFICAR (tipos que sustituyen `any`)             |
| `src/app/features/devops-agent/services/devops-agent-api.service.ts`                            | MODIFICAR                                          |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`                          | MODIFICAR (solo constantes y tipo de `auditStory`) |
| `src/app/features/devops-agent/components/planning-management/planning-management.component.ts` | MODIFICAR (solo el `signal<any[]>` de `:187`)      |
| **Cualquier `.spec.ts` existente**                                                              | **NO TOCAR**                                       |

### 1.5 Reglas aplicables

De `rules/angular-rules.md`:

- **§1** — Estructura de directorios: `core/{guards,interceptors,services}` y `shared/`.
- **§5** — Prohibido `any`; usar `unknown` con *type guard* o definir la interfaz de contrato. Tipado explícito en retornos públicos. Sin números mágicos.
- **§6** — Pruebas Vitest GIVEN/WHEN/THEN con `provideHttpClient()` + `provideHttpClientTesting()`.
- **§7** — Política de No-Asunción: **prohibido inventar el contrato de una respuesta**.

De la raíz del monorepo: sin secretos ni URLs sensibles en código (`.github/copilot-instructions.md`).

### 1.6 Decisiones pendientes que bloquean

| ID        | Pregunta                                                                           | Estado     | Acción si sigue ABIERTA                                                                                                                                 |
|-----------|------------------------------------------------------------------------------------|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-03** | ¿`cancelTask` sigue en `POST '/'` (JSON-RPC) o pasa a una ruta explícita del BFF?  | 🔴 ABIERTA | Conservar `POST '/'` tal cual, moviéndolo a `api-endpoints.ts` como `TASKS_CANCEL_RPC = '/'` con comentario que remita a DP-03. **No cambiar la ruta.** |
| **DP-06** | ¿`5 s` / `30 s` de sondeo están confirmados? ¿Hay tope de reintentos?              | 🔴 ABIERTA | Trasladar los valores actuales sin modificarlos y **sin añadir topes**.                                                                                 |
| **DP-09** | ¿Cuál es el contrato real de `/api/planning/ingest`, `/…/chunks` y la cancelación? | 🔴 ABIERTA | **DETENERSE Y PREGUNTAR** para esos tres. `auditStory` sí puede tiparse: es `SendMessageResponse`, ya usado en `planning-dashboard.component.ts:743`.   |

> ⚠️ **DP-09 es el bloqueo duro.** Inventar interfaces produce un tipado falso, peor que `any`.
> Alternativa aceptable si el usuario lo autoriza: derivar los tipos de `azure-devops-backend`,
> que es la fuente de verdad del contrato.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar el punto de partida

```bash
corepack pnpm install
corepack pnpm test    # DEBE dar 188 passed (188)
corepack pnpm build   # DEBE dar 434.21 kB initial
```

Si el número de pruebas no es 188, **detenerse**: el árbol no está en el estado que dejó la Fase 01.

#### T-02 — Crear `src/environments/`

```ts
// src/environments/environment.ts
export const environment = {
  production: true,
  apiBaseUrl: '',   // rutas relativas: el BFF se sirve tras el mismo origen
} as const;
```

```ts
// src/environments/environment.development.ts
export const environment = {
  production: false,
  apiBaseUrl: '',   // en desarrollo lo resuelve proxy.conf.json
} as const;
```

> **No inventar URLs.** Hoy todas las rutas son relativas y las resuelve `proxy.conf.json`
> (`localhost:8081` y `localhost:8082`). `apiBaseUrl` queda vacío para **no cambiar comportamiento**.

En `angular.json`, dentro de `configurations.development`, añadir:

```json
"fileReplacements": [
  {
    "replace": "src/environments/environment.ts",
    "with": "src/environments/environment.development.ts"
  }
]
```

#### T-03 — Crear `src/app/core/config/api-endpoints.ts`

Trasladar los **10 endpoints** de §1.2 a constantes tipadas. Firmas exactas:

```ts
export const API_ENDPOINTS = {
  AGENT_CARD: '/.well-known/agent-card.json',
  MESSAGE_SEND: '/message:send',
  PLANNING_INGEST: '/api/planning/ingest',
  PLANNING_INITIATIVES: '/api/planning/initiatives',
  DEVOPS_DASHBOARD: '/api/devops/dashboard',
  DEVOPS_DASHBOARD_STREAM: '/api/devops/dashboard/stream',
  TASKS: '/api/tasks',
  /** DP-03 abierta: la cancelación viaja como JSON-RPC contra la RAÍZ. No cambiar sin resolverla. */
  TASKS_CANCEL_RPC: '/',
} as const;

export const initiativeUrl = (id: string): string =>
  `${API_ENDPOINTS.PLANNING_INITIATIVES}/${id}`;
export const initiativeCellUrl = (id: string): string => `${initiativeUrl(id)}/cell`;
export const initiativeChunksUrl = (id: string): string => `${initiativeUrl(id)}/chunks`;
```

#### T-04 — Crear `src/app/core/config/app-tuning.ts`

```ts
/** DP-06 abierta: valores trasladados literalmente del código actual. No modificar. */
export const POLLING_INTERVAL_IDLE_MS = 30000;
export const POLLING_INTERVAL_ACTIVE_MS = 5000;
```

#### T-05 — Crear el interceptor de errores HTTP

`src/app/core/interceptors/http-error.interceptor.ts`: interceptor **funcional**
(`HttpInterceptorFn`) que:

1. Captura `HttpErrorResponse`.
2. Extrae un mensaje legible con la MISMA precedencia que usa hoy el código:
   `error.error || error.message || <mensaje por defecto>`.
3. Notifica vía `NotificationService`.
4. **Vuelve a lanzar el error** (`throwError`), para no alterar el comportamiento de los
   `subscribe({ error })` existentes.

> ⚠️ **Crítico:** si el interceptor «se traga» el error, romperá las pruebas de caracterización de
> la Fase 01 que verifican el mensaje `'Error de red'` en el chat y el texto de `dashboardError`.

#### T-06 — Crear `NotificationService`

`src/app/core/services/notification.service.ts`: canal único con una señal de notificaciones. En esta fase **solo se registra**; la sustitución de los 10 `console.error` es de la Fase 08 (D-20).

#### T-07 — Erradicar los 9 `any` (⚠️ requiere DP-09)

| Ocurrencia                                      | Sustitución                                                       |
|-------------------------------------------------|-------------------------------------------------------------------|
| `auditStory(): Observable<any>`                 | `Observable<SendMessageResponse>` — **seguro, no requiere DP-09** |
| `uploadPlanning(): Observable<any>`             | Requiere DP-09                                                    |
| `getInitiativeChunks(): Observable<any[]>` (×2) | Requiere DP-09 → definir `PlanningChunk`                          |
| `cancelTask(): Observable<any>`                 | Requiere DP-09 → definir `JsonRpcResponse`                        |
| `signal<any[]>` en `planning-management:187`    | `signal<PlanningChunk[]>` (deriva de DP-09)                       |

Añadir a `devops-agent.model.ts` únicamente las interfaces cuyo contrato esté **confirmado**.

#### T-08 — Sustituir literales por constantes

En `devops-agent-api.service.ts` y `devops-agent-state.service.ts`: reemplazar rutas e intervalos por las constantes de T-03 y T-04. **Ni un valor puede cambiar.**

#### T-09 — Registrar el interceptor

```ts
// src/app/app.config.ts
provideHttpClient(withInterceptors([httpErrorInterceptor])),
```

#### T-10 — Validaciones de cierre

```bash
corepack pnpm test    # 188 + las nuevas de core/, cero fallos
corepack pnpm build
grep -rn ': any\|<any>\|any\[\]' src/app --include='*.ts' | grep -v '.spec.ts'   # vacío
grep -rn "http\.\(get\|post\|put\|delete\)<.*>('/" src/app --include='*.ts' | grep -v '.spec.ts'  # vacío
git diff --stat -- 'src/app/**/*.spec.ts'   # SOLO archivos nuevos de core/
```

### 2.2 Qué NO hacer

1. **No modificar ningún `.spec.ts` existente.** Son el contrato de la Fase 01. Si uno falla, el refactor cambió comportamiento: **revertir**, no adaptar la prueba.
2. **No cambiar ninguna ruta**, ni siquiera el `POST '/'` (DP-03).
3. **No cambiar ningún intervalo** de sondeo (DP-06).
4. **No inventar interfaces** para contratos no confirmados (DP-09).
5. **No tocar componentes** salvo la línea 187 de `planning-management`.
6. **No abordar la separación de flujos, los stores ni el `loading`**: son de las fases 05-07.
7. **No sustituir todavía los `console.error`** por notificaciones (D-20 → Fase 08). El interceptor se crea y se registra, pero el flujo de errores existente no se altera.
8. **No añadir dependencias** a `package.json`.
9. **No introducir `strict-ssl=false`** ni `.npmrc`.

### 2.3 Criterios de aceptación

- [ ] Existe `src/app/core/{config,interceptors,services}` con `index.ts`.
- [ ] Existe `src/environments/` y `angular.json` declara el `fileReplacements`.
- [ ] Cero `any` en código productivo (verificado por `grep`).
- [ ] Cero rutas literales en llamadas `http.*`.
- [ ] `POLLING_INTERVAL_IDLE_MS` y `POLLING_INTERVAL_ACTIVE_MS` en uso.
- [ ] Interceptor funcional registrado en `app.config.ts` que **relanza** el error.
- [ ] Pruebas nuevas para el interceptor y el `NotificationService`.
- [ ] **Las 188 pruebas de la Fase 01 verdes sin modificarse.**
- [ ] `corepack pnpm build` correcto y sin superar el presupuesto de 500 kB.

### 2.4 Checklist de calidad (`rules/angular-rules.md` §8)

- [ ] Componentes tocados siguen siendo `standalone: true`
- [ ] Cero `*ngIf` / `*ngFor` / `*ngSwitch`
- [ ] Estado reactivo con Signals u Observables seguros
- [ ] Cero fugas de memoria nuevas
- [ ] Botones de solo icono con `aria-label`
- [ ] Cero `any`
- [ ] Cero llamadas HTTP fuera de la capa de API
- [ ] Tipado explícito en métodos públicos
- [ ] Sin números mágicos
- [ ] Pruebas GIVEN / WHEN / THEN
- [ ] Sin copys ni rutas inventadas (§7)
- [ ] Sin secretos
- [ ] `corepack pnpm build` correcto
- [ ] `corepack pnpm test` sin fallos nuevos

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                   | Resultado esperado                                  |
|-----:|------------------------------------------------------------------------------------------|-----------------------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                        | Confirmar DP-03, DP-06 y DP-09                      |
| P-02 | **Si DP-09 sigue 🔴 ABIERTA: DETENERSE y preguntar al usuario**                          | Bloqueo resuelto                                    |
| P-03 | Exportar `NODE_EXTRA_CA_CERTS` y ejecutar `corepack pnpm install`                        | Dependencias listas                                 |
| P-04 | `corepack pnpm test`                                                                     | 188 passed (188)                                    |
| P-05 | Crear `src/environments/` y el `fileReplacements` (T-02)                                 | `corepack pnpm build` correcto                      |
| P-06 | Crear `core/config/api-endpoints.ts` (T-03)                                              | Constantes tipadas                                  |
| P-07 | Crear `core/config/app-tuning.ts` (T-04)                                                 | Intervalos nombrados                                |
| P-08 | Crear `NotificationService` + prueba (T-06)                                              | Verde                                               |
| P-09 | Crear el interceptor + prueba (T-05)                                                     | Verde; **relanza el error**                         |
| P-10 | Registrar el interceptor en `app.config.ts` (T-09)                                       | `corepack pnpm test`: 188 siguen verdes             |
| P-11 | Añadir interfaces confirmadas a `devops-agent.model.ts` (T-07)                           | Compila                                             |
| P-12 | Erradicar los 9 `any` (T-07)                                                             | `grep` vacío                                        |
| P-13 | Sustituir rutas e intervalos por constantes (T-08)                                       | `grep` vacío                                        |
| P-14 | `corepack pnpm test`                                                                     | Cero fallos; ningún `.spec.ts` de la Fase 01 tocado |
| P-15 | `corepack pnpm build`                                                                    | Correcto, dentro del presupuesto                    |
| P-16 | Actualizar `DECISIONES_PENDIENTES.md` con lo resuelto                                    | Bitácora al día                                     |
| P-17 | **Escribir `docs/resultados/RESULTADO-FASE-02.md`** con `_PLANTILLA_RESULTADO.md`        | Trazabilidad                                        |
| P-18 | Actualizar `docs/plan/ESTADO.md`: Fase 02 🟢 + métricas                                  | Tablero al día                                      |
| P-19 | **Generar `docs/fases/fase-03.md`** con `_PLANTILLA_FASE.md`                             | Continuidad                                         |
| P-20 | Commit: `refactor(core): crear capa core con configuracion, interceptor y tipos sin any` | Versionado                                          |

> ⚠️ **P-17 y P-19 son innegociables.** Sin resultado no hay trazabilidad y sin `fase-03.md` el
> trabajo no es reanudable. En cualquiera de los dos casos la fase se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la Fase 03

**Nombre.** FASE 03 — Externalización de copys y prompts. **Riesgo.** Bajo. **Bloqueada por:** DP-04 y DP-07. **Deudas.** D-06, D-19, D-21. *(D-07 ya fue saldada en la Fase 01: no incluirla.)*

**Archivos a crear**

| Ruta                                               | Contenido                                                                                        |
|----------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `features/devops-agent/domain/chat-greetings.ts`   | `GENERAL_GREETING`, `REFINEMENT_GREETING`, `GENERAL_RESET_GREETING`, `REFINEMENT_RESET_GREETING` |
| `features/devops-agent/domain/chat-prompts.ts`     | `buildRefinementPrompt(id, title)`, `buildAuditPrompt(id)`                                       |
| `features/devops-agent/domain/chat-suggestions.ts` | Las 3 sugerencias de `chat-input:52-59`                                                          |
| `features/devops-agent/domain/tech-tags.ts`        | Las 5 etiquetas de `info-cards:37`                                                               |

**Restricción absoluta.** La Fase 03 **mueve** los textos: prohibido alterar un solo carácter (DP-04). Las pruebas de la Fase 01 los verifican literalmente y deben seguir verdes:

- `«shows exactly the current hardcoded suggestions»` (chat-input)
- `«shows exactly the current hardcoded technology tags»` (info-cards)
- `«the two greetings are different texts»` y `«clearRefinementChat … contiene "Chat limpio"»`

**Criterio de cierre.** Cero copys markdown dentro de `devops-agent-state.service.ts`
(hoy ~90 líneas) y las 188+ pruebas verdes sin modificarse.

