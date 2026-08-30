# PROMPT DE ARRANQUE — FASE 03

> Copiar y pegar en una sesión **nueva**. Es autocontenido a propósito: no depende del historial de
> la sesión que cerró la Fase 02.

---

### Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 03** de un
plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. Las Fases 01 y 02 están
**completadas y commiteadas**; tú continúas desde ahí.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confirma en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia).
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-03.md` — **tu fase**: Paso bloqueante, Contexto, Instrucciones
   y checklist.
4. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` — **el contrato que NO puedes romper**. Es el
   documento central de esta fase.
5. `azure-devops-mcp/docs/resultados/BASELINE.md` y `docs/fases/fase-02.md` §4 — cifras medidas.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **67 pruebas, 0 fallos**.
- Cobertura: `domain/model` **0 %** (sin `src/test`, D-26) · `domain/usecase` **68,2 %** ·
  `mcp-server` **70,2 %** · `rest-consumer` **90,0 %** · `app-service` **48,4 %**.
- La sentencia WIQL está **congelada carácter a carácter** en `WiqlCharacterizationTest` (8 ramas).
- ArchUnit `Rule_2.2`: **1 violación real**, en *warning*, y **no llega a Sonar** (D-25): el
  `issues.json` sale vacío, así que **la única verificación fiable es el log del test**.
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit. Módulos: `:app-service`, `:model`, `:usecase`, `:mcp-server`,
  `:rest-consumer`.

### ⛔ Tu primer paso es BLOQUEANTE: DP-03

**No escribas código hasta que el propietario responda.** Pregunta, en concreto:

1. ¿El DTO que sustituya a `WorkItemsBatchRequest` **conserva el nombre de clase**?
2. ¿Se autoriza **renombrar el tipo de dominio** para cerrar `Rule_2.2`, o se saca del dominio, o se
   documenta una excepción a la regla?
3. ¿`JsonPatchOperation` se duplica en DTO de entrada y de salida, o basta uno compartido?

Si no hay respuesta: **detente**, documenta el bloqueo en `fase-03.md` §4, deja la fase abierta y
pasa a la Fase 04 (no depende técnicamente de ésta). Es el §8.5 del plan maestro.

### Decisiones YA RESUELTAS — NO las replantees ni las contradigas

- **DP-01 — La seguridad se queda laxa y ya es configuración.** `mcp.security.mode` (`PERMISSIVE`
  por defecto / `ENFORCED`), leído de `MCP_SECURITY_MODE`. **No lo toques, no lo actives, no lo
  elimines.** En `PERMISSIVE` la identidad anónima porta los dos roles a propósito.
- **DP-02 / B-01 — El token no cambia de formato.** `adapter.restconsumer.token` recibe
  `Base64(":" + PAT)` ya calculado; se valida al arranque. **Ningún token real en el repositorio.**
- **B-03 — Los nombres de rol son literales** (`MCP.AZURE_DEVOPS.READ` / `.WRITE`) y viven en
  `mcp-server/.../mcp/security/McpRoles.java`. No los cambies.
- **Los nombres de campo del cable son intocables** (`CONTRATO-MCP.md` §3): `op`, `path`, `value`,
  `from`, `ids`, `fields`, `expand`, `errorPolicy`. El nombre de la **clase** puede discutirse; el
  del **campo**, no.

### El objetivo de la fase, en una frase

**Que el dominio deje de ser el contrato de cable en los dos extremos.**

Hoy Jackson deserializa modelos de `domain/model` desde el payload MCP y el `WebClient` los serializa
tal cual hacia Azure DevOps. No hay **ni un solo mapper** en la frontera de salida, cuando
`spring-rules.md` los declara obligatorios. Y `WorkItemsBatchRequest` —un *request* HTTP viviendo en
el dominio— es la única violación de ArchUnit `Rule_2.2`.

### Reglas innegociables

1. **Cero cambios en el contrato MCP público**: ni nombres de tool, ni nombres de parámetro, ni
   nombres de campo, ni forma del resultado. El agente y el BFF son clientes reales.
2. **Las 67 pruebas heredadas deben seguir pasando sin modificarlas.** `RestConsumerTest` es tu red
   de seguridad para la frontera de salida; `WiqlCharacterizationTest`, para la entrada.
3. **No toques la seguridad de la Fase 02.** Si mueves una firma, el `@PreAuthorize` se mueve con
   ella y `McpToolsAuthorizationTest` sigue verde.
4. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`.
5. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres o contratos,
   **detente y pregunta**.
6. **Fuera de alcance:** el WIQL y las rutas (D-07/08/09 → Fase 04); partir `RestConsumer` (D-10 →
   Fase 05); errores y cortacircuitos (D-06/D-13 → Fase 06); `@Setter` e inmutabilidad (D-16 →
   Fase 07); D-19, D-20, D-23, D-25, D-26, D-27 → Fases 07 y 08.
7. **Un commit** al cerrar: `refactor(mcp_contract): separar los dto de cable de los modelos de dominio`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-03.md`, en orden y
sin adelantar pasos, empezando por el **paso 0 bloqueante**. Marca cada casilla conforme la completes.

Comandos de verificación (PowerShell, Windows):

```
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon
```

### Definición de Hecho

- [ ] DP-03 respondida por el propietario **o** bloqueo documentado y fase dejada abierta.
- [ ] `.\gradlew.bat build` en **verde**, con **≥ 67** pruebas y **0** fallos.
- [ ] **0** pruebas heredadas modificadas.
- [ ] **0** modelos de dominio deserializados como `@McpToolParam`.
- [ ] **0** modelos de dominio serializados por el `WebClient`.
- [ ] **≥ 3** mappers en la frontera de salida y mapper de entrada en el entry-point.
- [ ] ArchUnit `Rule_2.2` a **0 violaciones reales**, verificado **en el log** (no en `issues.json`).
- [ ] **0** nombres de campo del cable modificados.
- [ ] `CONTRATO-MCP.md` §3 actualizado.
- [ ] Plan maestro actualizado: §3, §6, §9 y cabecera.
- [ ] Bloque **Resultado** de `fase-03.md` relleno con lo alcanzado **de verdad**.
- [ ] **`docs/fases/fase-04.md` generado**, con **DP-04** marcada como bloqueante en su primer paso.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.

