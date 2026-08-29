# RESULTADO — FASE 04: Separación de Flujos (Strategy)

> **Ejecutada:** 2026-08-28 · **Estado final:** 🟢 COMPLETADA
> **Commit:** `refactor(agent_chat): separar flujos de conversacion con patron strategy`
> **Instrucciones:** `docs/fases/FASE-04-separacion-de-flujos.md`

---

## 1. Objetivo de la fase

Sacar los seis flujos de conversación de `AgentChatUseCase` (violación de SRP) y unificar los
**seis bloques `onErrorResume` idénticos** en uno solo (**D-02**).

---

## 2. Qué se construyó

### Dominio — nuevo paquete `usecase/chat/handler`

```
domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/handler/
├── ChatFlowHandler.java          (interfaz: supports() + handle())
├── ChatFlowContext.java          (record: userText, contextId, resolution + requireWorkItemId())
├── ChatFlowDispatcher.java       (EnumMap + validación de exhaustividad al construir)
├── PromptVariables.java          (nombres de variables de plantilla, fuente única)
├── GeneralFlowHandler.java
├── QualityAuditFlowHandler.java
├── RefinementFlowHandler.java
├── ApprovalFlowHandler.java
├── DivisionFlowHandler.java
└── PlanningDraftFlowHandler.java

domain/usecase/src/main/java/co/com/bancolombia/usecase/chat/
└── A2AResponseFactory.java       (construcción de Task/Artifact/Message del protocolo A2A)
```

### Pruebas

```
domain/usecase/src/test/java/co/com/bancolombia/usecase/chat/handler/
├── ChatFlowHandlerTest.java      (17 pruebas en 6 clases @Nested — una por handler)
├── ChatFlowDispatcherTest.java   (9 pruebas, incluidas las 3 de fail-fast)
└── ChatFlowContextTest.java      (8 pruebas del contrato de entrada)
```

### Caso de uso

`AgentChatUseCase` queda en **89 líneas**: `chat()`, `chatAndRespond()`, `executeChat()` y un
`elapsedSince()`. Nada más.

---

## 3. Deudas saldadas

| ID | Deuda | Cómo se resolvió |
|---|---|---|
| **D-02** | Seis bloques `onErrorResume` con cuerpo idéntico | **Uno solo**, en `executeChat()`. Los handlers devuelven `Mono<String>` y dejan propagar el error |
| **D-02 (SRP)** | Seis métodos `run*Flow` en la misma clase | Seis clases, una por intención, con solo las dependencias que cada una usa |
| **D-12 (resto)** | Nombres de variables de plantilla repetidos | `PromptVariables`, fuente única |

### Mejora colateral: `A2AResponseFactory`

Los objetivos de línea no se alcanzaban solo moviendo los flujos: quedaban 82 líneas de
construcción de `Task`/`Artifact`/`Message`. Se extrajeron a una fábrica sin estado. El caso de uso
deja así de conocer el detalle del protocolo A2A y pasa de 171 a **89 líneas**.

### Fail-fast del dispatcher

```java
new ChatFlowDispatcher(handlers);   // lanza IllegalStateException si:
                                    //   - falta el handler de alguna AgentIntent
                                    //   - hay dos handlers para la misma intención
                                    //   - la lista viene vacía
```

Si mañana alguien añade un valor a `AgentIntent` y olvida su handler, **la aplicación no arranca**.
Antes ese olvido habría sido un `NullPointerException` en producción.

---

## 4. Métricas obtenidas

| Métrica | Antes (Fase 03) | Después |
|---|---:|---:|
| Líneas `AgentChatUseCase` | 347 | **89** (−258) |
| Métodos `run*Flow` en el caso de uso | 6 | **0** |
| Bloques `onErrorResume` genéricos | 6 | **1** |
| Argumentos del constructor | 11 | **4** |
| Clases > 300 líneas | 2 | **1** (solo `Handler.java`, Fase 06) |
| Tests totales del proyecto | 152 | **186** |
| Cobertura `domain/usecase` | 84,5% (561/664) | **95,4%** (618/648) |
| Cobertura de los 6 handlers | n/a | **100%** |
| Cobertura `ChatFlowDispatcher` / `ChatFlowContext` | n/a | **100%** / **100%** |
| Cobertura `A2AResponseFactory` | n/a | 98,5% (135/137) |

> El único `onErrorResume` restante fuera del caso de uso está en `PlanningDraftFlowHandler` y
> **no es manejo genérico de error**: es la regla de negocio «si el vector store cae, genera la
> propuesta sin contexto RAG».

### Suites tras la fase

| Suite | Tests | Fallos |
|---|---:|---:|
| `GeneralFlowHandler` | 2 | 0 |
| `QualityAuditFlowHandler` | 4 | 0 |
| `RefinementFlowHandler` | 3 | 0 |
| `ApprovalFlowHandler` | 3 | 0 |
| `DivisionFlowHandler` | 2 | 0 |
| `PlanningDraftFlowHandler` | 3 | 0 |
| `ChatFlowDispatcher` | 9 | 0 |
| `ChatFlowContext` | 8 | 0 |
| `AgentChatUseCase - Caracterización del enrutamiento` | 17 | 0 |
| `AgentChatUseCase - Caracterización del parseo` | 7 | 0 |
| `IntentResolver` | 33 | 0 |
| Resto (model, adaptadores, arquitectura, API) | 95 | 0 |
| **Total** | **186** | **0** |

---

## 5. Comandos ejecutados

```powershell
.\gradlew.bat :usecase:test    # verde a la primera; ninguna aserción de caracterización tocada
.\gradlew.bat build            # BUILD SUCCESSFUL
.\gradlew.bat jacocoMergedReport --no-configuration-cache
```

**Control de sanidad:** las 24 pruebas de caracterización (17 de enrutamiento + 7 de parseo)
pasaron **sin modificar ni una sola aserción**. Solo cambió su `setUp()`, que ahora arma el
dispatcher con los seis handlers apuntando a los mismos mocks. Es la prueba de que esta fase movió
código de sitio sin cambiar comportamiento.

---

## 6. Desviaciones respecto a las instrucciones

| Desviación | Justificación |
|---|---|
| **Se creó `A2AResponseFactory`**, que no figuraba en la lista de archivos | Sin ella el caso de uso se quedaba en 171 líneas, lejos del criterio de ≤120. La construcción de `Task`/`Artifact`/`Message` es detalle del protocolo, no orquestación: extraerla es la misma regla de SRP que motiva la fase. Resultado: 89 líneas, por debajo incluso del objetivo final del plan maestro (≤100) |
| **Un solo archivo de prueba para los seis handlers** (`ChatFlowHandlerTest` con 6 `@Nested`), en vez de seis archivos | Comparten mocks, constantes y helpers de captura. Seis archivos habrían duplicado ~60 líneas de andamiaje cada uno. La separación lógica se conserva mediante clases anidadas, y el reporte de pruebas las muestra como suites independientes |
| **Se añadió `ChatFlowContextTest`**, no previsto | La cobertura de `ChatFlowContext` se quedaba en 79,6% por las ramas de validación. Con él llega al 100% |
| **Los handlers se inicializan en `@BeforeEach`**, no como campos `final` | En clases `@Nested`, JUnit crea la instancia **antes** de que `MockitoExtension` inicialice los mocks: un campo `final` habría capturado nulos |
| **`PromptVariables` es package-private** | Solo la usan los handlers de su mismo paquete. Exponerla ampliaría la superficie pública del dominio sin necesidad |

---

## 7. Checklist de calidad

| Criterio (`spring-rules.md` §7) | Estado |
|---|---|
| `domain/model` sin Spring, persistencia ni serialización | ✅ intacto en esta fase |
| `domain/usecase` sin anotaciones de Spring | ✅ los handlers son POJO; el wiring vive en `UseCasesConfig` |
| Single Responsibility (§3-S) | ✅ un flujo, una clase |
| Open/Closed (§3-O) | ✅ añadir una intención = crear un handler y registrarlo |
| Dependency Inversion (§3-D) | ✅ los handlers dependen de puertos, nunca de adaptadores |
| Sin secretos hardcodeados (S2068) | ✅ |
| `Optional<T>` en retornos opcionales (S2259) | ✅ |
| Complejidad cognitiva ≤ 15 | ✅ `executeChat()` = 1 `if` + 1 cadena reactiva |
| Llaves `{}` en todo control de flujo (S1117) | ✅ |
| Sin números mágicos (S109) | ✅ `RAG_MAX_RESULTS` viajó con su handler |
| Tests GIVEN/WHEN/THEN con Mockito | ✅ 35 pruebas nuevas |
| Cobertura de clases nuevas ≥ 90% | ✅ 100% en 8 de 9; 98,5% en la novena |
| `AgentChatUseCase` ≤ 120 líneas | ✅ **89** |
| Argumentos del constructor ≤ 5 | ✅ **4** |
| Build verde | ✅ |

---

## 8. Hallazgos

| Hallazgo | Atender en |
|---|---|
| **La Fase 05 se queda casi sin trabajo**: el constructor ya bajó de 11 a 4 argumentos y el `// TODO Fase 05` desapareció con los campos `String`. Los `String` de configuración sobreviven repartidos entre 5 handlers, que es donde los Value Objects siguen aportando | Fase 05 (replanteada) |
| `UseCasesConfig` lee `HISTORIA_USUARIO.md` **cuatro veces**, una por handler que la necesita. Funciona, pero es I/O repetido en el arranque y se resuelve solo al introducir `CorporateKnowledge` | Fase 05 |
| `AgentChatUseCase` baja al 75,9% de cobertura: lo no cubierto es el camino `chat()` asíncrono contra `NoOpAgentResponseAdapter`, que sigue sin pruebas | Fase 07 (DP-05) |
| `ArchitectureTest` mantiene sus 2 advertencias preexistentes (`Rule_2.7` ×5, `Rule_2.2` ×1). Las 10 clases nuevas **no** añaden violaciones: `Handler` no es sufijo tecnológico para `Rule_2.2` | Fase 07 |
| `Handler.java` (495 líneas) es ya la **única** clase de más de 300 líneas del proyecto | Fase 06 |

---

## 9. Estado al cerrar

- **Siguiente fase generada:** `FASE-05-value-objects-de-configuracion.md`
- **Decisiones abiertas:** DP-05 (bloquea Fase 07)
- **Pendiente de ratificación:**
  1. Eliminación de la «Regla de Mínimos de 5 puntos» (Fase 01).
  2. Regla del pronombre relativo en las palabras clave genéricas (Fase 03).
- **Verificación clave:** las 24 pruebas de caracterización pasaron **sin tocar ninguna aserción**.

