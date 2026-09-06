# RESULTADO — FASE 01: Baseline y red de seguridad

> **Ejecutada:** 2026-08-29 · **Estado final:** 🟢 COMPLETADA
> **Instrucciones:** `docs/fases/fase-01.md` · **Plan:** `docs/plan/plan-maestro.md` v2.0
> **Código productivo modificado:** **cero líneas**

---

## 1. Objetivo de la fase

Construir la red que hace verificables las siete fases siguientes: congelar con pruebas de
caracterización los tres flujos vivos del BFF (planificación, auditoría del backlog y delegación en
el agente), reparar el test de wiring que daba verde en falso, y publicar el baseline medido.

---

## 2. Qué se construyó

### Pruebas nuevas

| Archivo                                                     | Módulo           | Pruebas |
|-------------------------------------------------------------|------------------|--------:|
| `usecase/ingestplanning/IngestPlanningSpecUseCaseTest.java` | `domain/usecase` |       8 |
| `usecase/manageplanning/ManagePlanningUseCaseTest.java`     | `domain/usecase` |       9 |
| `usecase/searchplanning/SearchPlanningSpecUseCaseTest.java` | `domain/usecase` |       7 |
| `api/PlanningRoutesCharacterizationTest.java`               | `reactive-web`   |      15 |
| `api/DashboardRoutesCharacterizationTest.java`              | `reactive-web`   |      12 |

### Pruebas reescritas o ampliadas

| Archivo                                             |      Antes | Después | Motivo                                                                                                                      |
|-----------------------------------------------------|-----------:|--------:|-----------------------------------------------------------------------------------------------------------------------------|
| `config/UseCasesConfigTest.java`                    |  1 (falsa) |   **5** | Capturaba `UnsatisfiedDependencyException` y respondía `assertTrue(true)`: daba verde **mientras el contexto no arrancaba** |
| `mcpclient/adapter/ChatGatewayAdapterTest.java`     | 3 (falsas) |  **10** | No instanciaba el adaptador: reejecutaba su expresión regular sobre unas cadenas                                            |
| `usecase/dashboard/DevOpsDashboardUseCaseTest.java` |          4 |  **18** | Faltaba congelar la resolución de rutas de Azure DevOps y el formato del reporte                                            |

### Documentación

- `docs/resultados/_PLANTILLA_RESULTADO.md`
- `docs/resultados/RESULTADO-FASE-01.md` (este archivo)

---

## 3. Decisiones aplicadas

| ID        | Resolución del usuario                                           | Efecto en esta fase                                                                                                                        |
|-----------|------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-01** | Reactivar A2A **como cliente**, con control del estado de tareas | Las pruebas del adaptador documentan de forma ejecutable que hoy **la `Task` se descarta** (D-23), en vez de darlo por bueno               |
| **DP-08** | **No bloqueante y reactivo de extremo a extremo**                | Todas las pruebas nuevas usan `StepVerifier` sobre `Mono`/`Flux`. **Cero `block()`, cero `subscribe()` manual** en las 51 pruebas escritas |

---

## 4. Métricas obtenidas

| Métrica                                    | Antes |                         Después |
|--------------------------------------------|------:|--------------------------------:|
| **Tests totales**                          |    41 |                         **117** |
| Tests fallando                             |    15 | **15** (los mismos, ni uno más) |
| Tests que daban verde en falso             |     4 |                           **0** |
| Cobertura `domain/usecase` (instrucciones) |   n/d |          **85,8 %** (1497/1745) |
| Cobertura `mcp-client`                     |  ~0 % |            **24,3 %** (109/449) |
| Cobertura `app-service`                    |   n/d |              **67,4 %** (29/43) |
| Líneas de código productivo modificadas    |     — |                           **0** |

### Detalle por suite

| Suite                                 |   Tests | Fallos |
|---------------------------------------|--------:|-------:|
| `DevOpsDashboardUseCaseTest`          |      18 |      0 |
| `PlanningRoutesCharacterizationTest`  |      15 |      0 |
| `DashboardRoutesCharacterizationTest` |      12 |      0 |
| `ChatGatewayAdapterTest`              |      10 |      0 |
| `ManagePlanningUseCaseTest`           |       9 |      0 |
| `IngestPlanningSpecUseCaseTest`       |       8 |      0 |
| `SearchPlanningSpecUseCaseTest`       |       7 |      0 |
| `AgentChatUseCaseTest`                |       7 |      0 |
| `ArchitectureTest`                    |       6 |      0 |
| `UseCasesConfigTest`                  |       5 |      0 |
| `MicrometerMetricPublisherTest`       |       1 |      0 |
| `RouterRestLegacySunsetTest`          |       1 |      0 |
| `RouterRestTest`                      |      11 |  **9** |
| `JsonRpcErrorContractTest`            |       5 |  **5** |
| `RouterRestLegacyToggleTest`          |       2 |  **1** |
| **Total**                             | **117** | **15** |

### Violaciones de ArchUnit (baseline, antes sin medir)

`ArchitectureTest` degrada sus fallos a `WARNING` mediante `checkWithWarning`, de modo que nunca
aparecían. Medidas con `-i`:

| Regla                                                               | Violaciones |
|---------------------------------------------------------------------|------------:|
| `Rule_2.7` — los beans solo deben tener atributos `final`           |       **6** |
| `Rule_2.2` — las clases de dominio no deben llevar sufijos técnicos |       **4** |
| **Total**                                                           |      **10** |

Ambas advierten: *«This will cause a build error in future»*. Se saldan en la Fase 08.

---

## 5. Comandos ejecutados

```powershell
.\gradlew.bat :usecase:test --rerun-tasks
.\gradlew.bat :app-service:test --tests '*UseCasesConfigTest*' --rerun-tasks
.\gradlew.bat :reactive-web:test --tests '*PlanningRoutesCharacterizationTest*' --rerun-tasks
.\gradlew.bat :reactive-web:test --tests '*DashboardRoutesCharacterizationTest*' --rerun-tasks
.\gradlew.bat :mcp-client:test --rerun-tasks
.\gradlew.bat test
.\gradlew.bat :usecase:jacocoTestReport :mcp-client:jacocoTestReport :app-service:jacocoTestReport
git --no-pager status --porcelain -- azure-devops-backend
```

**Resultado:** 117 pruebas, 15 fallos —los 15 heredados—; `git status` confirma **cero cambios en
`src/main`**.

---

## 6. Desviaciones respecto a las instrucciones

| Desviación                                                             | Justificación                                                                                                                                                                                                                      |
|------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No se usó `MockWebServer` para `ChatGatewayAdapterTest`                | No está en el classpath y añadir una dependencia nueva excede el alcance de una fase que no debe tocar producción. Se usó `reactor.netty.http.server.HttpServer`, que ya viene con WebFlux y permite capturar cuerpo y ruta reales |
| No se generó `docs/resultados/baseline-fase-01.md` como archivo aparte | El baseline se integró en este `RESULTADO-FASE-01.md` para seguir la convención de `azure-devops-agent/docs/resultados`, que el usuario pidió replicar. El «mapa de integración» pedido en T-08 está en §8                         |
| `jacocoMergedReport` no se pudo ejecutar                               | Falla porque depende de `test`, y `test` arrastra los 15 fallos heredados. Se midió por módulo con `jacocoTestReport`. Se resolverá solo cuando la Fase 02 ponga el build en verde                                                 |
| Cobertura de `reactive-web` no medida                                  | Mismo motivo: su `jacocoTestReport` depende de un `test` en rojo                                                                                                                                                                   |
| Dos pruebas se ajustaron tras verlas fallar                            | Ver §8, hallazgo H-1. El comportamiento real difería de lo previsto en las instrucciones; se corrigió la **prueba**, nunca el código                                                                                               |

---

## 7. Checklist de calidad

| Criterio (`rules/spring-rules.md` §7)            | Estado                                            |
|--------------------------------------------------|---------------------------------------------------|
| `domain/model` sin dependencias técnicas         | ✅ sin cambios                                    |
| `domain/usecase` sin anotaciones de Spring       | ✅ sin cambios                                    |
| Sin secretos hardcodeados (S2068)                | ✅                                                |
| Complejidad cognitiva ≤ 15                       | ✅                                                |
| Llaves `{}` en todo control de flujo (S1117)     | ✅                                                |
| Sin números mágicos (S109)                       | ✅ constantes con nombre en todas las suites      |
| Convención `givenX_whenY_thenZ`                  | ✅ en las 51 pruebas nuevas                       |
| Patrón GIVEN / WHEN / THEN explícito             | ✅ comentado en cada prueba                       |
| `@ExtendWith(MockitoExtension.class)` en dominio | ✅                                                |
| Reactivo puro, sin `block()` (DP-08)             | ✅ **cero** `block()` y cero `subscribe()` manual |
| Build sin fallos nuevos                          | ✅ exactamente los 15 heredados                   |

---

## 8. Hallazgos

### H-1 · Un fallo del agente en el stream SSE devuelve 500, no un evento de error

**Hallazgo nuevo, no previsto en el plan.** Con MCP habilitado, si `getDashboardInitialData` falla,
el error se propaga **antes** de que WebFlux escriba la cabecera `text/event-stream`. El cliente
recibe un **500 con cuerpo JSON**, no un stream.

Consecuencia en el front: `EventSource.onerror` cierra la conexión y completa el observable **en
silencio**, así que el usuario no distingue «este sprint no tiene historias» de «el agente se cayó».

→ Registrado como **D-31**, se atiende en la **Fase 06** (rediseño del flujo SSE).

### H-2 · El BFF no valida la entrada, pese a ser su misión declarada

`maxResults=no-es-un-numero` revienta en `Integer.parseInt` **antes** de entrar en la cadena
reactiva, de modo que el `onErrorResume` del handler no lo captura y la excepción escapa como error
genérico del servidor: **500 en vez de 400**. Confirma **D-28**. Congelado en
`PlanningRoutesCharacterizationTest`; se corrige en la **Fase 03**.

### H-3 · Cuatro pruebas daban verde sin probar nada

| Prueba                       | Qué hacía en realidad                                                                                                                                                                           |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `UseCasesConfigTest` (1)     | Capturaba `UnsatisfiedDependencyException` y respondía `assertTrue(true)`. El log confirma que el contexto **no arrancaba** por falta de `PlanningVectorStorePort`, y aun así el test era verde |
| `ChatGatewayAdapterTest` (3) | Reejecutaba la expresión regular `<think>.*?</think>` sobre tres cadenas, sin instanciar `ChatGatewayAdapter`                                                                                   |

Es la segunda vez que aparece el patrón: ya se documentó en `CIERRE-DEL-PLAN.md` §7.2 del proyecto
agente. **Un test puede mentir**, y el build no lo detecta.

### H-4 · El adaptador al agente no tiene timeout

`ChatGatewayAdapter` no declara ningún timeout. Si el agente no responde, la llamada queda colgada
indefinidamente y con ella el stream SSE del dashboard. Documentado con una prueba que deberá
invertirse en la **Fase 02**.

### H-5 · Mapa de integración actual (T-08)

| Llamada del front                  | `proxy.conf.json`         | Destino real                  | Estado                                           |
|------------------------------------|---------------------------|-------------------------------|--------------------------------------------------|
| `POST /message:send`               | `/message:send` → `:8082` | **Agente, directo**           | ⚠️ Salta el BFF y usa el endpoint legacy vencido |
| `GET /.well-known/agent-card.json` | no proxiado               | `:4200`                       | 🔴 **rota**                                      |
| `POST /` (`tasks/cancel`)          | no proxiado               | `:4200` devuelve `index.html` | 🔴 **rota**                                      |
| `GET /api/tasks`                   | `/api` → `:8081`          | BFF                           | 🟡 funciona **por la tabla compartida**          |
| `GET /api/planning/**`             | `/api` → `:8081`          | BFF                           | ✅                                               |
| `GET /api/devops/**`               | `/api` → `:8081`          | BFF                           | ✅                                               |

Las dos rutas rotas quedan congeladas con una prueba que **espera 404** y que la Fase 03 deberá
invertir.

### H-6 · La cobertura de `domain/usecase` está inflada

El 85,8 % incluye `AgentChatUseCase`, 537 líneas de **código muerto** que sus 7 pruebas cubren
generosamente. Al eliminarlo en la Fase 02, la cifra reflejará la realidad del código que sí se
ejecuta.

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `docs/fases/fase-02.md` — Cliente A2A del agente y control de tareas
- **Decisiones resueltas en esta fase:** DP-01, DP-08
- **Decisiones abiertas:** DP-07 (Fase 03), DP-02 (Fase 04), DP-03 (Fase 05), DP-04 (Fase 06), DP-05
  y DP-06 (Fase 07)
- **Deudas nuevas registradas:** **D-31** (fallo SSE indistinguible de «sin datos»)
- **Pendiente de ratificación:** ninguno
- **Bloqueo para la Fase 02:** ninguno. DP-08 quedó resuelta: no bloqueante y reactivo de extremo a
  extremo

