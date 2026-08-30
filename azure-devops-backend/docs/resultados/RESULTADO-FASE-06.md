# RESULTADO — Fase 06

> **Fase:** [`fase-06.md`](../fases/fase-06.md) · **Cierre:** 2026-08-29
> **Deudas objetivo:** D-05, D-10, D-11, D-31 · más **DP-04**
> **Fase anterior:** [`RESULTADO-FASE-05.md`](RESULTADO-FASE-05.md)

---

## 1. Objetivo

Rediseñar el flujo SSE del tablero y sacar del BFF los tres olores que quedaban alrededor de él: un
entry-point que hacía de todo (D-05), una escritura de ficheros síncrona dentro del dominio (D-10),
un repliegue a datos simulados repartido por cuatro sitios (D-11) y, sobre todo, **un modo de fallo
que el usuario no podía ver** (D-31).

---

## 2. Decisiones

### 2.1 DP-04 — destino de los reportes

**Resuelta como opción A: solo disco, detrás de un `ReportStoragePort`.**

No se comprobó —ni se asumió— que el bucket S3 configurado sea accesible desde el entorno de
ejecución ni con qué credenciales. El puerto deja esa puerta abierta sin coste: añadir S3 más
adelante es escribir otro adaptador, y el dominio no se entera.

### 2.2 Bloqueantes menores

| #        | Resuelto                                                  | Consecuencia                                                           |
|----------|-----------------------------------------------------------|------------------------------------------------------------------------|
| **B-05** | Un fallo al guardar el reporte **solo se registra**       | El stream continúa; el tablero ya se entregó                           |
| **B-06** | El stream **emite un evento `ERROR`** en vez del 500 mudo | Cambio de contrato aditivo; obliga a tocar 1 prueba de caracterización |
| **B-07** | El repliegue a mock **no existe en producción**           | 4 puntos → 1, gobernado por configuración, fuera del entry-point       |

### 2.3 Excepciones sobre el test de caracterización

`DashboardRoutesCharacterizationTest` sigue siendo intocable. Se autorizaron **por escrito** dos
excepciones nuevas, documentadas en el javadoc de la propia clase con el formato de S-2:

- **S-3.** Una línea de *stub* en `setUp`: `saveDashboardReport` devuelve `Mono<Void>` desde que
  D-10 sacó la escritura del caso de uso, y Mockito devuelve `null` para los métodos que retornan
  `Mono`. Sin el stub el stream moría con un NPE **contra un doble de prueba**, no contra el código.
  Ninguna aserción se tocó.
- **S-4.** Dos pruebas cambian porque el comportamiento que fijaban **deja de existir**:
  `givenInitialFailsWithMcpEnabled_...` (el 500 que B-06 elimina) y `givenMcpDisabledAndFailure_...`
  (que ni compilaba, porque `isMcpEnabled()` desaparece de la API pública del caso de uso).

**Las diez pruebas restantes siguen intactas y verdes.**

### 2.4 Decisión no prevista: `UseCasesConfig` pasa a inyección por constructor

Añadir la propiedad de B-07 subió *Rule_2.7* de 6 a 7 violaciones —la regla se cuenta **por campo**,
no por clase—. En vez de aceptar la subida se convirtió toda la clase a inyección por constructor:
la regla baja a **3**. Es la misma trampa que costó un ciclo en la Fase 05.

---

## 3. Qué se construyó

### T-01 · `ReportStoragePort` y el fin de la escritura síncrona (D-10)

| Dónde                                                        | Qué                                                                                   |
|--------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `domain/model/.../dashboard/gateways/ReportStoragePort.java` | Puerto nuevo: `Mono<Void> save(String reportName, String content)`                    |
| `infrastructure/driven-adapters/report-storage-file/`        | **Módulo nuevo** con `FileReportStorageAdapter`                                       |
| `DevOpsDashboardUseCase.saveDashboardReport`                 | `void` → `Mono<Void>`; compone el contenido y el nombre, ya no sabe qué es un fichero |

El adaptador confina la escritura en `Schedulers.boundedElastic()` —el bucle de eventos no se toca—,
escribe en UTF-8 y **rechaza nombres que se salgan del directorio configurado**: el nombre se
compone a partir de lo que teclea el usuario, así que es entrada externa.

El directorio es configurable (`report.storage.directory`, por defecto `reports`): el valor
observable no cambia.

### T-02 · El ciclo de vida de la `Task` sale del `Handler` (D-05)

`api/dashboard/DashboardTaskTracker` concentra las transiciones WORKING → COMPLETED / FAILED.

> **Hallazgo al extraerlo (H-2).** La primera versión llamaba a `taskStoreGateway.save(...)` en
> **tiempo de ensamblado** en vez de en tiempo de suscripción: la tarea se guardaba aunque el paso
> nunca llegara a ejecutarse, y la marca de tiempo era la del ensamblado. El `Mono.defer` del código
> original lo evitaba sin que se notara. Lo cazó la prueba de caracterización (3 guardados en vez de
> 2) y está anotado en el javadoc del método.

### T-03 y T-04 · La orquestación sale del `Handler` y el fallo se ve (D-05, D-31)

`api/dashboard/DashboardStreamOrchestrator` se ocupa del flujo. La clave de D-31 cabe en una frase:
**el stream ya nunca termina en error**. Cualquier fallo se convierte en un evento `ERROR` y el
flujo se completa, que es la única forma de que WebFlux llegue a escribir la cabecera
`text/event-stream`.

```
Antes:  agente caído → 500 con cuerpo JSON → EventSource.onerror → observable completado en silencio
Ahora:  agente caído → 200 text/event-stream → event: ERROR {"message":"..."} → el usuario lo ve
```

`DashboardUpdateEvent` gana un campo `message` y la constante `ERROR`. El cambio es **aditivo**:
`INITIAL` y `BATCH_UPDATE` conservan exactamente los mismos campos, con `message` a `null`.

### T-05 · Repliegue a mock unificado (D-11, B-07)

| Antes                                                | Ahora                                 |
|------------------------------------------------------|---------------------------------------|
| `DevOpsDashboardUseCase.fallbackOrPropagate`         | **`askWithFallback`, el único punto** |
| `getDashboardData` → `onErrorResume`                 | usa `askWithFallback`                 |
| `getDashboardInitialData` → `onErrorResume`          | usa `askWithFallback`                 |
| `Handler`: 2 `onErrorResume` + el `catch` del parseo | **eliminados**                        |

`isMcpEnabled()` y `getMockDashboardData()` **desaparecen de la API pública** del caso de uso: un
adaptador HTTP no tiene por qué saber si el canal de IA está habilitado.

El interruptor deja de ser «hay MCP» y pasa a ser lo que de verdad es: «quiero datos de mentira»
(`dashboard.mock-fallback.enabled`, por defecto **apagado**). Sin declarar, se deduce del cliente
MCP para no alterar el comportamiento anterior.

### T-06 · El mapper, en un solo sitio

`DashboardDtoMapper` se invoca **exclusivamente desde `DashboardStreamOrchestrator`**. Cierra el
segundo paso de B-03 que la Fase 05 dejó planificado.

---

## 4. Métricas

|                                         | Antes (Fase 05)          | Ahora          |
|-----------------------------------------|--------------------------|----------------|
| Pruebas / fallos                        | 219 / 0                  | **227 / 0**    |
| `Handler`                               | 382 líneas               | **192 líneas** |
| `handleDevOpsDashboardStream`           | ~90 líneas               | **15 líneas**  |
| Puntos de repliegue a mock              | 4                        | **1**          |
| `block()` / `subscribe()` en `src/main` | 0                        | **0**          |
| I/O síncrona en `domain/usecase`        | sí (`Files.writeString`) | **no**         |
| Cobertura `domain/model`                | 94,8 %                   | **94,8 %**     |
| Cobertura `domain/usecase`              | 97,6 %                   | **98,9 %**     |
| ArchUnit *Rule_2.7*                     | 6                        | **3**          |
| ArchUnit *Rule_2.2* (backend)           | 0                        | **0**          |

---

## 5. Comandos

```powershell
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-backend
.\gradlew test --console=plain 2>&1 | Select-String -Pattern '^BUILD|FAILED|was violated'
.\gradlew jacocoMergedReport --no-configuration-cache --console=plain
```

---

## 6. Desviaciones respecto al plan

1. **Se modificó el test «intocable»** en tres puntos, con autorización expresa (S-3 y S-4, §2.3).
   `fase-06.md` §3 preveía **una** prueba afectada; fueron **dos**, más un stub. La segunda no era
   evitable: B-07 elimina el método que esa prueba stubeaba.
2. **Se creó un módulo Gradle nuevo** (`report-storage-file`) en vez de alojar el adaptador en
   `app-service`, donde viven los de `PromptTemplatePort` y `DashboardFallbackPort`. Motivo:
   aquéllos leen recursos del *classpath*; éste hace **entrada/salida real**, y
   `rules/spring-rules.md` §88 sitúa esos adaptadores en `driven-adapters`.
3. **Se corrigió `Rule_2.7` sobre la marcha**, igual que en la Fase 05 (§2.4).
4. **El frontend no se tocó.** El módulo objetivo de la fase es el backend; el consumo del evento
   `ERROR` queda como deuda **D-40**.

---

## 7. Hallazgos

- **H-1.** El repliegue a mock no se podía unificar sin romper una prueba de caracterización, porque
  esa prueba **stubeaba el propio interruptor** (`isMcpEnabled()`). Una prueba que fija un olor lo
  protege: es el precio de caracterizar antes de refactorizar, y hay que pagarlo por escrito.
- **H-2.** Extraer código reactivo a un colaborador **cambia el momento en que se evalúan las
  llamadas**. Lo que dentro de un `Mono.defer` era perezoso, en un método suelto se vuelve ansioso.
  Ver T-02.
- **H-3.** *Rule_2.7* se cuenta **por campo**, no por clase: añadir un `@Value` a una clase que ya
  violaba la regla **sí** sube el contador.
- **H-4.** Las pruebas del reporte escribían en el `reports/` del propio repositorio y lo dejaban
  sucio. Al pasar el dominio a un puerto, desaparecieron del dominio y las del adaptador usan
  `@TempDir`.
- **H-5.** `DashboardMarkdownReport` estaba ya tan separado que T-01 no tuvo que tocarlo ni una
  línea. La Fase 04 dejó el trabajo hecho sin saberlo.

---

## 8. Deudas nuevas

| #        | Deuda                                                                                                                                                                                                                     | Dónde                   |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| **D-40** | El frontend **no maneja el evento `ERROR`**: el backend ya cuenta el fallo, pero nadie lo escucha todavía. Sin esto, D-31 está cerrada a medias                                                                           | `azure-devops-frontend` |
| **D-41** | Con varias réplicas o sistema de ficheros efímero, **los reportes en disco se pierden**. Es la consecuencia conocida de DP-04 opción A; el puerto ya deja la puerta abierta a S3                                          | `azure-devops-backend`  |
| **D-42** | `DashboardStreamOrchestrator` y `DashboardTaskTracker` se instancian con `new` dentro del `Handler` para no alterar el `@ContextConfiguration` del test de caracterización. Cuando ese test se jubile, deberían ser beans | `azure-devops-backend`  |

---

## 9. Estado del control de versiones

⚠️ **Nada se ha confirmado, por decisión expresa del usuario.** El árbol acumula el trabajo de las
Fases 02 a 06. El plan acordado sigue siendo **arreglar primero los issues de SonarQube y confirmar
después, por fases**. Los mensajes deberán seguir `COMMIT_RULES.md`.

---

## 10. Checklist de cierre

- [x] `gradlew test` verde, ≥ 219 pruebas, 0 fallos → **227 / 0**
- [x] Las 12 pruebas de `DashboardRoutesCharacterizationTest` verdes (2 reescritas bajo S-4)
- [x] Cero `block()` / `subscribe()` manual en `src/main`
- [x] Cero I/O síncrona dentro de `domain/usecase`
- [x] Puntos de repliegue a mock: 4 → 1
- [x] `handleDevOpsDashboardStream` por debajo de 30 líneas → **15**
- [x] Cobertura `domain/usecase` ≥ 90 % y `domain/model` ≥ 94,8 %
- [x] `RESULTADO-FASE-06.md`
- [x] `plan-maestro.md` y `fase-07.md` actualizados


