# RESULTADO — FASE 01: Externalización de Prompts

> **Ejecutada:** 2026-08-28 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `3efe9a7` — `refactor(agent_prompts): externalizar plantillas de prompt a recursos`
> **Instrucciones:** `docs/fases/FASE-01-externalizacion-de-prompts.md`
>
> *Registro reconstruido a partir de la salida real de la ejecución.*

---

## 1. Objetivo de la fase

Sacar del dominio las ~171 líneas de plantillas de prompt (deuda **D-03**), eliminar el
`String.format` posicional y aplicar el criterio de estimación por horas aprobado en DP-06.

---

## 2. Qué se construyó

### Dominio (puro, sin Spring)

```
domain/model/src/main/java/co/com/bancolombia/model/prompt/
├── PromptTemplateId.java                    (enum, 5 plantillas)
├── gateways/PromptTemplatePort.java         (puerto: render(id, variables))
└── exceptions/PromptTemplateException.java  (excepción de dominio)
```

### Infraestructura (módulo Gradle nuevo)

```
infrastructure/driven-adapters/prompt-template/
├── build.gradle
└── .../prompttemplate/ClasspathPromptTemplateAdapter.java
```

Registrado en `settings.gradle` como `:prompt-template` y añadido a `app-service/build.gradle`.

### Recursos

```
applications/app-service/src/main/resources/prompts/
├── fase1-propuesta-inicial.md
├── fase2-historia-estructurada.md
├── division-historias.md
├── refinamiento-historia.md
├── auditoria-calidad.md
└── fragmentos/
    └── tabla-estimacion.md      <- fuente única de DP-06
```

---

## 3. Decisiones aplicadas

### DP-04 — Ruta y nombres aprobados

Se crearon **5** plantillas en lugar de las 6 propuestas: `creacion-estructurada.md` quedó
descartada por DP-07.

### DP-06 — Estimación por horas

Tabla oficial aportada por el usuario:

| Story Point | Esfuerzo (Horas) | Complejidad |
|---:|---|---|
| 1 | < 1 hora | Muy baja |
| 2 | 1 - 4 horas | Baja |
| 3 | 4 - 8 horas (1 día) | Baja |
| 5 | 8 - 24 horas (1-3 días) | Media |
| 8 | 24 - 30 horas (1 semana) | Media - Alta |
| 13 | > 30 horas (> 1 semana) | Alta |

**Decisión de diseño:** la tabla vive en **un único fragmento compartido** que el adaptador inyecta
automáticamente en las 4 plantillas que estiman. Modificar un rango exige tocar un solo archivo, y
las 4 plantillas quedan alineadas por construcción — incluida la de auditoría.

**Sub-decisión aplicada:** se eliminó la «Regla de Mínimos de 5 puntos». Con la tabla, 5 SP = 8-24
horas, por lo que la regla afirmaba que «publicar un CRUD nunca puede costar menos de 8 horas»,
contradiciendo el criterio por horas y el ejemplo de `HISTORIA_USUARIO.md:73` («2 = publicar un
nuevo endpoint»). Además el prompt de auditoría ya afirmaba lo contrario: **el agente creaba con una
regla y auditaba con la opuesta**.

> ⚠️ Pendiente de ratificación explícita del usuario. Revertir es reintroducir el párrafo en
> `prompts/fase2-historia-estructurada.md`.

### DP-07 — Eliminación de código inalcanzable

Eliminados `buildPrompt()` y su plantilla inline de creación estructurada. La rama `else` de
`handleSpecialCommands()` envía ahora el texto del usuario directamente (la validación de contenido
vacío ya ocurría antes, en `executeChat()`).

---

## 4. Mejoras colaterales

| Antes | Después |
|---|---|
| `String.format` posicional; `AUDIT_QUALITY_PROMPT_TEMPLATE` recibía `workItemId` **3 veces** en posiciones distintas | Sustitución **por nombre**: `{{workItemId}}` |
| Tabla de porcentajes con escapado `%%` | `%` normal. Hay un test que verifica que `%%` no reaparezca |
| Cambiar una palabra de un prompt exigía recompilar | Es editar un `.md` |
| Un marcador sin resolver llegaba al LLM | El adaptador **aborta** con excepción de dominio antes de enviarlo |

---

## 5. Métricas obtenidas

| Métrica | Antes | Después |
|---|---:|---:|
| Líneas `AgentChatUseCase` | 644 | **505** (−139) |
| Constantes de prompt en el dominio | 5 | **0** |
| Tests totales del proyecto | 46 | **69** |
| Cobertura `prompt-template` | n/a | **94,2%** (195/207) |
| Cobertura `domain/usecase` | 81,7% | 81,5% |
| Argumentos del constructor | 9 | **10** (temporal, `// TODO Fase 05`) |

### Suites tras la fase

| Suite | Tests | Fallos |
|---|---:|---:|
| `AgentChatUseCase - Caracterización del enrutamiento` | 16 | 0 |
| `AgentChatUseCase - Caracterización del parseo` | 7 | 0 |
| `ClasspathPromptTemplateAdapter` | 10 | 0 |
| `Contenido real de las plantillas de prompt` | 7 | 0 |
| `ArchitectureTest` | 6 | 0 |
| `RouterRestTest` | 10 | 0 |
| `JsonRpcErrorContractTest` | 5 | 0 |
| Resto (legacy toggle/sunset, MCP, métricas, config) | 8 | 0 |
| **Total** | **69** | **0** |

---

## 6. Comandos ejecutados

```powershell
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat jacocoMergedReport --no-configuration-cache
```

**Resultado:** `BUILD SUCCESSFUL` · 69/69 pruebas en verde.

---

## 7. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **No se hizo el test de equivalencia byte a byte** | Al aplicar DP-06, el contenido de 4 de las 5 plantillas cambia por decisión de negocio; una comparación literal contra las constantes originales carecía de sentido. Se sustituyó por `PromptTemplateContentTest` (7 pruebas): integridad estructural (ningún marcador sin resolver) y contenido normativo (tabla presente, «Regla de Mínimos» ausente, umbral de 8 anunciado, `%%` ausente) |
| **Sí se modificaron los tests de la Fase 00** | Inevitable: añadir una dependencia al constructor rompe su compilación. Se aprovechó para cambiar la identificación de flujo, de buscar subcadenas del prompt a comparar el `PromptTemplateId` solicitado — criterio equivalente pero más preciso. **Los 16 + 7 casos y sus aserciones de comportamiento se conservan íntegros** |
| **Los recursos de prueba del adaptador son sintéticos** | Las plantillas reales viven en `app-service` (por DP-04), no en el módulo del adaptador. Se usan recursos propios en `src/test/resources` para probar la mecánica, y `PromptTemplateContentTest` en `app-service` valida el contenido real |
| **`AgentChatUseCase` quedó en 505 líneas, no en ~470** | Se añadieron 8 constantes `VAR_*` con los nombres de las variables de plantilla, para evitar literales de cadena repartidos por el código |

---

## 8. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| Estructura de paquetes respetada | ✅ |
| `domain/model` sin Spring, persistencia ni serialización | ✅ |
| `domain/usecase` sin anotaciones de Spring | ✅ |
| Carga técnica exclusivamente en `driven-adapters` | ✅ `ClassPathResource` solo en el adaptador |
| Excepciones técnicas traducidas a dominio | ✅ `IOException` → `PromptTemplateException` |
| Sin secretos hardcodeados (S2068) | ✅ |
| `Objects.requireNonNull` en las entradas del puerto (S2259) | ✅ |
| Complejidad cognitiva ≤ 15 | ✅ |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ |
| Tests GIVEN/WHEN/THEN | ✅ |
| Build verde | ✅ |

---

## 9. Detalles de implementación destacables

- **Caché al arranque:** las 5 plantillas y el fragmento se leen una única vez en el constructor del
  bean y se guardan en un `Map` inmutable. Nunca se lee del classpath durante una petición.
- **Validación previa a la sustitución:** los marcadores requeridos se extraen de la plantilla
  *original* y se comparan contra las variables disponibles. Verificar sobre el resultado habría
  producido falsos positivos si el contenido inyectado contuviera llaves dobles.
- **Sustitución literal:** se usa `String.replace(CharSequence, CharSequence)`, no `replaceAll`, de
  modo que valores con `$`, `\` o `{}` se insertan tal cual. Hay una prueba específica para ello.

---

## 10. Estado al cerrar

- **Siguiente fase generada:** `FASE-02-value-objects-de-estimacion.md`
- **Decisiones abiertas:** DP-03 (bloquea Fase 02), DP-05 (bloquea Fase 07)
- **Pendiente de ratificación:** eliminación de la «Regla de Mínimos de 5 puntos»

