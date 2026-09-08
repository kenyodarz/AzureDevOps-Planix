# PROMPT DE ARRANQUE — FASE 08

## Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 08**, la
**última**, de un plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. Las
Fases 01 a **07** están **completadas y commiteadas**; tú continúas desde ahí y **cierras el plan**.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confirma en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia). §6 es la
   **Política de No-Asunción**, que ha gobernado las siete fases anteriores.
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-08.md` — **tu fase**: paso bloqueante, contexto, instrucciones
   y checklist.
4. `azure-devops-mcp/docs/fases/fase-07.md` §4 — cifras medidas al cerrar la fase anterior, sus
   **cuatro desviaciones** y **dos correcciones de cifras del plan**.
5. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` **§1**, **§2.6**, **§4** y **§6** — §1 son las
   capacidades anunciadas, §2.6 es `HealthTool`, §4 es `queryByWiql` *(las tres cosas que esta fase
   decide)* y §6 es el contrato de errores, **que no se toca**.
6. `azure-devops-mcp/docs/fases/fase-07.md` §4.1 y §4.4 — qué construyó la Fase 07, **qué no debes
   deshacer**, y el hallazgo sobre la red de seguridad autorreferencial de la Fase 03.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **205 pruebas, 0 fallos**.
- Cobertura *(líneas, JaCoCo)*: `domain/model` **96,2 %** · `domain/usecase` **97,8 %** ·
  `mcp-server` **84,3 %** · `rest-consumer` **92,3 %** · `app-service` **58,2 %** ← **el único por
  debajo de su objetivo (60 %)**.
- Mutaciones (Pitest): **98 %** `domain/model` · **97 %** `domain/usecase` · **84 %**
  `rest-consumer`.
- **`domain/model` es INMUTABLE:** **0 clases** con `@Setter`/`@Data`; **15 objetos de valor**
  (`record`) + **4 tipos de excepción**. **4 paquetes**: `workitem/`, `team/`, `exception/`,
  `common/`.
- **Las tres fronteras de contrato están cerradas:** entrada MCP (1 mapper), salida hacia Azure
  DevOps (5 mappers) y **salida hacia el cliente MCP** (4 DTOs de respuesta + `McpResponseMapper`,
  Fase 07). **Ningún modelo de dominio cruza ninguna de las tres.**
- **Tres adaptadores**: `WorkItemQueryAdapter` **143** · `WorkItemCommandAdapter` **106** ·
  `TeamScopeAdapter` **112**. Cada uno con **dos constructores** (el de Spring con `@Autowired` y
  uno
  de conveniencia **que existe para que las pruebas heredadas no se toquen**).
- **Tres puertos** de dominio + 1 de observabilidad. **0 puertos huérfanos.**
- **Excepciones de dominio: 3** (+1 base) con códigos **`AZDO_NOT_FOUND`**, **`AZDO_UNAUTHORIZED`**,
  **`AZDO_UNAVAILABLE`**. **Errores técnicos crudos hacia el cliente: 0.**
- **Cortacircuitos: 3**, los tres configurados (50 % / 10 / 10 s). **Timeouts: 4 por operación**
  (10 s · **30 s lote** · 10 s · 5 s), más los de Netty de 5 s **intactos**.
- `AzureDevOpsTools` **188 líneas**, **sin ninguna regla de negocio** *(B-12 lo aceptó
  formalmente)*.
- **Tres redes de seguridad congeladas:** la sentencia WIQL **carácter a carácter** (8 ramas), el
  cuerpo HTTP saliente (4 cuerpos) y **el JSON de respuesta hacia MCP contra literales** (4 formas).
- ArchUnit `Rule_2.2`: ✅ **0 violaciones reales**. **D-25 sigue viva**: el `issues.json` sale vacío,
  así que **la única verificación fiable es el log del test**. ArchUnit corre como **`warning`**.
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit · **Jackson 3 (`tools.jackson`)**. Módulos: `:app-service`,
  `:model`, `:usecase`, `:mcp-server`, `:rest-consumer`.

### ⚠️ Tu primer paso es BLOQUEANTE: DP-08

**No escribas código hasta que el propietario responda.** §5 del plan no le asignaba decisión a esta
fase — igual que no se la asignó a la Fase 07, **y apareció una**. Aquí están identificadas de
antemano porque **tres de las siete deudas son decisiones de producto, no de refactor**:

1. **(a) D-19 — `queryByWiql`:** su `@McpTool` lleva **comentado** desde siempre, pero el método es
   `public`, tiene `@PreAuthorize` activo y caso de uso, adaptador y pruebas que funcionan. **¿Se
   expone o se retira?** Exponerla añade al contrato una tool que acepta **WIQL crudo del
   cliente**, lo contrario de la misión de §1 del plan; retirarla borra código probado.
2. **(b) D-20 — `HealthTool`:** duplica el actuator y devuelve
   `"Server:  v1.0.0 - Package: co.com.bancolombia"`, con versión **hardcodeada** y un **placeholder
   sin rellenar**. `checkHealth` y `getServerInfo` **son contrato público** (§2.6). **¿Se retira, se
   arregla o se deja?**
3. **(c) Capacidades:** el servidor anuncia `resource: true` y `prompt: true` con **cero**
   implementaciones. Ponerlas a `false` es lo honesto, **pero es observable**.
4. **(d) ¿ArchUnit pasa a `error`?** Hoy hay 0 violaciones reales, así que no rompería el build —
   pero es una decisión de política de equipo.
5. **B-13:** `UseCasesConfig` combina `@ComponentScan(REGEX "^.+UseCase$")` con **siete `@Bean`
   manuales del mismo tipo**. La Fase 01 midió que **el escaneo es inerte**. ¿Se retira el
   `@ComponentScan` o se retiran los `@Bean`?
6. **B-14:** `spring.devtools.add-properties` se conservó porque la dependencia
   `runtimeOnly('spring-boot-devtools')` **existe**. ¿Se retira del artefacto o se queda?
7. **B-15:** `McpAuditAspect` llama a `proceed()` **antes** de resolver el contexto de seguridad y
   no
   audita la rama no reactiva. ¿Es auditoría de cumplimiento o traza de diagnóstico?

Si no hay respuesta: **haz solo la parte que no depende de DP-08** — **D-25**, **D-27** y **B-13**,
que
son saneamiento interno sin comportamiento observable— y **documenta el bloqueo del resto en
`fase-08.md` §4**.

### El objetivo de la fase, en una frase

**Que los dos mecanismos que llevan todo el plan diciendo que todo está bien —el informe de ArchUnit
y `UseCasesConfigTest`— sean capaces, por primera vez, de decir que algo está mal.**

`checkWithWarning` descarta en silencio toda incidencia cuyo fichero no resuelva: los `issues.json`
salen `{"issues":[],"rules":[]}` **haya o no violaciones** —lo demostró la Fase 01, cuando sí había
una y el log gritaba `Rule_2.2 ... violated (1 times)`—. Y `UseCasesConfigTest` envuelve todo en un
`catch (UnsatisfiedDependencyException) { assertTrue(true); }` sobre un contexto que **no puede
arrancar** porque no registra ningún gateway: su única aserción **probablemente no se ha ejecutado
jamás**. Que hoy ambos «coincidan con la realidad» es casualidad, no garantía.

### Decisiones YA RESUELTAS — NO las replantees ni las contradigas

- **DP-01 — La seguridad se queda laxa y ya es configuración.** `mcp.security.mode` (`PERMISSIVE`
  por defecto / `ENFORCED`), leído de `MCP_SECURITY_MODE`. **No lo toques.**
- **DP-02 / B-01 — El token no cambia de formato.** `adapter.restconsumer.token` recibe
  `Base64(":" + PAT)` ya calculado. **Ningún token real en el repositorio.**
- **B-03 — Los nombres de rol son literales** (`MCP.AZURE_DEVOPS.READ` / `.WRITE`), en
  `mcp-server/.../mcp/security/McpRoles.java`.
- **DP-03 — La frontera de contrato existe y no se deshace.** El tipo de dominio se llama **
  `WorkItemBatchCriteria`**.
- **DP-04 — El repliegue se conserva y se mide.** Sigue vivo, **año del calendario incluido**,
  contado por `azuredevops.teamscope.fallback` con `WARN`. Los tipos por defecto y
  `User Story → Historia de Usuario` son **regla de dominio** (`WorkItemTypes`).
- **DP-05 — 3 adaptadores, 3 puertos, y así se quedan.** **B-05:** los nombres de cortacircuito
  (`workItemQuery`, `workItemCommand`, `teamScope`) son **DEFINITIVOS**. **B-07:** los tres
  adaptadores **comparten el `WebClient`**.
- **DP-06 — El contrato de errores es público y estable.** Forma `CODIGO: mensaje neutro`. Cuatro
  códigos: **`AZDO_NOT_FOUND`**, **`AZDO_UNAUTHORIZED`**, **`AZDO_UNAVAILABLE`**, **
  `MCP_INTERNAL_ERROR`**. El cuerpo original de Azure DevOps **se registra en `ERROR` y NO se
  propaga**. **B-09:** versiones de API en `@ConfigurationProperties`. **B-10:** las dos URLs de
  equipo **NO** se parametrizan.
- **DP-07 — El dominio es inmutable y la frontera de salida hacia MCP está cerrada.**
  **(a)** 4 DTOs de respuesta + `McpResponseMapper` en `mcp-server/.../mcp/dto/`. **(b)** las 9
  clases mutables son **`record`**. **(c)** las invariantes son **las mínimas que no cambian nada
  observable**: copia defensiva preservando **orden y nulos**, `WiqlQuery` no vacía,
  `JsonPatchOperation` con `op` y `path`. **Se rechazaron expresamente** `id` no nulo, `ids` no
  vacío y normalizar nulos a colecciones vacías. **(d)** `WorkItem.fields` **sigue siendo un
  `Map<String,Object>` abierto**. **B-11:** enunciado de D-21 reescrito. **B-12:** se aceptó
  formalmente que importa «0 reglas de negocio» y **no** el recuento de líneas — **
  `AzureDevOpsTools` (188) y `WorkItemQueryAdapter` (143) NO se parten**.
- **Los nombres de campo del cable son intocables**: `op`, `path`, `value`, `from`, `ids`, `fields`,
  `expand`, `errorPolicy`. Hay prueba por reflexión.

### Restricción técnica dura, descubierta en la Fase 04

**El módulo `domain/usecase` NO admite ninguna dependencia más allá de `:model`.** La tarea
`validateStructure` del plugin de Clean Architecture de Bancolombia falla con *«Use case module is
invalid»*. Se comprobó añadiendo `slf4j-api`. **No lo intentes de nuevo:** si necesitas registrar
algo desde el dominio, hazlo a través de un **puerto**, como hizo `TeamScopeFallbackMetrics`.

### Aviso de infraestructura, heredado de las Fases 06 y 07

El build puede fallar con
`java.nio.file.NoSuchFileException: ...\app-service\build\test-results\test\binary\in-progress-results-generic.bin`.
**No es un fallo del código**: es un resto corrupto combinado con la caché de configuración de
Gradle. Se resuelve borrando `applications\app-service\build\test-results` y ejecutando con
`--no-configuration-cache`.

### Reglas innegociables

1. **No deshagas nada de las Fases 01 a 07.** Las siete decisiones (DP-01 a DP-07) están cerradas.
2. **Cero cambios en el contrato MCP público** —nombres de tool, de parámetro, de campo, forma del
   resultado, códigos de error— salvo lo que **DP-08** autorice expresamente. Si autoriza algo,
   **documéntalo en `CONTRATO-MCP.md`**, como hizo la Fase 06 con el contrato de errores.
3. **Las tres caracterizaciones deben seguir verdes**: `WiqlCharacterizationTest`,
   `OutboundPayloadCharacterizationTest` y **`McpResponsePayloadCharacterizationTest`**. Son la red
   de seguridad. Si una se pone roja, **algo observable cambió**.
4. **No elimines las tolerancias de `DomainInvariantsTest`.** Existen para documentar por qué el
   dominio **no** rechaza un `defaultValue` en blanco, un `id` nulo o un lote vacío. Endurecerlas
   **reabre DP-04**.
5. **Las 205 pruebas deben seguir pasando.** Cualquier prueba heredada que necesites tocar:
   **pregunta primero.** *(Excepción evidente: `UseCasesConfigTest` es el objeto de D-27; arreglarla
   es el encargo.)*
6. **T-03 no está hecho hasta que lo demuestres.** Arreglar el informe de ArchUnit exige introducir
   una **violación deliberada**, ver que aparece en el `issues.json` y retirarla. Sin esa prueba, es
   indistinguible de no haberlo arreglado — que es exactamente la deuda D-25.
7. **ArchUnit no pasa a `error` antes de arreglar el informe** (T-03 antes que T-05).
8. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres, contratos o
   reglas de negocio, **detente y pregunta**. Las Fases 03, 04, 05, 06 y 07 se detuvieron y las
   cinco
   veces fue correcto.
9. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`, ni
   `skills/`, `overlays/` y el resto de `docs/`.
10. **Un commit** al cerrar:
    `chore(mcp_config): tipar la configuracion y activar las reglas de arquitectura`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-08.md`, en orden y
sin adelantar pasos, empezando por el **paso 0 bloqueante**. Marca cada casilla conforme la
completes.

Comandos de verificación (PowerShell, Windows):

```
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon
```

### Definición de Hecho

- [ ] DP-08 respondida por el propietario **o** bloqueo documentado y parte independiente ejecutada.
- [ ] `.\gradlew.bat build` en **verde**, con **≥ 205** pruebas y **0** fallos.
- [ ] **Informe de ArchUnit arreglado y DEMOSTRADO** con una violación deliberada que aparece en el
  `issues.json` (D-25).
- [ ] **`UseCasesConfigTest` puede fallar**: sin `catch` que garantice el verde, sin bean de relleno
  que satisfaga la aserción por sí solo (D-27).
- [ ] **1 solo mecanismo de wiring por bean** (D-05, B-13).
- [ ] ArchUnit ejecutado como **`error`** *(si DP-08 (d) lo autoriza)*.
- [ ] **0** tools comentadas con código vivo *(D-19 decidida)*.
- [ ] **0** capacidades MCP anunciadas sin implementación *(si DP-08 (c) lo autoriza)*.
- [ ] Cobertura `app-service` **≥ 60 %**; ninguna otra baja.
- [ ] Las **tres** pruebas de caracterización **verdes**.
- [ ] `CONTRATO-MCP.md` actualizado con **todo** cambio observable.
- [ ] **`docs/resultados/CIERRE-DEL-PLAN.md` escrito.**
- [ ] Plan maestro actualizado (§3, §6, §7, §9, cabecera) y **marcado como 🟢 CERRADO**.
- [ ] Bloque **Resultado** de `fase-08.md` relleno con lo alcanzado **de verdad**.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.

