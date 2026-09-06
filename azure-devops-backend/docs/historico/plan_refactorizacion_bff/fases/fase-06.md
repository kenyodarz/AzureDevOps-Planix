# FASE 06 — El flujo SSE y la salida de los reportes

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Estado:** 🟢 **CERRADA** (2026-08-29)
> **Deudas objetivo:** **D-05, D-10, D-11, D-31** · más **DP-04** — todas cerradas
> **Resultado:** [`RESULTADO-FASE-06.md`](../resultados/RESULTADO-FASE-06.md)
> **Arranque en sesión nueva:** [`HANDOFF-FASE-06.md`](../plan/HANDOFF-FASE-06.md)
> **Fase anterior:** [`fase-05.md`](fase-05.md) → [
`RESULTADO-FASE-05.md`](../resultados/RESULTADO-FASE-05.md)
> **Fase siguiente:** [`fase-07.md`](fase-07.md)

---

## 1. Contexto

### 1.1 De dónde vienes

La Fase 05 está cerrada. Al arrancar esta fase existe:

- **Build en verde: 219 pruebas, 0 fallos.** MCP también verde.
- El tablero **ya tiene dominio**: `BacklogAudit`, `BacklogMetrics`, `StoryQuality` y `StoryUpdate`,
  todos inmutables. `partition`, `withUpdates`, `recalculatedFrom` y `unaudited` viven ahí.
- `Handler` **ya no piensa**: cero reglas de negocio, cero mutaciones in-place, cero `sharedData`.
- El fallo del año está cerrado en el BFF **y** en el MCP.
- `DashboardDtoMapper` es la frontera entre el JSON y el dominio.
- Cobertura: `domain/model` 94,8 % · `domain/usecase` 97,6 %.

### 1.2 El problema

`Handler` sigue siendo demasiadas cosas a la vez, y el flujo SSE tiene un modo de fallo que el
usuario no puede ver.

| Deuda    | Dónde                                        | Qué pasa                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|----------|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **D-05** | `Handler.handleDevOpsDashboardStream`        | Un solo método mezcla HTTP, orquestación del SSE, parseo, mapeo y **el ciclo de vida completo de la `Task` A2A** (WORKING → COMPLETED / FAILED). Son cuatro responsabilidades en una cadena de ~90 líneas.                                                                                                                                                                                                                                                                                        |
| **D-31** | El mismo método                              | 🔴 **Con MCP habilitado, si el agente falla al arrancar, el cliente recibe un 500 con cuerpo JSON, no un stream SSE con un evento de error.** WebFlux no llega a escribir la cabecera `text/event-stream`. El front (`EventSource.onerror`) cierra la conexión y completa el observable **en silencio**: el usuario no distingue «no hay datos» de «el agente se cayó». Es el mismo modo de fallo silencioso que la Fase 05 acaba de eliminar del `IterationPath`, pero en la capa de transporte. |
| **D-11** | `Handler` y `DevOpsDashboardUseCase`         | El repliegue a mock está **duplicado en cuatro puntos** y `isMcpEnabled()` se ha filtrado hasta el entry-point: un adaptador HTTP no debería saber si el canal de IA está habilitado.                                                                                                                                                                                                                                                                                                             |
| **D-10** | `DevOpsDashboardUseCase.saveDashboardReport` | Escribe en disco desde un método `void` **síncrono**, dentro de un caso de uso, en mitad de una cadena reactiva. Bloquea el hilo del event loop y no hay forma de saber si falló.                                                                                                                                                                                                                                                                                                                 |

### 1.3 Por qué D-31 es la más importante

La Fase 05 demostró que **el peor fallo no es el que rompe, sino el que no se ve**. El año del
`IterationPath` devolvía un tablero vacío sin error, sin aviso y sin log, y por eso sobrevivió tanto
tiempo. D-31 es exactamente el mismo patrón una capa más arriba: cuando el agente se cae, el usuario
ve un tablero vacío y no tiene manera de saber por qué.

---

## 2. 🔴 DP-04 — decisión pendiente, bloquea la fase

**Pregunta:** ¿dónde deben acabar los reportes `.md`?

`plan-maestro.md` la enuncia así: *«Destino de los reportes `.md`: disco, el bucket S3 ya
configurado y sin usar, o ambos.»*

|       | Opción                                             | Consecuencia                                                                                                                                                                 |
|-------|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **A** | Solo disco, detrás de un `ReportStoragePort`       | Cambio mínimo. El puerto ya permite añadir S3 después sin tocar el dominio. En un despliegue con varias réplicas o sistema de ficheros efímero, **los reportes se pierden**. |
| **B** | Solo S3                                            | Usa el bucket que ya está configurado y sin estrenar. Añade una dependencia de red al camino de cierre del stream.                                                           |
| **C** | Ambos, con S3 como principal y disco como respaldo | Lo más robusto y lo más caro.                                                                                                                                                |

**Hace falta saber, antes de decidir:** si el bucket está realmente accesible desde el entorno de
ejecución y con qué credenciales. **No se asume nada** (Política de No-Asunción).

### Bloqueantes menores, a confirmar junto con DP-04

| #        | Pregunta                                                                                        | Recomendación                                                                                                                              |
|----------|-------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| **B-05** | ¿La escritura del reporte debe **fallar** el stream si no se puede guardar, o solo registrarse? | **Solo registrarse.** El tablero ya se entregó al usuario; perder el reporte no debería invalidar la consulta.                             |
| **B-06** | Para D-31, ¿el stream debe emitir un **evento SSE de tipo `ERROR`** antes de cerrarse?          | **Sí.** Es lo único que permite al frontend distinguir «vacío» de «roto». Implica un cambio de contrato: hay que coordinarlo con el front. |
| **B-07** | ¿El repliegue a mock debe seguir existiendo en producción?                                      | **No.** Unificarlo en **un solo punto** del dominio y dejarlo gobernado por configuración, fuera del entry-point.                          |

---

## 3. Lo que esta fase hará (borrador, sujeto a DP-04)

- **T-01** · `ReportStoragePort` en el dominio, con su adaptador según DP-04, y
  `saveDashboardReport` pasa a devolver `Mono<Void>` (D-10).
- **T-02** · El ciclo de vida de la `Task` A2A sale del `Handler` a un colaborador propio (D-05).
- **T-03** · La orquestación del stream sale del `Handler`; el entry-point vuelve a ser lo que debe
  ser: traducir HTTP (D-05).
- **T-04** · El stream SSE emite un evento de error en vez de un 500 mudo, según B-06 (D-31).
- **T-05** · Repliegue a mock **unificado en un punto**; `isMcpEnabled()` deja de asomar por el
  entry-point (D-11).
- **T-06** · `DashboardDtoMapper` queda invocado desde un único sitio, cerrando el segundo paso de
  B-03 que la Fase 05 dejó planificado.

### Criterios de cierre propuestos

- [ ] `gradlew test` verde, **≥ 219 pruebas**, 0 fallos.
- [ ] Las 12 pruebas de `DashboardRoutesCharacterizationTest` verdes. **Si B-06 se aprueba, la que
  fija el 500 (`givenInitialFailsWithMcpEnabled_...`) cambiará necesariamente**: es la única
  autorización de modificación que esta fase necesita, y debe quedar por escrito igual que S-2.
- [ ] Cero `block()` / `subscribe()` manual en `src/main`.
- [ ] Cero I/O síncrona dentro de `domain/usecase`.
- [ ] Puntos de repliegue a mock: 4 → **1**.
- [ ] `Handler.handleDevOpsDashboardStream` por debajo de **30 líneas**.
- [ ] Cobertura `domain/usecase` ≥ 90 %; `domain/model` no baja de 94,8 %.
- [ ] `RESULTADO-FASE-06.md` con la estructura habitual.
- [ ] `plan-maestro.md` y `fase-07.md` actualizados.

---

## 4. Lo que NO entra en esta fase

- Unificar las tres fachadas de logging → **Fase 08** (D-12).
- El módulo `mcp-client` casi vacío → **Fase 08** (D-32).
- Migrar los prompts al MCP / agente → **D-35**, plan propio.
- Medir el coste real del lote y la concurrencia → **D-38**, requiere entorno con datos.
- Pruebas de integración del adaptador REST del MCP → **D-36**, fuera del alcance del BFF.
- Cambiar el proveedor de LLM o el protocolo MCP → fuera del plan.

---

## 5. Resultado

> Detalle completo en [`RESULTADO-FASE-06.md`](../resultados/RESULTADO-FASE-06.md).

- **Fecha de cierre:** 2026-08-29
- **DP-04 resuelta como:** **A** — solo disco, detrás de `ReportStoragePort`. No se asumió nada
  sobre el bucket S3; añadirlo después es escribir otro adaptador.
- **B-05 / B-06 / B-07:** solo registrar / sí, evento `ERROR` / no, unificado en 1 punto
- **Tests totales / fallidos:** 227 / 0
- **Puntos de repliegue a mock:** 4 → **1**
- **Líneas de `handleDevOpsDashboardStream`:** ~90 → **15** (`Handler`: 382 → 192)
- **Cobertura `domain/model` / `domain/usecase`:** 94,8 % / 98,9 %
- **Hallazgos no previstos:** extraer código reactivo a un colaborador convirtió en ansiosa una
  llamada que el `Mono.defer` original hacía perezosa; *Rule_2.7* se cuenta por campo y no por clase
  (se aprovechó para bajarla de 6 a 3).
- **Deudas nuevas detectadas:** **D-40** (el frontend aún no maneja el evento `ERROR`), **D-41**
  (reportes en disco se pierden con réplicas o FS efímero), **D-42** (colaboradores del stream
  instanciados con `new` para no alterar el test de caracterización).

