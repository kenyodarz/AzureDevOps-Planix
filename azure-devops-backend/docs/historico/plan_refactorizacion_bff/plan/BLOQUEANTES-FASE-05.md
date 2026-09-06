# BLOQUEANTES — Fase 05

> **Estado:** 🔴 **LA FASE 05 NO ARRANCA HASTA CERRAR ESTE DOCUMENTO**
> **Fecha:** 2026-08-29 · **Fase anterior:** [
`RESULTADO-FASE-04.md`](../resultados/RESULTADO-FASE-04.md)
> **Para decidir:** 4 bloqueantes — **B-01**, **B-02**, **B-03**, **B-04**

---

## 0. Resumen ejecutivo

| #        | Bloqueante                                           | Recomendación                                     | Sin ella la Fase 05…                          |
|----------|------------------------------------------------------|---------------------------------------------------|-----------------------------------------------|
| **B-01** | ¿De dónde sale el año del `IterationPath`?           | **Opción C** — preguntar a Azure DevOps           | No puede tocar `IterationPath` (T-01, T-02)   |
| **B-02** | ¿Cuánto vale el lote de auditoría y quién manda?     | **Opción B** — configurable, valor por defecto 10 | No puede mudar la partición al dominio (T-03) |
| **B-03** | ¿Qué pasa con los sufijos `...Response` del dominio? | **Opción A por pasos** — tipos nuevos + DTOs      | No puede tocar `domain/model` (T-05, T-06)    |
| **B-04** | ¿Se levanta el «fuera de alcance» del MCP?           | **Sí, acotado a `resolveIterationPath`**          | **B-01 no tiene solución real**               |

> **B-04 es la llave.** Si se mantiene la restricción de `plan-maestro.md` §10, B-01 solo admite
> paliativos. Decidir B-04 primero ahorra discutir dos veces lo mismo.

---

## 1. 🔴 Hallazgo que reordena la discusión

Antes de las opciones, tres hechos **verificados en código**, no supuestos. Cambian lo que parecía
razonable en la Fase 04.

### H-A · La resolución de rutas está triplicada, y el MCP tiene el mismo bug

`azure-devops-mcp/.../mcp/tools/AzureDevOpsTools.java:168-179`

```java
private String resolveIterationPath(String project, String cleanSprint) {
    if (!cleanSprint.startsWith(project)) {
        if (cleanSprint.matches("Sprint \\d+")) {
            int currentYear = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).getYear();
            return project + '\\' + currentYear + '\\' + cleanSprint;   // ← el mismo fallo
        }
        return project + '\\' + cleanSprint;
    }
    return cleanSprint;
}
```

Es **el mismo código, carácter por carácter**, que `DevOpsDashboardUseCase.resolveSprint`. Y lo
mismo ocurre con `resolveAreaPath` (161-166) frente a `resolveCell`.

La lógica vive hoy en **tres sitios**:

1. El **BFF**, en `resolveCell` / `resolveSprint`.
2. El **MCP**, en `resolveAreaPath` / `resolveIterationPath`.
3. El **prompt**, que además le explica al agente cómo se forman las rutas.

**Consecuencia práctica:** hoy el BFF envía el path ya completo, el MCP comprueba
`startsWith(project)` → verdadero → lo devuelve intacto. **El trabajo del BFF es redundante.** Y
arreglar el año solo en el BFF no arregla nada: en cuanto alguien mande el nombre corto, el MCP
vuelve a calcularlo mal.

> Esto invalida la idea de «arreglamos el año en la Fase 05 dentro del BFF». **No se puede.**

### H-B · El patrón correcto ya existe en el MCP… pero solo para el área

En la misma clase, 25 líneas más arriba (`AzureDevOpsTools.java:135-142`):

```java
return getTeamFieldValuesUseCase.getTeamFieldValues(organization, project, teamNameParam)
        .

map(TeamFieldValues::getDefaultValue)
        .

onErrorResume(e ->{
        log.

warn("No se pudo obtener el AreaPath dinámico...");
            return Mono.

just(resolveAreaPath(project, cleanTeam));   // ← concatenar es el FALLBACK
        })
```

Para el **AreaPath**, el MCP **le pregunta a Azure DevOps** y solo concatena si la consulta falla.
Para el **IterationPath** nadie hizo lo equivalente.

> La opción «preguntar en vez de calcular» **no es un salto arquitectónico**: es replicar un patrón
> ya implementado, probado y en producción, en la misma clase. Eso abarata radicalmente la opción C
> de B-01.

### H-C · El frontend no sabe nada de iteraciones

`azure-devops-frontend/.../planning-dashboard.component.ts:34-62` — la célula y el sprint son **dos
inputs de texto libre**. No hay selector, no hay lista, no hay llamada que enumere sprints. Y en el
MCP **no existe** ninguna herramienta `listIterations` / `getTeamIterations`.

> Esto **descarta** la opción «que el front mande el `IterationPath` completo»: hoy el front no lo
> conoce, y para que lo conozca habría que crear la herramienta en el MCP igualmente. Es decir,
> **no evita** tocar el MCP; solo añade trabajo de frontend encima.

---

## 2. B-01 · ¿De dónde sale el año del `IterationPath`?

### Por qué importa

Un sprint que va de diciembre de 2025 a enero de 2026 **es de 2025**, porque así se llama su
`IterationPath`. `LocalDate.now().getYear()` lo busca en el año en curso, no lo encuentra, y Azure
DevOps devuelve una iteración vacía.

**El tablero sale sin ítems. Sin error, sin aviso, sin log.** El usuario no distingue «este sprint
no tiene historias» de «preguntamos por un sprint que no existe». Es el peor modo de fallo posible y
ocurre **cada enero**.

### Opciones

|       | Opción                                       | Coste               | Resuelve el fallo          | Toca otros repos     |
|-------|----------------------------------------------|---------------------|----------------------------|----------------------|
| **A** | Se sigue calculando; se documenta            | nulo                | ❌ No                      | No                   |
| **B** | Se configura (`azure-devops.iteration-year`) | bajo                | ❌ Falla por olvido humano | No                   |
| **C** | **Se pregunta a Azure DevOps**               | **medio** (ver H-B) | ✅ **Sí**                  | **Sí — MCP**         |
| **D** | Lo manda el frontend                         | alto                | ✅ Sí, pero…               | Sí — MCP **y** front |

**Por qué A no sirve:** documentar un fallo silencioso no lo hace menos silencioso. Si se elige A,
como mínimo hay que **fallar ruidosamente**: si la iteración devuelve cero ítems, decirlo.

**Por qué B es peor de lo que parece:** sustituye un fallo de calendario por un fallo de memoria
humana. Y en enero conviven sprints de dos años, así que un único valor global tampoco basta.

**Por qué D queda descartada:** por H-C. No ahorra tocar el MCP y añade trabajo de frontend y un
cambio de contrato.

### ✅ Recomendación: **Opción C**

Añadir en el MCP la resolución de iteración por consulta, **con la misma forma que ya tiene el
área**:

```java
// Simétrico a getTeamFieldValues, en la misma clase
return getTeamIterationsUseCase.resolve(organization, project, team, sprintName)
        .

map(Iteration::getPath)
        .

onErrorResume(e ->Mono.

just(resolveIterationPath(project, cleanSprint))); // fallback actual
```

Tres razones:

1. **El patrón ya existe** (H-B). No se inventa nada: se completa una simetría que quedó a medias.
2. **Es el único sitio donde el arreglo surte efecto.** Por H-A, arreglarlo en el BFF no sirve.
3. **Mantiene el fallback.** Si Azure DevOps no responde, el comportamiento es exactamente el de
   hoy:
   no se introduce ninguna regresión.

**Y en el BFF, como consecuencia:** `resolveCell` y `resolveSprint` **se eliminan**. El BFF pasa a
enviar el nombre corto (`"Sprint 247"`, `"EQU1096 - EXODIA"`) y deja de construir rutas de Azure
DevOps, que nunca fueron asunto suyo. Esto salda D-09 de verdad, no a medias.

> ⚠️ **Requiere B-04 en «sí».** Si B-04 se mantiene cerrado, la recomendación de repliegue es **A
> con fallo ruidoso**, y B queda registrada como deuda nueva.

---

## 3. B-02 · ¿Cuánto vale el lote de auditoría y quién manda?

### Hechos verificados

- El valor es `10`, hoy nombrado `AUDIT_BATCH_SIZE` en `Handler.java:46`. La Fase 04 **no lo
  cambió**.
- **No se encontró ningún límite técnico que lo justifique**: ni `maxWorkItems`, ni tamaño de
  página, ni límite de tokens en el agente o el MCP. Todo apunta a un número escrito a mano.
- El timeout del agente es **120 s** (`application.yaml:5`, `AGENT_TIMEOUT:120s`) **por llamada**.
- Los lotes se lanzan con `flatMapSequential` (`Handler.java:224`), que **ordena la salida pero
  ejecuta en paralelo**, con concurrencia por defecto de 256. Con 100 ítems son **10 llamadas
  simultáneas** al agente, y por tanto al LLM.
- `DashboardRoutesCharacterizationTest:229-250` fija que 25 ítems producen lotes de **10, 10 y 5**.

### El riesgo real no es el 10, es la concurrencia

Nadie limitó `flatMapSequential`. Un sprint grande dispara tantas llamadas concurrentes al LLM como
lotes haya. Bajar el tamaño de lote **empeora** esto: más lotes, más concurrencia.

### Opciones

|       | Opción                                     | Consecuencia                                              |
|-------|--------------------------------------------|-----------------------------------------------------------|
| **A** | Constante de dominio, sigue en 10          | La prueba de caracterización no se toca                   |
| **B** | **Propiedad configurable, por defecto 10** | La prueba no se toca; se puede afinar sin recompilar      |
| **C** | Cambiar el valor                           | **Rompe la prueba intocable** — habría que parametrizarla |

### ✅ Recomendación: **Opción B**, más un límite de concurrencia

1. `audit.batch-size` configurable, **valor por defecto 10**. Así la prueba de caracterización sigue
   verde sin tocarla, y el valor deja de requerir un despliegue para cambiarse.
2. **Acotar `flatMapSequential(..., N)`** con un tope explícito (propuesta: `2`). Es el cambio que
   de verdad protege al agente y al LLM.
3. Registrar **deuda nueva** para medir el coste real de un lote y ajustar ambos números con datos.

> No se recomienda cambiar el `10` ahora: no hay evidencia para elegir otro número, y cambiarlo
> obliga a tocar la única prueba que garantiza que la refactorización no rompió nada.

---

## 4. B-03 · Los sufijos `...Response` en el dominio

### Hechos verificados

- `DashboardResponse`, `DashboardMetricsResponse` y `DashboardStoryItemResponse` viven en
  `domain/model/dashboard/`.
- ArchUnit ya lo señala en cada build: *Rule_2.2: Domain classes should not be named with technology
  suffixes — violated (3 times)*. Hoy es **warning**; el propio mensaje avisa: *«This will cause a
  build error in future»*.
- Son **mutables** (`setQualityScore`, `setFeedback`…) y `Handler` los muta in-place dentro de la
  cadena reactiva. Ese es D-06, y **no se puede saldar sin resolver esto primero**.

### Opciones

|       | Opción                                                                 | Coste | Salda D-06         | Salda D-13        |
|-------|------------------------------------------------------------------------|-------|--------------------|-------------------|
| **A** | Tipos de dominio nuevos e inmutables + DTOs en `reactive-web` + mapper | alto  | ✅                 | ✅                |
| **B** | Renombrar en sitio (`DashboardResponse` → `BacklogAudit`)              | bajo  | ❌ Siguen mutables | 🟡 Solo el nombre |
| **C** | No tocar; silenciar la regla                                           | nulo  | ❌                 | ❌                |

### ✅ Recomendación: **Opción A, ejecutada por pasos**

B es cosmética: calla a ArchUnit y deja el problema real —la mutabilidad— intacto, que es
precisamente lo que impide saldar D-06. C aplaza un error de build anunciado.

Ahora bien, A completa en una sola fase es mucho. Propuesta de reparto:

- **Fase 05** — `BacklogAudit` y `StoryQuality` inmutables en el dominio, con el mapper hacia los
  actuales `...Response`, que se quedan en `reactive-web` como DTO de salida. Esto **sí** salda
  D-06.
- **Fase 06** — al rediseñar el flujo SSE, `Handler` deja de conocer los tipos de dominio y el mapeo
  queda en un solo punto.

---

## 5. 🔑 B-04 · ¿Se levanta el «fuera de alcance» del MCP?

### La restricción

`plan-maestro.md` §10, literal: *«Fuera de alcance: modificar `azure-devops-agent` y
`azure-devops-mcp`.»*

### Por qué hay que decidirla ahora

Por **H-A**, la resolución del `IterationPath` que causa el fallo del año **está en el MCP**.
Mientras §10 siga cerrada:

- **B-01 solo admite paliativos** (A o B).
- **D-09 no se puede saldar de verdad**: el BFF puede dejar de duplicar, pero la fuente sigue mal.
- **D-35** (llevar los prompts al MCP y al agente) queda bloqueada indefinidamente.

### Opciones

|       | Opción                  | Alcance                                                                 |
|-------|-------------------------|-------------------------------------------------------------------------|
| **A** | Se mantiene §10 cerrada | B-01 → paliativo. Deuda nueva registrada                                |
| **B** | **Se levanta acotada**  | Solo `AzureDevOpsTools.resolveIterationPath` + su caso de uso y pruebas |
| **C** | Se levanta del todo     | El MCP y el agente entran al plan; obliga a replanificar                |

### ✅ Recomendación: **Opción B — levantar acotada**

Una excepción **nominal y escrita**, no una apertura general:

> *Se autoriza modificar `azure-devops-mcp` **exclusivamente** para añadir la resolución de
> `IterationPath` por consulta a Azure DevOps, simétrica a la que ya existe para `AreaPath`,
> conservando la concatenación actual como fallback. Cualquier otro cambio en ese repositorio sigue
> fuera de alcance.*

Motivos: es un cambio **aditivo** (no borra el camino actual), **simétrico** a código que ya existe
y funciona, y de **superficie mínima**. C obliga a replanificar el plan maestro entero, que no es lo
que esta fase necesita.

---

## 6. Qué necesito de ti

Cuatro respuestas. Con eso cierro `fase-05.md` y arranco:

| #        | Pregunta                                                      | Recomendación                                               |
|----------|---------------------------------------------------------------|-------------------------------------------------------------|
| **B-04** | ¿Levanto §10 **acotada al `resolveIterationPath` del MCP**?   | **Sí (B)** ← *decidir primero*                              |
| **B-01** | ¿El año se pregunta a Azure DevOps?                           | **C** (requiere B-04 = sí) · si no, **A con fallo ruidoso** |
| **B-02** | ¿Lote configurable con 10 por defecto + tope de concurrencia? | **B + tope 2**                                              |
| **B-03** | ¿Tipos de dominio inmutables por pasos?                       | **A por pasos**                                             |

> Si prefieres, responde solo **B-04** y **B-01**: son los que se condicionan entre sí. B-02 y B-03
> no bloquean a los demás y puedo dejarlos escritos como propuesta firme para que los confirmes al
> revisar `fase-05.md`.

---

## 7. Lo que NO es bloqueante

Para acotar la conversación:

- **DP-04** (destino de los reportes `.md`) → es de la **Fase 06**.
- **DP-05** y **DP-06** → son de la **Fase 07**.
- **D-35** (prompts al MCP/agente) → requiere plan propio; **no** entra en la Fase 05 aunque B-04 se
  levante, porque la excepción propuesta es nominal y no la cubre.
- **D-12** (tres fachadas de logging) y **D-32** (`mcp-client` casi vacío) → **Fase 08**.

