# PROMPT DE ARRANQUE — FASE 06

> **Cómo se usa.** Copia todo lo que hay debajo de la línea en una **sesión nueva**. Es
> autocontenido a propósito (`plan-maestro.md` §8.7): no depende del historial de ninguna sesión
> anterior.

---

### Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 06** de un
plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. Las Fases 01, 02, 03, 04
y 05 están **completadas y commiteadas**; tú continúas desde ahí.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confirma en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia).
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-06.md` — **tu fase**: paso bloqueante, contexto, instrucciones
   y checklist.
4. `azure-devops-mcp/docs/fases/fase-05.md` §4 — cifras medidas al cerrar la fase anterior y sus
   **cinco desviaciones documentadas**.
5. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` §2 y §3 — qué es contrato público y qué no.
6. `azure-devops-mcp/docs/fases/fase-05.md` §4.1 — el reparto de adaptadores y mappers que **no
   debes duplicar**.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **148 pruebas, 0 fallos**.
- Cobertura *(líneas, JaCoCo)*: `domain/model` **94,7 %** · `domain/usecase` **97,8 %** ·
  `mcp-server` **80,7 %** · `rest-consumer` **88,4 %** · `app-service` **58,2 %**.
- **Tres adaptadores**, ya no uno: `WorkItemQueryAdapter` **115** líneas ·
  `WorkItemCommandAdapter` **80** · `TeamScopeAdapter` **78**. Todos **≤ 120**.
- **Tres puertos** de dominio: `WorkItemQueryPort`, `WorkItemCommandPort`
  (`model/workitem/gateways/`) y `TeamScopePort` (`model/team/gateways/`), más
  `TeamScopeFallbackMetrics`. **0 puertos huérfanos.** `domain/model` tiene **2 paquetes**:
  `workitem/` y `team/`.
- `AzureDevOpsTools` **157 líneas**, **sin ninguna regla de negocio**.
- **Excepciones de dominio: 0.** Todo error técnico llega **crudo** al cliente MCP.
- **Cortacircuitos: 3** (`workItemQuery`, `workItemCommand`, `teamScope`), **ninguno declarado** en
  `application.yaml`, que sigue declarando `testGet` y `testPost` del scaffold.
- **Timeouts:** solo los de Netty, **5 s compartidos** por todas las llamadas, lote incluido.
- La sentencia WIQL sigue **congelada carácter a carácter** (8 ramas), en `domain/usecase`.
- El cuerpo HTTP de salida sigue **congelado carácter a carácter** (4 cuerpos).
- ArchUnit `Rule_2.2`: ✅ **0 violaciones reales**. **D-25 sigue viva**: el `issues.json` sale vacío,
  así que **la única verificación fiable es el log del test**.
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit. Módulos: `:app-service`, `:model`, `:usecase`, `:mcp-server`,
  `:rest-consumer`.

### ⛔ Tu primer paso es BLOQUEANTE: DP-06

**No escribas código hasta que el propietario responda.** Pregunta, en concreto:

1. **¿Qué debe ver el cliente MCP ante un fallo de Azure DevOps?** (a) forma exacta del error
   —mensaje, código estable, ambos—; (b) ¿se propaga el cuerpo original de Azure DevOps, se resume
   o se oculta?; (c) ¿se distingue 404 de 401 de 5xx, y con cuántas excepciones de dominio?;
   (d) ¿un fallo rompe la tool o devuelve resultado vacío? **Ojo: DP-04 ya decidió conservar el
   repliegue de rutas para NO convertir un tablero vacío en un error. No lo contradigas sin que el
   propietario lo diga.**
2. **Valores de resiliencia:** `failureRateThreshold`, `slidingWindowSize` y
   `waitDurationInOpenState` para las **tres** instancias, y **timeout por operación** (D-24),
   especialmente el de lote. ¿Se retiran `testGet` y `testPost` del YAML?
3. **B-08:** ¿dónde se traduce el error: en el adaptador, en el entry-point (`McpErrorTranslator`),
   o en ambos con responsabilidades distintas?
4. **B-09:** D-18, ¿las versiones de API pasan a `@ConfigurationProperties` o a un objeto de valor
   de dominio?
5. **B-10:** las **dos** consultas de ámbito de equipo llevan `api-version=7.0` **literal en la
   ruta**. ¿Se parametrizan —lo que **cambia una llamada HTTP**— o se dejan?

Si no hay respuesta: **detente**, documenta el bloqueo en `fase-06.md` §4, deja la fase abierta y
pasa a la Fase 07 (no depende técnicamente de ésta). Es el §8.5 del plan maestro.

### Decisiones YA RESUELTAS — NO las replantees ni las contradigas

- **DP-01 — La seguridad se queda laxa y ya es configuración.** `mcp.security.mode` (`PERMISSIVE`
  por defecto / `ENFORCED`), leído de `MCP_SECURITY_MODE`. **No lo toques.**
- **DP-02 / B-01 — El token no cambia de formato.** `adapter.restconsumer.token` recibe
  `Base64(":" + PAT)` ya calculado. **Ningún token real en el repositorio.**
- **B-03 — Los nombres de rol son literales** (`MCP.AZURE_DEVOPS.READ` / `.WRITE`) y viven en
  `mcp-server/.../mcp/security/McpRoles.java`.
- **DP-03 — La frontera de contrato existe y no se deshace.** 1 mapper de entrada
  (`McpToolDtoMapper`) y 5 de salida (`consumer/mapper/`). **Ningún modelo de dominio puede volver a
  exponerse como `@McpToolParam` ni ser serializado por el `WebClient`.** El tipo de dominio se llama
  **`WorkItemBatchCriteria`**.
- **DP-04 — El repliegue se conserva y se mide; las reglas de tipos son dominio.** El repliegue por
  concatenación **sigue vivo, año del calendario incluido**, contado por
  `azuredevops.teamscope.fallback` con `WARN`. Los tipos por defecto y la traducción
  `User Story → Historia de Usuario` son **regla de dominio** (`WorkItemTypes`). El caso de uso
  recibe un **objeto comando** `ListWorkItemsCommand`.
- **DP-05 — Los adaptadores y los puertos ya están segregados, y así se quedan.** 3 adaptadores,
  3 puertos, 2 paquetes de dominio. **B-05: los tres nombres de cortacircuito son DEFINITIVOS**
  (`workItemQuery`, `workItemCommand`, `teamScope`) — se renombraron **para que esta fase los
  configure**, no para que los vuelva a cambiar. **B-06:** el puerto huérfano se borró. **B-07:** los
  tres adaptadores **comparten el `WebClient`** de `RestConsumerConfig` y el helper
  `consumer/ApiVersions`, que centraliza el ternario de versión **con los literales originales**.
- **Los nombres de campo del cable son intocables** (`CONTRATO-MCP.md` §3): `op`, `path`, `value`,
  `from`, `ids`, `fields`, `expand`, `errorPolicy`. Hay una prueba por reflexión que lo verifica.

### Restricción técnica dura, descubierta en la Fase 04

**El módulo `domain/usecase` NO admite ninguna dependencia más allá de `:model`.** La tarea
`validateStructure` del plugin de Clean Architecture de Bancolombia falla con *«Use case module is
invalid»*. Se comprobó añadiendo `slf4j-api`. **No lo intentes de nuevo:** si necesitas registrar
algo desde el dominio, hazlo a través de un **puerto** que transporte la información al adaptador,
como hizo `TeamScopeFallbackMetrics`.

### El objetivo de la fase, en una frase

**Que ningún error técnico de Azure DevOps llegue crudo al cliente MCP, y que ningún parámetro de
resiliencia siga siendo un fantasma.**

Hoy no hay **ni una sola excepción de dominio** en el repositorio: un `401`, un `404` o un
`TF401232` viajan tal cual como `WebClientResponseException`, una excepción de Spring. Y los **tres**
cortacircuitos corren con la configuración **por defecto** de Resilience4j porque **nadie los ha
declarado nunca** en el YAML.

### Reglas innegociables

1. **Esta es la primera fase del plan que SÍ cambia comportamiento observable.** Por eso DP-06 es
   bloqueante: **la forma del error es contrato público** desde el instante en que se traduce el
   primero. Nada se decide por cuenta propia.
2. **Ni una URL, ni un parámetro de consulta, ni un cuerpo JSON, ni una cabecera cambian**, salvo lo
   que **B-10** autorice expresamente. `OutboundPayloadCharacterizationTest` y los tres
   `*AdapterTest` son la red de seguridad.
3. **Cero cambios en el contrato MCP público**: ni nombres de tool, ni de parámetro, ni de campo, ni
   la forma del resultado **de éxito**.
4. **Las 148 pruebas deben seguir pasando.** Las **dos** pruebas de caracterización de D-13 en
   `TeamScopeAdapterTest` están escritas para cambiar de tipo esperado en esta fase — pero **solo si
   DP-06 lo dice**. Cualquier otra prueba heredada que necesites tocar: **pregunta primero**. Las
   Fases 03, 04 y 05 obtuvieron autorizaciones **puntuales**; no son un permiso general.
5. **La traducción de errores no se duplica.** Hay **tres** adaptadores: si el traductor acaba
   copiado tres veces, la fase ha fallado. Mismo criterio con el que la Fase 05 repartió los mappers.
6. **No toques** la seguridad de la Fase 02, la frontera de la Fase 03, el flujo de la Fase 04 ni el
   reparto de la Fase 05.
7. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres, contratos o
   reglas de negocio, **detente y pregunta**. Las Fases 03, 04 y 05 se detuvieron y las cuatro veces
   fue correcto.
8. **Fuera de alcance:** retirar los `@Setter` del modelo preexistente y el dominio rico (D-16 →
   Fase 07); D-05, D-19, D-20, D-23, D-25, D-27 → Fase 08.
9. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`.
10. **Un commit** al cerrar:
    `refactor(error_handling): traducir los fallos de azure devops a excepciones de dominio`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-06.md`, en orden y
sin adelantar pasos, empezando por el **paso 0 bloqueante**. Marca cada casilla conforme la
completes.

Comandos de verificación (PowerShell, Windows):

```
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon
```

### Definición de Hecho

- [ ] DP-06 respondida por el propietario **o** bloqueo documentado y fase dejada abierta.
- [ ] `.\gradlew.bat build` en **verde**, con **≥ 148** pruebas y **0** fallos.
- [ ] **≥ 3** excepciones de dominio, en `domain/model`, **sin Spring, sin HTTP, sin Jackson**.
- [ ] **0** errores técnicos que lleguen crudos al cliente MCP.
- [ ] **1** solo traductor de errores, **compartido** por los tres adaptadores. **0 duplicados.**
- [ ] **3 de 3** cortacircuitos con configuración declarada y elegida.
- [ ] **Timeout por operación**, incluido el de lote. **0** timeouts compartidos sin elegir.
- [ ] **0** URLs, cuerpos, parámetros de consulta o cabeceras alterados **salvo lo que B-10 autorice**.
- [ ] Cobertura de `rest-consumer` **≥ 88,4 %** (no debe bajar).
- [ ] ArchUnit a **0** violaciones reales, verificado **en el log**.
- [ ] **0** modelos de dominio expuestos como `@McpToolParam` o serializados por el `WebClient`.
- [ ] `CONTRATO-MCP.md` con una **sección nueva sobre el contrato de errores**.
- [ ] Plan maestro actualizado: §3, §6, §7, §9 y cabecera.
- [ ] Bloque **Resultado** de `fase-06.md` relleno con lo alcanzado **de verdad**.
- [ ] **`docs/fases/fase-07.md` y `docs/fases/PROMPT-FASE-07.md` generados**.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.

