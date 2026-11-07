# ADR-0001: Adopción de Clean Architecture Scaffold Bancolombia

* **Fecha:** 2026-06-15
* **Estado:** Aceptado
* **Autor(es):** Architecture Team
* **Aprobadores:** Lead Architect

---

## 1. Contexto y Planteamiento del Problema

El ecosistema de integración con Azure DevOps y modelos de lenguaje corporativos requiere una base
arquitectónica robusta, extensible y mantenible en Java 21. Si la lógica de negocio, la interacción
con LLMs y los adaptadores de infraestructura (REST, bases de datos vectoriales, transporte A2A) se
acoplan en controladores o servicios monolíticos de Spring, el sistema se vuelve frágil, difícil de
probar y vulnerable a cambios de proveedores de IA o APIs externas.

---

## 2. Opciones Evaluadas

| Opción                                                          | Ventajas                                                                                                                         | Desventajas                                                                                                  |
|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Opción A:** Arquitectura MVC tradicional de 3 capas           | Rápido desarrollo inicial con Spring Boot.                                                                                       | Acoplamiento severo del dominio a frameworks y bases de datos; testing lento dependiente de contexto Spring. |
| **Opción B:** Clean Architecture (Plugin Bancolombia 5.0.1+)    | Dominio puro e inmutable; testing unitario instantáneo; inversión de dependencias estricta; modularización en submódulos Gradle. | Mayor cantidad inicial de interfaces y DTOs de mapeo entre capas.                                            |
| **Opción C:** Microservicios independientes en Python (FastAPI) | Prototipado rápido con ecosistemas de IA (LangChain).                                                                            | Menor alineación con los estándares bancarios corporativos, observabilidad y resiliencia en JVM.             |

---

## 3. Decisión Adoptada

**Adoptamos Clean Architecture** como patrón estructurador obligatorio en `azure-devops-agent`,
`azure-devops-backend` y `azure-devops-mcp`, dividiendo cada proyecto en:

- `domain/model`: Entidades, Value Objects y puertos de salida puros (sin anotaciones técnicas).
- `domain/usecase`: Orquestación de lógica de negocio y aplicación pura.
- `infrastructure/entry-points`: Controladores HTTP REST, WebFlux o adaptadores MCP.
- `infrastructure/driven-adapters`: Implementaciones de acceso a datos, vector store y APIs
  externas.
- `applications/app-service`: Inicialización y cableado explícito de beans en `@Configuration`.

---

## 4. Consecuencias y Compromisos

### Positivas:

* Cobertura de pruebas unitarias superior al 94% en el dominio sin levantar Spring.
* Independencia absoluta de la lógica de negocio frente a frameworks y proveedores de LLM.
* 0 violaciones en auditorías estáticas con ArchUnit.

### Negativas / Riesgos Aceptados:

* Se requiere implementar mapeadores explícitos entre contratos DTO y entidades de dominio en la
  capa de infraestructura.

---

## 5. Referencias y Enlaces Relacionados

* [Clean Architecture — Aislando los detalles](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a)
* `rules/spring-rules.md`
