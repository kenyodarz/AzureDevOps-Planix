# RESULTADO — FASE 07 · Adaptadores y contrato HTTP

> **Plan:** `docs/plan/plan-maestro.md` v2.0 · **Fase:** [`fase-07.md`](../fases/fase-07.md)
> **Fase anterior:** [`RESULTADO-FASE-06.md`](RESULTADO-FASE-06.md)
> **Fecha de cierre:** 2026-08-29

---

## 1. Objetivo

Arreglar las dos fronteras del BFF que estaban mal traducidas: la de **entrada** (HTTP), donde
convivían dos criterios de traducción de errores, y la de **salida** (base de datos), donde el
modelo de dominio se serializaba tal cual. Deudas objetivo: **D-14, D-15, D-16, D-20**, más **D-40**
y **D-42**.

---

## 2. Decisiones

Todas se resolvieron el 2026-08-29, antes de escribir una línea de código.

| #          | Resuelta como                                                                                                                                                                              |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DP-05a** | El módulo pasa a llamarse **`task-store-postgres`**. Se autoriza actualizar la ruta codificada en `ArchitectureTest`                                                                       |
| **DP-05b** | **`planning_chunks` no admite columnas nuevas y no hay más consumidores previstos.** D-14 se cierra con entidad de persistencia + mapper **sobre el esquema actual**, sin DDL ni migración |
| **DP-06**  | Se parte del mapa que ya aplicaba `TaskHandler` y se extiende a todas las rutas, más `TimeoutException → 504`                                                                              |
| **B-08**   | **Sí**, se puede tocar el frontend, en tarea propia y explícita                                                                                                                            |
| **B-09**   | **Sí**, se autoriza reescribir las pruebas de caracterización cuyo código HTTP cambie por D-16, con el formato de S-2/S-3/S-4                                                              |
| **B-10**   | **No.** D-29/D-34 esperan a D-35                                                                                                                                                           |

### El mapa de DP-06

| Excepción                                                  | Código  | Por qué                                    |
|------------------------------------------------------------|---------|--------------------------------------------|
| `TaskNotFoundException`                                    | **404** | el recurso pedido no existe                |
| `InvalidAgentRequestException`, `IllegalArgumentException` | **400** | la petición del cliente es inválida        |
| `AgentUnavailableException`                                | **503** | el agente ni siquiera llegó a intentarlo   |
| `AgentExecutionException`                                  | **502** | el agente respondió, pero mal              |
| `TimeoutException`                                         | **504** | el agente no respondió a tiempo            |
| cualquier otra                                             | **500** | fallo del BFF; **no** es culpa del cliente |

El **cuerpo no cambia**: sigue siendo `{"error": "<mensaje>"}` en todas las rutas.

---

## 3. Qué se construyó

### T-01 · Un solo criterio de traducción de errores (D-16)

| Dónde                                   | Qué                                                                                              |
|-----------------------------------------|--------------------------------------------------------------------------------------------------|
| `api/error/ApiErrorTranslator.java`     | **Clase nueva.** El mapa de DP-06, en un único sitio                                             |
| `Handler`                               | Los **6** `onErrorResume` que devolvían 400 para todo pasan a `ApiErrorTranslator::toResponse`   |
| `TaskHandler`                           | Su mapa privado —el único que distinguía culpas— **se extrae** y la clase delega en el traductor |
| `api/error/ApiErrorTranslatorTest.java` | 7 pruebas que fijan el mapa completo                                                             |

Es la tercera versión de la misma familia de fallo que cerraron las fases 05 y 06: **información que
se pierde por el camino**. Un `IllegalStateException` de pgvector se le contaba al frontend como
«petición mal formada»; ahora sale como 500 y el frontend puede distinguir de quién es la culpa.

Es una **utilidad estática, sin inyección**: así no altera el `@ContextConfiguration` de las pruebas
de caracterización, que es la misma restricción que mantiene viva D-42.

> Los dos `ServerResponse.badRequest()` que quedan en `Handler` son la validación de `cell` y
> `sprint` **antes** de entrar en la cadena: son 400 legítimos y coherentes con el mapa.

### T-02 · DTOs de respuesta para planificación (D-15)

| Dónde                                                        | Qué                                                     |
|--------------------------------------------------------------|---------------------------------------------------------|
| `api/dto/planning/PlanningChunkResponse.java`                | DTO nuevo, con los mismos campos y nombres              |
| `api/dto/planning/PlanningDtoMapper.java`                    | Mapper, siguiendo el precedente de `DashboardDtoMapper` |
| `Handler.handleSearchPlanning` y `handleGetInitiativeChunks` | Dejan de devolver `PlanningChunk`                       |

**El JSON no cambia.** Lo que cambia es que ahora hay un sitio donde decidirlo: un renombrado en el
dominio ya no rompe el frontend en silencio.

### T-03 · Entidad de persistencia para los fragmentos (D-14)

| Dónde                                            | Qué                                                                                               |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `pgvector/entity/PlanningChunkEntity.java`       | Lo que **se guarda**, separado de lo que el dominio **es**                                        |
| `pgvector/entity/PlanningChunkEntityMapper.java` | La frontera que faltaba                                                                           |
| `PgVectorPlanningAdapter`                        | Serializa la entidad, nunca el modelo. Los dos puntos de deserialización se unifican en un método |
| `PlanningChunkEntityMapperTest.java`             | 4 pruebas; una fija **que el JSON almacenado no cambió**                                          |

Los nombres de los campos son exactamente los que producía la serialización directa del modelo, así
que el contenido de la metadata `chunk_json` es idéntico: **ninguna migración, ninguna columna
nueva** (DP-05b).

### T-04 · El módulo deja de mentir (D-20)

`task-store-inmemory` → **`task-store-postgres`**. Se movió con `git mv` (conserva el historial) y
se actualizaron los **tres** puntos que lo nombraban: `settings.gradle` (2 líneas),
`applications/app-service/build.gradle` (1) y la lista de rutas codificada de `ArchitectureTest`
(1). El paquete Java ya era `co.com.bancolombia.taskstore`: no hubo que tocarlo.

### T-05 · El frontend escucha el evento `ERROR` (D-40)

| Dónde                                    | Qué                                                                              |
|------------------------------------------|----------------------------------------------------------------------------------|
| `models/devops-agent.model.ts`           | `DashboardStreamEvent` y `DashboardStreamEventType`: el stream deja de ser `any` |
| `services/devops-agent-api.service.ts`   | Escucha `ERROR`, extrae el `message` y lo propaga como error del observable      |
| `services/devops-agent-state.service.ts` | Comentario del camino nuevo; el `error:` ya existía y ahora **recibe algo**      |
| `devops-agent-api.service.spec.ts`       | 3 pruebas nuevas (GIVEN/WHEN/THEN)                                               |

Con esto **D-31 queda cerrada del todo**. La Fase 06 hizo que el backend contara el fallo; hasta hoy
no había nadie escuchando:

```
Antes:  agente caído → evento ERROR → nadie lo escucha → onerror → observable completado en silencio
Ahora:  agente caído → evento ERROR → dashboardError$ → el usuario lee «agente caído»
```

---

## 4. Métricas

|                                                   | Fase 06 | Fase 07                           |
|---------------------------------------------------|---------|-----------------------------------|
| Pruebas backend / fallos                          | 227 / 0 | **238 / 0**                       |
| Pruebas frontend / fallos                         | 26 / 0  | **29 / 0**                        |
| Criterios de traducción de errores                | 2       | **1**                             |
| Rutas que devuelven modelos de dominio            | 3       | **1** (`/api/agent/card`, ver §6) |
| Adaptadores que serializan el dominio con Jackson | 3       | **2** (contrato `a2a`, ver §6)    |
| Módulos con nombre que contradice su contenido    | 1       | **0**                             |
| Cobertura `domain/model`                          | 94,8 %  | **94,8 %**                        |
| Cobertura `domain/usecase`                        | 98,9 %  | **98,9 %**                        |
| ArchUnit *Rule_2.7* / *Rule_2.2*                  | 3 / 0   | **3 / 0**                         |
| `.block()` / `.subscribe()` en `src/main`         | 0       | **0**                             |

---

## 5. Comandos

```powershell
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-backend
.\gradlew test --console=plain                       # BUILD SUCCESSFUL
.\gradlew jacocoMergedReport --no-configuration-cache # cobertura

cd ..\azure-devops-frontend
pnpm test --watch=false                              # 29 passed
pnpm exec tsc -p tsconfig.app.json --noEmit          # sin errores de tipos
```

---

## 6. Desviaciones respecto al plan

1. **Se modificó `ArchitectureTest`**, marcado como *«Please do not modify this file»*. Era
   inevitable: renombrar el módulo sin actualizar su ruta habría dejado `issues.json` escribiéndose
   en un directorio inexistente. **Se cambió una sola cadena**, autorizado en DP-05a.
2. **Se modificaron dos pruebas de caracterización** (S-5, §7 de sus javadoc), una en
   `DashboardRoutesCharacterizationTest` y otra en `PlanningRoutesCharacterizationTest`. Ambas
   fijaban el 400 indiscriminado que D-16 existe para eliminar. Ninguna otra aserción se tocó.
3. **`GET /api/agent/card` sigue devolviendo el modelo de dominio `AgentCard`,** y
   `PostgresTaskStoreAdapter` y `agent-client` siguen serializando el modelo `a2a` con Jackson. Es
   la misma clase de fallo que D-14 y D-15, pero **el modelo `a2a` es el contrato externo del
   protocolo A2A**: ponerle una frontera es exactamente D-29/D-34, que **B-10 dejó fuera de esta
   fase**. Aquí hay dos instrucciones que se rozan —el criterio de cierre «cero dominio por HTTP» y
   B-10— y se resuelve a favor de B-10, **por escrito y no en silencio**: queda como D-45 y D-44.
4. **T-06 (D-42) no se ejecutó.** Convertir `DashboardStreamOrchestrator` y `DashboardTaskTracker`
   en beans obliga a añadirlos al `@ContextConfiguration` de **dos** pruebas de caracterización, y
   B-09 solo autorizaba tocar las afectadas **por D-16**. No se forzó la interpretación. D-42 sigue
   viva y es material de la Fase 08.

---

## 7. Hallazgos

- **H-1.** Las dos pruebas que D-16 rompió son exactamente las dos que la Fase 01 había marcado en
  su javadoc como «comportamiento cuestionable, se congela hasta DP-06». **La caracterización
  predijo su propia caducidad**: cuando una prueba documenta por qué el comportamiento que fija está
  mal, romperla más tarde no es un accidente, es el plan cumpliéndose.
- **H-2.** El mapa de errores «nuevo» ya existía: lo escribió la Fase 03 dentro de `TaskHandler` y
  llevaba desde entonces con un comentario que decía «el mapa completo se decide en DP-06». La Fase
  07 no inventó un criterio, solo **dejó de tener dos**.
- **H-3.** El paquete Java del módulo renombrado (`co.com.bancolombia.taskstore`) nunca dijo
  «inmemory». La mentira estaba **solo en el nombre del módulo Gradle**, que es precisamente lo que
  un IDE no ayuda a encontrar.
- **H-4.** D-14 se cerró sin tocar la base de datos porque la entidad se diseñó para **reproducir
  byte a byte** el JSON que ya había. La prueba que lo fija vale más que el mapper: sin ella, el
  siguiente que renombre un campo de la entidad corrompe todo lo almacenado y no se entera.
- **H-5.** El frontend tenía el `error:` del `subscribe` escrito y correcto desde antes; lo que
  faltaba era **que alguien lo llamara**. D-40 se cerró con doce líneas en un servicio.

---

## 8. Deudas nuevas

| #        | Deuda                                                                                                                                                                                                                                                                                                                                                                             | Dónde                  |
|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| **D-43** | `GET /api/planning/initiatives` devuelve el `Map<String,Object>` crudo que salen de las columnas JSONB (`initiative_id`, `initiative_title`, `cell`): **el esquema de persistencia se publica tal cual por HTTP**. No es un modelo de dominio, así que no entraba en D-15, pero es la misma fuga por otro camino. No se tocó por no conocer todos los consumidores de esas claves | `azure-devops-backend` |
| **D-44** | `PostgresTaskStoreAdapter` y el cliente del agente **serializan el modelo `a2a` con Jackson dentro del adaptador**, igual que hacía `PgVectorPlanningAdapter` antes de D-14. Atada al contrato A2A → va con D-29/D-34                                                                                                                                                             | `azure-devops-backend` |
| **D-45** | `GET /api/agent/card` devuelve `AgentCard`, un modelo de dominio, sin DTO intermedio. Misma causa y mismo destino que D-44                                                                                                                                                                                                                                                        | `azure-devops-backend` |

**Siguen vivas:** D-41 (reportes en disco), **D-42** (colaboradores del stream con `new`).

---

## 9. Estado del control de versiones

**Nada se ha confirmado.** El árbol de trabajo sigue acumulando las fases 02 a 07, según la decisión
del usuario de arreglar primero los issues de SonarQube y confirmar después, por fases.

El renombrado de T-04 se hizo con `git mv`, de modo que git lo registra como movimiento y **el
historial del adaptador se conserva** cuando se confirme.

---

## 10. Checklist de cierre

- [x] `gradlew test` verde, ≥ 227 pruebas, 0 fallos → **238 / 0**
- [x] Un solo criterio de traducción de errores; cero `onErrorResume` que devuelvan 400
  indiscriminadamente
- [x] Cero modelos de dominio devueltos como respuesta HTTP *(salvo `a2a`, §6.3)*
- [x] Cero serialización del dominio con Jackson dentro de adaptadores *(salvo `a2a`, §6.3)*
- [x] Ningún módulo con nombre que contradiga su contenido
- [x] Cobertura `domain/usecase` ≥ 90 % (98,9 %) y `domain/model` no baja de 94,8 %
- [x] ArchUnit: *Rule_2.2* en 0 y *Rule_2.7* no sube de 3
- [x] Frontend en verde y sin `any` en el código tocado
- [x] `RESULTADO-FASE-07.md` con la estructura habitual
- [x] `plan-maestro.md` y `fase-08.md` actualizados

