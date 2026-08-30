# CIERRE DEL PLAN — Refactorización del BFF `azure-devops-backend`

> **Estado del plan:** 🟢 **CERRADO** · **Fecha:** 2026-08-29
> **Plan:** [`plan-maestro.md`](../plan/plan-maestro.md) v2.0 · **Fases ejecutadas:** 8 de 8
> **Última fase:** [`RESULTADO-FASE-08.md`](RESULTADO-FASE-08.md)
> **Alcance:** este documento cierra el plan del BFF. **No cierra las deudas de otros
repositorios.**
> **Deudas:** 48 registradas · **33 saldadas** · **15 vivas** (detalle y dueño en §3).

---

## 1. De dónde se salió y a dónde se llegó

|                                        | Al abrir el plan                       | Al cerrarlo                                 |
|----------------------------------------|----------------------------------------|---------------------------------------------|
| Build backend                          | 🔴 **rojo**: 15 de 41 pruebas fallando | 🟢 **238 pruebas, 0 fallos**                |
| Build frontend                         | sin cobertura del camino de error      | 🟢 **29 pruebas, 0 fallos**                 |
| Cobertura `domain/model`               | ~0 %                                   | **94,8 %**                                  |
| Cobertura `domain/usecase`             | ~0 % en 3 de 5 casos de uso            | **98,9 %**                                  |
| ArchUnit *Rule_2.2*                    | 3                                      | **0**                                       |
| ArchUnit *Rule_2.7*                    | 6                                      | **0**                                       |
| Clase más larga (productiva)           | `AgentChatUseCase`, **537 líneas**     | `A2APayloadMapper`, **351**                 |
| `Handler`                              | **382 líneas**, 5 responsabilidades    | **177**, una                                |
| Criterios de traducción de errores     | 2, en el mismo entry-point             | **1**                                       |
| Fachadas de logging                    | 3, mezcladas por capa                  | **2**, una por capa y justificadas          |
| Módulos que mienten sobre su contenido | 2                                      | **0**                                       |
| Deudas registradas                     | 45                                     | **45 registradas · 33 saldadas · 12 vivas** |

---

## 2. Recorrido de las ocho fases

|   Fase | Qué resolvió                                                                                                                                                                                                                                                                  | Deudas saldadas                                    |    Pruebas |
|-------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|-----------:|
| **01** | **La red de seguridad.** `UseCasesConfigTest` capturaba `UnsatisfiedDependencyException` y daba verde: el plan empezó arreglando el instrumento de medida, porque sin él ninguna fase posterior habría podido demostrar nada                                                  | D-17                                               |          — |
| **02** | **Build a verde y el cerebro fuera del BFF.** Se eliminó `AgentChatUseCase` (537 líneas replicando al agente), se abandonó el endpoint `/message:send` ya vencido por JSON-RPC, y `sendMessage` dejó de tirar a la basura la `Task` que devolvía el agente                    | D-01, D-02, D-22, D-23                             | 41 → verde |
| **03** | **El BFF ocupa por fin su sitio.** El front dejó de ir directo al agente; aparecieron `tasks/get`, `tasks/cancel` y la *agent card*; se cortó la integración por base de datos compartida y se empezó a validar la entrada, que era literalmente su misión                    | D-21, D-24, D-25, D-26, D-28, D-30, D-33           |          — |
| **04** | **El dominio deja de ser un almacén de prompts.** 184 de 430 líneas de `DevOpsDashboardUseCase` eran tres prompts y un JSON simulado: ni un prompt ni un mock son lógica de negocio                                                                                           | D-08                                               |          — |
| **05** | **Inmutabilidad y reglas en su sitio.** Se acabó la mutación in-place dentro de la cadena reactiva y el `sharedData[1]` compartido; la partición en lotes y el recálculo de métricas bajaron del entry-point al dominio                                                       | D-06, D-07, D-09, D-13, D-19                       |          — |
| **06** | **El fallo se cuenta.** Un agente caído devolvía **500 mudo** en mitad de un SSE; pasó a ser un evento `ERROR`. El `Handler` bajó de 382 a 192 líneas y el repliegue a mock pasó de estar en 4 sitios a uno                                                                   | D-05, D-10, D-11, D-31                             |    **227** |
| **07** | **Fronteras.** Un solo criterio de traducción de errores en todo el entry-point; el dominio deja de viajar a la base de datos y por HTTP; `task-store-inmemory` pasa a llamarse como lo que es; el frontend por fin escucha el evento `ERROR`                                 | D-14, D-15, D-16, D-20, D-40                       |    **238** |
| **08** | **Jubilar andamios.** Se fue el módulo `mcp-client` —el BFF no habla MCP con nadie—, se retiró la configuración que no leía nadie, quedó una fachada de logging por capa y los dos colaboradores que se construían con `new` para no despeinar una prueba pasaron a ser beans | D-04, D-12, D-32, D-42 (+ D-27 y D-03 verificadas) |    **238** |

### El hilo que las une

Las ocho fases atacan **la misma familia de fallo, en capas distintas**: *información que se pierde
por el camino*.

- La Fase 01 la encontró en las **pruebas** (un error capturado que se daba por éxito).
- La 02, en el **contrato con el agente** (una `Task` descartada).
- La 06, en el **stream** (un fallo que llegaba como 500 mudo).
- La 07, en el **traductor de errores** (todo error contado como «petición mal formada»).
- La 08, en la **configuración** (una bandera de MCP decidiendo, sin que se viera, si el usuario ve
  un tablero real o uno simulado).

Y las tres últimas fases confirmaron algo incómodo: **el plan maestro se equivocaba en varios puntos
sobre el propio código**. D-27 y D-03 estaban saldadas sin marcar; D-04 describía residuos que ya no
existían; D-32 daba por vacío un módulo que sostenía el classpath de otros dos. Ese es el argumento
más fuerte a favor de verificar en el código antes de planificar sobre un documento.

---

## 3. Estado final de las 45 deudas

### 3.1 Saldadas — 33

| Deuda                                      | Cerrada en                            | Deuda                                     | Cerrada en                            |
|--------------------------------------------|---------------------------------------|-------------------------------------------|---------------------------------------|
| D-01 Build rojo                            | Fase 02                               | D-19 Números mágicos                      | Fase 05                               |
| D-02 `AgentChatUseCase` replica el agente  | Fase 02                               | D-20 Módulo `task-store-inmemory` mentía  | Fase 07                               |
| **D-03 DTOs JSON-RPC sin uso**             | **ya saldada; verificada en Fase 08** | D-21 El front va directo al agente        | Fase 03                               |
| **D-04 Residuos en `application.yaml`**    | **Fase 08**                           | D-22 Endpoint legacy vencido              | Fase 02                               |
| D-05 `Handler` con 5 responsabilidades     | Fase 06                               | D-23 Se descarta la `Task`                | Fase 02                               |
| D-06 Mutación in-place y estado compartido | Fase 05                               | D-24 Integración por BD compartida        | Fase 03                               |
| D-07 Reglas de negocio en el entry-point   | Fase 05                               | D-25 Faltan `tasks/get` y `tasks/cancel`  | Fase 03                               |
| D-08 Prompts y mock en el dominio          | Fase 04                               | D-26 Nadie sirve la *agent card*          | Fase 03                               |
| D-09 Resolución de rutas duplicada         | Fase 05                               | **D-27 `agent.url` ausente**              | **ya saldada; verificada en Fase 08** |
| D-10 I/O de ficheros en el caso de uso     | Fase 06                               | D-28 El BFF no valida nada                | Fase 03                               |
| D-11 Fallback a mock en 4 puntos           | Fase 06                               | D-30 No se propaga `contextId`            | Fase 03                               |
| **D-12 Tres fachadas de logging**          | **Fase 08**                           | D-31 Fallo del SSE como 500 mudo          | Fase 06                               |
| D-13 Dominio anémico y mutable             | Fase 05                               | **D-32 Módulo `mcp-client` casi vacío**   | **Fase 08**                           |
| D-14 Dominio serializado en el adaptador   | Fase 07                               | D-33 Gateway y adaptador sin consumidor   | Fase 03                               |
| D-15 Dominio devuelto por HTTP             | Fase 07                               | D-40 El frontend ignora el evento `ERROR` | Fase 07                               |
| D-16 Todo error traducido a 400            | Fase 07                               | **D-42 Colaboradores con `new`**          | **Fase 08**                           |
| D-17 La prueba de cableado mentía          | Fase 01                               |                                           |                                       |

### 3.2 Vivas — 12, con dueño

| #        | Deuda                                                                                                          | Por qué sigue viva                                                                                                        | **Dueño / destino**                                  |
|----------|----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| **D-18** | Cobertura ~0 % en `Handler`, adaptadores y parte de los casos de uso                                           | Nunca fue objetivo de ninguna fase. El dominio sí quedó cubierto (94,8 % / 98,9 %); lo que falta es infraestructura       | **Equipo BFF** · trabajo continuo, sin plan propio   |
| **D-29** | Modelo `a2a` duplicado íntegro entre BFF y agente, sin contrato compartido ni versión                          | Toca **tres repositorios**. Excluida por B-10                                                                             | **Plan conjunto BFF + agente**                       |
| **D-34** | El agente no declara el contrato de respuesta de `GET /api/tasks`                                              | Es la otra mitad de D-29                                                                                                  | **Plan conjunto BFF + agente**                       |
| **D-35** | Los prompts deben acabar en el MCP y en el agente, no en `resources/` del BFF                                  | La Fase 04 hizo el primer tiempo. El segundo exige D-29 resuelta y levantar la restricción §10 del plan                   | **Plan propio**, posterior a D-29                    |
| **D-36** | `RestConsumerTest` del MCP no cubre `getTeamFieldValues` ni `getTeamIterations`                                | Otro repositorio                                                                                                          | **`azure-devops-mcp`**                               |
| **D-37** | El repliegue del MCP produce una ruta con el año del calendario                                                | Otro repositorio; además exige **medir**                                                                                  | **`azure-devops-mcp`**                               |
| **D-38** | El `10` del lote y el `2` de concurrencia siguen sin respaldo empírico                                         | Requiere entorno con datos y carga real. La Fase 05 los hizo configurables, que es lo máximo que se podía hacer sin medir | **Equipo BFF** · requiere entorno                    |
| **D-39** | El MCP arrastra una violación preexistente de *Rule_2.2*                                                       | Otro repositorio                                                                                                          | **`azure-devops-mcp`**                               |
| **D-41** | Los reportes `.md` viven sólo en disco: con varias réplicas se pierden al reiniciar                            | Consecuencia consciente de DP-04. Llevarlos a S3 exige confirmar acceso al bucket                                         | **Equipo BFF** · requiere acceso a S3                |
| **D-43** | `GET /api/planning/initiatives` publica el `Map<String,Object>` crudo del JSONB                                | No se conocen todos los consumidores de esas claves; cambiarlo a ciegas rompe al frontend                                 | **Equipo BFF** · requiere inventario de consumidores |
| **D-44** | `PostgresTaskStoreAdapter` y el cliente del agente serializan el modelo `a2a` con Jackson dentro del adaptador | Misma causa que D-14, pero sobre el contrato externo A2A: va con D-29/D-34                                                | **Plan conjunto BFF + agente**                       |
| **D-45** | `GET /api/agent/card` devuelve `AgentCard`, un modelo de dominio, sin DTO                                      | Igual que D-44                                                                                                            | **Plan conjunto BFF + agente**                       |

### 3.3 Nuevas, detectadas al cerrar — 3

| #        | Deuda                                                                                                                                                                                                                                              | **Dueño**                                                  |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| **D-46** | `spring.ai.openai.embedding.model` en el `yaml` frente a `spring.ai.openai.embedding.options.model` en el código: la clave declarada podría no alimentar nada, y el BFF estaría embebiendo con el defecto del código sin que nadie lo haya elegido | **Equipo BFF** · confirmar contra Spring AI 2.0.0          |
| **D-47** | `spring-ai-starter-model-openai` probablemente sobra en `pgvector-store`, que publica su propio `EmbeddingModel` `@Primary`. El **starter** sobra; el **`EmbeddingModel` no**                                                                      | **Equipo BFF** · verificar arrancando contra pgvector real |
| **D-48** | **Agente y BFF escriben vectores en la misma tabla `planning_chunks`, cada uno con su modelo de embeddings.** Si no coinciden, la búsqueda por similitud **no falla: devuelve resultados malos en silencio**                                       | **Plan propio** · decisión del usuario: se diseñará aparte |

#### D-48 en detalle — la única deuda nueva que no es cosmética

Al cerrar el plan se comprobó que **el BFF no se limita a leer planificaciones ya vectorizadas por
el agente**. `POST /api/planning/ingest` —que el frontend llama en
`devops-agent-api.service.ts:33`— termina en `vectorStore.add(documents)`
(`PgVectorPlanningAdapter:76`), y la búsqueda en `similaritySearch(...)` (línea 110). **Las dos
operaciones calculan embeddings.** Luego hay dos servicios escribiendo en el mismo espacio
vectorial, cada uno con su propia configuración de modelo, y nada que garantice que es el mismo.

Es **la misma clase de fallo que D-24** —integración por base de datos compartida—, que el plan
cortó para la tabla `agent_tasks` en la Fase 03 y que **sigue viva para el almacén vectorial**. Y es
peor en un aspecto: `agent_tasks` fallaba de forma visible, mientras que mezclar vectores de dos
modelos distintos **no lanza ningún error**. La consulta devuelve filas, sólo que las equivocadas.

D-46 lo hace plausible por accidente: si la clave que se declara en el `yaml` no es la que lee el
código, el modelo del BFF no lo eligió nadie.

**Decisión del usuario al cerrar el plan: no se toca en la Fase 08.** Se abordará con una solución
de diseño propia, no con un parche de configuración.

---

## 4. Qué queda vivo, resumido

De las 15 deudas vivas, **ninguna es un fallo de diseño del flujo del BFF**, pero **una sí lo es de
la integración** (D-48):

- **5 dependen de un contrato compartido con el agente** (D-29, D-34, D-35, D-44, D-45). Son una
  sola cosa vista desde cinco sitios: *el modelo `a2a` está duplicado y sin versionar*. Mientras eso
  siga así, poner DTOs en el BFF sólo mueve el problema.
- **1 es integración por base de datos compartida, en su segunda encarnación** (D-48): el plan la
  cortó para `agent_tasks` y sigue viva para el almacén vectorial. **Es la que puede hacer daño en
  silencio.**
- **3 viven en otro repositorio** (D-36, D-37, D-39) → `azure-devops-mcp`.
- **3 necesitan algo que no es código** (D-38 datos de carga, D-41 acceso a S3, D-43 inventario de
  consumidores).
- **2 son verificaciones de configuración** (D-46, D-47).
- **1 es trabajo continuo** (D-18, cobertura de infraestructura).

**La recomendación de cierre es abordar D-29/D-34 primero**, porque desbloquea cinco deudas de una
vez y es la única que hoy obliga a mantener excepciones escritas —D-44 y D-45 nacieron de resolver a
favor de B-10 una contradicción con el criterio de cierre de la Fase 07—. **D-48 va inmediatamente
después, o antes si la planificación vectorizada se usa en serio**: es la única deuda viva que
degrada un resultado sin dejar rastro en ningún log.

---

## 5. Lo que este plan deja escrito además del código

- **8 documentos de fase** (`docs/fases/`) y **8 de resultado** (`docs/resultados/`), cada uno con
  sus desviaciones declaradas.
- **13 decisiones de negocio registradas** (B-01 … B-13) y las decisiones de plan (DP-04, DP-05,
  DP-06, DP-08) que fijaron criterios técnicos.
- **6 excepciones a las pruebas de caracterización** (S-1 … S-6), cada una con qué se cambió, por
  qué era inevitable y qué **no** se tocó. Ninguna prueba de caracterización se modificó sin dejar
  constancia.
- **Cuatro handoffs** para arrancar fases en sesión nueva.

Ese rastro es lo que permitió que la Fase 08 descubriera que el plan maestro se equivocaba sobre su
propio código sin tener que volver a investigarlo todo.

---

## 6. Estado de git al cerrar

**El plan se cierra sin ningún commit.** Hay trabajo sin confirmar desde la Fase 02, ahora con ocho
fases encima. Cuando se confirme, debe hacerse con el formato de `COMMIT_RULES.md`:
`COMMIT_TYPE(SCOPE): DESCRIPTION`.

