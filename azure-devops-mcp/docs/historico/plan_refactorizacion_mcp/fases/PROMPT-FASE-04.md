# PROMPT DE ARRANQUE — FASE 04

> Documento **autocontenido**. Está escrito para una sesión nueva que **no tiene ningún historial**
> de las fases anteriores. Cópialo íntegro como primer mensaje.

---

### Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 04** de un
plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. Las Fases 01, 02 y 03
están **completadas y commiteadas**; tú continúas desde ahí.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confirma en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia).
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-04.md` — **tu fase**: Paso bloqueante, Contexto, Instrucciones
   y checklist.
4. `azure-devops-mcp/docs/resultados/BASELINE.md` §7.1 — **la sentencia WIQL congelada carácter a
   carácter y sus 8 ramas**. Es el documento central de esta fase.
5. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` §2.4 — **los 6 comportamientos tolerantes que
   son contrato de facto**.
6. `azure-devops-mcp/docs/fases/fase-03.md` §4 — cifras medidas al cerrar la fase anterior.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **77 pruebas, 0 fallos**.
- Cobertura: `domain/model` **0 %** (sin `src/test`, D-26) · `domain/usecase` **68,2 %** ·
  `mcp-server` **73,1 %** · `rest-consumer` **88,1 %** · `app-service` **48,4 %**.
- `AzureDevOpsTools` **277 líneas** (objetivo del plan: ≤ 120). `RestConsumer` **186**.
- La sentencia WIQL está **congelada carácter a carácter** en `WiqlCharacterizationTest` (8 ramas).
- El cuerpo HTTP de salida está **congelado carácter a carácter** en
  `OutboundPayloadCharacterizationTest` (4 cuerpos).
- ArchUnit `Rule_2.2`: ✅ **0 violaciones reales** (D-17 saldada en la Fase 03). Pero **D-25 sigue
  viva**: el `issues.json` sale vacío aunque haya violaciones, así que **la única verificación
  fiable
  es el log del test**.
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit. Módulos: `:app-service`, `:model`, `:usecase`, `:mcp-server`,
  `:rest-consumer`.

### ⛔ Tu primer paso es BLOQUEANTE: DP-04

**No escribas código hasta que el propietario responda.** Pregunta, en concreto:

1. **¿El repliegue por concatenación se conserva?** (a) sí, con métrica y log de aviso; (b) sí, pero
   con el año del calendario **eliminado**, devolviendo error si Azure DevOps no responde; (c) se
   retira por completo.
2. **¿Los tipos por defecto (`'Historia de Usuario','Habilitador'`) y el mapeo
   `User Story → Historia de Usuario` son regla de dominio o son configuración?** De la respuesta
   depende si acaban en un Value Object inmutable o en `@ConfigurationProperties`.
3. **B-04:** ¿el nuevo `ListWorkItemsByTeamAndSprintUseCase` recibe un objeto comando
   (`ListWorkItemsCommand`) o los seis parámetros sueltos?

Si no hay respuesta: **detente**, documenta el bloqueo en `fase-04.md` §4, deja la fase abierta y
pasa a la Fase 05 (no depende técnicamente de ésta). Es el §8.5 del plan maestro.

### Decisiones YA RESUELTAS — NO las replantees ni las contradigas

- **DP-01 — La seguridad se queda laxa y ya es configuración.** `mcp.security.mode` (`PERMISSIVE`
  por defecto / `ENFORCED`), leído de `MCP_SECURITY_MODE`. **No lo toques, no lo actives, no lo
  elimines.** En `PERMISSIVE` la identidad anónima porta los dos roles a propósito.
- **DP-02 / B-01 — El token no cambia de formato.** `adapter.restconsumer.token` recibe
  `Base64(":" + PAT)` ya calculado; se valida al arranque. **Ningún token real en el repositorio.**
- **B-03 — Los nombres de rol son literales** (`MCP.AZURE_DEVOPS.READ` / `.WRITE`) y viven en
  `mcp-server/.../mcp/security/McpRoles.java`. No los cambies.
- **DP-03 — La frontera de contrato ya existe y no se deshace.** Hay **1 mapper de entrada**
  (`mcp-server/.../mcp/dto/McpToolDtoMapper`) y **5 de salida**
  (`rest-consumer/.../consumer/mapper/`). **Ningún modelo de dominio puede volver a exponerse como
  `@McpToolParam` ni ser serializado por el `WebClient`.** El tipo de dominio se llama **
  `WorkItemBatchCriteria`** (antes `WorkItemsBatchRequest`): no revivas el nombre viejo.
- **Los nombres de campo del cable son intocables** (`CONTRATO-MCP.md` §3): `op`, `path`, `value`,
  `from`, `ids`, `fields`, `expand`, `errorPolicy`. Hay una prueba por reflexión que lo verifica.

### El objetivo de la fase, en una frase

**Que el entry-point deje de ser el sistema.**

`listWorkItemsByTeamAndSprint` (44 líneas dentro de `AzureDevOpsTools`) **es** el sistema: limpia
cadenas, resuelve el `AreaPath`, resuelve el `IterationPath`, normaliza los tipos de work item,
**redacta la sentencia WIQL con `String.format`** y encadena la consulta. Las otras cinco tools son
pasamanos, y seis de los siete casos de uso son delegantes vacíos de 17–18 líneas. La capa de
aplicación está vacía **porque la lógica se quedó arriba**. Y `spring-rules.md` es explícito para
`entry-points`: **«Cero lógica de negocio»**.

### Reglas innegociables

1. **La sentencia WIQL debe salir idéntica, carácter a carácter, en las 8 ramas.** Si cambia un
   espacio, el tablero sale vacío y nadie se entera. `WiqlCharacterizationTest` es tu red de
   seguridad.
2. **Cero cambios en el contrato MCP público**: ni nombres de tool, ni nombres de parámetro, ni
   nombres de campo, ni forma del resultado. El agente y el BFF son clientes reales.
3. **Las 77 pruebas heredadas deben seguir pasando sin modificarlas.** La Fase 03 obtuvo una
   autorización **puntual** para renombrar 3 líneas de `RestConsumerTest`; **no es un permiso
   general**. Si vuelves a necesitar tocar una prueba heredada, **pregunta primero**.
4. **No toques la seguridad de la Fase 02.** El `@PreAuthorize` **se queda en el método de la tool**
   aunque el cuerpo se vaya al caso de uso; `McpToolsAuthorizationTest` sigue verde.
5. **No deshagas la frontera de la Fase 03.** Los Value Objects nuevos son de **dominio**: no pueden
   viajar por el cable.
6. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres, contratos o
   reglas de negocio, **detente y pregunta**. La Fase 03 se detuvo dos veces y las dos fueron
   correctas.
7. **Fuera de alcance:** partir `RestConsumer` y reagrupar los paquetes de `domain/model`
   (D-10/D-14/D-15 → Fase 05); errores y cortacircuitos (D-06/D-13/D-18/D-24 → Fase 06); retirar los
   `@Setter` del modelo existente (D-16 → Fase 07); D-05, D-19, D-20, D-23, D-25, D-27 → Fase 08. *(
   Los Value Objects **nuevos** que crees sí nacen inmutables.)*
8. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`.
9. **Un commit** al cerrar:
   `refactor(work_item_query): mover la construccion del wiql al caso de uso`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-04.md`, en orden y
sin adelantar pasos, empezando por el **paso 0 bloqueante**. Marca cada casilla conforme la
completes.

Comandos de verificación (PowerShell, Windows):

```
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon
```

### Definición de Hecho

- [ ] DP-04 respondida por el propietario **o** bloqueo documentado y fase dejada abierta.
- [ ] `.\gradlew.bat build` en **verde**, con **≥ 77** pruebas y **0** fallos.
- [ ] **0** pruebas heredadas modificadas.
- [ ] **La sentencia WIQL es idéntica en las 8 ramas**, verificado por `WiqlCharacterizationTest`
  **sin modificarla**.
- [ ] `AzureDevOpsTools` **≤ 120 líneas**.
- [ ] **0** reglas de negocio de Azure DevOps en `entry-points`.
- [ ] **0** `String.format` de WIQL fuera del dominio.
- [ ] El repliegue por concatenación **está medido** (contador + `log.warn`).
- [ ] `domain/model/src/test` **creado** (empieza a saldar D-26).
- [ ] Cobertura `domain/usecase` **≥ 85 %**.
- [ ] **0** modelos de dominio expuestos como `@McpToolParam` o serializados por el `WebClient`
  *(no deshacer la Fase 03)*.
- [ ] `CONTRATO-MCP.md` §2.4 actualizado, comportamiento tolerante por comportamiento tolerante.
- [ ] Plan maestro actualizado: §3, §6, §9 y cabecera.
- [ ] Bloque **Resultado** de `fase-04.md` relleno con lo alcanzado **de verdad**.
- [ ] **`docs/fases/fase-05.md` y `docs/fases/PROMPT-FASE-05.md` generados**, con **DP-05** marcada
  como bloqueante en el primer paso.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.

