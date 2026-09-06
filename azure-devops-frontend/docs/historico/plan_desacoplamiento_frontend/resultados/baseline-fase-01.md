# BASELINE — FASE 01

> **Medido:** 2026-08-31 · **Entorno:** macOS · Node v24.20.0 · pnpm 11.8.0 (vía corepack)
> **Commit de referencia:** estado del repositorio antes de la Fase 01
> Este documento **sustituye** los valores «declarados» del plan maestro §2.1 y §6.

---

## 0. Nota de entorno

El registro público de npm estaba inaccesible por interceptación TLS corporativa (`SELF_SIGNED_CERT_IN_CHAIN`). Se resolvió **sin desactivar la verificación TLS**, exportando las CA de confianza del llavero de macOS y apuntando Node a ellas:

```bash
mkdir -p ~/.ca
security find-certificate -a -p /System/Library/Keychains/SystemRootCertificates.keychain  > ~/.ca/corp-ca.pem
security find-certificate -a -p /Library/Keychains/System.keychain                        >> ~/.ca/corp-ca.pem
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
```

> ⚠️ **`strict-ssl` permanece en `true`.** No se añadió ningún `.npmrc` al repositorio ni se
> desactivó ninguna comprobación de seguridad. Toda ejecución posterior de `pnpm` en este entorno
> requiere exportar `NODE_EXTRA_CA_CERTS`.

---

## 1. Hallazgo bloqueante H-1 — El proyecto no compilaba

**Primera ejecución de `pnpm test` y `pnpm build`: ambas fallaron con el mismo error.**

```
✘ [ERROR] TS2729: Property 'refinementMessages$' is used before its initialization.
    src/app/features/devops-agent/services/devops-agent-state.service.ts:24:9
✘ [ERROR] TS2729: Property 'initialGreeting' is used before its initialization.
    src/app/features/devops-agent/services/devops-agent-state.service.ts:55:78
```

La deuda **D-07** no era un riesgo latente: era un **fallo activo y bloqueante**. El repositorio estaba en un estado que no compila y, por tanto, ni siquiera podía ejecutar sus 29 pruebas existentes.

**Consecuencia sobre el plan:** la restricción «cero líneas modificadas en `src/app/**` fuera de
`.spec.ts`» era imposible de cumplir. Ver la excepción **S-01** en §5.

---

## 2. Baseline de ejecución (tras aplicar S-01)

### 2.1 `pnpm test`

```
✓ src/app/shared/pipes/markdown-parser.pipe.spec.ts              (8 tests)
✓ src/app/features/devops-agent/services/devops-agent-state.service.spec.ts (10 tests)
✓ src/app/features/devops-agent/services/devops-agent-api.service.spec.ts   (9 tests)
✓ src/app/app.spec.ts                                            (2 tests)

Test Files  4 passed (4)
     Tests  29 passed (29)
  Duration  703ms
```

**Baseline de fallos: 0.** Toda fase posterior debe mantener este número.

### 2.2 `pnpm build`

```
Initial chunk files | Names               |  Raw size | Estimated transfer size
main-PUBS3JDE.js    | main                | 206.52 kB |                37.55 kB
chunk-C7OXJF2L.js   | -                   | 189.94 kB |                54.92 kB
styles-7YDT32YQ.css | styles              |  37.72 kB |                 6.82 kB
                    | Initial total       | 434.18 kB |                99.28 kB

Lazy chunk files    | Names               |  Raw size | Estimated transfer size
chunk-24IUBKYD.js   | devops-agent-routes | 104.52 kB |                23.68 kB
```

**Presupuestos de `angular.json`:** `initial` 434.18 kB < 500 kB de aviso. ✅ Sin advertencias. Margen disponible antes del aviso: **65.82 kB**.

---

## 3. Conteo de líneas de `src/app/**/*.ts` (excluidas las pruebas)

Comando: `find src/app -name '*.ts' -not -name '*.spec.ts' -exec wc -l {} + | sort -rn`

|   Líneas | Archivo                                                                                  |
|---------:|------------------------------------------------------------------------------------------|
|  **753** | `features/devops-agent/components/planning-dashboard/planning-dashboard.component.ts`    |
|  **418** | `features/devops-agent/services/devops-agent-state.service.ts` *(era 406; +12 por S-01)* |
|  **276** | `features/devops-agent/pages/devops-agent-home.page.ts`                                  |
|  **259** | `features/devops-agent/components/planning-management/planning-management.component.ts`  |
|      127 | `features/devops-agent/components/planning-upload/planning-upload.component.ts`          |
|      123 | `features/devops-agent/services/devops-agent-api.service.ts`                             |
|      121 | `features/devops-agent/components/chat-messages/chat-messages.component.ts`              |
|      110 | `features/devops-agent/models/devops-agent.model.ts`                                     |
|       97 | `features/devops-agent/components/chat-input/chat-input.component.ts`                    |
|       72 | `shared/pipes/markdown-parser.pipe.ts`                                                   |
|       38 | `features/devops-agent/components/info-cards/info-cards.component.ts`                    |
|       28 | `features/devops-agent/components/agent-info/agent-info.component.ts`                    |
|       22 | `app.config.ts`                                                                          |
|       12 | `app.ts`                                                                                 |
|        9 | `features/devops-agent/devops-agent.routes.ts`                                           |
|        9 | `app.routes.ts`                                                                          |
|        7 | `features/devops-agent/components/index.ts`                                              |
|        2 | `features/devops-agent/services/index.ts`                                                |
| **2483** | **TOTAL**                                                                                |

**Los 4 archivos más grandes concentran 1706 líneas: el 68,7 % del código productivo.**

---

## 4. Métricas M-01 a M-12 medidas

|     # | Métrica                                  | Declarado en el plan |               **Medido** | Objetivo | Comando                                       |
|------:|------------------------------------------|---------------------:|-------------------------:|---------:|-----------------------------------------------|
|  M-01 | Líneas `devops-agent-state.service.ts`   |                  406 |                  **418** |     ≤ 60 | `wc -l`                                       |
|  M-02 | Líneas del store más grande              |                  406 |                  **418** |    ≤ 150 | `wc -l`                                       |
|  M-03 | Líneas `planning-dashboard.component.ts` |                  754 |                  **753** |    ≤ 200 | `wc -l`                                       |
|  M-04 | Ocurrencias de `any`                     |                    5 |                 **9** ⚠️ |        0 | `grep -rn ': any\|<any>\|any\[\]'`            |
|  M-05 | Llamadas `.subscribe()` en producción    |                    4 |                **17** ⚠️ |        — | `grep -rn '\.subscribe('`                     |
| M-05b | Usos de `takeUntilDestroyed`             |                    0 |                    **0** |        — | `grep -rn 'takeUntilDestroyed'`               |
|  M-06 | Temporizadores sin limpieza              |                    1 |                 **3** ⚠️ |        0 | `grep -rn 'setTimeout\|setInterval'`          |
|  M-07 | Componentes con `OnPush`                 |                0 / 9 |                **0 / 9** |    100 % | `grep -rn 'ChangeDetectionStrategy'`          |
|  M-08 | Componentes con `.spec.ts`               |                0 / 9 |                **0 / 9** |    100 % | `find -name '*.spec.ts'`                      |
|  M-09 | Archivos `.spec.ts` totales              |                    4 |                    **4** |     ≥ 18 | `find -name '*.spec.ts'`                      |
|  M-10 | Llamadas `http.*` con ruta literal       |                   11 |                   **10** |        0 | `grep -rn "http\.\(get\|post\|put\|delete\)"` |
|  M-11 | Números mágicos (intervalos + umbrales)  |                   12 |                   **12** |        0 | inspección manual                             |
|  M-12 | Señales `loading` independientes         |                    1 |                    **1** |        4 | inspección manual                             |
|  M-18 | `pnpm build`                             |                   ✅ |    **❌ → ✅ tras S-01** |       ✅ | `pnpm build`                                  |
|  M-19 | `pnpm test`                              |                   ✅ | **❌ → 29/29 tras S-01** |       ✅ | `pnpm test`                                   |
|  M-20 | Archivos con `aria-label`                |            por medir | **4 archivos / 11 usos** |        — | `grep -rc 'aria-label'`                       |

### 4.1 Correcciones al plan maestro

| Métrica  | Corrección    | Motivo                                                                                                                                                                                                                                       |
|----------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **M-04** | 5 → **9**     | El diagnóstico solo contó las firmas. Faltaban los parámetros de tipo de `http.post<any>` / `http.get<any[]>` (api `:33`, `:105`, `:121`) y un `signal<any[]>` en `planning-management.component.ts:187` **no detectado en el diagnóstico**. |
| **M-05** | 4 → **17**    | El diagnóstico contó solo las suscripciones en componentes. Hay 13 más dentro del servicio de estado. La métrica se redefine como «llamadas `.subscribe()` en código productivo».                                                            |
| **M-06** | 1 → **3**     | Además del `pollingTimer`, hay `setTimeout` en `chat-input.component.ts:81` y `chat-messages.component.ts:114`, ambos sin limpieza.                                                                                                          |
| **M-10** | 11 → **10**   | Recuento real de llamadas `http.*`.                                                                                                                                                                                                          |
| **M-01** | 406 → **418** | Efecto de la excepción S-01 (+12 líneas de comentarios de bloque).                                                                                                                                                                           |

### 4.2 Detalle de M-04 — las 9 ocurrencias de `any`

| Archivo                                                           | Línea | Ocurrencia                                                    |
|-------------------------------------------------------------------|------:|---------------------------------------------------------------|
| `components/planning-management/planning-management.component.ts` |   187 | `signal<any[]>([])` ⚠️ **nueva, no estaba en el diagnóstico** |
| `services/devops-agent-state.service.ts`                          |   304 | `auditStory(id: string): Observable<any>`                     |
| `services/devops-agent-state.service.ts`                          |   415 | `getInitiativeChunks(id: string): Observable<any[]>`          |
| `services/devops-agent-api.service.ts`                            |    32 | `uploadPlanning(...): Observable<any>`                        |
| `services/devops-agent-api.service.ts`                            |    33 | `this.http.post<any>(...)`                                    |
| `services/devops-agent-api.service.ts`                            |   104 | `getInitiativeChunks(...): Observable<any[]>`                 |
| `services/devops-agent-api.service.ts`                            |   105 | `this.http.get<any[]>(...)`                                   |
| `services/devops-agent-api.service.ts`                            |   112 | `cancelTask(...): Observable<any>`                            |
| `services/devops-agent-api.service.ts`                            |   121 | `this.http.post<any>('/', payload)`                           |

### 4.3 Detalle de M-06 — los 3 temporizadores sin limpieza

| Archivo                                               | Línea | Uso                               | Riesgo                                                                                  |
|-------------------------------------------------------|------:|-----------------------------------|-----------------------------------------------------------------------------------------|
| `services/devops-agent-state.service.ts`              |   334 | `setTimeout` recursivo del sondeo | 🔴 **Fuga real**: `providedIn:'root'`, se reprograma indefinidamente y nunca se cancela |
| `components/chat-input/chat-input.component.ts`       |    81 | Redimensionado del textarea       | 🟡 Diferido de un ciclo; puede disparar tras destruir el componente                     |
| `components/chat-messages/chat-messages.component.ts` |   114 | Auto-scroll al final              | 🟡 Ídem                                                                                 |

> **Nueva deuda D-22:** los `setTimeout` de `chat-input` y `chat-messages` no se cancelan en
> `ngOnDestroy`. Asignada a la **Fase 08**.

### 4.4 Detalle de M-20 — accesibilidad

| Archivo                                                           | Usos de `aria-label` |
|-------------------------------------------------------------------|---------------------:|
| `pages/devops-agent-home.page.ts`                                 |                    2 |
| `components/planning-management/planning-management.component.ts` |                    6 |
| `components/planning-upload/planning-upload.component.ts`         |                    2 |
| `components/chat-input/chat-input.component.ts`                   |                    1 |
| `components/planning-dashboard/planning-dashboard.component.ts`   |             **0** ⚠️ |
| **Total**                                                         |               **11** |

Hay **38 usos de iconos `pi pi-`** en el código productivo. El componente con más superficie interactiva (`planning-dashboard`, 753 líneas) **no declara ni un solo `aria-label`**.

> **Nueva deuda D-23:** `planning-dashboard.component.ts` sin `aria-label` en sus controles.
> Asignada a la **Fase 07**.

---

## 5. Excepción S-01 — Reordenamiento de declaraciones para restaurar el build

- **Autorizada por:** hallazgo bloqueante H-1. Sin ella la Fase 01 es inejecutable.
- **Archivo:** `src/app/features/devops-agent/services/devops-agent-state.service.ts`
- **Naturaleza:** **puramente mecánica.** Se reordenaron las declaraciones de campos en tres bloques con dependencia lineal:
  1. **Bloque 1** — constantes de saludo (`generalGreeting`, `initialGreeting`).
  2. **Bloque 2** — `BehaviorSubject` privados, que consumen el bloque 1.
  3. **Bloque 3** — `Observable` públicos, que consumen el bloque 2.
- **Lo que NO se tocó:**
  - Ni un carácter de los textos de los saludos.
  - Ninguna firma, ningún nombre de campo, ningún modificador de acceso.
  - Ningún método, ninguna lógica, ningún valor.
  - El alias `messages` se conserva íntegro (D-13 sigue viva, pendiente de DP-01).
- **Verificación:** `pnpm build` correcto y las 29 pruebas preexistentes siguen verdes **sin modificarlas**.
- **Efecto sobre el plan:** D-07 queda **saldada**. La Fase 03 ya no debe abordarla; se le retira esa responsabilidad y conserva D-06, D-19 y D-21.

---

## 6. Deudas nuevas detectadas en esta fase

| ID       | Deuda                                                                                                        | Severidad | Evidencia | Fase asignada |
|----------|--------------------------------------------------------------------------------------------------------------|-----------|-----------|---------------|
| **D-22** | `setTimeout` sin cancelar en `chat-input:81` y `chat-messages:114`                                           | 🟡        | §4.3      | 08            |
| **D-23** | `planning-dashboard.component.ts` sin ningún `aria-label` (753 líneas, 0 usos)                               | 🟠        | §4.4      | 07            |
| **D-24** | `signal<any[]>` en `planning-management.component.ts:187`                                                    | 🟠        | §4.2      | 02            |
| **D-25** | El repositorio se entregó en estado no compilable; no hay verificación de build en CI                        | 🔴        | §1        | 08            |
| **D-26** | `PlanningManagementComponent` inyecta `DevopsAgentApiService` **directamente**, saltándose la capa de estado | 🟠        | §7        | 05            |
| **D-27** | **Estado de vista en campos planos bajo change detection ZONELESS: la UI no se repinta**                     | 🔴        | §8        | 05            |

---

## 7. Hallazgo H-2 — Un componente de presentación habla con la red

`src/app/features/devops-agent/components/planning-management/planning-management.component.ts:190`

```ts
private readonly api = inject(DevopsAgentApiService);
...
this.api.getInitiativeChunks(init.initiative_id).subscribe({ ... });   // :243
```

Es el **único** artefacto bajo `components/` que inyecta el cliente HTTP. `rules/angular-rules.md` §1 reserva `components/` para *dumb components* de presentación pura y §8 exige que «todas las llamadas a backend HTTP estén centralizadas en servicios de API, nunca directamente en el componente».

Registrado como **D-26**, asignado a la Fase 05.

---

## 8. Hallazgo H-3 — DEFECTO VIVO: la tabla de planeaciones no se repinta (zoneless)

### 8.1 La aplicación corre sin zona

`src/app/app.config.ts` **no declara** `provideZoneChangeDetection` ni
`provideZonelessChangeDetection`. En Angular 22 el modo por defecto es **zoneless**: la detección de cambios solo se dispara cuando algo notifica explícitamente al planificador (un signal que cambia, un `AsyncPipe` que emite, un evento del DOM enlazado en plantilla o un `markForCheck`).

### 8.2 Evidencia reproducible

Con `PlanningManagementComponent` montado y publicando dos iniciativas en el servicio de estado:

```
LEN= 2   LOADING= false
HTML= ... <h3 ...>No hay iniciativas indexadas</h3> ...
```

El componente **tiene los datos** (`editableInitiatives.length === 2`) y **no está cargando**
(`loading() === false`), pero el DOM sigue mostrando el estado vacío. La cadena
`@if (loading()) {} @else if (editableInitiatives.length === 0) {} @else { tabla }` no se reevalúa porque `editableInitiatives` es un **campo plano** mutado desde una suscripción RxJS (`planning-management.component.ts:208-215`) y ningún dependiente reactivo cambió.

### 8.3 Por qué la aplicación «funciona» hoy

Por accidente. `loadInitiatives()` conmuta `loading$` a `true` y de vuelta a `false`
(`devops-agent-state.service.ts:173-185`). Ese `BehaviorSubject` alimenta el **signal** `loading()`
del componente (`:204-206`), y es el cambio de ese signal —no la llegada de los datos— lo que fuerza el repintado y hace que la tabla aparezca.

### 8.4 Por qué es un riesgo directo para el plan

**La Fase 05 separa el `loading` por flujo (D-16).** En cuanto `PlanningManagementComponent` deje de observar el `loading` compartido, desaparecerá el único disparador reactivo que hoy repinta su tabla y **la gestión de planeaciones dejará de mostrar datos**.

> ⚠️ **Restricción para la Fase 05:** migrar `editableInitiatives` a `signal()` **antes** de tocar
> la separación del `loading`. El orden inverso rompe la funcionalidad.

### 8.5 Alcance del defecto

| Componente            | Estado de vista                                          | ¿Repinta hoy?                                  |
|-----------------------|----------------------------------------------------------|------------------------------------------------|
| `planning-management` | `editableInitiatives` (campo plano)                      | ❌ Solo por el rebote accidental de `loading`  |
| `planning-dashboard`  | `dashboardData`, `errorMessage`, filtros (campos planos) | ⚠️ Depende del rebote de `loading` del stream  |
| `devops-agent-home`   | `activeTab` (campo plano)                                | ✅ Sus `AsyncPipe` mantienen la vista reactiva |

Registrado como **D-27**, severidad 🔴, asignado a la Fase 05.

