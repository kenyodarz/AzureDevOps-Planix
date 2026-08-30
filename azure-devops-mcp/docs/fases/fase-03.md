# FASE 03 — Frontera de contrato: DTOs y mappers

> **Plan:** [`docs/plan/plan-maestro.md`](../plan/plan-maestro.md) · **Estado:** ⚪ PENDIENTE
> **Deudas que ataca:** D-11, D-17 · **Decisión bloqueante:** ⛔ **DP-03 (ABIERTA)**
> **Riesgo:** Medio · **Depende de:** Fases 01 y 02 (cerradas) · **Habilita:** Fases 04 a 08
> **Regla de oro de esta fase:** **ningún nombre de campo del cable cambia.** La clase puede
> renombrarse; el JSON, no.

---

## 0. ⛔ PASO BLOQUEANTE — DP-03

**No se escribe una sola línea de código antes de que el propietario responda.** Esta fase toca
justamente los tipos que **Jackson deserializa desde el payload MCP**: equivocarse aquí **rompe al
agente y al BFF en silencio**, porque MCP no valida esquemas al vuelo y el fallo no aparece hasta que
un campo llega vacío.

**La pregunta de DP-03, en concreto:**

> **¿Se permite renombrar tipos de `domain/model`?** Sacar `WorkItemsBatchRequest` del dominio cierra
> **D-17** (la única violación de ArchUnit `Rule_2.2`), pero es un tipo que Jackson construye desde
> el payload MCP.

Lo que hay que decidir, desglosado:

| # | Cuestión                                                                                          | Opciones                                                                                      |
|---|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| 1 | ¿El DTO que sustituya a `WorkItemsBatchRequest` **conserva el nombre de clase** en el entry-point? | (a) sí, mismo nombre · (b) se renombra a `WorkItemsBatchInput`/similar · (c) desaparece del entry-point |
| 2 | ¿Se autoriza **renombrar la clase de dominio** para cerrar `Rule_2.2`?                            | (a) sí · (b) no, se añade una excepción a la regla · (c) el tipo sale del dominio por completo |
| 3 | ¿`JsonPatchOperation` se duplica en DTO de entrada **y** DTO de salida, o basta uno compartido?    | (a) dos DTOs, uno por frontera · (b) uno compartido en el entry-point                          |

> ✅ **Lo que ya está cerrado y NO se vuelve a preguntar:** los **nombres de campo** del cable son
> intocables. `CONTRATO-MCP.md` §3 es explícito: `op`, `path`, `value`, `from` de
> `JsonPatchOperation`, e `ids`, `fields`, `expand`, `errorPolicy` de `WorkItemsBatchRequest`
> **deben conservarse exactamente**. Cambiar el nombre de la **clase** puede ser seguro; cambiar el
> de un **campo** no lo es nunca.

Conforme a `spring-rules.md` §6 y al §8.5 del plan maestro: si DP-03 no se resuelve, **la fase se
detiene, se documenta el bloqueo en su §4 y se pasa a la Fase 04**, que no depende técnicamente de
ésta.

---

## 1. Contexto

### 1.1 Estado real del sistema al arrancar esta fase

Medido al cerrar la Fase 02 (2026-08-30), no estimado:

| Dato                                        | Valor                                                        |
|---------------------------------------------|--------------------------------------------------------------|
| Build                                       | 🟢 **VERDE** — **67 pruebas, 0 fallos**                      |
| Cobertura `domain/model`                    | **0 %** *(el módulo no tiene `src/test`, D-26)*              |
| Cobertura `domain/usecase`                  | **68,2 %**                                                   |
| Cobertura `mcp-server`                      | **70,2 %**                                                   |
| Cobertura `rest-consumer`                   | **90,0 %**                                                   |
| Cobertura `app-service`                     | **48,4 %**                                                   |
| Sentencia WIQL                              | **congelada carácter a carácter**, 8 ramas                   |
| Contrato MCP                                | inventariado y actualizado en [`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) |
| ArchUnit `Rule_2.2`                         | **1 violación real**, en *warning*, **no exportada a Sonar** (D-25) |

Documentos de referencia: [`BASELINE.md`](../resultados/BASELINE.md) ·
[`CONTRATO-MCP.md`](../resultados/CONTRATO-MCP.md) ·
[`ACTIVACION-SEGURIDAD.md`](../resultados/ACTIVACION-SEGURIDAD.md)

### 1.2 Lo que dejó hecho la Fase 02 y afecta a ésta

- ✅ **D-01 a D-04 saldadas.** La postura de acceso es configuración (`mcp.security.mode`), no
  código. **No la toques**: sigue en `PERMISSIVE` por defecto y así debe quedarse (DP-01).
- ✅ **Las 8 tools llevan `@PreAuthorize`**, con los roles centralizados en
  `mcp-server/.../mcp/security/McpRoles.java`. Si esta fase mueve firmas de método, **las
  anotaciones se mueven con ellas** y `McpToolsAuthorizationTest` debe seguir en verde.
- ✅ **19 pruebas nuevas** que congelan el comportamiento de seguridad. Son ahora parte del
  patrimonio heredado: **no se modifican**.
- 🔸 **`spring.devtools.add-properties` se conservó** deliberadamente: su dependencia existe.

### 1.3 El problema que esta fase resuelve

`CONTRATO-MCP.md` §3 lo enuncia sin ambigüedad: **el dominio es el contrato de cable en los dos
extremos**.

```java
// ENTRADA — Jackson construye un modelo de DOMINIO desde el payload MCP
@McpToolParam(...) List<JsonPatchOperation> patch      // AzureDevOpsTools

// SALIDA — el WebClient serializa el modelo de DOMINIO tal cual
.bodyValue(patch)     // RestConsumer → List<JsonPatchOperation>
.bodyValue(request)   // RestConsumer → WorkItemsBatchRequest
.bodyValue(query)     // RestConsumer → WiqlQuery
```

Tres consecuencias, todas medibles:

1. **Cualquier cambio en el JSON de Azure DevOps rompe el dominio.** El acoplamiento es directo y no
   hay ninguna capa donde absorberlo.
2. **`spring-rules.md` declara los mappers obligatorios** en `driven-adapters`, y aquí hay **cero**
   en la frontera de salida.
3. **`WorkItemsBatchRequest` viola `Rule_2.2`** por llamarse como se llama, y su nombre delata
   exactamente el problema: es un *request* HTTP viviendo en el dominio.

### 1.4 Lo que esta fase deliberadamente NO hace

- **No toca la seguridad** (Fase 02, cerrada). Ni el modo, ni las anotaciones, ni el token.
- **No mueve el WIQL, las rutas ni la normalización de tipos**: eso es D-07/D-08/D-09, **Fase 04**.
- **No parte `RestConsumer`** en adaptadores: es D-10, **Fase 05**.
- **No traduce errores** ni toca cortacircuitos: D-13, D-06, **Fase 06**.
- **No convierte el modelo en Value Objects inmutables** ni retira los `@Setter`: D-16, **Fase 07**.
- **No arregla el informe de ArchUnit para Sonar** (D-25) ni crea `src/test` en `domain/model`
  (D-26): **Fases 07 y 08**. Ojo: por D-25, **el único modo fiable de verificar `Rule_2.2` es leer
  el log del test**, no el `issues.json`.

---

## 2. Instrucciones

> Todas condicionadas al desenlace de **DP-03**.

### T-01 · DTOs de entrada en el entry-point (D-11)

**Ficheros:** `mcp-server/.../mcp/dto/` *(nuevo)* · `AzureDevOpsTools.java`

1. Crear los DTOs de entrada que hoy son modelos de dominio, **conservando literalmente los nombres
   de campo** (`op`, `path`, `value`, `from`; `ids`, `fields`, `expand`, `errorPolicy`).
2. Sustituir los tipos de `@McpToolParam` por los DTOs.
3. Los `@PreAuthorize` de la Fase 02 **acompañan al método**: ninguno se pierde por el camino.

**Validación:** `AzureDevOpsToolsTest`, `WiqlCharacterizationTest` y `McpToolsAuthorizationTest`
siguen en verde **sin modificarse**.

### T-02 · Mapper de la frontera de entrada

**Ficheros:** `mcp-server/.../mcp/dto/McpToolDtoMapper.java` *(nuevo)*

Mapper estático DTO → dominio. Sin Spring, sin lógica de negocio: solo traducción.

### T-03 · DTOs y mappers de la frontera de salida (D-11)

**Ficheros:** `rest-consumer/.../consumer/dto/` y `.../consumer/mapper/` *(nuevos)* ·
`RestConsumer.java`

1. DTOs propios del adaptador para los cuerpos que hoy serializa el `WebClient` desde el dominio.
2. Mappers dominio → DTO (salida) y DTO → dominio (respuesta), **obligatorios** según
   `spring-rules.md`.
3. **Sin cambiar ni una URL, ni un parámetro de consulta, ni la forma del JSON enviado.**

**Validación:** `RestConsumerTest` (11 pruebas) sigue en verde **sin modificarse**. Es la red de
seguridad: compara peticiones reales contra `MockWebServer`.

### T-04 · Cerrar `Rule_2.2` (D-17)

Según lo que decida **DP-03**: renombrar el tipo de dominio, sacarlo del dominio, o documentar la
excepción. **La regla debe quedar a cero violaciones reales**, verificado **en el log** del
`ArchitectureTest` (por D-25, el `issues.json` no sirve como prueba).

### T-05 · Actualizar documentación y generar la Fase 04

1. `CONTRATO-MCP.md` §3: dejar constancia de que el dominio **ya no es** el contrato de cable, y de
   qué nombre de clase quedó en cada frontera.
2. Plan maestro: §3 (D-11, D-17), §6 (métricas), §9 (bitácora) y cabecera.
3. Rellenar §4 de este documento y marcar el checklist.
4. **Generar `docs/fases/fase-04.md`** con **DP-04** marcada como bloqueante en su primer paso.
5. Commit: `refactor(mcp_contract): separar los dto de cable de los modelos de dominio`

---

## 3. Orden de Ejecución

- [ ] **0.** ⛔ **Plantear DP-03 al propietario y esperar respuesta.** No continuar sin ella.
- [ ] **1.** Releer `CONTRATO-MCP.md` §2 y §3 completos: son la lista de lo que no puede cambiar.
- [ ] **2.** Inventariar cada punto donde un tipo de dominio cruza una frontera (entrada y salida).
- [ ] **3.** Crear los DTOs de entrada en `mcp-server`, con los nombres de campo intactos. **(T-01)**
- [ ] **4.** Crear `McpToolDtoMapper` y enchufarlo en las tools. **(T-02)**
- [ ] **5.** `.\gradlew.bat build` verde. Las 67 pruebas heredadas, **sin modificar**.
- [ ] **6.** Crear los DTOs de salida y sus mappers en `rest-consumer`. **(T-03)**
- [ ] **7.** Verificar con `RestConsumerTest` que el JSON enviado es **byte a byte el mismo**. **(T-03)**
- [ ] **8.** Aplicar lo que decida DP-03 sobre `WorkItemsBatchRequest`. **(T-04)**
- [ ] **9.** Comprobar en el **log** del `ArchitectureTest` que `Rule_2.2` está a **0**. **(T-04)**
- [ ] **10.** `.\gradlew.bat build` verde de nuevo.
- [ ] **11.** Actualizar `CONTRATO-MCP.md` §3 y el plan maestro (§3, §6, §9, cabecera). **(T-05)**
- [ ] **12.** Rellenar **Resultado** en §4 y marcar este checklist. **(T-05)**
- [ ] **13.** **Generar `docs/fases/fase-04.md`** con DP-04 bloqueante. **(Regla de Continuidad)**
- [ ] **14.** Commit: `refactor(mcp_contract): separar los dto de cable de los modelos de dominio`

---

## 4. Resultado

> Rellenar **al cerrar la fase**, con lo alcanzado de verdad.

| Métrica                                                 |   Antes | Después |
|---------------------------------------------------------|--------:|--------:|
| Modelos de dominio deserializados como `@McpToolParam`   |   **2** |       — |
| Modelos de dominio serializados por el `WebClient`       |   **3** |       — |
| Mappers en la frontera de salida                         |   **0** |       — |
| Mappers en la frontera de entrada                        |   **0** |       — |
| ArchUnit `Rule_2.2` (violaciones reales, según el log)   |   **1** |       — |
| Nombres de campo del cable modificados                   |     n/a |  **0** |
| Pruebas totales                                          |  **67** |       — |
| Pruebas heredadas **modificadas**                        |     n/a |  **0** |

**Decisión DP-03:** *(pendiente)*
**Cambios en el contrato MCP público:** *(pendiente — debe ser «ninguno»)*
**Bloqueos encontrados:** *(pendiente)*
**Fase siguiente generada:** ☐ `docs/fases/fase-04.md`

