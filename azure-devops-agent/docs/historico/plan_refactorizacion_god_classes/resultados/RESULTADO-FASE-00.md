# RESULTADO — FASE 00: Baseline y Caracterización

> **Ejecutada:** 2026-08-28 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `07f9419` — `test(agent_chat): agregar pruebas de caracterizacion del enrutamiento de flujos`
> **Instrucciones:** `docs/fases/FASE-00-baseline-y-caracterizacion.md`
>
> *Registro reconstruido a partir de la salida real de la ejecución.*

---

## 1. Objetivo de la fase

Congelar el comportamiento actual de `AgentChatUseCase` (644 líneas, sin un solo test) mediante
pruebas de caracterización, antes de iniciar cualquier refactorización. Establecer las métricas
baseline.

---

## 2. Qué se hizo

| Acción | Detalle |
|---|---|
| Test de enrutamiento | `AgentChatUseCaseCharacterizationTest` — **16 pruebas** |
| Test de parseo | `AgentChatUseCaseParsingTest` — **7 pruebas** |
| Dependencias de test | **No fue necesario** modificar `build.gradle`: `spring-boot-starter-test` y `reactor-test` ya se heredan de `main.gradle` |
| Código productivo | **Cero líneas modificadas** |

### Cobertura de casos

| Flujo / caso | Pruebas |
|---|---:|
| General (contextId, palabras clave) | 3 |
| Auditoría de calidad | 1 |
| Refinamiento | 1 |
| Planificación RAG (incluido fallo del vector store) | 2 |
| Aprobación (incluida normalización de tildes) | 2 |
| División | 1 |
| Casos borde (texto vacío, mensaje nulo, error del LLM) | 3 |
| Defectos conocidos (DP-01 ×2, DP-07 ×1) | 3 |
| Parseo y post-procesado de la respuesta del LLM | 7 |
| **Total** | **23** |

---

## 3. Métricas obtenidas

| Métrica | Antes | Después |
|---|---:|---:|
| Tests en `domain/usecase` | 0 | **23** |
| Cobertura `domain/usecase` (instrucciones) | 0% | **81,7%** (884/1082) |
| Mutation score (pitest) | n/d | 62% (72/116) — test strength 81% |
| Líneas `AgentChatUseCase` | 644 | 644 (sin cambios, por diseño) |

---

## 4. Comandos ejecutados

```powershell
.\gradlew.bat :usecase:test --tests "co.com.bancolombia.usecase.chat.*"
.\gradlew.bat build
.\gradlew.bat jacocoMergedReport --no-configuration-cache
git --no-pager status --porcelain -- azure-devops-agent
```

**Resultado:** `BUILD SUCCESSFUL` · 23/23 pruebas en verde · `git status` confirmó cero cambios en
`src/main`.

---

## 5. Decisiones aplicadas

| ID | Resolución del usuario | Efecto en esta fase |
|---|---|---|
| **DP-01** | Corregir la precedencia de intención | Las pruebas 15 y 16 capturan el comportamiento **defectuoso actual**, con Javadoc avisando de que se invertirán en la Fase 03 |
| **DP-02** | Umbral de división `> 8` (fuente: `HISTORIA_USUARIO.md:71` «>8 Requiere Dividir») | Se añadió la prueba de 9 puntos, que hoy **no** dispara alerta |

---

## 6. Hallazgos

### DP-07 — Código inalcanzable (hallazgo nuevo)

Al trazar el enrutamiento se descubrió que la creación estructurada de HU con tareas hijas **nunca
puede ejecutarse**:

```text
"CREATE_STRUCTURED_USER_STORY"  ->  28 caracteres
shouldSearchPlanning()          ->  ¿< 25 chars? NO  ->  TRUE
                                ->  runPlanningSimilarityFlow()  [gana siempre]
buildPrompt()                   ->  INALCANZABLE
```

Todo el bloque `createWorkItem` + `updateWorkItem` con jerarquía
`System.LinkTypes.Hierarchy-Reverse` estaba escrito pero muerto.

### Otros hallazgos técnicos

| Hallazgo | Atender en |
|---|---|
| ArchUnit `Rule_2.7` violada 5 veces. Advertencia hoy; *«This will cause a build error in future»* | Fase 07 |
| ArchUnit `Rule_2.2` violada 1 vez | Fase 07 |
| `jacocoMergedReport` incompatible con la configuration cache por el plugin `pitest`. Workaround: `--no-configuration-cache` | Fase 07 |
| `domain/model` sin ningún test (0% cobertura) | Fase 02 / 07 |

---

## 7. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| `domain/model` sin dependencias técnicas | ✅ |
| `domain/usecase` sin anotaciones de Spring | ✅ |
| Sin secretos hardcodeados (S2068) | ✅ |
| Complejidad cognitiva ≤ 15 | ✅ |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ constantes en los tests |
| Nombres `givenX_whenY_thenZ` | ✅ |
| Build verde | ✅ |

---

## 8. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| No se usó `@InjectMocks` | El constructor recibe 9 argumentos con 4 `String` consecutivos: la inyección de Mockito es ambigua y frágil. Se instancia manualmente en `@BeforeEach`, documentado con Javadoc. Desaparece en la Fase 05 |
| Se añadió una prueba no prevista (9 puntos) | Consecuencia directa de la resolución de DP-02 |

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `FASE-01-externalizacion-de-prompts.md` (bloqueada por DP-04 y DP-06)
- **Decisiones abiertas:** DP-03, DP-04, DP-05, DP-06, DP-07

