# Informe de Escaneo de Dependencias y Oportunidades de Mejora Técnica

> **Fecha:** 2026-11-03  
> **Ámbito de Auditoría:** Monorepo Azure DevOps (`azure-devops-agent`, `azure-devops-backend`,
> `azure-devops-frontend`, `azure-devops-mcp`)  
> **Estándar Rector:** [
> `assets/templates/DEPENDENCY_SCAN.template.md`](file:///C:/Users/minaj/Work/GitHub/Labs/AzureDevOps/.agents/skills/codebase-docs-archify/assets/templates/DEPENDENCY_SCAN.template.md)

---

## 1. Resumen Ejecutivo

Se ha realizado una auditoría exhaustiva de la configuración de construcción (`build.gradle`,
`gradle.properties`, `package.json`), las versiones declaradas de frameworks y la arquitectura de
dependencias de los cuatro componentes del monorepo.

El proyecto presenta una base técnica moderna y madura:

* **Java 21 LTS** unificado con Gradle Wrapper en los microservicios.
* Scaffold oficial de **Clean Architecture Bancolombia 5.0.1** aplicado estrictamente.
* Ecosistema **Spring AI 2.0.1** configurado mediante el protocolo estándar OpenAI hacia
  **LiteLLM**.
* **Angular 21/22 Standalone** con arquitectura reactiva de Signals y pruebas unitarias rápidas
  mediante **Vitest**.
* Eliminación exitosa de clases monolíticas en el agente (407 pruebas en verde con
  cobertura $\ge 94\%$).

No obstante, se identifican hallazgos puntuales en observabilidad, normalización de dependencias y
resiliencia que deben abordarse antes del paso a producción.

---

## 2. Matriz de Dependencias y Evaluación de Riesgos

| Dependencia / Componente      | Versión Declarada  | Estado      | Nivel de Riesgo | Recomendación                                                                                                                     |
|-------------------------------|--------------------|-------------|-----------------|-----------------------------------------------------------------------------------------------------------------------------------|
| **Clean Architecture Plugin** | `5.0.1`            | Vigente     | 🟢 **Óptimo**   | Plugin oficial actualizado; estructura de capas y submódulos cumple todas las reglas de aislamiento.                              |
| **Java Platform**             | `JDK 21 LTS`       | Vigente     | 🟢 **Óptimo**   | Soporte para Virtual Threads, Records y Pattern Matching aprovechado en modelos de dominio.                                       |
| **Spring AI Starter**         | `2.0.1`            | Vigente     | 🟢 **Óptimo**   | Alineado con Spring Framework 7 y Spring Boot 4.x.                                                                                |
| **Jackson 3 (`JsonMapper`)**  | `3.x`              | Normalizado | 🟢 **Óptimo**   | Se reemplazó el legacy `ObjectMapper` por `JsonMapper` inmutable en adaptadores MCP (commit `6b05530`).                           |
| **Angular & Vitest**          | `21.2.17` / `22.x` | Vigente     | 🟢 **Óptimo**   | Standalone Components con Signals y OnPush; suites de test independientes sin Karma.                                              |
| **AWS SDK S3 Dependency**     | `v2.x`             | Opcional    | 🟡 **Medio**    | Introducido para soporte cloud en storage; verificar que en despliegues on-premise no añada sobrepeso innecesario al fat-JAR.     |
| **Actuator & Prometheus**     | Parcial            | Incompleto  | 🔴 **Alto**     | `management.endpoints.web.exposure.include` solo expone `health`; falta `prometheus` y `metrics` para observabilidad corporativa. |

---

## 3. Hallazgo 1: Déficit de Observabilidad y Métricas de Inferencia

### 3.1 Diagnóstico

En `azure-devops-agent/applications/app-service/src/main/resources/application.yaml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health" # Falta prometheus y metrics
```

* No se están recolectando métricas de latencia de inferencia de LLM (tiempo de respuesta del
  gateway LiteLLM).
* No se mide el consumo de tokens (prompt tokens vs completion tokens) por solicitud de usuario o
  sesión de planeación.
* Ausencia de métricas de acierto/fallo en la resolución de intenciones (`IntentResolver`).

### 3.2 Recomendación de Solución

1. Exponer métricas de Prometheus en Actuator:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: "health,info,prometheus,metrics"
   ```
2. Instrumentar `AgentChatUseCase` con contadores y timers de **Micrometer**:
   ```java
   Timer.builder("agent.chat.duration")
        .tag("intent", intent.name())
        .register(meterRegistry)
        .record(() -> ...);
   ```

---

## 4. Hallazgo 2: Políticas de Resiliencia en Conectores Externos

### 4.1 Diagnóstico

* `azure-devops-mcp` interactúa con Azure DevOps Cloud REST API v7. Ante degradaciones de red o
  *rate limits* (HTTP 429) de Azure DevOps, no existe una política explícita de reintentos
  exponenciales con jitter ni Circuit Breaker configurado.
* `azure-devops-agent` delega a LiteLLM. Si el gateway sufre un timeout, la llamada reactiva puede
  retener hilos de memoria si no se fija un `.timeout(...)` estricto en el WebClient.

### 4.2 Recomendación de Solución

1. Incorporar **Resilience4j** (`resilience4j-spring-boot3` o reactivo) en los driven-adapters de
   `azure-devops-mcp`.
2. Establecer timeout reactivo obligatorio en llamadas a endpoints externos:
   ```java
   this.webClient.post()
       .retrieve()
       .bodyToMono(String.class)
       .timeout(Duration.ofSeconds(15))
       .retryWhen(Retry.backoff(3, Duration.ofMillis(500)).jitter(0.5));
   ```

---

## 5. Próximos Pasos Priorizados

1. **Prioridad 1 (Observabilidad):** Habilitar el endpoint `/actuator/prometheus` en el agente y el
   BFF para consumo por Grafana/Prometheus.
2. **Prioridad 2 (Resiliencia):** Configurar timeouts y Circuit Breakers en `azure-devops-mcp`
   contra Azure DevOps Services.
3. **Prioridad 3 (CI/CD):** Incorporar escaneo continuo de vulnerabilidades mediante Trivy o
   SonarQube en el pipeline de Pull Requests.
