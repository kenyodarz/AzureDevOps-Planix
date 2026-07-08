# Guía de Agentes de IA y Desarrolladores para el Microservicio Agente (AGENTS.md)

Este repositorio contiene el **módulo del Agente de Inteligencia (Ciberagente)**, implementado en **Java Spring Boot Reactivo (WebFlux)** y estructurado bajo el patrón de arquitectura hexagonal utilizando el **Scaffold Clean Architecture de Bancolombia**. Su rol es el procesamiento autónomo de tareas Agent-to-Agent (A2A), flujos de chat inteligente y soporte para almacenamiento y búsqueda de planificación estructurada (`PlanningChunk` en Vector Stores).

Cualquier agente de IA, Copilot, asistente virtual o desarrollador humano que trabaje en esta base de código **debe** cumplir de manera obligatoria y sin excepciones con las reglas de arquitectura, estilo y calidad definidas en este documento y en su referencia correspondiente (`rules/spring-rules.md`).

---

## 1. Stack Tecnológico y Estructura del Agente (Java Spring Boot Reactivo)

El microservicio utiliza un modelo de concurrencia reactivo con las siguientes capas del Scaffold Clean Architecture:

1. **`domain/model` — El Núcleo Puro del Agente**:
   - Modelos de dominio bajo las subcarpetas:
     - `a2a/`: Estructuración de tareas (`Task`), estados (`TaskState`/`TaskStatus`), mensajes y configuraciones generales del agente (`AgentInterface`, `AgentSkill`).
     - `chat/`: Mensajería del cliente y contratos de gateways del agente (`ChatGateway`, `TaskStoreGateway`, `AgentResponseGateway`).
     - `planning/`: Unidades o Chunks de planeación (`PlanningChunk`) y contrato de almacenamiento vectorial (`PlanningVectorStorePort`).
   - **Regla Estricta**: Sin dependencias de frameworks. Las capas del dominio nunca deben acoplarse a librerías de OpenAI, LangChain u otros LLM frameworks. Toda abstracción se hace en forma de puertos/gateways en el modelo.
2. **`domain/usecase` — Casos de Uso Core del Agente**:
   - Contiene la orquestación pura (ej. flujos de interacción conversacional del agente, ejecución y estado de las tareas, e indexación/búsqueda de fragmentos de planeación o lineamientos).
   - **Regla Estricta**: No contiene anotaciones de Spring (el cableado para inyectar estos beans es en `applications/app-service`). Inyección explícita por constructor (`final` + `@RequiredArgsConstructor` de Lombok).
3. **`infrastructure/driven-adapters` — Integración con Modelos y Persistencia**:
   - Adaptadores de salida que implementan los gateways. Es donde vive la integración real con los servicios de IA, almacenamiento vectorial de queries, base de datos local R2DBC/JPA y conectores API de terceros.
   - **Mappers Obligatorios**: Todos los objetos técnicos o DTOs externos que devuelven los modelos de lenguaje o integradores de persistencia se deben mapear obligatoriamente a las entidades del dominio puro antes de pasarse al usecase. Las entidades técnicas nunca se propagan hacia el núcleo del negocio.
4. **`infrastructure/entry-points/reactive-web` — API de Comunicación con el Agente**:
   - Entry point reactivo (ej. `RouterRest` y `Handler`) que expone los flujos de comunicación con el agente mediante controladores y peticiones REST asíncronas para actualizar tableros, historias o enviar payloads batch de eventos.
   - **Regla Estricta**: Cero lógica de negocio ni toma de decisiones. El payload se valida y se pasa directamente al caso de uso correspondiente.
5. **`applications/app-service` — Inicialización y Wiring**:
   - Clase principal ejecutable y configuraciones donde se inyectan y registran los beans del dominio en Spring.

---

## 2. Acceso a las Reglas de Arquitectura

Para consultar detalles específicos de buenas prácticas en Java Spring Boot, patrones de diseño y calidad estricta, recurre a:
* 📗 **Reglas del Backend (Spring Boot)**: [rules/spring-rules.md](rules/spring-rules.md)

---

## 3. Mensajes de Commit Estrictos

Todos los commits realizados en este repositorio deben cumplir con el estándar definido de manera obligatoria en `COMMIT_RULES.md`:

Formato: `COMMIT_TYPE(SCOPE): DESCRIPTION`

- **COMMIT_TYPE**: Minúscula (`feat`, `fix`, `docs`, `refactor`, `test`, `security`, `chore`, etc.).
- **SCOPE**: Entre paréntesis, en `snake_case` (ej. `agent_chat`, `planning_vector`, `r2dbc_adapter`).
- **DESCRIPTION**: En español, comenzando con minúscula, claro y conciso.

*Ejemplo*: `feat(agent_chat): implementar gateway para mapeo de mensajes conversacionales`

---

## 4. Política de No-Asunción (Principio Transversal)

**Nunca asumas información que no esté explícita**. Al diseñar contratos de IA, roles de prompts, estados y campos de tareas, o lógicas de integración, si algo no está documentado en un spec, **detente y pregunta** al usuario.

---

## 5. Pruebas Unitarias y Calidad (Testing & Quality)

- **GIVEN / WHEN / THEN**: Todos los archivos de pruebas (`*Test.java`) deben estructurar sus casos bajo esta regla de legibilidad pragmática (AAA).
- **Aislamiento de Secretos y API Keys (S2068 - BLOCKER)**: Está tajantemente prohibido quemar llaves de API (ej. OpenAI, Azure OpenAI), passwords de bases de datos o tokens de acceso de Azure DevOps de manera literal en el código o archivos properties/YAML. Utilizar inyección segura de valores mapeados de variables del entorno del sistema.
- **Null Safety (S2259 - BLOCKER)**: Emplear `Optional<T>` ante retornos opcionales de persistencias de mensajes o búsquedas vectoriales. Validar asertivamente que no se realicen desreferencias directas potencialmente nulas.
- **Complejidad Cognitiva ≤ 15**: Los algoritmos de orquestación de tareas inteligentes deben ser legibles y modulares. Extraer la lógica de bifurcaciones pesadas a métodos privados semánticos.
- **Bloques con Llaves Obligatorios (S1117)**: Todo control de flujo (`if`, `else`, `for`) debe tener llaves `{}` obligatoriamente, sin excepciones.

---

## 6. Checklist de Cumplimiento de Agente

Antes de dar una tarea por finalizada, comprueba:
- [ ] ¿La capa `domain/model` está 100% limpia de anotaciones técnicas, mappers externos y SDKs de LLM específicos?
- [ ] ¿La capa `domain/usecase` no tiene anotaciones de Spring (wiring completo en `app-service`)?
- [ ] ¿Se crearon los mappers de conversión correspondientes en los `driven-adapters`?
- [ ] ¿Se implementaron las pruebas de JUnit de forma asertiva bajo el patrón GIVEN/WHEN/THEN?
- [ ] ¿El commit cumple perfectamente con la estructura y tipo definido en `COMMIT_RULES.md`?
- [ ] ¿Se validó que no existan secretos expuestos o números mágicos en el código modificado?
- [ ] ¿Se revisó que todos los condicionales (`if`/`else`) lleven sus llaves `{}` de bloque correspondientes?
