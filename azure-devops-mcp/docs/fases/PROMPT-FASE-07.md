# PROMPT DE ARRANQUE — FASE 07

> Copia este contenido como primer mensaje de una **sesión nueva**. Es **autocontenido**: no depende
> del historial de la sesión que cerró la Fase 06.

---

## Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 07** de un
plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. Las Fases 01, 02, 03, 04,
05 y **06** están **completadas y commiteadas**; tú continúas desde ahí.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confirma en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia). Presta atención
   a §1: *«Clases ricas en comportamiento e invariantes de negocio. Prohibido el modelo anémico»*.
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-07.md` — **tu fase**: paso bloqueante, contexto, instrucciones
   y checklist.
4. `azure-devops-mcp/docs/fases/fase-06.md` §4 — cifras medidas al cerrar la fase anterior y sus
   **seis desviaciones documentadas**.
5. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` **§3.4** y **§6** — §3.4 dice qué se sigue
   serializando desde el dominio hacia el cliente MCP *(la trampa de esta fase)*; §6 es el
   **contrato de errores**, nuevo y público desde la Fase 06.
6. `azure-devops-mcp/docs/fases/fase-06.md` §4.1 — qué construyó la Fase 06 y **qué no debes tocar**.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **184 pruebas, 0 fallos**.
- Cobertura *(líneas, JaCoCo)*: `domain/model` **95,8 %** · `domain/usecase` **97,8 %** ·
  `mcp-server` **83,3 %** · `rest-consumer` **92,3 %** · `app-service` **58,2 %**.
- **Tres adaptadores**: `WorkItemQueryAdapter` **143** · `WorkItemCommandAdapter` **106** ·
  `TeamScopeAdapter` **112**. Cada uno con **dos constructores**: el de Spring (`@Autowired`, con
  `AzureDevOpsAdapterProperties`) y uno de conveniencia con valores por defecto **que existe para
  que las pruebas heredadas no se toquen**.
- **Tres puertos** de dominio (`WorkItemQueryPort`, `WorkItemCommandPort`, `TeamScopePort`) + 1 de
  observabilidad. **0 puertos huérfanos.**
- `domain/model` tiene **3 paquetes**: `workitem/`, `team/` y **`exception/`**.
- **Excepciones de dominio: 3** (+1 base abstracta `AzureDevOpsException`), con códigos estables
  **`AZDO_NOT_FOUND`**, **`AZDO_UNAUTHORIZED`**, **`AZDO_UNAVAILABLE`**. **Ya son inmutables**: no
  entran en D-16.
- **Errores técnicos crudos hacia el cliente: 0.** Los 7 puntos de salida se traducen con **un solo**
  `AzureDevOpsErrorTranslator`, y el entry-point les da forma con `McpErrorTranslator`.
- **Cortacircuitos: 3**, los tres **declarados y configurados** en `application.yaml`
  (50 % / 10 / 10 s). `testGet` y `testPost` **retirados**.
- **Timeouts: 4 por operación** (consulta 10 s · **lote 30 s** · comando 10 s · ámbito de equipo 5 s),
  más los de Netty de 5 s, **que siguen intactos**.
- **Versiones de API:** 2 en `@ConfigurationProperties` (mismos literales) + **2 literales en la
  ruta** de las consultas de equipo (B-10, deliberado).
- `AzureDevOpsTools` **173 líneas**, **sin ninguna regla de negocio**.
- **`@Setter` en las 21 clases de `domain/model`** ← **el objetivo de esta fase (D-16)**.
- La sentencia WIQL sigue **congelada carácter a carácter** (8 ramas) y el cuerpo HTTP de salida
  también (4 cuerpos).
- ArchUnit `Rule_2.2`: ✅ **0 violaciones reales**. **D-25 sigue viva**: el `issues.json` sale vacío,
  así que **la única verificación fiable es el log del test**.
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit. Módulos: `:app-service`, `:model`, `:usecase`, `:mcp-server`,
  `:rest-consumer`.

### ⚠️ Tu primer paso es BLOQUEANTE: DP-07

**No escribas código hasta que el propietario responda.** El objetivo (retirar el `@Setter`) es
clarísimo; **el camino tiene una trampa** que las seis fases anteriores esquivaron y ésta no puede.

**La trampa:** `CONTRATO-MCP.md` §3.4 dice que **cinco** clases de `domain/model` —`WorkItem`,
`WorkItemRelation`, `WorkItemReference`, `WiqlResult` y `TeamFieldValues`— **siguen serializándose
desde el dominio hacia el cliente MCP**. La Fase 03 cerró la frontera de entrada y la de
salida-hacia-Azure-DevOps, pero **la de salida-hacia-MCP nunca se cerró**. Y `WiqlResult` es el
retorno de `listWorkItemsByTeamAndSprint`, **la tool más usada del sistema**.

Pregunta, en concreto:

1. **(a)** ¿Se cierra también la **frontera de salida hacia el cliente MCP** —DTOs de respuesta +
   mappers, como hizo la Fase 03 en las otras dos fronteras— o el dominio sigue serializándose tal
   cual? Cerrarla es lo coherente con DP-03, pero son **5 DTOs y 5 mappers nuevos**; no cerrarla
   obliga a que el dominio **siga siendo serializable**, lo que limita cuánto puede enriquecerse.
2. **(b)** ¿Las 21 clases pasan a **`record`** o se quedan como clases con `@Getter`/`@Builder`
   **sin `@Setter`**? ⚠️ Un `record` cambia los nombres de accesor (`getId()` → `id()`) y, si (a)
   dice que el dominio se sigue serializando, **eso cambia los nombres de campo del JSON en
   silencio**.
3. **(c)** ¿Qué **invariantes** debe imponer cada agregado? `spring-rules.md` prohíbe el modelo
   anémico, pero **cuáles son las reglas es negocio, no refactor**.
4. **(d)** ¿Se toca `WorkItem.fields`, hoy un `Map<String,Object>` abierto? Es lo más anémico del
   repositorio, pero también lo que permite que el cliente pida cualquier campo de Azure DevOps sin
   cambiar el servidor.
5. **B-11:** el enunciado de **D-21** en §3 del plan sigue con el texto viejo y menciona
   `RestConsumerTest`, **una clase que ya no existe** (se dividió en tres en la Fase 05). ¿Se
   reescribe o se tacha como saldada de hecho?
6. **B-12:** `AzureDevOpsTools` lleva **173 líneas** y `WorkItemQueryAdapter` **143**, frente a un
   objetivo de ≤ 120 en ambos casos. Es deuda **heredada y documentada**. ¿Se parten o se acepta
   formalmente que lo que importa es «0 reglas de negocio» y no el recuento?

Si no hay respuesta: **detente**, documenta el bloqueo en `fase-07.md` §4, deja la fase abierta y
pasa a la Fase 08 (no depende técnicamente de ésta). Es el §8.5 del plan maestro.

### ⚠️ El precedente que NO debes ignorar al escribir invariantes

El javadoc de `TeamScope` (`domain/model/.../model/workitem/TeamScope.java`) dice literalmente:

> *Por qué solo se rechazan los nulos y no las cadenas en blanco. Azure DevOps puede devolver un
> `defaultValue` vacío para el campo de área de una célula mal configurada. […] Rechazar aquí las
> cadenas en blanco convertiría ese tablero vacío en un error, que es **exactamente** el cambio de
> comportamiento observable que DP-04 §0.1 descartó al elegir la opción (a).*

**Endurecer una invariante puede reabrir DP-04 sin querer.** Cada invariante nueva la declara el
propietario en DP-07 §0.2(c), no tú.

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
  3 puertos. **B-05: los tres nombres de cortacircuito son DEFINITIVOS** (`workItemQuery`,
  `workItemCommand`, `teamScope`) **y ya están configurados**. **B-07:** los tres adaptadores
  **comparten el `WebClient`** de `RestConsumerConfig`.
- **DP-06 — El contrato de errores es público y estable.** Forma `CODIGO: mensaje neutro`. Cuatro
  códigos: **`AZDO_NOT_FOUND`**, **`AZDO_UNAUTHORIZED`**, **`AZDO_UNAVAILABLE`**,
  **`MCP_INTERNAL_ERROR`**. El cuerpo original de Azure DevOps **se registra en `ERROR` y NO se
  propaga**. **La tool falla ante un error, PERO el repliegue de rutas se respeta intacto.**
  **B-08:** se traduce en **dos capas** —`AzureDevOpsErrorTranslator` (técnico → dominio) y
  `McpErrorTranslator` (dominio → mensaje MCP)—, **una sola instancia de cada**. **B-09:** las
  versiones de API son `@ConfigurationProperties`. **B-10:** las dos URLs de equipo **NO** se
  parametrizan. **Nada de esto se replantea.**
- **Los nombres de campo del cable son intocables** (`CONTRATO-MCP.md` §3.4): `op`, `path`, `value`,
  `from`, `ids`, `fields`, `expand`, `errorPolicy`. Hay una prueba por reflexión que lo verifica.

### Restricción técnica dura, descubierta en la Fase 04

**El módulo `domain/usecase` NO admite ninguna dependencia más allá de `:model`.** La tarea
`validateStructure` del plugin de Clean Architecture de Bancolombia falla con *«Use case module is
invalid»*. Se comprobó añadiendo `slf4j-api`. **No lo intentes de nuevo:** si necesitas registrar
algo desde el dominio, hazlo a través de un **puerto** que transporte la información al adaptador,
como hizo `TeamScopeFallbackMetrics`.

### Aviso de infraestructura, heredado de la Fase 06

El build puede fallar con
`java.nio.file.NoSuchFileException: ...\app-service\build\test-results\test\binary\in-progress-results-generic.bin`.
**No es un fallo del código**: es un resto corrupto combinado con la caché de configuración de
Gradle. Se resuelve borrando `applications\app-service\build\test-results` y ejecutando con
`--no-configuration-cache`.

### El objetivo de la fase, en una frase

**Que un `WorkItem` válido no pueda dejar de serlo a mitad de un flujo reactivo, y que las cinco
clases que hoy son el contrato de salida dejen de serlo por accidente.**

Hoy hay **`@Setter` en las 21 clases** de `domain/model` y **cero invariantes** en las 15 que no son
Value Objects de la Fase 04. Es el modelo anémico literal que `spring-rules.md` §1 prohíbe.

### Reglas innegociables

1. **Escribe la prueba de caracterización del JSON de salida ANTES de tocar una sola clase.** Es el
   paso 3 del checklist y no es opcional: cinco clases de dominio **son** hoy el contrato de
   respuesta de las seis tools, y un `record` cambia los nombres de accesor **en silencio**.
2. **Cero cambios en el contrato MCP público** —nombres de tool, de parámetro, de campo, forma del
   resultado— salvo lo que **DP-07** autorice expresamente.
3. **Cero cambios en el contrato de errores de la Fase 06.** Los cuatro códigos son públicos.
4. **Ni una URL, ni un parámetro de consulta, ni un cuerpo JSON, ni una cabecera cambian.**
   `OutboundPayloadCharacterizationTest` y los tres `*AdapterTest` son la red de seguridad.
5. **Las 184 pruebas deben seguir pasando.** Cualquier prueba heredada que necesites tocar:
   **pregunta primero**. Las Fases 03, 04, 05 y 06 obtuvieron autorizaciones **puntuales**; no son un
   permiso general.
6. **No endurezcas invariantes que reabran DP-04.** Lee el javadoc de `TeamScope` antes de escribir
   la primera validación.
7. **No toques** la seguridad (Fase 02), la frontera de entrada (Fase 03), el flujo (Fase 04), el
   reparto de adaptadores (Fase 05) ni la traducción de errores (Fase 06).
8. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres, contratos o
   reglas de negocio, **detente y pregunta**. Las Fases 03, 04, 05 y 06 se detuvieron y las cinco
   veces fue correcto.
9. **Fuera de alcance:** D-05, D-19, D-20, D-22, D-23, D-25, D-27 y pasar ArchUnit a `error` →
   **Fase 08**.
10. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`.
11. **Un commit** al cerrar:
    `refactor(domain_model): convertir el modelo en objetos de valor inmutables`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-07.md`, en orden y
sin adelantar pasos, empezando por el **paso 0 bloqueante**. Marca cada casilla conforme la
completes.

Comandos de verificación (PowerShell, Windows):

```
cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon
```

### Definición de Hecho

- [ ] DP-07 respondida por el propietario **o** bloqueo documentado y fase dejada abierta.
- [ ] `.\gradlew.bat build` en **verde**, con **≥ 184** pruebas y **0** fallos.
- [ ] **0** clases de `domain/model` con `@Setter`.
- [ ] **≥ 1 invariante de negocio declarada** por agregado, **todas autorizadas por DP-07 §0.2(c)**.
- [ ] **0** nombres de campo del JSON de salida alterados, verificado por prueba de caracterización.
- [ ] **0** cambios en el contrato de errores de la Fase 06.
- [ ] **0** URLs, cuerpos, parámetros de consulta o cabeceras alterados.
- [ ] Cobertura `domain/model` **≥ 95,8 %** y `domain/usecase` **≥ 97,8 %** (no deben bajar).
- [ ] Mutaciones eliminadas (Pitest) **≥ 60 %** en `domain/model` y `domain/usecase`.
- [ ] ArchUnit a **0** violaciones reales, verificado **en el log**.
- [ ] El repliegue de DP-04 **sigue funcionando**, verificado por `ResolveTeamScopeUseCaseTest`.
- [ ] `CONTRATO-MCP.md` §3.4 **actualizado**: ya no puede decir «queda anotada para la Fase 07».
- [ ] Plan maestro actualizado: §3, §6, §7, §9 y cabecera.
- [ ] Bloque **Resultado** de `fase-07.md` relleno con lo alcanzado **de verdad**.
- [ ] **`docs/fases/fase-08.md` y `docs/fases/PROMPT-FASE-08.md` generados**.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.

