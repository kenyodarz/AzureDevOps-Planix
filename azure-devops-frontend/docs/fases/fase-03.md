# FASE 03 — Externalización de copys y prompts

> **Estado:** 🔴 BLOQUEADA (por DP-04; DP-07 acota el alcance) · **Depende de:** FASE 02 🟢 · **Riesgo:** Bajo
> **Commit al cerrar:** `refactor(devops_agent): externalizar copys y prompts a la capa domain`
> **Siguiente fase:** `fase-04.md`
> **Regla de oro:** las **200 pruebas vigentes deben seguir verdes SIN modificar ningún `.spec.ts`.**

---

## 1. CONTEXTO

### 1.1 De dónde venimos

La Fase 02 cerró el 2026-08-31 (`docs/resultados/RESULTADO-FASE-02.md`) dejando:

- **200 pruebas verdes en 14 archivos** (188 de caracterización de la Fase 01, intactas, + 12 nuevas de `core/`).
- **Existe `src/app/core/{config,interceptors,services}`** con barrel export (D-10, D-13 ✅).
- **Existe `src/environments/`** con `fileReplacements` declarado en `angular.json` (D-14 ✅).
- **Cero `any` en código productivo** (D-05 y D-24 ✅). Los tipos `IngestResponse`, `PlanningChunk`,
  `JsonRpcResponse<T>` y `CancelTaskResponse` se derivaron del backend con autorización explícita del usuario (DP-09 🟢).
- **Cero rutas literales** en llamadas `http.*`: todas viven en `core/config/api-endpoints.ts`
  (D-08 ✅).
- Los intervalos de sondeo viven en `core/config/app-tuning.ts` (D-09 ✅).
- `pnpm build`: **435.66 kB** initial, dentro del presupuesto de 500 kB.
- Deuda nueva **D-28** (precedencia de error que muestra `[object Object]`), asignada a la Fase 08.

**Entorno de ejecución (obligatorio en esta máquina).** El registro npm está interceptado por TLS corporativo. Antes de cualquier comando:

```bash
cd azure-devops-frontend
export NODE_EXTRA_CA_CERTS=~/.ca/corp-ca.pem
export COREPACK_ENABLE_DOWNLOAD_PROMPT=0
# usar siempre: corepack pnpm <comando>   (pnpm no está en el PATH)
```

Si `~/.ca/corp-ca.pem` no existe, regenerarlo:

```bash
mkdir -p ~/.ca
security find-certificate -a -p /System/Library/Keychains/SystemRootCertificates.keychain  > ~/.ca/corp-ca.pem
security find-certificate -a -p /Library/Keychains/System.keychain                        >> ~/.ca/corp-ca.pem
```

> **Prohibido** añadir `.npmrc` al repositorio o poner `strict-ssl=false`.

### 1.2 Problema que resuelve esta fase

Los textos de cara al usuario y los prompts enviados al modelo están **embebidos como literales dentro de la lógica**. Un cambio de copy obliga hoy a tocar un servicio de estado de 424 líneas.

| Deuda    |                                         Medición | Evidencia (archivo:línea)                                                                                          |
|----------|-------------------------------------------------:|--------------------------------------------------------------------------------------------------------------------|
| **D-06** | ~**40 líneas** de markdown dentro de un servicio | `services/devops-agent-state.service.ts:34-75` (`generalGreeting` e `initialGreeting`)                             |
| **D-06** |                            2 saludos de reinicio | `devops-agent-state.service.ts:140` («Chat general limpio…») y `:240` («Chat limpio (Asistente de Refinamiento)…») |
| **D-19** |   2 prompts de negocio interpolados en la lógica | `devops-agent-state.service.ts:296` (`refineStoryInChat`) y `:311` (`auditStory`)                                  |
| **D-21** |   Catálogos de UI en componentes de presentación | `chat-input.component.ts:52-59` (3 sugerencias) y `info-cards.component.ts:37` (5 etiquetas)                       |

#### Inventario EXACTO de lo que se mueve

| Origen                                  | Constante destino                       | Contenido                                                                 |
|-----------------------------------------|-----------------------------------------|---------------------------------------------------------------------------|
| `devops-agent-state.service.ts:34-43`   | `GENERAL_GREETING`                      | `Message` completo del Chat General                                       |
| `devops-agent-state.service.ts:44-75`   | `REFINEMENT_GREETING`                   | `Message` completo del Asistente de Refinamiento (~30 líneas de markdown) |
| `devops-agent-state.service.ts:136-143` | `GENERAL_RESET_GREETING`                | `Message` de `clearGeneralChat()`                                         |
| `devops-agent-state.service.ts:236-243` | `REFINEMENT_RESET_GREETING`             | `Message` de `clearRefinementChat()`                                      |
| `devops-agent-state.service.ts:296`     | `buildRefinementPrompt(storyId, title)` | Prompt de `refineStoryInChat`                                             |
| `devops-agent-state.service.ts:311`     | `buildAuditPrompt(id)`                  | `` `audita la calidad de (ID:${id})` ``                                   |
| `chat-input.component.ts:52-59`         | `CHAT_SUGGESTIONS`                      | Las 3 sugerencias, con `label` y `value`                                  |
| `info-cards.component.ts:37`            | `TECH_TAGS`                             | `['Spring AI', 'WebFlux', 'MCP', 'Azure DevOps', 'OpenAI']`               |

> El mensaje de error del chat (`devops-agent-state.service.ts:406`, *«…Asegúrate de que corre en el
> puerto 8081»*) **NO se toca en esta fase**: es objeto directo de la pregunta abierta de DP-04.

### 1.3 Estado esperado al terminar

1. Existe `src/app/features/devops-agent/domain/` con 4 archivos y su `index.ts`.
2. `devops-agent-state.service.ts` **no contiene ni una línea de markdown**: pasa de 424 a ≈ 340 líneas.
3. Ningún componente de `components/` declara catálogos de texto propios.
4. `grep -c '###' src/app/features/devops-agent/services/devops-agent-state.service.ts` → **0**.
5. **Las 200 pruebas siguen verdes sin haber tocado un solo `.spec.ts`.**

### 1.4 Archivos involucrados

| Ruta                                                                          | Acción                                            |
|-------------------------------------------------------------------------------|---------------------------------------------------|
| `src/app/features/devops-agent/domain/chat-greetings.ts`                      | CREAR                                             |
| `src/app/features/devops-agent/domain/chat-prompts.ts`                        | CREAR                                             |
| `src/app/features/devops-agent/domain/chat-suggestions.ts`                    | CREAR                                             |
| `src/app/features/devops-agent/domain/tech-tags.ts`                           | CREAR                                             |
| `src/app/features/devops-agent/domain/index.ts`                               | CREAR (barrel)                                    |
| `src/app/features/devops-agent/domain/chat-prompts.spec.ts`                   | CREAR                                             |
| `src/app/features/devops-agent/services/devops-agent-state.service.ts`        | MODIFICAR (sustituir literales por importaciones) |
| `src/app/features/devops-agent/components/chat-input/chat-input.component.ts` | MODIFICAR (solo `:52-59`)                         |
| `src/app/features/devops-agent/components/info-cards/info-cards.component.ts` | MODIFICAR (solo `:37`)                            |
| `src/app/core/**`                                                             | NO TOCAR                                          |
| **Cualquier `.spec.ts` existente**                                            | **NO TOCAR**                                      |

### 1.5 Reglas aplicables

De `rules/angular-rules.md`:

- **§1** — Estructura por feature: los catálogos de negocio no pertenecen a `components/`.
- **§5** — Tipado explícito; `as const` para catálogos inmutables; cero `any`.
- **§6** — Pruebas Vitest GIVEN / WHEN / THEN.
- **§7** — **Política de No-Asunción: prohibido reescribir un copy de cara al usuario.**

De la raíz del monorepo (`.github/copilot-instructions.md`): sin secretos. Atención especial al copy que menciona el puerto 8081.

### 1.6 Decisiones pendientes que bloquean

| ID        | Pregunta                                                                                                                                                                                                        | Estado         | Acción si sigue ABIERTA                                                                                                                                       |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-04** | ¿Los copys están aprobados por negocio y se conservan carácter por carácter? En concreto, ¿se mantiene *«Asegúrate de que corre en el puerto 8081»*, que expone un detalle de infraestructura al usuario final? | 🔴 ABIERTA     | **MOVER los textos sin alterar un solo carácter.** No reescribir, no corregir la errata *«aprovadores»* de `chat-input:55`, no eliminar la mención al puerto. |
| **DP-07** | ¿Entra `@angular/localize` en el alcance?                                                                                                                                                                       | ⚪ INFORMATIVA | Centralizar en constantes **en español**. **No** introducir i18n ni añadir dependencias.                                                                      |

> ⚠️ Se aplica también la **restricción R-4** de `ESTADO.md`: mientras DP-04 siga abierta, los copys
> se **mueven**, nunca se reescriben.

---

## 2. INSTRUCCIONES

### 2.1 Qué hacer

#### T-01 — Verificar el punto de partida

```bash
corepack pnpm install
corepack pnpm test    # DEBE dar 200 passed (200) en 14 archivos
corepack pnpm build   # DEBE dar 435.66 kB initial
```

Si el número de pruebas no es 200, **detenerse**: el árbol no está en el estado que dejó la Fase 02.

#### T-02 — Crear `domain/chat-greetings.ts`

Cuatro constantes tipadas como `Message` (importado de `../models/devops-agent.model`):

```ts
export const GENERAL_GREETING: Message = { /* copiado de :34-43 */ };
export const REFINEMENT_GREETING: Message = { /* copiado de :44-75 */ };
export const GENERAL_RESET_GREETING: Message = { /* copiado de :136-143 */ };
export const REFINEMENT_RESET_GREETING: Message = { /* copiado de :236-243 */ };
```

> **Copiar y pegar, no transcribir.** Los backticks escapados (`` \` ``) del bloque de código
> markdown de `initialGreeting` deben conservarse tal cual.

#### T-03 — Crear `domain/chat-prompts.ts`

```ts
export const buildRefinementPrompt = (storyId: string, title: string): string => `…`;
export const buildAuditPrompt = (id: string): string => `audita la calidad de (ID:${id})`;
```

Las plantillas se copian literalmente de `:296` y `:311`.

#### T-04 — Crear `domain/chat-suggestions.ts` y `domain/tech-tags.ts`

```ts
export interface ChatSuggestion { readonly label: string; readonly value: string; }
export const CHAT_SUGGESTIONS: readonly ChatSuggestion[] = [ /* las 3 de chat-input:52-59 */ ] as const;
export const TECH_TAGS: readonly string[] = ['Spring AI', 'WebFlux', 'MCP', 'Azure DevOps', 'OpenAI'] as const;
```

#### T-05 — Crear `domain/index.ts`

Barrel que reexporta los cuatro archivos.

#### T-06 — Sustituir en `devops-agent-state.service.ts`

Reemplazar los campos `generalGreeting` / `initialGreeting` y los dos `resetGreeting` locales por las constantes importadas, y los dos prompts por las funciones. **Conservar el orden de declaración de los tres bloques** documentado en el comentario de `:26-33`: la excepción S-01 de la Fase 01 existe precisamente porque usar un campo antes de declararlo produce `TS2729`.

> Al externalizar los saludos, los `BehaviorSubject` del bloque 2 dejan de depender del bloque 1.
> **No reordenar por iniciativa propia**: limitarse a sustituir, y dejar el comentario explicativo.

#### T-07 — Sustituir en los dos componentes

`chat-input.component.ts`: `protected readonly suggestions = CHAT_SUGGESTIONS;`
`info-cards.component.ts`: `protected readonly tags = TECH_TAGS;`

**No renombrar los campos**: las plantillas y las pruebas de la Fase 01 los referencian por nombre.

#### T-08 — Prueba nueva de los constructores de prompt

`domain/chat-prompts.spec.ts`, formato GIVEN / WHEN / THEN: verificar que `buildAuditPrompt('X-1')`
devuelve exactamente `audita la calidad de (ID:X-1)` y que `buildRefinementPrompt` interpola id y título en las posiciones correctas.

#### T-09 — Validaciones de cierre

```bash
corepack pnpm test    # 200 + las nuevas, cero fallos
corepack pnpm build
grep -c '###' src/app/features/devops-agent/services/devops-agent-state.service.ts   # 0
wc -l src/app/features/devops-agent/services/devops-agent-state.service.ts           # ≈ 340
git diff --stat -- 'src/app/**/*.spec.ts'   # SOLO el archivo nuevo de domain/
```

### 2.2 Qué NO hacer

1. **No modificar ningún `.spec.ts` existente.** Si uno falla, el refactor cambió un copy:
   **revertir**, no adaptar la prueba.
2. **No alterar un solo carácter de ningún texto** (DP-04 / R-4). Incluye la errata *«aprovadores»*
   y la mención al puerto 8081.
3. **No introducir `@angular/localize`** ni ninguna dependencia (DP-07).
4. **No tocar `core/`**: es el resultado cerrado de la Fase 02.
5. **No tocar la capa de API ni los endpoints**: es la Fase 04.
6. **No separar stores ni el `loading`**: son las Fases 05-07.
7. **No corregir D-28** (`[object Object]`) ni sustituir `console.error`: es la Fase 08.
8. **No reordenar los bloques 1-3 del servicio de estado** más allá de lo que exija la sustitución.

### 2.3 Criterios de aceptación

- [ ] Existe `features/devops-agent/domain/` con los 4 archivos de copys y su `index.ts`.
- [ ] `grep -c '###'` sobre el servicio de estado devuelve **0**.
- [ ] `devops-agent-state.service.ts` baja de 424 a ≈ 340 líneas.
- [ ] `chat-input` e `info-cards` no declaran catálogos propios.
- [ ] Las 200 pruebas siguen verdes **sin modificarse**, incluidas:
  `«shows exactly the current hardcoded suggestions»` (chat-input),
  `«shows exactly the current hardcoded technology tags»` (info-cards),
  `«the two greetings are different texts»` y
  `«clearRefinementChat … contiene "Chat limpio"»` (state service).
- [ ] `corepack pnpm build` correcto y dentro del presupuesto de 500 kB.

### 2.4 Checklist de calidad (obligatoria, de `rules/angular-rules.md` §8)

- [ ] Todos los componentes creados o tocados son `standalone: true`
- [ ] Cero `*ngIf` / `*ngFor` / `*ngSwitch`: solo `@if` / `@for` / `@switch`
- [ ] Estado reactivo con Signals u Observables seguros
- [ ] Cero fugas de memoria: `AsyncPipe` o `takeUntilDestroyed()`
- [ ] Botones de solo icono con `aria-label`
- [ ] Cero `any` en variables, parámetros o retornos
- [ ] Cero llamadas HTTP fuera de la capa de servicios de API
- [ ] Tipado explícito en todos los métodos públicos
- [ ] Sin números mágicos: literales extraídos a constantes tipadas
- [ ] Pruebas Vitest en formato GIVEN / WHEN / THEN
- [ ] Sin copys ni rutas inventadas (Política de No-Asunción §7)
- [ ] Sin secretos, tokens ni credenciales
- [ ] `corepack pnpm build` correcto
- [ ] `corepack pnpm test` sin fallos nuevos respecto al baseline de 200

---

## 3. ORDEN DE EJECUCIÓN

| Paso | Acción                                                                                                 | Resultado esperado                           |
|-----:|--------------------------------------------------------------------------------------------------------|----------------------------------------------|
| P-01 | Leer `docs/plan/ESTADO.md` y `docs/plan/DECISIONES_PENDIENTES.md`                                      | Confirmar DP-04 y DP-07                      |
| P-02 | **Si DP-04 sigue 🔴 ABIERTA: aplicar R-4** (mover sin reescribir) o preguntar si se detecta ambigüedad | Alcance acotado                              |
| P-03 | Exportar `NODE_EXTRA_CA_CERTS` y ejecutar `corepack pnpm install`                                      | Dependencias listas                          |
| P-04 | `corepack pnpm test`                                                                                   | 200 passed (200)                             |
| P-05 | Crear `domain/chat-greetings.ts` (T-02)                                                                | Copys copiados literalmente                  |
| P-06 | Crear `domain/chat-prompts.ts` (T-03)                                                                  | Prompts parametrizados                       |
| P-07 | Crear `domain/chat-suggestions.ts` y `domain/tech-tags.ts` (T-04)                                      | Catálogos tipados                            |
| P-08 | Crear `domain/index.ts` (T-05)                                                                         | Barrel                                       |
| P-09 | Sustituir en `devops-agent-state.service.ts` (T-06)                                                    | Compila; sin markdown                        |
| P-10 | `corepack pnpm test`                                                                                   | Las 200 siguen verdes                        |
| P-11 | Sustituir en `chat-input` e `info-cards` (T-07)                                                        | Compila                                      |
| P-12 | Añadir `domain/chat-prompts.spec.ts` (T-08)                                                            | Verde                                        |
| P-13 | `corepack pnpm test`                                                                                   | Cero fallos; ningún `.spec.ts` previo tocado |
| P-14 | `corepack pnpm build`                                                                                  | Correcto, dentro del presupuesto             |
| P-15 | Actualizar `DECISIONES_PENDIENTES.md` con lo resuelto                                                  | Bitácora al día                              |
| P-16 | **Escribir `docs/resultados/RESULTADO-FASE-03.md`** con `_PLANTILLA_RESULTADO.md` y métricas reales    | Trazabilidad                                 |
| P-17 | Actualizar `docs/plan/ESTADO.md`: Fase 03 🟢 + métricas + bitácora                                     | Tablero al día                               |
| P-18 | **Generar `docs/fases/fase-04.md`** con `_PLANTILLA_FASE.md`                                           | Checkpoint de continuidad                    |
| P-19 | Commit: `refactor(devops_agent): externalizar copys y prompts a la capa domain`                        | Versionado                                   |

> ⚠️ **Los pasos P-16 y P-18 son innegociables.** Sin el registro de resultado no hay trazabilidad,
> y sin el MD de la siguiente fase el trabajo no es reanudable tras una desconexión. En cualquiera
> de los dos casos la fase se considera **incompleta**.

### 3.1 Contenido mínimo del MD de la Fase 04

**Nombre.** FASE 04 — Segregación de la capa de API. **Riesgo.** Medio. **Bloqueada por:** DP-03 *(ya resuelta en la Fase 02: se conserva `POST '/'`)*. **Deudas.** D-08 *(cierre definitivo)*, D-11, D-26 *(preparación)*.

**Problema.** `devops-agent-api.service.ts` (133 líneas) mezcla cuatro dominios sin relación: la tarjeta del agente y el chat A2A, la planeación vectorizada, el tablero de DevOps (con un
`EventSource` construido a mano) y las tareas JSON-RPC. Un cambio en el stream SSE obliga a tocar el mismo archivo que la carga de planeaciones.

**Archivos a crear**

| Ruta                                                           | Contenido                                                                                             |
|----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `features/devops-agent/services/api/agent-chat-api.service.ts` | `getAgentCard`, `sendMessage`                                                                         |
| `features/devops-agent/services/api/planning-api.service.ts`   | `uploadPlanning`, `getInitiatives`, `deleteInitiative`, `updateInitiativeCell`, `getInitiativeChunks` |
| `features/devops-agent/services/api/dashboard-api.service.ts`  | `getDashboardData`, `getDashboardDataStream`                                                          |
| `features/devops-agent/services/api/tasks-api.service.ts`      | `getTasks`, `cancelTask` (conserva `POST '/'`, DP-03 🟢)                                              |
| `features/devops-agent/services/api/index.ts`                  | Barrel                                                                                                |

**Restricción absoluta.** `DevopsAgentApiService` **debe conservarse como fachada delegante** hasta la Fase 05: `devops-agent-api.service.spec.ts` (18 pruebas) y
`planning-management.component.spec.ts` lo inyectan por nombre y **no pueden modificarse** (R-3). Eliminarlo aquí rompería el contrato de caracterización.

**Criterio de cierre.** Cuatro servicios de API con responsabilidad única, ninguno por encima de 60 líneas, la fachada delegando sin lógica propia y las 200+ pruebas verdes sin modificarse.

