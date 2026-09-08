# PROMPT DE ARRANQUE — Fase 02

> Fichero autocontenido para **iniciar la Fase 02 en una sesión nueva**, sin depender del historial
> de la sesión que ejecutó la Fase 01. Copiar el bloque de §2 tal cual.

---

## 1. Cómo usarlo

1. Abrir una sesión nueva en el workspace `AzureDevOps`.
2. Copiar **todo** el bloque de §2 como primer mensaje.
3. El agente debe **leer los cinco documentos indicados antes de escribir código**. Si empieza a
   editar sin haberlos leído, detenerlo.

---

## 2. Prompt (copiar desde aquí)

```
### Rol y objetivo

Actúa como ingeniero senior de Spring Boot y Clean Architecture. Vas a ejecutar la **Fase 02** de un
plan de refactorización ya en marcha sobre el proyecto `azure-devops-mcp`. La Fase 01 está
**completada y commiteada**; tú continúas desde ahí.

**Proyecto:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp`
**Reglas obligatorias:** `C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\rules\spring-rules.md`

### Paso 0 — Lectura obligatoria (NO escribas código antes de completarlo)

Lee, en este orden, y confírmame en 10 líneas qué entendiste antes de tocar nada:

1. `rules/spring-rules.md` — estándar de Clean Architecture y calidad (Bancolombia).
2. `azure-devops-mcp/docs/plan/plan-maestro.md` — visión global, 27 deudas, 8 fases, decisiones.
3. `azure-devops-mcp/docs/fases/fase-02.md` — **tu fase**: Contexto, Instrucciones y checklist.
4. `azure-devops-mcp/docs/resultados/BASELINE.md` — cifras medidas al cerrar la Fase 01.
5. `azure-devops-mcp/docs/resultados/CONTRATO-MCP.md` — contrato MCP público que NO puedes romper.

### Estado real al arrancar (medido, no estimado)

- Build 🟢 VERDE: **48 pruebas, 0 fallos**.
- Cobertura: `domain/model` **0 %** (no tiene `src/test`) · `domain/usecase` **68,2 %** ·
  `mcp-server` **71,3 %** · `rest-consumer` **71,5 %** · `app-service` **23,6 %**.
- La sentencia WIQL está **congelada carácter a carácter** en `WiqlCharacterizationTest` (8 ramas).
- Stack: Java 25 · Spring Boot 4.1.0 · Spring AI 2.0.0-RC1 (MCP `STATELESS`/`ASYNC`) · Reactor ·
  Resilience4j · Lombok · ArchUnit. Módulos: `:app-service`, `:model`, `:usecase`, `:mcp-server`,
  `:rest-consumer`.

### Decisiones YA RESUELTAS por el propietario — NO las vuelvas a plantear ni las cambies

- **DP-01 — La seguridad se queda laxa; lo que cambia es el mecanismo.** El sistema es una POC y
  **todavía no existe el Service Principal**, por eso ni el agente ni el BFF envían token. El flujo
  objetivo es: el front obtiene del IDP (Entra ID) un token **de usuario** válido **solo para el
  BFF**; el BFF llama al agente; y el agente obtiene un **segundo token M2M**, distinto, para hablar
  con el MCP. El objetivo declarado es enviar a pre y producción una **copia 1 a 1** del binario.
  **No elimines nada de la seguridad existente.**
- **DP-02 — El PAT viene de variable de entorno o secreto.** El usuario de servicio aún no existe;
  en local cada desarrollador usa el suyo. **Ningún token real puede aparecer en código, pruebas,
  logs ni documentación.**
- **B-01 — El formato del token NO cambia.** La propiedad `adapter.restconsumer.token` sigue
  recibiendo el **Base64 ya calculado**; se conserva el ensamblado del prefijo `Basic `. Migrar al
  PAT crudo rompería los entornos ya configurados. Lo que sí corriges: quitar el default
  `your-token-here`, **validar al arranque** que el valor sea Base64 decodificable y contenga `:`, y
  **documentar** el formato. Dato correcto para el javadoc: el encabezado es `Basic Base64(":" +
  PAT)` — usuario vacío, dos puntos, PAT; **el PAT no es la fusión de dos claves**.
- **B-02 — Propiedad, no perfil de Spring.** Usa `mcp.security.mode` (`PERMISSIVE` por defecto /
  `ENFORCED`), leída de `MCP_SECURITY_MODE`. El modo debe ser una variable de entorno visible en el
  log de arranque, no un efecto lateral del perfil activo.
- **B-03 — Los nombres de rol se conservan literalmente**: `MCP.AZURE_DEVOPS.READ` y
  `MCP.AZURE_DEVOPS.WRITE`. Centralízalos en constantes. No los cambies: sin App Registration real
  sería asumir un contrato inexistente (`spring-rules.md` §6).

### El objetivo de la fase, en una frase

**No es cerrar la seguridad. Es que cerrarla deje de exigir una recompilación.**

Hoy el interruptor es el compilador: `.anyExchange().permitAll()` está escrito en Java y tres
`@PreAuthorize` están **comentados**. Eso rompe el objetivo de «copia 1 a 1» de DP-01 por tres vías:
activar la seguridad obliga a recompilar (el binario de producción no sería el probado); el código
comentado **no compila ni se prueba** (seis anotaciones se estrenarían en producción); y un
`permitAll` sin explicación **es indistinguible de un olvido**.

Al terminar debe cumplirse: el mismo `.jar` sirve para POC y producción; `MCP_SECURITY_MODE=enforced`
es lo único necesario por parte del MCP; las **ocho** tools tienen autorización **declarada,
compilada y probada en los dos modos**; y **cero cambios observables** con la configuración por
defecto.

Truco clave para que `PERMISSIVE` no cambie el comportamiento con las anotaciones activas: conceder
en ese modo una identidad anónima portadora de los roles de lectura y escritura.

### Reglas innegociables

1. **Cero cambios observables en modo `PERMISSIVE`.** Las 48 pruebas heredadas deben seguir pasando
   **sin modificarlas**. Si alguna cambia de resultado, el modo por defecto no es equivalente: para
   y revisa.
2. **No rompas el contrato MCP público** (`CONTRATO-MCP.md`): ni nombres de tool, ni nombres de
   parámetro, ni forma del resultado. El agente y el BFF son clientes reales.
3. **No actives `ENFORCED` en ningún entorno.** Se construye y se prueba; se queda apagado.
4. **No toques** `azure-devops-agent`, `azure-devops-backend` ni `azure-devops-frontend`. Solo
   documenta lo que necesitarán.
5. **Política de No-Asunción** (`spring-rules.md` §6): ante cualquier duda de nombres, contratos o
   comportamiento, **detente y pregunta**. No inventes propiedades ni endpoints.
6. **Fuera de alcance de esta fase** (cada una tiene la suya): cortacircuitos D-06 → Fase 06; DTOs y
   `Rule_2.2` D-11/D-17 → Fase 03; el WIQL D-07/D-08/D-09 → Fase 04; partir `RestConsumer` D-10 →
   Fase 05; D-19, D-25, D-26, D-27 → Fases 07 y 08.
7. **Un commit** al cerrar, con el formato de `COMMIT_RULES.md`:
   `security(mcp_server): hacer conmutable la politica de acceso y sanear la configuracion`

### Cómo trabajar

Sigue **exactamente** el checklist de §3 «Orden de Ejecución» de `docs/fases/fase-02.md`, en orden y
sin adelantar pasos. Marca cada casilla conforme la completes.

Cada borrado de configuración (T-04) debe ir **precedido de un `grep` que demuestre que nadie lo
usa**, y el resultado del `grep` se anota en el bloque **Resultado**. Es el criterio del plan.

Comandos de verificación (PowerShell, Windows):

```

cd C:\Users\minaj\Work\GitHub\Labs\AzureDevOps\azure-devops-mcp
.\gradlew.bat build --no-daemon

```

### Definición de Hecho

- [ ] `.\gradlew.bat build` en **verde**, con **≥ 48** pruebas y **0** fallos.
- [ ] **0** pruebas heredadas modificadas.
- [ ] Las 8 tools con autorización declarada y compilada; **0** anotaciones de seguridad comentadas.
- [ ] `listWorkItemsByTeamAndSprint` **tiene** su `@PreAuthorize` (hoy no existe ni comentado).
- [ ] Los dos modos probados: sin token, con rol y sin rol.
- [ ] `/h2-console/**` y `spring.h2.console` retirados, **con el `grep` que lo justifica**.
- [ ] Default `your-token-here` eliminado y formato validado al arranque.
- [ ] **Ningún** token real en el repositorio.
- [ ] `docs/resultados/ACTIVACION-SEGURIDAD.md` creado.
- [ ] `CONTRATO-MCP.md` §5 actualizado.
- [ ] Plan maestro actualizado: §3 (deudas saldadas), §6 (métricas), §9 (bitácora) y cabecera.
- [ ] Bloque **Resultado** de `fase-02.md` relleno con lo alcanzado **de verdad**.
- [ ] **`docs/fases/fase-03.md` generado** (Regla de Continuidad, §8 del plan maestro), con **DP-03**
      marcada como bloqueante en su primer paso.
- [ ] Commit hecho con el mensaje indicado.

Empieza por el **Paso 0**.
```

---

## 3. Nota para quien retome sin este fichero

El punto de reanudación canónico está en el **§8 «Protocolo de continuidad»** del plan maestro:
leer §9 (Bitácora), abrir el último `fase-NN.md` sin cerrar y continuar por el **primer paso sin
marcar** de su Orden de Ejecución.

