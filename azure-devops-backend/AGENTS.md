# Guía de Agentes de IA y Desarrolladores para el Backend (AGENTS.md)

Este repositorio contiene exclusivamente el **módulo de Backend** de la aplicación, implementado en **Java Spring Boot Reactivo (WebFlux)**. Está estructurado bajo el patrón de arquitectura hexagonal utilizando el estándar del **Scaffold Clean Architecture de Bancolombia**.

Cualquier agente de IA, Copilot, asistente virtual o desarrollador humano que trabaje en este workspace **debe** cumplir de manera obligatoria y sin excepciones con las reglas de arquitectura, estilo y calidad definidas en este documento y sus referencias directas en el archivo de reglas del backend (`rules/spring-rules.md`).

---

## 1. Stack Tecnológico y Estructura del Backend (Java Spring Boot)

El backend de este repositorio se compone exclusivamente de componentes reactivos en los siguientes submódulos Gradle:

1. **`domain/model` — El Núcleo Puro del Negocio**:
   - Contiene las entidades del dominio, objetos de valor e interfaces de puertos (**gateways**) que definen los contratos para comunicarse con el mundo exterior (BD, servicios, etc.).
   - **Regla Estricta**: No posee lógica de Spring (`@Component`, `@Autowired`, `@Value`), anotaciones de Jackson (`@JsonProperty`) ni dependencias de persistencia/JPA (`@Entity`, `@Table`). Todo es Java base puro con Lombok limitado a utilidades genéricas (`@Getter`, `@Setter`, `@Builder`).
2. **`domain/usecase` — Orquestación de Casos de Uso**:
   - Implementa la lógica de negocio y flujos orquestados (ej. buscar especificaciones de planeación y métricas de tablero).
   - **Regla Estricta**: Cero anotaciones de Spring. El wiring y registro de beans se realiza en la capa `applications/app-service` mediante clases de configuración con `@Configuration` y métodos anotados con `@Bean`. La inyección de dependencias a gateways es nativa de Java por constructor (`final` + `@RequiredArgsConstructor` de Lombok).
3. **`infrastructure/driven-adapters` — Adaptadores Concretos (Salida)**:
   - Implementaciones técnicas reales de los gateways declarados en el modelo (WebClient, repositorios JPA/R2DBC, servicios externos, vector store).
   - **Mappers Obligatorios**: La persistencia u otros adaptadores externos manejan sus propios DTOs u entidades técnicas. Éstos **deben mapearse obligatoriamente** a/desde la entidad del dominio puro. Las entidades con anotaciones técnicas nunca se propagan al core o a los casos de uso.
4. **`infrastructure/entry-points` — Puntos de Entrada (Reactores)**:
   - Adaptadores que exponen las capacidades de la API al exterior (ej. controladores, routers y handlers reactivos bajo `/reactive-web`).
   - **Regla Estricta**: Cero lógica de negocio; solo validación inicial, deserialización, llamadas de orquestación a los usecases y mapeo del resultado (o control de errores centralizado con `@ControllerAdvice`).
5. **`applications/app-service` — Inicialización y Cableado**:
   - El módulo ejecutable que arranca con la función `public static void main` y que define las clases de configuración para el wiring ordenado de toda la aplicación.

---

## 2. Acceso a las Reglas de Arquitectura

Para consultar el estándar detallado, buenas prácticas y ejemplos prácticos de codificación del backend, recurre al archivo dedicado:
* 📗 **Reglas del Backend (Spring Boot)**: [rules/spring-rules.md](rules/spring-rules.md)

---

## 3. Mensajes de Commit Estrictos

Todos los commits realizados en este repositorio deben cumplir con el estándar definido de manera obligatoria en `COMMIT_RULES.md`:

Formato: `COMMIT_TYPE(SCOPE): DESCRIPTION`

- **COMMIT_TYPE**: Minúscula (`feat`, `fix`, `docs`, `refactor`, `test`, `security`, `chore`, etc.).
- **SCOPE**: Entre paréntesis, escrito en `snake_case` (ej. `planning_usecase`, `metrics_service`, `db_adapter`).
- **DESCRIPTION**: En español, comenzando con minúscula, claro y conciso.

*Ejemplo correcto*: `feat(planning_usecase): calcular métricas dinámicas para el dashboard`

---

## 4. Política de No-Asunción (Principio Transversal)

**Nunca asumas información que no esté explícita**. Ante cualquier duda, ambigüedad o decisión técnica/de negocio no estipulada, el agente **debe detenerse y preguntar** directamente al usuario.

### Ejemplos comunes que requieren detenerse:
- Esquemas de bases de datos, nombres de nuevas columnas o llaves.
- Estructura exacta o nulabilidad de un payload de entrada/salida (request/response).
- Comportamientos límites o excepciones a lanzar ante errores específicos.
- Propiedades sensibles de configuración a registrar como secretos.

---

## 5. Pruebas Unitarias y Calidad (Testing & Quality)

- **GIVEN / WHEN / THEN**: Todos los archivos de pruebas (`*Test.java` en el backend) deben estructurar sus casos lógicos bajo esta convención semántica descriptiva (AAA - Arrange-Act-Assert).
- **Sin Secretos Hardcodeados (S2068 - BLOCKER)**: Jamás colocar contraseñas, URLs de endpoints locales o tokens en duro. Utilizar placeholders de Spring y variables de entorno del sistema.
- **Null Safety (S2259 - BLOCKER)**: Utilizar `Optional<T>` ante consultas de base de datos u operaciones opcionales. Realizar comprobaciones proactivas antes de desreferenciar punteros para evitar NullPointerExceptions.
- **Complejidad Cognitiva ≤ 15**: Métodos limpios, modulares y directos. Aplicar *Early Returns* para reducir la anidación en flujos de decisión complejos.
- **Sin Números Mágicos**: Extraer los literales numéricos a constantes estáticas finales autoexplicativas (`private static final int MAX_RETRY_ATTEMPTS = 3`).
- **Bloques con Llaves Obligatorios (S1117)**: Todo condicional o bucle (`if`, `else`, `for`) debe tener obligatoriamente llaves `{}` sin excepciones, independientemente de que contenga una sola línea.

---

## 6. Checklist de Cumplimiento de Agente

Antes de dar una tarea por finalizada, comprueba:
- [ ] ¿La estructura del paquete respeta `domain/model`, `domain/usecase`, `driven-adapters`, `entry-points`?
- [ ] ¿La capa `domain/model` se mantiene 100% limpia de anotaciones de Spring, persistencia y de serialización?
- [ ] ¿La capa `domain/usecase` no tiene anotaciones de Spring u inyección mágica (el cableado es en `app-service`)?
- [ ] ¿Se crearon correctamente los mappers en la capa de `driven-adapters` y se mantuvieron las entidades fuera del dominio?
- [ ] ¿Los tests unitarios usan la nomenclatura descriptiva GIVEN/WHEN/THEN basándose en JUnit 5 y Mockito?
- [ ] ¿El commit sigue fielmente el estándar `COMMIT_RULES.md`?
- [ ] ¿Se validó que no existan secretos expuestos o números mágicos en el código modificado?
- [ ] ¿Se revisó que todos los condicionales (`if`/`else`) lleven sus llaves `{}` de bloque correspondientes?
