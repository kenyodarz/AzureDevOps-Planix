# RESULTADO — FASE 02: Núcleo transversal `core/`, configuración y erradicación de `any`

> **Ejecutada:** 2026-08-31 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(core): crear capa core con configuracion, interceptor y tipos sin any`
> **Instrucciones:** `docs/fases/fase-02.md`
> **Código productivo modificado:** 5 archivos existentes tocados · 9 archivos nuevos ·
> **cero `.spec.ts` de la Fase 01 modificados**

---

## 1. Objetivo de la fase

Crear la capa `core/` que `rules/angular-rules.md` §1 exige y que **no existía**, externalizar la configuración transversal (rutas del BFF, intervalos de sondeo, entornos) y erradicar por completo el uso de `any` en código productivo. Se saldan **D-05, D-24, D-10, D-14** y parcialmente **D-08, D-09 y D-20**, sin alterar un solo comportamiento observable: las 188 pruebas de caracterización de la Fase 01 siguen verdes **sin haber tocado ninguna**.

---

## 2. Qué se construyó

| Artefacto                                                           | Acción        | Detalle                                                         |
|---------------------------------------------------------------------|---------------|-----------------------------------------------------------------|
| `src/environments/environment.ts`                                   | **CREADO**    | `production: true`, `apiBaseUrl: ''` (D-14)                     |
| `src/environments/environment.development.ts`                       | **CREADO**    | `production: false`, `apiBaseUrl: ''`                           |
| `src/app/core/config/api-endpoints.ts`                              | **CREADO**    | 8 constantes + 4 constructores de URL (D-08)                    |
| `src/app/core/config/app-tuning.ts`                                 | **CREADO**    | `POLLING_INTERVAL_IDLE_MS`, `POLLING_INTERVAL_ACTIVE_MS` (D-09) |
| `src/app/core/interceptors/http-error.interceptor.ts`               | **CREADO**    | `HttpInterceptorFn` que notifica y **relanza** (D-10)           |
| `src/app/core/services/notification.service.ts`                     | **CREADO**    | Canal único basado en `signal()` (semilla de D-20)              |
| `src/app/core/index.ts`                                             | **CREADO**    | Barrel export de la capa                                        |
| `angular.json`                                                      | MODIFICADO    | `fileReplacements` en `configurations.development`              |
| `src/app/app.config.ts`                                             | MODIFICADO    | `provideHttpClient(withInterceptors([httpErrorInterceptor]))`   |
| `…/models/devops-agent.model.ts`                                    | MODIFICADO    | +5 tipos derivados del backend (DP-09)                          |
| `…/services/devops-agent-api.service.ts`                            | MODIFICADO    | 10 rutas → constantes · 5 `any` → tipos                         |
| `…/services/devops-agent-state.service.ts`                          | MODIFICADO    | 4 literales de sondeo → constantes · 2 `any` → tipos            |
| `…/components/planning-management/planning-management.component.ts` | MODIFICADO    | Solo `signal<any[]>` → `signal<PlanningChunk[]>` (D-24)         |
| **Cualquier `.spec.ts` de la Fase 01**                              | **NO TOCADO** | `git diff --stat -- 'src/app/**/*.spec.ts'` → **vacío** ✅      |

### 2.1 Pruebas nuevas o reescritas

| Archivo                                            |    Casos antes |           Casos después |
|----------------------------------------------------|---------------:|------------------------:|
| `core/services/notification.service.spec.ts`       | 0 (no existía) |                   **5** |
| `core/interceptors/http-error.interceptor.spec.ts` | 0 (no existía) |                   **7** |
| *Todas las demás (Fase 01)*                        |            188 | **188 — sin modificar** |
| **TOTAL**                                          |        **188** |                 **200** |

Las 12 pruebas nuevas siguen el formato GIVEN / WHEN / THEN y usan
`provideHttpClient()` + `provideHttpClientTesting()` conforme a §6.

---

## 3. Decisiones aplicadas

| ID        | Resolución                                                                                                                                                                                         | Efecto en esta fase                                                                                                                                                                              |
|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-09** | 🟢 **RESUELTA** (2026-08-31): el usuario autorizó **derivar los tipos de `azure-devops-backend` / `azure-devops-agent`**, la fuente de verdad del contrato.                                        | Se crearon `IngestResponse`, `PlanningChunk`, `JsonRpcError`, `JsonRpcResponse<T>` y `CancelTaskResponse`. **Ningún campo fue inventado**; cada uno cita su archivo de origen.                   |
| **DP-03** | 🟢 **RESUELTA** (2026-08-31): «vamos por la acción recomendada» → conservar `POST '/'`.                                                                                                            | `TASKS_CANCEL_RPC = '/'` en `api-endpoints.ts`, con comentario que remite a DP-03. **La ruta no cambió.**                                                                                        |
| **DP-06** | 🟡 **PARCIAL** (2026-08-31): el usuario aclaró que los intervalos *«no eran para actualizar el dashboard en vivo»* y que **le quedó como deuda técnica reconocida**. No confirmó valores ni topes. | Se aplica la **acción por defecto**: valores trasladados literalmente (`30000`, `5000`), **sin modificarlos y sin añadir topes**. Documentado en `app-tuning.ts`. Queda abierta para la Fase 06. |

### 3.1 Trazabilidad de los tipos derivados (DP-09)

| Tipo TypeScript                       | Origen en el monorepo                                                    | Forma                                                                  |
|---------------------------------------|--------------------------------------------------------------------------|------------------------------------------------------------------------|
| `IngestResponse`                      | `azure-devops-backend/.../api/Handler.java:60-69`                        | `{ message: string }`                                                  |
| `PlanningChunk`                       | `azure-devops-backend/.../dto/planning/PlanningChunkResponse.java:23-29` | `{ id, initiativeId, sectionName, content, metadata }` — **camelCase** |
| `JsonRpcResponse<T>` / `JsonRpcError` | `azure-devops-agent/.../api/JsonRpcResponse.java:13-20`                  | `{ jsonrpc, id, result?, error? }`                                     |
| `CancelTaskResponse`                  | `azure-devops-agent/.../JsonRpcResponseFactory.java:74-82`               | `JsonRpcResponse<{ task: AgentTask }>`                                 |
| `SendMessageResponse` (`auditStory`)  | Ya existía en el modelo; usado en `planning-dashboard.component.ts:743`  | Sin cambios                                                            |

> Nota de contraste: `Initiative` llega en **snake_case** (`initiative_id`) porque procede del
> adaptador `PgVectorPlanningAdapter`, mientras que `PlanningChunk` llega en **camelCase** porque
> procede de un DTO Java serializado por Jackson. La discrepancia es del backend; **no se unificó**.

---

## 4. Métricas obtenidas

|    # | Métrica                                      |     Antes |                           Después |          Objetivo |
|-----:|----------------------------------------------|----------:|----------------------------------:|------------------:|
| M-04 | Ocurrencias de `any` en producción           |     **9** |                          **0** ✅ |                 0 |
| M-10 | Rutas literales en llamadas `http.*`         |    **10** |                          **0** ✅ |                 0 |
| M-11 | Números mágicos de sondeo                    |    4 usos | **0** (2 constantes nombradas) ✅ |                 0 |
| M-13 | Existe `core/{config,interceptors,services}` |        ❌ |                            **✅** |                ✅ |
| M-14 | Existe `environments/` + `fileReplacements`  |        ❌ |                            **✅** |                ✅ |
| M-09 | Archivos `.spec.ts`                          |        12 |                            **14** |              ≥ 18 |
|    — | Casos de prueba                              |       188 |                           **200** |                 — |
| M-18 | `pnpm build` — initial total                 | 434.21 kB |                  **435.66 kB** ✅ |          < 500 kB |
| M-19 | `pnpm test`                                  |   188/188 |                    **200/200** ✅ |             verde |
| M-01 | Líneas `devops-agent-state.service.ts`       |       418 |                424 *(+6 imports)* | ≤ 60 (Fase 05/06) |
| M-03 | Líneas `planning-dashboard.component.ts`     |       753 |                   753 *(intacto)* |   ≤ 200 (Fase 07) |
| M-05 | `.subscribe()` en producción                 |        17 |                    17 *(intacto)* |           Fase 08 |
| M-06 | Temporizadores sin limpieza                  |         3 |                     3 *(intacto)* |        Fase 06/08 |

**Margen de presupuesto:** 435.66 kB frente al aviso de 500 kB → **64.34 kB disponibles**. El coste de la capa `core/` es de **+1.45 kB** sobre el bundle inicial.

---

## 5. Comandos ejecutados

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0

# --- Punto de partida verificado (T-01) ---
corepack pnpm install     # Already up to date. Done in 138ms using pnpm v11.8.0
corepack pnpm test        # Test Files 12 passed (12) | Tests 188 passed (188)   ✅ contrato Fase 01
corepack pnpm build       # Initial total 434.21 kB                              ✅ coincide

# --- Cierre (T-10) ---
corepack pnpm test        # Test Files 14 passed (14) | Tests 200 passed (200)
corepack pnpm build       # Initial total 435.66 kB | 99.81 kB transfer

grep -rn ': any\|<any>\|any\[\]' src/app src/environments --include='*.ts' | grep -v '\.spec\.ts'
# → (vacio)

grep -rnE "http\.(get|post|put|delete)(<[^>]*>)?\('/" src/app --include='*.ts' | grep -v '\.spec\.ts'
# → (vacio)

grep -rn "30000\|5000" src/app --include='*.ts' | grep -v '\.spec\.ts'
# → solo src/app/core/config/app-tuning.ts (las 2 constantes + su comentario)

git diff --stat -- 'src/app/**/*.spec.ts'
# → (vacio)   ✅ R-3 respetada: ningún .spec.ts de la Fase 01 fue modificado
```

---

## 6. Hallazgos no previstos

| ID      | Hallazgo                                                                                                                                                                                                                                                                   | Acción tomada                                                                                                                                                                                                                                                                                                                                 |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **H-1** | **`console.error` recontado: son 16, no 10.** El recuento de `fase-02.md` §1.2 para D-20 se quedó corto; incluye los `catch` del parseo SSE y los de los cuatro `subscribe({error})` del servicio de estado.                                                               | Métrica corregida. **D-20 sigue asignada a la Fase 08** con el valor real de 16. No se sustituyó ninguno en esta fase (prohibición 2.2.7).                                                                                                                                                                                                    |
| **H-2** | **El cuerpo de error HTTP no siempre es un `string`.** La precedencia actual del código (`error.error \|\| error.message`) interpola objetos JSON directamente en el mensaje al usuario, produciendo `Error: [object Object]`.                                             | **No se corrigió** para no alterar el comportamiento congelado por las pruebas. El interceptor sí normaliza correctamente (`extractHttpErrorMessage` lee `body.message` cuando el cuerpo es un objeto), pero solo alimenta el `NotificationService`, que aún no se consume. Registrado como **D-28**, asignado a la **Fase 08** junto a D-20. |
| **H-3** | **`environment.apiBaseUrl` es hoy la identidad.** Al estar vacío, `${BASE}/api/...` produce exactamente la misma cadena que el literal anterior, así que las pruebas de la Fase 01 que hacen `httpMock.expectOne('/api/planning/initiatives')` siguen pasando sin cambios. | Verificado: 18/18 pruebas de `devops-agent-api.service.spec.ts` verdes. La indirección queda lista sin coste de comportamiento.                                                                                                                                                                                                               |

### 6.1 Deuda nueva registrada

| ID       | Deuda                                                                                                                                                  | Severidad | Fase asignada |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|---------------|
| **D-28** | La precedencia `error.error \|\| error.message` interpola objetos y muestra `[object Object]` al usuario (`devops-agent-state.service.ts:167`, `:271`) | 🟡        | 08            |

---

## 7. Checklist de calidad

| Ítem (`rules/angular-rules.md` §8)                | Estado | Evidencia                                                                |
|---------------------------------------------------|--------|--------------------------------------------------------------------------|
| Componentes tocados siguen `standalone: true`     | ✅     | `planning-management.component.ts:10` intacto                            |
| Cero `*ngIf` / `*ngFor` / `*ngSwitch`             | ✅     | No se tocó ninguna plantilla                                             |
| Estado reactivo con Signals u Observables seguros | ✅     | `NotificationService` usa `signal()` + `computed()`                      |
| Cero fugas de memoria nuevas                      | ✅     | El interceptor no abre suscripciones; `catchError` es un operador        |
| Botones de solo icono con `aria-label`            | ✅     | No se añadió ningún control de UI                                        |
| Cero `any`                                        | ✅     | `grep` vacío (M-04 = 0)                                                  |
| Cero llamadas HTTP fuera de la capa de API        | ✅     | Sin cambios; D-26 sigue viva y asignada a la Fase 05                     |
| Tipado explícito en métodos públicos              | ✅     | Todos los métodos de `NotificationService` y del interceptor lo declaran |
| Sin números mágicos                               | ✅     | `POLLING_INTERVAL_*_MS`                                                  |
| Pruebas GIVEN / WHEN / THEN                       | ✅     | 12 casos nuevos con esa estructura                                       |
| Sin copys ni rutas inventadas (§7)                | ✅     | DP-09 y DP-03 resueltas por el usuario; DP-06 por acción documentada     |
| Sin secretos                                      | ✅     | `apiBaseUrl: ''`, ninguna URL ni credencial en el código                 |
| `corepack pnpm build` correcto                    | ✅     | 435.66 kB, sin advertencias de presupuesto                               |
| `corepack pnpm test` sin fallos nuevos            | ✅     | 200/200                                                                  |
| Sin `.npmrc` ni `strict-ssl=false`                | ✅     | `git status` no muestra `.npmrc`                                         |

---

## 8. Desviaciones respecto a las instrucciones

| Desviación                                                                                                                        | Justificación                                                                                                                                                                                                                                                                          |
|-----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `api-endpoints.ts` compone las rutas con `${environment.apiBaseUrl}` en lugar de literales planos, como sugería la firma de T-03. | T-02 exige crear `environments/` con `apiBaseUrl`. Dejarlo sin consumir lo convertiría en código muerto y no cerraría D-14 de forma verificable. Con `apiBaseUrl: ''` la concatenación es **la identidad**: el valor final es idéntico carácter a carácter y las pruebas lo confirman. |
| Se añadieron dos constructores de URL no listados en T-03: `initiativeUrl` (usado por `deleteInitiative`) y `dashboardStreamUrl`. | T-08 exige que **no quede ninguna ruta literal**. `DELETE /api/planning/initiatives/${id}` y la URL del stream SSE eran literales sin constante asignada en la firma propuesta. `initiativeUrl` ya venía implícito como base de `initiativeCellUrl`.                                   |
| `PlanningChunk.sectionName` se tipa `string \| null` en vez de `string`.                                                          | El campo procede de un `String` de Java, nullable por definición, y la plantilla ya lo trata como opcional (`chunk.sectionName \|\| 'N/A'`, `planning-management.component.ts:153`). Tiparlo como no nulo sería una asunción.                                                          |
| DP-06 no quedó resuelta del todo.                                                                                                 | El usuario respondió que era deuda técnica reconocida, sin confirmar valores ni topes. Se aplicó la acción por defecto documentada, que **conserva el comportamiento actual**. La decisión sigue abierta para la Fase 06.                                                              |

---

## 9. Estado al cerrar

- Siguiente fase generada: **`docs/fases/fase-03.md`** ✅
- Decisiones resueltas en esta fase: **DP-03** 🟢, **DP-09** 🟢 · **DP-06** 🟡 parcial
- Decisiones abiertas: DP-01, DP-02, DP-04, DP-05, DP-06 *(parcial)*, DP-08, DP-10 · DP-07 informativa
- `docs/plan/ESTADO.md` actualizado: **sí**
- Restricción **R-3 cumplida**: cero `.spec.ts` de la Fase 01 modificados
- La Fase 03 está **bloqueada por DP-04 y DP-07**

