# PROMPT DE ARRANQUE — FASE 05

> Prompt **autocontenido**: no depende del historial de ninguna sesión anterior (§8.7 del plan
> maestro). Cópialo tal cual en una sesión nueva.

---

### Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 05** de un
plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. Las Fases 01, 02, 03 y 04
están **completadas y commiteadas**; tú continúas desde ahí.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confirma en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia).
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-05.md` — **tu fase**: paso bloqueante, contexto, instrucciones
   y checklist.
4. `azure-devops-mcp/docs/fases/fase-04.md` §4 — cifras medidas al cerrar la fase anterior, y sus
   **cinco desviaciones documentadas**.
5. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` §3 — qué DTO vive en cada frontera y por qué.
6. `azure-devops-mcp/docs/fases/fase-03.md` §4.1 — el reparto de mappers que **no debes duplicar**.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **148 pruebas, 0 fallos**.
- Cobertura: `domain/model` **94,7 %** · `domain/usecase` **97,8 %** · `mcp-server` **80,7 %** ·
  `rest-consumer` **88,2 %** · `app-service` **58,2 %**.
- `RestConsumer` **191 líneas**, con **7 gateways** implementados en una sola clase.
  `AzureDevOpsTools` **157 líneas**, ya **sin ninguna regla de negocio**.
- La sentencia WIQL sigue **congelada carácter a carácter** (8 ramas), ahora en `domain/usecase`.
- El cuerpo HTTP de salida sigue **congelado carácter a carácter** (4 cuerpos).
- ArchUnit `Rule_2.2`: ✅ **0 violaciones reales**. **D-25 sigue viva**: el `issues.json` sale vacío,
  así que **la única verificación fiable es el log del test**.
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit. Módulos: `:app-service`, `:model`, `:usecase`, `:mcp-server`,
  `:rest-consumer`.

### ⛔ Tu primer paso es BLOQUEANTE: DP-05

**No escribas código hasta que el propietario responda.** Pregunta, en concreto:

1. **¿Se autoriza reagrupar los paquetes de `domain/model`** (`getworkitem`, `createworkitem`, … →
   `workitem` + `team`)? (a) paquetes **y** puertos (7 → 3); (b) solo paquetes; (c) no reagrupar.
   Cambia rutas de importación en los cinco módulos y toca el `ArchitectureTest`, que lleva escrito
   «Please do not modify this file».
2. **B-05:** al partir `RestConsumer`, ¿los siete `@CircuitBreaker` **conservan sus nombres
   literales** o se renombran por adaptador? (Sus instancias hoy **no existen** en el YAML: D-06.)
3. **B-06:** el puerto huérfano `workitem/gateways/WorkItemRepository`, ¿se **borra** o se conserva
   como interfaz agregadora?
4. **B-07:** ¿los adaptadores comparten un único `WebClient` y un único helper de versión de API, o
   cada uno construye el suyo?

Si no hay respuesta: **detente**, documenta el bloqueo en `fase-05.md` §4, deja la fase abierta y
pasa a la Fase 06 (no depende técnicamente de ésta). Es el §8.5 del plan maestro.

### Decisiones YA RESUELTAS — NO las replantees ni las contradigas

- **DP-01 — La seguridad se queda laxa y ya es configuración.** `mcp.security.mode` (`PERMISSIVE`
  por defecto / `ENFORCED`), leído de `MCP_SECURITY_MODE`. **No lo toques.**
- **DP-02 / B-01 — El token no cambia de formato.** `adapter.restconsumer.token` recibe
  `Base64(":" + PAT)` ya calculado. **Ningún token real en el repositorio.**
- **B-03 — Los nombres de rol son literales** (`MCP.AZURE_DEVOPS.READ` / `.WRITE`) y viven en
  `mcp-server/.../mcp/security/McpRoles.java`.
- **DP-03 — La frontera de contrato existe y no se deshace.** 1 mapper de entrada
  (`McpToolDtoMapper`) y 5 de salida (`consumer/mapper/`). **Ningún modelo de dominio puede volver a
  exponerse como `@McpToolParam` ni ser serializado por el `WebClient`.** El tipo de dominio se
  llama **`WorkItemBatchCriteria`**.
- **DP-04 — El repliegue se conserva y se mide; las reglas de tipos son dominio.** El repliegue por
  concatenación **sigue vivo, año del calendario incluido**, ahora contado por
  `azuredevops.teamscope.fallback` con `WARN`. Los tipos por defecto y la traducción
  `User Story → Historia de Usuario` son **regla de dominio** (`WorkItemTypes`), **no
  configuración**. El caso de uso recibe un **objeto comando** `ListWorkItemsCommand`.
- **Los nombres de campo del cable son intocables** (`CONTRATO-MCP.md` §3): `op`, `path`, `value`,
  `from`, `ids`, `fields`, `expand`, `errorPolicy`. Hay una prueba por reflexión que lo verifica.

### Restricción técnica dura, descubierta en la Fase 04

**El módulo `domain/usecase` NO admite ninguna dependencia más allá de `:model`.** La tarea
`validateStructure` del plugin de Clean Architecture de Bancolombia falla con *«Use case module is
invalid»*. Se comprobó añadiendo `slf4j-api`. **No lo intentes de nuevo:** si necesitas registrar
algo desde el dominio, hazlo a través de un **puerto** que transporte la información al adaptador,
como hizo `TeamScopeFallbackMetrics`.

### El objetivo de la fase, en una frase

**Que ningún adaptador tenga siete responsabilidades.**

`RestConsumer` implementa **siete gateways** en una sola clase de 191 líneas, de modo que **no hay
forma de probar un flujo sin arrastrar los otros seis**. Y `domain/model` tiene **siete paquetes y
siete puertos para un solo agregado**, organizados por operación CRUD en lugar de por agregado.

### Reglas innegociables

1. **Ni una URL, ni un parámetro de consulta, ni un cuerpo JSON, ni una cabecera cambian.** Esta
   fase
   mueve código de sitio. `RestConsumerTest` y `OutboundPayloadCharacterizationTest` son la red de
   seguridad.
2. **Cero cambios en el contrato MCP público**: ni nombres de tool, ni de parámetro, ni de campo, ni
   la forma del resultado.
3. **Las 148 pruebas deben seguir pasando.** Las Fases 03 y 04 obtuvieron autorizaciones
   **puntuales** para tocar pruebas heredadas; **no son un permiso general**. Si vuelves a
   necesitarlo, **pregunta primero**.
4. **No toques la seguridad de la Fase 02** ni la frontera de la Fase 03 ni el flujo de la Fase 04.
5. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres, contratos o
   reglas de negocio, **detente y pregunta**. Las Fases 03 y 04 se detuvieron y las tres veces fue
   correcto.
6. **Fuera de alcance:** errores y cortacircuitos (D-06/D-13/D-18/D-24 → Fase 06); retirar los
   `@Setter` del modelo preexistente (D-16 → Fase 07); D-05, D-19, D-20, D-23, D-25, D-27 → Fase 08.
   **Tampoco es de esta fase** partir `AzureDevOpsTools` en dos beans para bajar de 120 líneas: está
   anotado como decisión pendiente del propietario.
7. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`.
8. **Un commit** al cerrar:
   `refactor(rest_consumer): segregar los adaptadores de azure devops por agregado`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-05.md`, en orden y
sin adelantar pasos, empezando por el **paso 0 bloqueante**. Marca cada casilla conforme la
completes.

Comandos de verificación (PowerShell, Windows):

```
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon
```

### Definición de Hecho

- [ ] DP-05 respondida por el propietario **o** bloqueo documentado y fase dejada abierta.
- [ ] `.\gradlew.bat build` en **verde**, con **≥ 148** pruebas y **0** fallos.
- [ ] **0** URLs, cuerpos, parámetros de consulta o cabeceras alterados.
- [ ] **0** gateways implementados por una clase con más de una responsabilidad.
- [ ] Cada adaptador resultante **≤ 120 líneas**.
- [ ] **0** puertos huérfanos.
- [ ] Cobertura de `rest-consumer` **≥ 88,2 %** (no debe bajar).
- [ ] ArchUnit a **0** violaciones reales, verificado **en el log**.
- [ ] **0** modelos de dominio expuestos como `@McpToolParam` o serializados por el `WebClient`.
- [ ] Plan maestro actualizado: §3, §6, §7, §9 y cabecera.
- [ ] Bloque **Resultado** de `fase-05.md` relleno con lo alcanzado **de verdad**.
- [ ] **`docs/fases/fase-06.md` y `docs/fases/PROMPT-FASE-06.md` generados**, con **DP-06** marcada
  como bloqueante en el primer paso.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.

