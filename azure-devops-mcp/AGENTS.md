<!-- BANCOLOMBIA:SKILL:java-devops-repo-kickstart:START -->

# Guía de Agentes de IA y Desarrolladores (AGENTS.md)

## 0. Commits (leer antes de generar un mensaje de Git)

El botón *Generate Commit Message* de Copilot/Junie **no debe** usar Conventional Commits en inglés.

- Obligatorio: `COMMIT_TYPE(scope): descripción en español`
- Scope entre paréntesis, `snake_case`, nunca omitirlo
- Prohibido: `add`, `update`, `fix`, `implement`, `create`, `improve`
- Archivos `.md` / `docs/` → `docs(...)`, nunca `feat`
- Incorrecto: `feat: add comprehensive guide for Azure DevOps configuration and governance`
- Correcto: `docs(azure_devops): agregar guia integral de configuracion y gobernanza`

Detalle en `COMMIT_MESSAGE_CONVENTION.md`.

---

Este repositorio contiene un proyecto estructurado únicamente de backend (Java Spring Boot
Reactivo). Cualquier agente de IA, Copilot, asistente virtual o desarrollador humano que trabaje en
este workspace **debe** cumplir de manera obligatoria y sin excepciones con las reglas de
arquitectura, estilo y calidad definidas en este documento y sus referencias.

---

## 1. Stack Tecnológico y Estructura del Proyecto

El repositorio se compone del módulo de backend:

1. **Backend (Java Spring Boot Reactivo)**: Estructurado bajo el patrón de arquitectura hexagonal y
   generado mediante el **Scaffold Clean Architecture de Bancolombia**
   ([GitHub Repository](https://github.com/bancolombia/scaffold-clean-architecture)). Los módulos se
   dividen en:
    - `domain/model`: Núcleo de dominio puro (entidades, value objects, gateways/puertos).
    - `domain/usecase`: Casos de uso de negocio puros.
    - `infrastructure/entry-points/reactive-web`: Adaptadores de entrada reactivos (controladores
      REST).
    - `infrastructure/driven-adapters`: Adaptadores de salida (en caso de requerirse persistencia,
      web clients, colas, etc.).
    - `applications/app-service`: Módulo ejecutable y configuración de Spring.

---

## 2. Acceso a las Reglas de Arquitectura

Para garantizar un código limpio, desacoplado y mantenible, se han documentado las reglas detalladas
en el directorio `rules/` en la raíz del repositorio:

* 📗 **Reglas del Backend (Spring Boot)**: [rules/spring-rules.md](rules/spring-rules.md)
    * *Puntos clave*: Dominio y casos de uso 100% puros (sin acoplamiento de Spring o JPA), mappers
      en driven-adapters (si los hubiere), CQS (Command-Query Separation) en puertos e interfaces,
      inyección nativa por constructor, principios SOLID, complejidad cognitiva ≤ 15 y prevención
      estricta de vulnerabilidades de SonarQube (S2068 y S2259).
* 🏛️ **Catálogo Integral de Reglas de Arquitectura y
  Reactividad**: [rules/ALL_ARCHITECTURE_RULES.md](rules/ALL_ARCHITECTURE_RULES.md)
    * *Puntos clave*: Taxonomía oficial de reglas auditadas por ArchUnit, Gradle
      (`validateStructure`), BlockHound y SonarQube:
        * **Grupo 1 (Reactividad - Rule_1.1 a Rule_1.5)**: Cero código bloqueante en hilos reactivos
          (BlockHound), manejo declarativo con `.onErrorResume()`, prohibición absoluta de
          `.subscribe()` en dominio y adaptadores, aislamiento obligatorio de I/O bloqueante en
          `publishOn(Schedulers.boundedElastic())`, y uso de clientes asíncronos en AWS SDK v2
          (`CompletableFuture` / `Mono.fromFuture()`).
        * **Grupo 2 (Clean Architecture & Concurrencia - Rule_2.1 a Rule_2.7)**: Pureza total de
          `domain/model` (falla build con `validateStructure` ante dependencias externas), clases de
          dominio sin sufijos tecnológicos (`DTO`, `Request`, `Response`), puertos y entidades sin
          nombres de tecnologías (`mongo`, `rabbit`, `dynamo`), inmutabilidad obligatoria (atributos
          `final`) en Casos de Uso (concurrencia segura) y Beans de Spring con inyección por
          constructor.

---

## 3. Mensajes de Commit Estrictos y Semantic Release

Todos los commits realizados en este repositorio deben cumplir con el estándar de Conventional
Commits y Semantic Release definido en `COMMIT_MESSAGE_CONVENTION.md`. Tanto agentes de IA (Copilot,
Junie, etc.)
como desarrolladores deben seguir este formato sin excepciones:

Formato: `COMMIT_TYPE(SCOPE): DESCRIPTION` o `COMMIT_TYPE(SCOPE)!: DESCRIPTION` (en caso de Breaking
Change).

* **COMMIT_TYPE**: Minúscula (`feat`, `fix`, `perf`, `security`, `docs`, `style`, `refactor`,
  `test`, `build`, `ci`, `chore`, `deprecated`, `removed`).
* **SCOPE**: Entre paréntesis, escrito estrictamente en `snake_case` (ej. `auth_service`,
  `reactive_web`, `user_usecase`, `commit_hooks`).
* **!**: Opcional, reservado únicamente para cambios que rompen retrocompatibilidad (*Breaking
  Changes* para versiones MAJOR).
* **DESCRIPTION**: **Estrictamente en español** (PROHIBIDO el inglés como `add...`, `update...`,
  `fix...`), comenzando con minúscula, claro y conciso sin punto final.

*Ejemplo estándar (MINOR)*: `feat(auth_service): agregar filtro de seguridad jwt reactivo`  
*Ejemplo documentación (NO RELEASE)*:
`docs(commit_hooks): agregar guia de hooks y semantic release`  
*Ejemplo breaking change (MAJOR)*:
`feat(auth_api)!: cambiar contrato de autenticacion eliminando soporte basico`

---

## 4. Política de No-Asunción (Principio Transversal)

**Nunca asumas información que no esté explícita**. Ante cualquier duda, ambigüedad o decisión de
negocio/técnica no especificada, el agente **debe detenerse y preguntar** directamente al usuario.

### Ejemplos de dudas que requieren detenerse:

- Nombres de nuevas tablas, columnas, variables de configuración en YAML o rutas HTTP.
- Shape exacto de payloads de entrada/salida (requests/responses), nulabilidad u opcionalidad.
- Textos, copys de botones, labels, placeholders, flujos visuales de UI y selección de iconos.
- Si una propiedad es confidencial y debe configurarse como secreto o si es de acceso público.

---

## 5. Pruebas Unitarias y Aseguramiento de Calidad (Testing & Quality)

- **GIVEN / WHEN / THEN**: Todos los archivos de pruebas (`*Test.java` en el backend) deben
  estructurar sus casos de prueba bajo esta convención semántica clara.
- **Sin Secretos Hardcodeados (S2068)**: Bajo ninguna circunstancia se subirán tokens, contraseñas o
  URLs de entornos locales embebidos en el código.
- **Null Safety (S2259)**: El backend debe protegerse de NullPointerExceptions utilizando
  `Optional<T>` o verificaciones correspondientes de nulabilidad (como chequeos reactivos Mono/Flux
  en WebFlux) en retornos opcionales de BD y chequeos preventivos.
- **Sin Números Mágicos**: Extraer los literales numéricos a constantes estáticas bien nombradas
  (ej. `private static final int MAX_RETRY_ATTEMPTS = 3`).
- **Bloques con Llaves Obligatorios (S1117)**: Todo condicional o bucle (`if`, `else`, `for`) debe
  tener llaves `{}` de forma obligatoria, incluso si es de una sola línea.
- **Pruebas Manuales y Delegación de Commits**: Si la tarea requiere de pruebas o verificaciones
  manuales (por ejemplo, ejecutar el backend localmente, confirmar persistencia o borrado manual en
  bases de datos, etc.), el agente **no debe realizar el commit de los cambios**. Esta acción de
  verificación y confirmación queda delegada en su totalidad al usuario.

---

## 6. Checklist de Cumplimiento de Agente

Antes de dar una tarea por finalizada, comprueba:

- [ ] ¿El dominio y casos de uso en Java están 100% limpios de anotaciones de Spring/JPA/etc?
- [ ] ¿Se respetan las reglas de reactividad (cero `.subscribe()`, sin llamadas bloqueantes en el
  Event Loop) y ArchUnit (`Rule_1.1` a `Rule_2.7`) detalladas
  en [rules/ALL_ARCHITECTURE_RULES.md](rules/ALL_ARCHITECTURE_RULES.md)?
- [ ] ¿Se crearon mappers correspondientes en los `driven-adapters` al transformar entidades a
  modelos de dominio?
- [ ] ¿Se implementaron pruebas unitarias con la convención GIVEN/WHEN/THEN?
- [ ] ¿El commit cumple perfectamente con la estructura y tipo definido en
  `COMMIT_MESSAGE_CONVENTION.md`?
- [ ] ¿Se verificó que no existan secretos expuestos o números mágicos en el código modificado?
- [ ] ¿Si el cambio requiere pruebas o verificaciones manuales, se delegó la confirmación y
  realización del commit al usuario sin realizarlo automáticamente?

<!-- BANCOLOMBIA:SKILL:java-devops-repo-kickstart:END -->

