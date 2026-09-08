# FASE 02 — Seguridad conmutable y saneamiento de la configuración

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** 🟢 **COMPLETADA**
> (2026-08-30)
> **Deudas que ataca:** D-01, D-02, D-03, D-04, D-22 · **Decisiones:** ✅ DP-01, ✅ DP-02, ✅ B-01, ✅
> B-02, ✅ B-03
> **Riesgo:** Medio · **Depende de:** Fase 01 (cerrada) · **Habilita:** Fases 03 a 08
> **Regla de oro de esta fase:** **el comportamiento observable en el modo por defecto NO cambia.**

---

## 1. Contexto

### 1.1 Estado actual del sistema

La Fase 01 cerró en verde y **sin tocar `src/main`**. El repositorio está medido y sus flujos
críticos congelados:

| Dato                                        | Valor al cerrar la Fase 01                                         |
|---------------------------------------------|--------------------------------------------------------------------|
| Build                                       | 🟢 **VERDE** — **48 pruebas, 0 fallos**                            |
| Cobertura `rest-consumer`                   | **71,5 %** (era 51,1 %)                                            |
| Cobertura `mcp-server`                      | **71,3 %**                                                         |
| Cobertura `app-service`                     | **23,6 %**                                                         |
| Cobertura `domain/usecase` / `domain/model` | **68,2 %** / **0 %** *(sin `src/test`)*                            |
| Sentencia WIQL                              | **congelada carácter a carácter**, 8 ramas                         |
| Contrato MCP                                | inventariado en [`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) |

Documentos de referencia: [`BASELINE.md`](../resultados/BASELINE.md) ·
[`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md)

### 1.2 Dependencias resueltas de fases previas

**De la Fase 01:**

- ✅ **D-06 confirmada** — los siete `@CircuitBreaker` reales no están configurados; los dos que sí
  lo están (`testGet`, `testPost`) **no los usa nadie**. *Se corrige en la Fase 06, no aquí.*
- ✅ **D-21 saldada** — `getTeamFieldValues` y `getTeamIterations` ya tienen prueba de integración.
- ✅ **D-17 confirmada** — `Rule_2.2` se viola una vez (`WorkItemsBatchRequest`). *Fase 03.*
- 🔽 **D-05 degradada a 🟡** — el `@ComponentScan` es **inerte**: cada caso de uso resuelve a un solo
  bean. Es código muerto que confunde, no un riesgo de arranque. *Fase 08.*
- 🆕 **D-25** — el informe de ArchUnit para Sonar **sale vacío** pese a existir la violación.
- 🆕 **D-26** — `domain/model` **no tiene `src/test`**: 21 clases, cero pruebas.
- 🆕 **D-27** — `UseCasesConfigTest` **miente**: registra su propio bean y captura la excepción de
  arranque con un `assertTrue(true)`.

**Decisiones ratificadas por el propietario (2026-08-30):**

- **DP-01** — El sistema es una **POC** y **no existe todavía el Service Principal**, por eso ni el
  agente ni el BFF envían token. El flujo objetivo es: el front obtiene del IDP un token **de
  usuario** válido **solo para el BFF**; el BFF llama al agente; el agente obtiene un **segundo
  token M2M** para hablar con el MCP. **La postura laxa se mantiene**; lo que cambia es que deja de
  estar cableada en el código.
- **DP-02** — El PAT vendrá de **variable de entorno o secreto**; el usuario de servicio aún no
  existe y en local cada desarrollador usa el suyo.
- **B-01** — **El formato del token NO cambia**: la propiedad sigue recibiendo el **Base64 ya
  calculado**. Migrar al PAT crudo haría fallar cada entorno ya configurado con el mismo 401 mudo
  que se pretendía evitar. Lo que se corrige es que **nada lo valida ni lo documenta**.

### 1.3 Alcance específico de esta fase

**El objetivo NO es cerrar la seguridad. Es que cerrarla deje de exigir una recompilación.**

Hoy el interruptor es el compilador: `.anyExchange().permitAll()` está escrito en Java y seis
`@PreAuthorize` están **comentados**. Eso rompe el objetivo declarado en DP-01 —enviar a pre y
producción una **copia 1 a 1** del binario— por tres vías:

1. Activar la seguridad obliga a recompilar ⇒ el binario de producción **no será** el probado.
2. El código comentado **no compila ni se prueba** ⇒ el día del SP, seis anotaciones se estrenarían
   en producción. Y `listWorkItemsByTeamAndSprint` **ni siquiera tiene la línea que descomentar**.
3. Un `permitAll` sin explicación **es indistinguible de un olvido**.

Al terminar esta fase debe cumplirse:

- **El mismo `.jar`** sirve para la POC y para producción.
- **`MCP_SECURITY_MODE=enforced`** es lo único necesario por parte del MCP para exigir token.
- **Las ocho tools tienen autorización declarada y compilada**, y **probada en los dos modos**.
- **Cero cambios observables** con la configuración por defecto.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No activa `enforced` en ningún entorno.** El valor por defecto es `permissive` y ahí se queda
  hasta que exista el Service Principal (fuera de alcance, §12 del plan maestro).
- **No toca el agente, el BFF ni el frontend.** Solo documenta lo que necesitarán.
- **No cambia el formato del token** (B-01).
- No corrige los cortacircuitos (D-06): es la Fase 06.
- No toca el WIQL, los DTOs ni `RestConsumer`: son las Fases 03, 04 y 05.
- No arregla D-25, D-26 ni D-27: son las Fases 07 y 08.

---

## 2. Instrucciones

### T-01 · Introducir el modo de seguridad conmutable (D-01)

**Ficheros a crear/modificar:**

- `applications/app-service/src/main/java/co/com/bancolombia/config/McpSecurityProperties.java` *(
  nuevo)*
- `applications/app-service/src/main/java/co/com/bancolombia/config/McpSecurityConfig.java`
- `applications/app-service/src/main/resources/application.yaml`

1. Crear un `record` con `@ConfigurationProperties(prefix = "mcp.security")` que exponga
   `SecurityMode mode` con dos valores: **`PERMISSIVE`** (por defecto) y **`ENFORCED`**.
   > **B-02 resuelta:** propiedad, **no** perfil de Spring. El objetivo de DP-01 es que el modo sea
   > una **variable de entorno visible en el log de arranque**, no un efecto lateral del perfil.
2. En `application.yaml` declarar `mcp.security.mode: "${MCP_SECURITY_MODE:PERMISSIVE}"`.
3. En `McpSecurityConfig.securityWebFilterChain`, sustituir el `.anyExchange().permitAll()` fijo por
   la rama correspondiente al modo:
    - `PERMISSIVE` → `.anyExchange().permitAll()` *(idéntico a hoy)*
    - `ENFORCED` → `.anyExchange().authenticated()`
    - `/actuator/health` y `/actuator/info` siguen abiertos **en los dos modos**.
4. **Registrar el modo al arrancar** con un `log.warn` inequívoco cuando sea `PERMISSIVE`:
   que nadie pueda confundir la laxitud con un descuido.

**Validación:** una prueba por modo que compruebe el resultado de una petición sin token (200 en
`PERMISSIVE`, 401 en `ENFORCED`).

---

### T-02 · Restituir y uniformar la autorización de las ocho tools (D-02)

**Ficheros a modificar:**

- `infrastructure/entry-points/mcp-server/.../tools/AzureDevOpsTools.java`
- `infrastructure/entry-points/mcp-server/.../tools/HealthTool.java`
- `applications/app-service/src/main/java/co/com/bancolombia/config/McpRoles.java` *(nuevo)*

1. Crear `McpRoles` con los nombres de rol como constantes.
   > **B-03 resuelta:** los nombres **se conservan literalmente** (`MCP.AZURE_DEVOPS.READ` y
   > `MCP.AZURE_DEVOPS.WRITE`). Cambiarlos sin App Registration real sería asumir un contrato que no
   > existe (`spring-rules.md` §6).
2. **Descomentar** los `@PreAuthorize` de `getWorkItem` y `getWorkItemsBatch`.
3. **Escribir el que falta** en `listWorkItemsByTeamAndSprint`, que nunca lo tuvo. Es la tool más
   usada: es el hueco más importante de la fase.
4. Decidir y aplicar la política de `checkHealth` y `getServerInfo`. **Propuesta:** dejarlas
   públicas —son sondas de vida, equivalentes a `/actuator/health`— pero **declararlo con una
   anotación explícita**, no por omisión.
5. **No tocar `queryByWiql`**: su `@McpTool` sigue comentado y es material de la Fase 08 (D-19).

> **Cómo `permissive` sigue funcionando con las anotaciones activas.** En `PERMISSIVE` se concede
> una identidad anónima portadora de `ROLE_MCP.AZURE_DEVOPS.READ` y `ROLE_MCP.AZURE_DEVOPS.WRITE`.
> Así los `@PreAuthorize` **se compilan, se ejecutan y se prueban** en los dos modos, y el
> comportamiento observable no cambia. Es el corazón de la fase.

**Validación:** `CONTRATO-MCP.md` §5 debe pasar de «2 activas / 3 comentadas / 3 ninguna» a **«8
declaradas y compiladas»**.

---

### T-03 · Sanear el token de Azure DevOps (D-04, B-01)

**Ficheros a modificar:**

- `infrastructure/driven-adapters/rest-consumer/.../config/RestConsumerConfig.java`
- `applications/app-service/src/main/resources/application.yaml`

1. **Eliminar el valor por defecto `your-token-here`.**
2. **Validar al arranque**, sin cambiar el formato (B-01): que el valor sea Base64 decodificable y
   que el texto decodificado **contenga `:`**.
    - En `ENFORCED`: si falta o es inválido → **falla el arranque** con un mensaje explícito.
    - En `PERMISSIVE`: arranca con un `WARN` inequívoco.
3. **Documentar el formato en la propia propiedad** (comentario en el YAML y javadoc): el valor es
   `Base64(":" + PAT)` — usuario vacío, dos puntos, PAT. **El PAT no es la fusión de dos claves.**
4. Conservar el ensamblado del prefijo `Basic ` tal cual, para no alterar entornos existentes.

**Validación:** ninguna prueba, ningún log y ningún fichero de este repositorio contiene un token
real (DP-02).

---

### T-04 · Retirar los residuos del scaffold (D-03, D-22)

**Ficheros a modificar:** `application.yaml`, `McpSecurityConfig.java`

**Cada borrado debe ir precedido de un `grep` que demuestre que nadie lo usa**, y el resultado del
`grep` se anota en el bloque **Resultado**. Es el criterio que el plan del BFF aplicó en su Fase 08.

1. `spring.h2.console.*` → **verificar que no existe dependencia de `com.h2database` en ningún
   `build.gradle` ni datasource configurado**, y eliminar.
2. `.pathMatchers("/h2-console/**").permitAll()` en `McpSecurityConfig` → eliminar.
3. `spring.devtools.add-properties` → contrastar con `runtimeOnly('spring-boot-devtools')` de
   `app-service/build.gradle` antes de decidir.
4. `spring.profiles.include: null` → eliminar.

---

### T-05 · Documentar lo que necesitan los clientes

**Fichero a crear:** `docs/resultados/ACTIVACION-SEGURIDAD.md`

No es burocracia: es la garantía de que el trabajo de esta fase sea utilizable el día del SP. Debe
contener el **procedimiento exacto** para pasar a `ENFORCED`:

1. Registrar la App en Entra ID y conceder `MCP.AZURE_DEVOPS.READ` / `.WRITE`.
2. Dotar al **agente** de la obtención de su token **M2M** hacia el MCP (distinto del token de
   usuario que el front obtiene para el BFF).
3. Exportar `MCP_SECURITY_MODE=enforced` y `AZURE_DEVOPS_TOKEN`.

Y declarar explícitamente qué **no** hace falta: **recompilar**. Ése es el entregable de la fase.

---

### T-06 · Actualizar el plan y generar la Fase 03

1. Marcar el checklist de §3 y rellenar **Resultado** en §4.
2. Actualizar el plan maestro: §3 (D-01 a D-04, D-22 saldadas), §6 (métricas de seguridad), §9
   (Bitácora) y la cabecera.
3. Actualizar `CONTRATO-MCP.md` §5 con el estado nuevo de autorización.
4. **Generar `docs/fases/fase-03.md`**, con **DP-03** marcada como bloqueante en su primer paso.
5. Commit: `security(mcp_server): hacer conmutable la politica de acceso y sanear la configuracion`

---

## 3. Orden de Ejecución

- [x] **1.** Releer `McpSecurityConfig.java` completo y `McpSecurityConfigTest.java` (1 sola
  prueba).
- [x] **2.** Crear `McpSecurityProperties` con `SecurityMode {PERMISSIVE, ENFORCED}`. **(T-01)**
- [x] **3.** Declarar `mcp.security.mode` en `application.yaml` con `PERMISSIVE` por defecto. **(
  T-01)**
- [x] **4.** Ramificar `authorizeExchange` por modo + `log.warn` de arranque en `PERMISSIVE`. **(
  T-01)**
- [x] **5.** Añadir la concesión de roles a la identidad anónima **solo** en `PERMISSIVE`. **(
  T-02)**
- [x] **6.** Crear `McpRoles` con los nombres de rol literales (B-03). **(T-02)**
- [x] **7.** Descomentar `@PreAuthorize` en `getWorkItem` y `getWorkItemsBatch`. **(T-02)**
- [x] **8.** **Escribir** el `@PreAuthorize` de `listWorkItemsByTeamAndSprint`. **(T-02)**
- [x] **9.** Declarar explícitamente la política de `checkHealth` y `getServerInfo`. **(T-02)**
- [x] **10.** Pruebas de autorización **en los dos modos**: sin token, con rol y sin rol. **(T-01,
  T-02)**
- [x] **11.** `.\gradlew.bat build` verde. **Verificar que las 48 pruebas heredadas siguen pasando
  sin modificarlas**: si alguna cambia de resultado, el modo por defecto no es equivalente.
- [x] **12.** Eliminar el default `your-token-here` y validar el formato al arranque. **(T-03)**
- [x] **13.** Documentar el formato `Base64(":" + PAT)` en el YAML y en el javadoc. **(T-03)**
- [x] **14.** `grep` de `h2` en todos los `build.gradle` y en el YAML; anotar el resultado. **(
  T-04)**
- [x] **15.** Retirar `spring.h2.console` y `/h2-console/**`. **(T-04)**
- [x] **16.** Revisar `spring.devtools` y `profiles.include: null`. **(T-04)**
- [x] **17.** `.\gradlew.bat build` verde de nuevo.
- [x] **18.** Crear `docs/resultados/ACTIVACION-SEGURIDAD.md`. **(T-05)**
- [x] **19.** Verificar que **ningún** fichero del repositorio contiene un token real (DP-02).
- [x] **20.** Actualizar `CONTRATO-MCP.md` §5 y el plan maestro (§3, §6, §9, cabecera).
- [x] **21.** Rellenar **Resultado** en §4 y marcar este checklist.
- [x] **22.** **Generar `docs/fases/fase-03.md`.** **(Regla de Continuidad)**
- [x] **23.** Commit:
  `security(mcp_server): hacer conmutable la politica de acceso y sanear la configuracion`

---

## 4. Resultado

> Cerrada el **2026-08-30**. Build 🟢 **VERDE**: **67 pruebas, 0 fallos**.

| Métrica                                          |            Antes |                                               Después |
|--------------------------------------------------|-----------------:|------------------------------------------------------:|
| Modos de seguridad declarados y probados         |            **0** |                                                 **2** |
| Recompilaciones para pasar a `enforced`          |            **1** |                                                 **0** |
| Tools con autorización declarada **y compilada** |          **2/8** |                                             **8 / 8** |
| Anotaciones de seguridad comentadas              |            **3** |                                                 **0** |
| Rutas abiertas a componentes inexistentes        |            **1** |                                                 **0** |
| Secretos con valor por defecto en YAML           |            **1** |                                                 **0** |
| Líneas de `application.yaml`                     |           **71** |                                                **77** |
| Pruebas totales                                  |           **48** |                                            **67** ▲19 |
| Pruebas heredadas **modificadas**                |              n/a |                                                 **0** |
| Cobertura `app-service`                          |       **23,6 %** |                                      **48,4 %** ▲24,8 |
| Cobertura `rest-consumer`                        |       **71,5 %** |                                      **90,0 %** ▲18,5 |
| Cobertura `mcp-server`                           |       **71,3 %** | **70,2 %** ▼1,1 *(clases nuevas sin cubrir del todo)* |
| Cobertura `domain/usecase` / `domain/model`      | **68,2 % / 0 %** |               **68,2 % / 0 %** = *(fuera de alcance)* |

**Pruebas nuevas (19), todas ejecutando los dos caminos:**

| Clase                         | Pruebas | Qué congela                                                  |
|-------------------------------|--------:|--------------------------------------------------------------|
| `McpSecurityModeTest`         |   **5** | 200/401 por modo, sondas abiertas siempre, roles del anónimo |
| `McpToolsAuthorizationTest`   |   **7** | Las 8 tools con rol y sin rol, sobre el proxy real           |
| `RestConsumerConfigTokenTest` |   **7** | Validación del formato del token al arranque                 |

**Resultado de los `grep` de T-04** *(excluyendo `build/`, `build-cache/`, `.gradle/` y `docs/`)*:

| Patrón buscado                                            |                                                                                          Coincidencias | Decisión                                       |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------:|------------------------------------------------|
| `h2database` en todos los `*.gradle`                      |                                                                                                  **0** | **Eliminar** `spring.h2.console`               |
| `h2-console` / `spring.h2` / `h2.console` en `src` y YAML |                                                                              **0** *(tras el borrado)* | **Eliminar** `/h2-console/**`                  |
| `datasource` / `jdbc:`                                    |                                                                                                  **0** | No hay base de datos: el residuo es inequívoco |
| `profiles`                                                |                                                                              **0** *(tras el borrado)* | **Eliminar** `profiles.include: null`          |
| `your-token-here`                                         |                                          **0** en configuración *(solo docs y un javadoc explicativo)* | Default retirado                               |
| `devtools`                                                | **2**: `application.yaml:6` **y** `app-service/build.gradle:13` (`runtimeOnly 'spring-boot-devtools'`) | 🔸 **CONSERVAR**                               |

> 🔸 **`spring.devtools.add-properties` NO se retiró, y el `grep` es la razón.** El paso 16 pedía
> «contrastar antes de decidir»: la dependencia
> `runtimeOnly('org.springframework.boot:spring-boot-devtools')`
> **existe** en `app-service/build.gradle:13`, así que la propiedad **no es un residuo huérfano**,
> sino configuración efectiva de una dependencia presente. Borrarla habría sido exactamente el tipo
> de suposición que el criterio del `grep` existe para evitar. Retirar la dependencia de devtools es
> una decisión de empaquetado que excede esta fase; queda anotada para la **Fase 08** (D-22
> parcial).

**Cambios observables en modo `PERMISSIVE`:** **ninguno**. Las **48** pruebas heredadas siguen
pasando **sin una sola modificación** (0 ficheros de prueba preexistentes tocados) y una petición
sin
token sigue respondiendo `200`, ahora porque la identidad anónima porta `ROLE_MCP.AZURE_DEVOPS.READ`
y `ROLE_MCP.AZURE_DEVOPS.WRITE`, no porque falte una anotación. El contrato MCP público
(`CONTRATO-MCP.md`) no cambió: ni nombres de tool, ni de parámetro, ni forma del resultado.

**Desviaciones respecto a las Instrucciones, y por qué:**

1. **`McpRoles` se creó en `mcp-server`** (`co.com.bancolombia.mcp.security`) y no en `app-service`,
   como sugería T-02. Motivo **técnico e insalvable**: la dependencia Gradle va `app-service →
   mcp-server`, nunca al revés, y las anotaciones que consumen las constantes viven en
   `AzureDevOpsTools` y `HealthTool`, dentro de `mcp-server`. Situarlo en `app-service` **no
   compilaría**. `app-service` sí lo importa sin problema.
2. **`McpSecurityProperties` se inyecta como parámetro del `@Bean`**, no del constructor de
   `McpSecurityConfig`. Motivo: `McpSecurityConfigTest` (heredada) construye la clase con cuatro
   argumentos; cambiar el constructor habría obligado a modificar una prueba heredada, que es
   justamente lo que la regla innegociable nº 1 prohíbe.
3. **El `@PreAuthorize` de `queryByWiql` se descomentó**, aunque T-02.5 decía «no tocar
   `queryByWiql`». Se respetó lo esencial — **su `@McpTool` sigue comentado y la tool no se
   expone**, que es lo que la Fase 08 debe decidir (D-19)—, pero dejar su anotación de seguridad
   comentada habría incumplido la Definición de Hecho («0 anotaciones de seguridad comentadas») y
   habría dejado un método `public` sin proteger en modo `ENFORCED`.

**Bloqueos encontrados:** ninguno. No hizo falta resolver ninguna `DP-nn` por cuenta propia.

**Fase siguiente generada:** ☑ [`docs/fases/fase-03.md`](fase-03.md) — con **DP-03** marcada como
bloqueante en su primer paso.
