# Catálogo Integral de Reglas de Arquitectura - Bancolombia

Este documento detalla la totalidad de las reglas de arquitectura del ecosistema Clean Architecture
de Bancolombia, explicando la taxonomía de numeración (`Grupo 1` y `Grupo 2`), por qué existen
saltos en los tests de ArchUnit y cómo se valida cada regla en las distintas etapas del ciclo de
vida del microservicio.

---

## 🧭 ¿Por qué la numeración tiene saltos en ArchUnit?

La taxonomía oficial clasifica las directrices de ingeniería en dos categorías principales:

* **Categoría 1: Reactive Programming** (Programación Reactiva no bloqueante).
* **Categoría 2: Clean Architecture & Concurrency** (Estructura de capas, inmutabilidad y separación
  de responsabilidades).

No todas las reglas son auditables mediante análisis estático de clases con **ArchUnit**.
Bancolombia distribuye la validación en múltiples mecanismos especializados:

| Mecanismo de Validación | Herramienta                        | Cuándo se Ejecuta                  | Alcance                                                                                                                  |
|:------------------------|:-----------------------------------|:-----------------------------------|:-------------------------------------------------------------------------------------------------------------------------|
| **ArchUnit**            | `ArchitectureTest` (`app-service`) | En fase de `test`                  | Bytecode, firmas de métodos, inmutabilidad de atributos y convenciones de nomenclatura.                                  |
| **Gradle Task**         | `validateStructure` / `vs`         | Antes de `compileJava`             | Grafo de dependencias entre módulos y aislamiento estricto de capas (`domain/model`, `domain/usecase`, infraestructura). |
| **Runtime Detection**   | **BlockHound**                     | En pruebas unitarias               | Detecta llamadas bloqueantes de I/O en hilos del Event Loop reactivo (`parallel`, `elastic`).                            |
| **Análisis Estático**   | **SonarQube / Linters**            | Pipeline de CI/CD / análisis local | Anti-patrones reactivos (ej. llamadas manuales a `.subscribe()`, manejo incorrecto de excepciones).                      |

---

## 📊 Matriz Completa de Reglas

| Identificador  | Nombre de la Regla                                                        | Dominio            | Mecanismo de Ejecución               | Estado en Scaffold                                                                    |
|:---------------|:--------------------------------------------------------------------------|:-------------------|:-------------------------------------|:--------------------------------------------------------------------------------------|
| **`Rule_1.1`** | No ejecutar código bloqueante en hilos reactivos                          | Reactividad        | **BlockHound** (Runtime)             | Activado por defecto en proyectos reactivos (`reactor-tools`)                         |
| **`Rule_1.2`** | Manejo declarativo de errores en flujos                                   | Reactividad        | **SonarQube / Code Review**          | Monitoreado en CI/CD (`onErrorResume`, `onErrorReturn`)                               |
| **`Rule_1.3`** | Prohibición de suscripciones explícitas (`.subscribe()`)                  | Reactividad        | **SonarQube / Code Review**          | El flujo debe delegarse al framework; el dominio no se suscribe                       |
| **`Rule_1.4`** | Aislamiento de I/O bloqueante heredada en Schedulers                      | Reactividad        | **SonarQube / Code Review**          | Uso obligado de `publishOn(Schedulers.boundedElastic())` si hay librerías bloqueantes |
| **`Rule_1.5`** | Flujos reactivos deben usar clientes asíncronos de AWS                    | Reactividad        | **ArchUnit** (`ArchitectureTest`)    | Implementado y evaluado en `app-service`                                              |
| **`Rule_2.1`** | Regla de dependencia unidireccional (Model sin dependencias externas)     | Clean Architecture | **Gradle** (`ValidateStructureTask`) | Falla el build si `domain/model` declara librerías no autorizadas                     |
| **`Rule_2.2`** | Clases de dominio sin sufijos tecnológicos (`DTO`, `Request`, `Response`) | Clean Architecture | **ArchUnit** + Generadores CLI       | Implementado en ArchUnit y en tareas `generateModel` / `generateUseCase`              |
| **`Rule_2.3`** | Casos de uso dependen únicamente del Model y puertos (Gateways)           | Clean Architecture | **Gradle** (`ValidateStructureTask`) | Exige que `usecase` dependa solo de `model` en `implementation`                       |
| **`Rule_2.4`** | Clases de dominio sin nombres de tecnologías (`rabbit`, `dynamo`, etc.)   | Clean Architecture | **ArchUnit** + Generadores CLI       | Implementado en ArchUnit y validado al generar clases                                 |
| **`Rule_2.5`** | Casos de uso con atributos estrictamente finales (`final`)                | Concurrencia       | **ArchUnit** (`ArchitectureTest`)    | Implementado; previene estado mutable compartido entre hilos                          |
| **`Rule_2.6`** | Campos en clases de dominio sin nombres de tecnologías                    | Clean Architecture | **ArchUnit** (`ArchitectureTest`)    | Implementado; campos en entidades sin nombres de infraestructura                      |
| **`Rule_2.7`** | Beans de Spring con atributos estrictamente finales                       | Concurrencia       | **ArchUnit** (`ArchitectureTest`)    | Implementado; exige inyección por constructor en componentes Spring                   |

---

## 🔎 Detalle de Reglas del Grupo 1: Programación Reactiva

### Rule_1.1: No ejecutar código bloqueante en hilos reactivos

* **Propósito:** El Event Loop de Netty/Reactor utiliza pocos hilos. Si un hilo se bloquea (ej.
  `Thread.sleep()`, JDBC síncrono, I/O tradicional de archivos), se congela la capacidad del
  servicio para atender peticiones concurrentes.
* **Mecanismo:** Se instrumenta la JVM en los tests unitarios con **BlockHound**. Si un método
  bloqueante se ejecuta en un hilo de Reactor, el test falla de inmediato con
  `BlockingOperationError`.
* **Solución:** Utilizar drivers reactivos (ej. R2DBC, WebClient) o derivar la ejecución a un
  scheduler aislado.

### Rule_1.2: Manejo declarativo de errores en flujos

* **Propósito:** No capturar excepciones con bloques imperativos `try-catch` dentro de pipelines
  reactivos, ya que las excepciones viajan como señales terminales de error a lo largo del flujo.
* **Solución:** Utilizar operadores como `.onErrorResume()`, `.onErrorMap()`, `.retryWhen()`.

### Rule_1.3: Prohibición de `.subscribe()` dentro del Dominio y Adaptadores

* **Propósito:** Invocar `.subscribe()` manualmente rompe la cadena reactiva, destruye la
  propagación de contexto, backpressure y trazabilidad transaccional.
* **Regla:** El dominio (`UseCase`) debe retornar `Mono<T>` o `Flux<T>`. El único componente que
  suscribe el flujo es el servidor de entrada (Spring WebFlux/Netty) en los EntryPoints.

### Rule_1.4: Aislamiento de I/O bloqueante heredada

* **Propósito:** Si es estrictamente necesario consumir una librería de terceros que sea
  síncrona/bloqueante, debe aislarse explícitamente en un pool elástico.
* **Solución:** Envolver la llamada en `Mono.fromCallable(...)` y planificarla con
  `.subscribeOn(Schedulers.boundedElastic())`.

### Rule_1.5: Flujos reactivos deben usar clientes asíncronos de AWS

* **Propósito:** Garantizar que al interactuar con AWS (S3, DynamoDB, SQS, KMS, SNS) se empleen
  clientes del AWS SDK v2 que retornen `CompletableFuture` (ej. `S3AsyncClient`, `KmsAsyncClient`)
  para integrarse con `Mono.fromFuture()`.
* **Mecanismo:** **ArchUnit** analiza todas las clases y detecta el uso de clientes síncronos
  (`S3Client`, `DynamoDbClient`, etc.), emitiendo alertas de arquitectura.

---

## 🏛️ Detalle de Reglas del Grupo 2: Clean Architecture & Concurrencia

### Rule_2.1: Regla de Dependencia - Dominio Aislado

* **Propósito:** La capa más interna (`domain/model`) contiene las reglas de negocio puras y no debe
  conocer detalles de bases de datos, frameworks web ni librerías externas.
* **Mecanismo:** La tarea de Gradle [`validateStructure`](../build.gradle) revisa la configuración
  `implementation` de `:model`. Si detecta librerías externas (fuera de Lombok en `compileOnly` o
  las autorizadas en `whitelistedDependencies`), detiene el build.

### Rule_2.2: Clases de dominio sin sufijos tecnológicos

* **Propósito:** Mantener el lenguaje ubicuo limpio de artefactos técnicos. Una entidad representa
  un concepto del negocio (ej. `User`), no un objeto de transferencia (`UserDTO`, `UserRequest`).
* **Mecanismo:** **ArchUnit** verifica que ninguna clase en `domain.model` termine en `Dto`,
  `Request` o `Response`.

### Rule_2.3: Casos de uso dependen únicamente del Model

* **Propósito:** Un `UseCase` orquesta la lógica del negocio mediante las entidades del dominio y
  los puertos (interfaces/gateways). No debe depender de módulos de infraestructura, drivers ni
  adaptadores externos.
* **Mecanismo:** La tarea [`validateStructure`](../build.gradle) valida que en el bloque de
  dependencias de `:usecase` la única dependencia sea `project(':model')`.

### Rule_2.4: Clases y Gateways sin nombres de tecnologías

* **Propósito:** Los puertos (interfaces de persistencia o mensajería) definidos en el dominio no
  deben atarse a la tecnología concreta.
* **Ejemplo:** Definir `AccountRepository` o `MessageSender`, evitando `MongoAccountRepository` o
  `RabbitMessageSender` en el dominio.
* **Mecanismo:** **ArchUnit** prohíbe que las clases contengan nombres de tecnologías (`mongo`,
  `postgres`, `redis`, `kafka`, `sqs`, `dynamo`, etc.).

### Rule_2.5: Inmutabilidad en Casos de Uso

* **Propósito:** En entornos concurrentes y reactivos, los UseCases son instanciados como Beans
  singleton compartidos por múltiples hilos. Si tienen atributos mutables (no `final`), se genera
  riesgo crítico de condiciones de carrera (race conditions).
* **Mecanismo:** **ArchUnit** exige que todos los campos declarados en clases que terminen en
  `UseCase` sean `final`.

### Rule_2.6: Atributos de modelos sin nombres de tecnologías

* **Propósito:** Las entidades no deben tener atributos como `dynamoPartitionKey` o
  `rabbitRoutingKey`, sino nombres del negocio como `id` o `topicDestination`.
* **Mecanismo:** **ArchUnit** inspecciona los atributos (`fields()`) en las clases del dominio.

### Rule_2.7: Beans con inyección por constructor e inmutabilidad

* **Propósito:** Eliminar la inyección mutable vía `@Autowired` sobre campos privados o `@Value` sin
  constructor. Al usar campos `final` e inyección por constructor, los componentes quedan
  completamente inicializados y son inmutables.
* **Mecanismo:** **ArchUnit** valida que todas las clases anotadas con `@Component`, `@Service`,
  `@Repository`, `@Controller`, `@RestController` y `@Configuration` tengan únicamente atributos
  `final`.
