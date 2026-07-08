# Guía Global de Agentes de IA y Desarrolladores para el Monorepo (AGENTS.md)

Este repositorio es un monorepo y contiene múltiples módulos que componen la plataforma de integración inteligente con Azure DevOps. Cualquier agente de IA, Copilot, automatización o desarrollador humano que trabaje en este workspace **debe** cumplir de manera obligatoria y sin excepciones con las reglas de arquitectura, estilo, commits, calidad y seguridad especificadas en este documento de forma consolidada.

---

## 1. Reglas Globales y de Operación Obligatorias

- **Absoluta No-Exposición de Secretos**: No subir en ningún caso secretos, keys, tokens (ej. OpenAI, Azure DevOps PAT, contraseñas) ni archivos de entorno `.env` al repositorio. Utiliza variables de entorno o almacenes de secretos adecuados. Las configuraciones locales deben manejarse utilizando placeholders genéricos estilo `${AI_API_KEY:dummy}` o en el archivo `.env.example`.
- **Estructura y Limpieza del Workspace**: Antes de confirmar cualquier cambio, verifica y asegúrate de que no se hayan agregado accidentalmente artefactos de compilación o carpetas locales temporales (`build/`, `dist/`, `node_modules/`, `.gradle/`, `.idea/`, `coverage/`, `reports/`).
- **Consistencia en Interfaces**: Al hacer modificaciones que afecten contratos de comunicación entre el frontend, backend o el servidor MCP, asegúrate de actualizar de forma consistente todas las interfaces compartidas y mantener la coherencia del monorepo.
- **Política de No-Asunción (Transversal)**: **Nunca asumas información que no esté explícita en los requerimientos**. Ante cualquier duda contractual, visual (copys, placeholders, diseño de UI), de nombres de campos, bases de datos o lógica técnica/de negocio, detén el desarrollo de inmediato y consulta al usuario.

---

## 2. Mapa Arquitectónico del Monorepo

El monorepo está distribuido en los siguientes módulos especializados con sus respectivos enfoques tecnológicos:

```
/ (Raíz del monorepo)
├── rules/                            # Manuales de arquitectura globales comunes
│   ├── spring-rules.md               # Buenas prácticas y arquitectura Backend (Java Spring Boot)
│   └── angular-rules.md              # Buenas prácticas y arquitectura Frontend (Angular)
│
├── azure-devops-backend/             # MICROSERVICIO BACKEND (Java Spring Boot WebFlux Reactivo)
│   └── (Estructura Clean Hexagonal para orquestación general de planeación y métricas)
│
├── azure-devops-agent/               # MICROSERVICIO CIBERAGENTE (Java Spring Boot WebFlux + IA)
│   └── (Procesamiento autónomo A2A, chat conversacional y almacenamiento vectorial de planeación)
│
├── azure-devops-mcp/                 # SERVIDOR MCP (Java Spring Boot - Gateway de Herramientas para LLMs)
│   └── (Implementa la especificación MCP exponiendo herramientas @McpTool automatizadas)
│
└── azure-devops-frontend/            # FRONTEND (Angular v21 Standalone, PrimeNG & Tailwind)
    └── (Interfaz de usuario reactiva, amigable y accesible)
```

---

## 3. Directrices Específicas por Módulo

### 3.1 Backend & Ciberagente (Java Spring Boot Reactivo — Clean Architecture Bancolombia)

Los submódulos de **Backend** (`azure-devops-backend`), **Agente** (`azure-devops-agent`) y **Servidor MCP** (`azure-devops-mcp`) se basan en el **Scaffold Clean Architecture de Bancolombia**, aplicando un desacoplamiento hexagonal riguroso:

1. **`domain/model` — El Núcleo Puro (Entidades, Puertos/Gateways)**:
   - Contiene la lógica central y las estructuras agregadas del negocio (ej. `Task`, `PlanningChunk`, `WorkItem`).
   - **Regla Estricta**: No posee lógica de Spring (`@Component`, `@Autowired`, `@Value`), anotaciones de Jackson (`@JsonProperty`) ni anotaciones/dependencias de bases de datos/JPA (`@Entity`, `@Table`). Todo es Java base puro, con Lombok limitado a utilidades generales de inmutabilidad (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
2. **`domain/usecase` — Casos de Uso Core (Orquestadores)**:
   - Implementa los flujos de negocio e interacciones de orquestación.
   - **Regla Estricta**: Cero anotaciones de Spring. El cableado (*wiring*) se realiza en la capa `applications/app-service` de manera imperativa mediante clases de configuración con `@Configuration` y métodos anotados con `@Bean`. La inyección de dependencias a los gateways de dominio es nativa de Java por constructor (`final` + `@RequiredArgsConstructor` de Lombok).
3. **`infrastructure/driven-adapters` — Adaptadores Concretos de Salida (I/O)**:
   - Implementa de manera tecnológica los adaptadores de salida para base de datos (JPA / R2DBC), clientes REST/WebClient, indexación vectorial o envío de mensajes/eventos asíncronos.
   - **Mappers Obligatorios**: Todos los modelos técnicos, entidades ORM o DTOs externos de APIs **deben mapearse obligatoriamente** a/desde la entidad del dominio puro. Las entidades de infraestructura nunca se propagan al core o a los casos de uso.
4. **`infrastructure/entry-points` — Adaptadores de Entrada (Reactores/HTTP/MCP)**:
   - Exponen los servicios hacia el exterior (ej. controladores HTTP, handlers reactivos, oyentes asíncronos o el gateway MCP).
   - **Regla Estricta**: Cero lógica de negocio; solo validación inicial de contratos, deserialización, delegación inmediata al caso de uso correspondiente del dominio y mapeo de la respuesta para el cliente.

*Para detalles y ejemplos prácticos, consulta:* `/rules/spring-rules.md`

---

### 3.2 Servidor MCP (Model Context Protocol — `azure-devops-mcp`)

- **Estructuración de Herramientas**: Las herramientas del protocolo MCP se exponen mediante la anotación `@McpTool` dentro del entry-point específico `entry-points/mcp-server/`.
- **Epecificaciones Claras**: Todo método `@McpTool` debe tener descripciones rigurosas y detalladas de sus parámetros con anotaciones claras de metadatos, permitiendo a los LLMs interpretar exactamente cuándo y cómo llamar a cada herramienta de Azure DevOps.
- **Flujo Desacoplado**: Al igual que el resto del backend, las herramientas de MCP no deben contener lógica compleja ni conectores de infraestructura en sí mismas; deben validar la entrada y delegar inmediatamente la lógica de negocio a un caso de uso puro en `domain/usecase`, de modo que el adaptador driven adecuado haga la llamada a Azure DevOps.

---

### 3.3 Frontend (Angular v21, Standalone, PrimeNG y Tailwind CSS — `azure-devops-frontend`)

El frontend de la UI reactiva debe cumplir con las siguientes directrices sin excepciones:

- **Estructura por Feature**: El código se organiza bajo `src/app/features/{nombre_feature}/` separando:
  - `pages/`: Smart Components conectados a rutas que controlan flujos de features.
  - `components/`: Dumb Components puramente visuales y reutilizables (solo inputs/outputs).
  - `services/`: Wrappers de API y servicios de encapsulado y control del estado reactivo de la feature.
- **Componentes Standalone**: Todos los componentes, directivas o pipes de Angular deben ser creados como standalone (`standalone: true`).
- **Control Flow de Angular Moderno**: Está totalmente prohibido usar `*ngIf`, `*ngFor` y `*ngSwitch`. Debes utilizar la nueva sintaxis nativa de control de flujo (`@if`, `@for`, `@switch`).
- **Reactividad Libre de Fugas (Memory Leaks)**:
  - Gestiona estados simples de UI mediante **Signals** (`signal()`, `computed()`).
  - Gestiona estados de negocio unificados mediante `BehaviorSubject` RxJS expuestos únicamente como públicos de forma inmutable mediante `.asObservable()`.
  - Evita fugas de memoria priorizando el uso de **`AsyncPipe`** en el HTML o cerrando de manera determinista todas las suscripciones mediante **`takeUntilDestroyed()`** de Angular Core.
- **Accesibilidad Estricta (A11Y)**: Todo botón o elemento interactivo compuesto puramente por iconos (sin texto descriptivo adjunto) **debe contener obligatoriamente un atributo `aria-label` descriptivo** para lectores de pantalla.

*Para detalles y ejemplos prácticos, consulta:* `/rules/angular-rules.md`

---

## 4. Estilo de Código, Calidad y Reglas de Calidad Relacionales

Cualquier cambio realizado en los submódulos Java o TypeScript debe respetar los criterios mínimos de calidad del proyecto:

- **Null Safety (S2259 - BLOCKER)**: Envolver siempre retornos de base de datos o clientes HTTP opcionales bajo `Optional<T>` y realizar chequeos seguros (`?.` o aserciones) antes de desreferenciar variables.
- **Complejidad Cognitiva ≤ 15 (MAJOR)**: Mantener métodos cortos y legibles aplicando patrones de retorno temprano (*Early Returns*). Extraer complejidad pesada a sub-métodos privados bien nombrados.
- **Bloques de Control con Llaves (S1117)**: Todos los bloques condicionales o bucles (`if`, `else`, `for`, `while`) deben poseer llaves `{}` obligatorias, incluso si ejecutan una sola instrucción lineal.
- **Nomenclatura Semántica en Tests**: En los módulos de pruebas unitarias (`*.spec.ts` con Vitest y `*Test.java` con JUnit 5/Mockito), estructura los tests bajo la convención lógica **GIVEN / WHEN / THEN** (AAA: Arrange-Act-Assert).

---

## 5. Reglas Obligatorias para Mensajes de Commit

Cada commit realizado dentro de este repositorio de forma automatizada o manual debe apegarse estrictamente a la especificación de `COMMIT_RULES.md`:

`COMMIT_TYPE(SCOPE): DESCRIPTION`

1. **COMMIT_TYPE**: Minúscula, debe ser uno de los siguientes tipos autorizados:
   - `feat` (Funcionalidad), `fix` (Corrección), `docs` (Cambio documental), `style` (Estilos/Formatos sin cambio de lógica), `refactor` (Reorganización de código no funcional), `perf` (Rendimiento), `test` (Casos de prueba), `build` (Sistemas de compilación), `ci` (Configuraciones de pipelines), `removed` (Borrado de obsoleto), `deprecated` (Obsoleto), `security` (Seguridad), `chore` (Tareas del mantenimiento general del repositorio).
2. **SCOPE**: Entre paréntesis, en minúsculas y usando `snake_case` según el módulo/contexto afectado (ej. `agent_chat`, `mcp_tools`, `user_module`, `navbar_component`).
3. **DESCRIPTION**: En español, con minúscula inicial, claro y de longitud concisa.

---

## 6. Lista de Verificación Cruzada (Checklist Unificado)

Antes de finalizar tu trabajo y entregar una tarea, asegúrate de marcar cada casilla:

- [ ] **Seguridad**: No hay credenciales, PATs de Azure DevOps, llaves de API o contraseñas quemadas en duro en el código ni en archivos `application.yml`/`.env`.
- [ ] **Estructura y Pureza (Backend)**: El model (`domain/model`) y usecases (`domain/usecase`) están totalmente puros e intactos de anotaciones Spring/Jackson; todo cableado está configurado en `app-service`.
- [ ] **Adaptadores (Backend)**: Todas las mutaciones SQL u operaciones externas mapean sus DTOs/entidades técnicas a entidades de dominio puro en los adaptadores driven.
- [ ] **Standalone & Flow (Frontend)**: Todos los componentes nuevos son standalone y usan el flujo moderno `@if`/`@for`/`@switch` en lugar de las directivas antiguas.
- [ ] **Fugas de Memoria (Frontend)**: Todas las suscripciones manuales de RxJS están acopladas de forma determinista para destruirse mediante `takeUntilDestroyed()`.
- [ ] **Accesibilidad (Frontend)**: Todo botón compuesto únicamente por iconos tiene definido su `aria-label` descriptivo.
- [ ] **Calidad general**: Todos los condicionales usan llaves `{}` de bloque, los retornos opcionales de bases de datos usan `Optional<T>`, y no superas los 15 puntos de complejidad cognitiva por método.
- [ ] **Pruebas**: Se han redactado pruebas unitarias usando de forma idónea la nomenclatura semántica GIVEN/WHEN/THEN.
- [ ] **Mensaje de Commit**: El commit respeta rigurosamente el formato estándar estandarizado del monorepo.

