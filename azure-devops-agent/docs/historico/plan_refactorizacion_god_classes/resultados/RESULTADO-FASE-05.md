# RESULTADO — FASE 05: Value Objects de Configuración

> **Ejecutada:** 2026-08-29 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(agent_config): agrupar configuracion en objetos de valor`
> **Instrucciones:** `docs/fases/FASE-05-value-objects-de-configuracion.md`

---

## 1. Objetivo de la fase

Dar tipo y validación a la configuración que viajaba como `String` sueltos por los constructores de
cuatro handlers (**D-03**), y eliminar la lectura repetida de recursos del classpath en el arranque.

---

## 2. Qué se construyó

### Dominio (puro, sin Spring)

```
domain/model/src/main/java/co/com/bancolombia/model/agent/
├── AzureDevOpsScope.java       (record: organization + project, ambos validados)
└── CorporateKnowledge.java     (record: storyTemplate + agileGuide + qualityStandards
                                 + auditStandards())

domain/model/src/test/java/co/com/bancolombia/model/agent/
├── AzureDevOpsScopeTest.java      (8 pruebas)
└── CorporateKnowledgeTest.java    (11 pruebas)
```

### Handlers

| Handler | Antes | Después |
|---|---|---|
| `QualityAuditFlowHandler` | 4 `String` | `AzureDevOpsScope` + `CorporateKnowledge` |
| `RefinementFlowHandler` | 3 `String` | `AzureDevOpsScope` + `CorporateKnowledge` |
| `ApprovalFlowHandler` | 2 `String` | `CorporateKnowledge` |
| `DivisionFlowHandler` | 1 `String` | `CorporateKnowledge` |
| `GeneralFlowHandler` / `PlanningDraftFlowHandler` | sin configuración | **sin cambios** |

### Prueba de cableado (añadida, ver §6)

```
applications/app-service/src/test/java/co/com/bancolombia/config/
└── UseCasesConfigWiringTest.java   (2 pruebas: el contexto real arranca)
```

---

## 3. Deudas saldadas

| ID | Deuda | Cómo se resolvió |
|---|---|---|
| **D-03** | Configuración como `String` sueltos, sin tipo ni validación | Dos *records* inmutables que se validan a sí mismos |
| **Hallazgo Fase 04** | `HISTORIA_USUARIO.md` se leía **4 veces** al arrancar | Un bean `CorporateKnowledge` lee cada recurso **una sola vez** |

### El defecto que ahora impide el compilador

Antes, esto compilaba sin protestar y solo fallaba al llamar a Azure DevOps:

```java
new QualityAuditFlowHandler(gw, port, project, org, guide, standards);  // ← invertidos
```

Ahora el parámetro es un `AzureDevOpsScope` y el error es imposible en el sitio de llamada. Además,
`new AzureDevOpsScope("", "proyecto")` lanza `IllegalArgumentException` al construirse.

### Conocimiento de negocio devuelto al dominio

`QualityAuditFlowHandler` concatenaba en línea `agileGuide + "\n\n" + qualityStandards`. Qué
documentos forman el criterio de auditoría es conocimiento de negocio, no detalle de un flujo: pasó
a `CorporateKnowledge.auditStandards()`.

---

## 4. Métricas obtenidas

| Métrica | Antes (Fase 04) | Después |
|---|---:|---:|
| `String` de configuración en constructores de handlers | 10 | **0** |
| Lecturas de recursos del classpath al arrancar | 6 | **3** (una por archivo) |
| Argumentos del constructor de `AgentChatUseCase` | 4 | 4 |
| Líneas `AgentChatUseCase` | 89 | 89 |
| Tests totales del proyecto | 186 | **207** |
| Cobertura `AzureDevOpsScope` | n/a | **100%** (29/29) |
| Cobertura `CorporateKnowledge` | n/a | **100%** (39/39) |
| Cobertura `domain/model` | 95,8% | **96,1%** (838/872) |
| Cobertura `domain/usecase` | 95,4% | **95,4%** (624/654) |

---

## 5. Validación de los criterios de aceptación

> El usuario pidió **validar explícitamente** el cumplimiento antes de pasar a la Fase 06. Cada
> criterio se comprobó con una orden concreta, no por inspección visual.

| # | Criterio | Cómo se verificó | Resultado |
|---|---|---|---|
| 1 | `AzureDevOpsScope` y `CorporateKnowledge` creados con validación | `:model:test` | ✅ 19 pruebas verdes |
| 2 | Ningún handler recibe `String` de configuración sueltos | `Select-String "private final String"` sobre `handler/*.java` | ✅ **cero coincidencias** |
| 3 | Cada recurso del classpath se lee una sola vez | `Select-String "classpath:"` en `UseCasesConfig` | ✅ **3 apariciones**, una por archivo |
| 4 | `auditStandards()` vive en el dominio | Prueba `givenKnowledge_whenAuditStandards_thenCombinesBothDocuments` | ✅ |
| 5 | Las 24 pruebas de caracterización verdes, sin cambiar aserciones | `:usecase:test` + revisión del diff | ✅ solo cambió el ensamblado en `setUp()` |
| 6 | Cobertura de clases nuevas ≥ 90% | `jacocoMergedReport` | ✅ **100%** en ambas |
| 7 | `.\gradlew.bat build` verde | Ejecutado | ✅ `BUILD SUCCESSFUL` |
| 8 | **El contexto de Spring arranca** | `UseCasesConfigWiringTest` (nueva) | ✅ 2/2 verdes |

### Sobre el criterio 8

`UseCasesConfigTest`, que existía desde el andamiaje inicial, **captura y descarta**
`UnsatisfiedDependencyException`:

```java
} catch (org.springframework.beans.factory.UnsatisfiedDependencyException e) {
    assertTrue(true, "Unsatisfied dependencies are expected...");
}
```

Es decir: **un cableado roto habría pasado como prueba verde**. Las fases 04 y 05 cambiaron el
wiring por completo y nada lo estaba verificando. `UseCasesConfigWiringTest` levanta el contexto
real con los puertos de infraestructura simulados y comprueba que:

- existen `AgentChatUseCase` y `ChatFlowDispatcher`;
- hay **exactamente un handler por cada valor de `AgentIntent`**;
- `AzureDevOpsScope` y `CorporateKnowledge` se construyen con el contenido **real** de los `.md`.

---

## 6. Comandos ejecutados

```powershell
.\gradlew.bat :model:test                       # 19 pruebas nuevas verdes antes de tocar handlers
.\gradlew.bat :usecase:test                     # verde; caracterización intacta
.\gradlew.bat :app-service:test --tests "*UseCasesConfigWiringTest*"   # 2/2 verdes
.\gradlew.bat build                             # BUILD SUCCESSFUL
.\gradlew.bat jacocoMergedReport --no-configuration-cache
```

**Resultado:** `BUILD SUCCESSFUL` · **207/207** pruebas en verde.

---

## 7. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **Se creó `UseCasesConfigWiringTest`**, no previsto en la lista de archivos | Al llegar al criterio «el contexto de Spring arranca» se descubrió que ninguna prueba lo verificaba: `UseCasesConfigTest` se traga la excepción de cableado. Sin esta prueba, el criterio no podía darse por cumplido, solo por supuesto |
| **`UseCasesConfigTest` se conservó sin tocar** | Es del andamiaje de Bancolombia. Sustituirlo excede el alcance; se complementa y se anota como hallazgo para la Fase 07 |
| **`CorporateKnowledge` agrupa los tres documentos**, aunque `ApprovalFlowHandler` solo use dos y `DivisionFlowHandler` uno | Dividirlo en tres Value Objects devolvería el problema original: constructores con varios parámetros del mismo tipo. Los tres documentos son una unidad conceptual —la documentación corporativa— y se cargan juntos |
| **Se actualizó el Javadoc de `@InjectMocks`** en la prueba de caracterización | Lo pedía el Paso E. La conclusión cambió: ya no es una limitación por ambigüedad de `String`, sino una decisión, porque el `ChatFlowDispatcher` debe ser **real** para que la suite caracterice el enrutamiento |

---

## 8. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| `domain/model` sin Spring, persistencia ni serialización | ✅ los dos *records* solo usan `java.lang` |
| `domain/usecase` sin anotaciones de Spring | ✅ |
| Value Objects inmutables con validación en el constructor compacto | ✅ |
| Modelo rico, no anémico | ✅ `auditStandards()` es comportamiento, no un getter |
| Sin secretos hardcodeados (S2068) | ✅ |
| `Optional<T>` en retornos opcionales (S2259) | ✅ |
| S107 (listas largas de parámetros) | ✅ el peor constructor pasa de 6 a 4 parámetros, todos de tipo distinto |
| Complejidad cognitiva ≤ 15 | ✅ |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ |
| Tests GIVEN/WHEN/THEN | ✅ 21 pruebas nuevas |
| Build verde | ✅ |

---

## 9. Hallazgos

| Hallazgo | Atender en |
|---|---|
| **`UseCasesConfigTest` (del andamiaje) descarta `UnsatisfiedDependencyException`: un cableado roto pasaría como prueba verde.** Mitigado con `UseCasesConfigWiringTest`, pero el test original sigue dando una señal falsa | Fase 07 |
| `AgentChatUseCase` sigue en 75,9% de cobertura: lo no cubierto es el camino `chat()` asíncrono contra `NoOpAgentResponseAdapter` | Fase 07 (DP-05) |
| `ArchitectureTest` mantiene sus 2 advertencias preexistentes (`Rule_2.7` ×5, `Rule_2.2` ×1). Las clases nuevas **no** añaden violaciones | Fase 07 |
| `Handler.java` (495 líneas) es la **única** clase de más de 300 líneas que queda en el proyecto | Fase 06 |
| Los ~15 modelos A2A de `domain/model` siguen sin pruebas propias | Fase 07 |

---

## 10. Estado al cerrar

- **Siguiente fase generada:** `FASE-06-separacion-del-entry-point.md`
- **Decisiones abiertas:** DP-05 (bloquea Fase 07)
- **Pendiente de ratificación:**
  1. Eliminación de la «Regla de Mínimos de 5 puntos» (Fase 01).
  2. Regla del pronombre relativo en las palabras clave genéricas (Fase 03).
- **Verificación clave:** los 8 criterios de aceptación se comprobaron **uno a uno con evidencia
  ejecutable**, no por inspección. El octavo obligó a crear la prueba que faltaba.

