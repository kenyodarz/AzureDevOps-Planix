# Análisis de brecha: monorepo AzureDevOps vs. Wynxx.ai

> Fecha: 2026-08-30
> Referencia externa: https://wynxx.ai/es/software-engineering
> Documento de estrategia. No sustituye a `AI_WORK_PLAN.md`, lo alimenta.

## 1. Qué es Wynxx (resumen operativo)

Wynxx es el ecosistema de IA empresarial de **GFT Technologies**. Su tesis central:

> "Programar es más rápido. Los cuellos de botella se trasladaron."

Es decir: los asistentes de código ya resolvieron el IDE. La fricción real quedó en **requisitos,
pruebas, revisión, seguridad y documentación**. Wynxx aplica IA a todo el SDLC con **contexto
compartido**, enfoque **spec-driven** y **gobernanza con supervisión humana**.

### Resultados que publica (línea base para comparar)

| Capacidad                        | Antes  | Después |
|----------------------------------|--------|---------|
| Generación de historias          | 30 min | 2 min   |
| Ejecución de pruebas (170 casos) | 5 días | 2 h     |
| Revisión de código               | 3 h    | 0,3 h   |
| Corrección de código             | 45 h   | 4 h     |
| Detección de vulnerabilidades    | 18 h   | 2 h     |
| Documentación técnica            | 32,4 h | 3,3 h   |

Caso de referencia: **Bradesco Seguros** — +40% productividad, −80% tiempo de corrección de código,
20 equipos.

### Sus cuatro pilares

1. **Ingeniería de software** (SDLC completo)
2. **Innovación** (crear, publicar y reutilizar capacidades de IA reguladas)
3. **Modernización** (legacy, extracción de reglas de negocio)
4. **Gobernanza** (políticas compartidas, acceso, auditoría y costes)

---

## 2. Dónde está este repo hoy

| Módulo                  | Estado real                                                                                                                                       |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `azure-devops-agent`    | **Maduro.** 305 tests, cobertura 99,7% / 99,2%, 0 violaciones ArchUnit, 6 `ChatFlowHandler`, JSON-RPC 2.0, Agent Card A2A, prompts externalizados |
| `azure-devops-mcp`      | Estructura limpia, integración Azure DevOps parcial (`ListWorkItemsByTeamAndSprintUseCase`), testing mínimo                                       |
| `azure-devops-backend`  | Scaffolding Clean Architecture sin casos de uso de negocio                                                                                        |
| `azure-devops-frontend` | Angular 22 standalone + Signals + Tailwind. UI de chat y planeación, sin auth                                                                     |

---

## 3. Mapa de capacidades: Wynxx vs. este repo

Leyenda: 🟢 cubierto · 🟡 parcial · 🔴 ausente

| Etapa SDLC de Wynxx                                           | Este repo | Evidencia / brecha                                                                                                                                                                                                                                         |
|---------------------------------------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Requisitos → Épica/Feature/Historia** (*Story Creator 2.0*) | 🟡        | `PlanningDraftHandler`, `RefinementHandler`, `DivisionHandler` y `ApprovalHandler` cubren el flujo conceptual. Falta jerarquía Épica→Feature→US formal y escritura real en Azure DevOps                                                                    |
| **Criterios de aceptación**                                   | 🟡        | Existe refinamiento; no hay contrato explícito de AC (Gherkin / INVEST)                                                                                                                                                                                    |
| **Estimación / división**                                     | 🟢        | `ComplexityEstimation`, umbral `> 8` puntos (DP-02), aviso explícito si el modelo no estima (DP-03). **Esto es una ventaja real**                                                                                                                          |
| **Arquitectura asistida**                                     | 🔴        | No existe                                                                                                                                                                                                                                                  |
| **Desarrollo / generación de código**                         | 🔴        | No existe                                                                                                                                                                                                                                                  |
| **Testing automatizado**                                      | 🔴        | Excelente testing *del propio producto*, cero capacidad de *generar* tests para terceros                                                                                                                                                                   |
| **Code review**                                               | 🟡        | `QualityAuditHandler` existe. Alcance y profundidad por validar                                                                                                                                                                                            |
| **Seguridad / vulnerabilidades**                              | 🟡        | SonarQube configurado para el propio build; no como capacidad ofrecida                                                                                                                                                                                     |
| **Documentación evolutiva**                                   | 🔴        | No existe                                                                                                                                                                                                                                                  |
| **Modernización legacy**                                      | 🔴        | No existe                                                                                                                                                                                                                                                  |
| **Contexto compartido / spec-driven**                         | 🟢        | `PlanningVectorStorePort` + `PgVectorPlanningAdapter` implementados y `datasource` configurado contra PostgreSQL real. **Despliegue en preproductivo diferido a propósito**: la POC sigue en edición. No es brecha, es secuenciación (ver §3.2)            |
| **Agnosticismo de modelo**                                    | 🟢        | **Resuelto vía gateway.** `spring-ai-starter-model-openai` actúa como cliente del *protocolo* OpenAI (estándar de facto), no como binding a OpenAI. Detrás está **LiteLLM**, que enruta a Google, Microsoft y AWS según lo que requiera cada app. Ver §3.1 |
| **Gobernanza: trazabilidad y auditoría**                      | 🔴        | Sin audit log, sin linaje de decisiones de IA                                                                                                                                                                                                              |
| **Gobernanza: control de acceso**                             | 🟡        | **IdP decidido: Microsoft Entra ID.** Pendiente el aprovisionamiento de los *Service Principals* de los microservicios, que es un proceso interno del Banco ajeno a este repo. No es deuda técnica, es dependencia externa (ver §3.2)                      |
| **Gobernanza: control de costes**                             | 🔴        | Sin medición de tokens ni presupuesto por tenant                                                                                                                                                                                                           |
| **Multi-tenant empresarial**                                  | 🔴        | No existe                                                                                                                                                                                                                                                  |
| **Human-in-the-loop**                                         | 🟢        | `ApprovalHandler` + UI de aprobación. **Bien planteado**                                                                                                                                                                                                   |

**Cobertura estimada del alcance de Wynxx: ~15%.**
Concentrada casi toda en una sola vertical: *requisitos e historias de usuario*.

### 3.1 Agnosticismo de modelo: ya resuelto vía LiteLLM

Wynxx presume de ser "agnóstico al modelo". **Este repo ya lo es**, por la vía correcta en entorno
bancario: un *gateway* corporativo en lugar de una abstracción propia.

`applications/app-service/src/main/resources/application.yaml`:

```yaml
spring:
  ai:
    openai:
      api-key: "${AI_API_KEY:dummy}"
      base-url: "${AI_BASE_URL:http://localhost:4141}"   # -> LiteLLM
      chat:
        model: "${AI_MODEL:gpt-5-mini}"
      embedding:
        model: "${AI_EMBEDDING_MODEL:text-embedding-ada-002}"
```

```text
        Agente (Spring AI)
              │  protocolo OpenAI
              ▼
          LiteLLM  ← gateway corporativo del Banco
              │
   ┌──────────┼──────────┐
   ▼          ▼          ▼
 Google   Microsoft     AWS
(Gemini)  (Azure OAI)  (Bedrock)
```

**Por qué esto es lo correcto y no deuda técnica:**

- `spring-ai-starter-model-openai` es un cliente del **protocolo** OpenAI, que es el estándar de
  facto de la industria. No es un acoplamiento al proveedor OpenAI.
- El enrutamiento por modelo, la cuota, el *fallback*, el *rate limiting* y el registro de costes
  viven en **LiteLLM**, no en la aplicación. Es el sitio correcto: se gobierna una vez para todas
  las apps del Banco, no app por app.
- Cambiar de modelo o de nube es una **variable de entorno** (`AI_MODEL`, `AI_BASE_URL`). Cero
  cambios de código, cero redespliegue de lógica.
- Construir una abstracción multi-proveedor propia **duplicaría** una capacidad que la plataforma ya
  ofrece, y sería un antipatrón.

> **Corrección respecto a la versión inicial de este documento:** se había marcado el
> agnosticismo de modelo como ausente. Era un error de lectura: la configuración ya está
> externalizada y apunta a un gateway. **No hay acción pendiente en este punto.**

**Único matiz a vigilar:** que el dominio no asuma capacidades específicas de un modelo (*function
calling*, ventana de contexto, formato de salida estructurada). Mientras eso se resuelva en el
adaptador y el dominio hable por `ChatGateway`, la portabilidad está garantizada.

### 3.2 Qué es deuda técnica y qué no

Distinción necesaria para no inflar la brecha. **No todo lo que falta es un defecto.**

| Punto                                                  | Clasificación                      | Justificación                                                                                                                                                                                                                              |
|--------------------------------------------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PostgreSQL/pgvector sin desplegar en preproductivo** | ✅ **Secuenciación deliberada**    | El adaptador está implementado y el `datasource` configurado contra PostgreSQL real. La POC sigue en edición; desplegar infraestructura preproductiva antes de estabilizar el contrato es trabajo que se tira. Correcto no hacerlo todavía |
| **Service Principals de los microservicios ausentes**  | ✅ **Dependencia externa**         | El IdP ya está decidido: **Microsoft Entra ID**. El aprovisionamiento de SP es un proceso interno del Banco con sus propios tiempos. No es controlable desde este repo                                                                     |
| **Sin `docker-compose` full stack**                    | ⚠️ **Conveniencia, no bloqueante** | Existe `deployment/Dockerfile`. Un compose local aceleraría el *onboarding* y las demos, pero no condiciona la arquitectura                                                                                                                |
| **Sin CI/CD**                                          | 🔴 **Deuda real**                  | Los *quality gates* (JaCoCo, Pitest, ArchUnit, Sonar) ya están configurados pero nadie los ejecuta automáticamente. El coste de añadirlos crece con el tiempo                                                                              |
| **`build/` y `build-cache/` versionados**              | 🔴 **Deuda real**                  | Contradice el propio `.gitignore` y `AGENTS.md`. Corrección de minutos                                                                                                                                                                     |
| **Audit log / linaje de decisiones de IA**             | 🔴 **Deuda real**                  | Es el núcleo del argumento de gobernanza. Nada externo lo bloquea                                                                                                                                                                          |
| **Escritura Épica→Feature→US en Azure DevOps**         | 🔴 **Funcionalidad pendiente**     | Es la promesa central del producto                                                                                                                                                                                                         |

> **Corrección respecto a la versión inicial:** este documento marcaba el pgvector no desplegado
> y la ausencia de autenticación como carencias críticas. Ambas lecturas eran erróneas: la
> primera es una decisión de secuenciación consciente y la segunda un bloqueo externo con la
> decisión de arquitectura ya tomada. **La brecha real es menor de lo que indicaba la v1.**

### 3.3 Lo que sí se puede avanzar sin los Service Principals

El bloqueo de los SP **no impide** dejar la seguridad lista para el día en que lleguen:

- Validación de JWT contra Entra ID (`issuer`, `audience`, JWKS) mediante
  `spring-boot-starter-oauth2-resource-server`. Es configuración, no requiere SP propio.
- Propagación del *claim* de tenant (`tid`) y del sujeto (`oid`) al contexto de la petición.
- Extracción de roles/grupos desde el token hacia autorización por método.
- Perfil `local` con emisor simulado para poder probar el flujo completo sin depender del Banco.

Así, cuando los SP se aprovisionen, la integración se reduce a cargar credenciales por variable de
entorno.

> ❓ **Pendiente de definir contigo:** qué es un "tenant" en este producto — ¿organización de
> Azure DevOps, proyecto, o equipo? De eso depende el modelo de aislamiento de datos y del
> vector store. **No lo asumo.**

---

## 4. Veredicto

### ¿Vas por buen camino?

**Sí, y por el camino difícil de imitar.** Tres decisiones ya tomadas son correctas y caras de
retrofitear:

1. **Clean Architecture con dominio puro.** Cambiar de LLM, de vector store o de Azure DevOps a Jira
   es sustituir un adaptador. Wynxx presume de agnosticismo de modelo; **tú ya lo tienes resuelto**
   vía LiteLLM y configuración externalizada (§3.1).
2. **Human-in-the-loop de primera clase.** `ApprovalHandler` no es un añadido, es un flujo. Eso es
   exactamente el discurso de gobernanza de Wynxx.
3. **Calidad verificable.** 99% de cobertura, ArchUnit, Pitest, BlockHound. Para vender gobernanza
   hay que predicar con el ejemplo.

### ¿Es complejo?

**Igualar Wynxx completo: no es viable ni deseable.** Wynxx es producto de GFT con equipos
dedicados, 20 equipos de clientes en producción y casos como Bradesco. Competir de frente en las 4
verticales es perder.

**Igualar la vertical de requisitos con gobernanza: totalmente viable.** Y ahí ya tienes un activo
diferencial que Wynxx no publicita: **estimación con umbral de división explícito y honestidad ante
la incertidumbre** (DP-03: si el modelo no estima, se avisa en vez de inventar). Eso es confianza, y
es tu cuña.

### Recomendación estratégica

> **No persigas el SDLC completo. Sé el mejor del mundo en `Necesidad → Backlog gobernado`
> sobre Azure DevOps.**

El benchmark a batir es concreto y público: **30 min → 2 min por historia**. Es medible, demostrable
y suficiente para justificar el producto.

---

## 5. Ruta por fases

Alineada con las fases de `AI_WORK_PLAN.md`.

### Fase A — Cerrar el ciclo (hacer que funcione de verdad)

Objetivo: pasar de POC a demo creíble end-to-end.

- [ ] Purgar `build/` y `build-cache/` del control de versiones (ya están en `.gitignore`).
- [ ] `docker-compose` local con **PostgreSQL + pgvector** para desarrollo y demos. *No implica
  desplegar en preproductivo:* eso se hace cuando la POC estabilice su contrato.
- [ ] Sustituir `task-store-inmemory` por persistencia real.
- [ ] Cerrar la **escritura** en Azure DevOps desde `azure-devops-mcp`: crear Épica, Feature y User
  Story con jerarquía y vínculos.
- [ ] Pipeline CI mínimo (GitHub Actions): build + test + Sonar en los 4 módulos.

**Criterio de salida:** `docker-compose up`, subir un caso de uso en la UI y ver las historias
creadas en Azure DevOps.

### Fase B — Gobernanza (el diferenciador frente a Copilot)

- [ ] **Resource server contra Microsoft Entra ID**: validación de JWT (`issuer`, `audience`, JWKS)
  con `spring-boot-starter-oauth2-resource-server`. **No depende de los Service Principals.**
- [ ] Perfil `local` con emisor simulado para probar el flujo completo sin depender del proceso
  interno.
- [ ] Propagación de `tid` (tenant) y `oid` (sujeto) al contexto de la petición.
- [ ] **Audit log inmutable**: prompt, modelo, versión, usuario, tokens, decisión, aprobador.
- [ ] **Medición de coste por tenant y por operación** (complementa lo que ya registra LiteLLM).
- [ ] Aislamiento multi-tenant en consultas y en el vector store — *requiere definir antes qué es un
  tenant (§3.3)*.
- [ ] ⏳ **Bloqueado por proceso interno:** cargar credenciales de los Service Principals por
  variable de entorno.

**Criterio de salida:** poder responder "¿quién generó esta historia, con qué modelo, cuánto costó y
quién la aprobó?".

### Fase C — Calidad del output (batir los 2 minutos)

- [ ] Jerarquía formal **Épica → Feature → User Story** como modelo de dominio.
- [ ] Criterios de aceptación estructurados (Gherkin) con validación **INVEST**.
- [ ] Verificar que el dominio no asuma capacidades específicas de un modelo (*function calling*,
  salida estructurada, ventana de contexto): deben resolverse en el adaptador, no en el dominio.
- [ ] Contexto corporativo vía RAG sobre `PlanningVectorStorePort` (glosario, estándares, historias
  previas del equipo).
- [ ] **Métrica de tiempo por historia instrumentada** para comparar contra el benchmark de 30→2
  min.

### Fase D — Expansión selectiva (solo si A/B/C están sólidas)

Ordenadas por adyacencia a lo que ya tienes:

1. **Documentación evolutiva** — reutiliza el mismo vector store y el mismo flujo de aprobación.
   Wynxx reporta 32,4 h → 3,3 h.
2. **Generación de casos de prueba desde los AC** — extensión natural de la Fase C.
3. Code review profundo — `QualityAuditHandler` ya es el punto de anclaje.

> Arquitectura, modernización legacy y generación de código: **fuera de alcance.**
> Ahí Wynxx tiene años de ventaja y no aporta a tu cuña.

---

## 6. Riesgos

| Riesgo                                                        | Mitigación                                                                                                                                     |
|---------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Desbalance entre módulos (agente al 70%, resto al 15%)        | Fase A obliga a nivelar antes de añadir features                                                                                               |
| Ausencia total de CI/CD                                       | Primer entregable de Fase A. **Deuda real**                                                                                                    |
| Fuga de capacidades específicas de un modelo hacia el dominio | Resolver en el adaptador; el dominio solo habla por `ChatGateway`                                                                              |
| Dependencia operativa de LiteLLM (punto único de fallo)       | Fuera del alcance de la app: es responsabilidad de la plataforma del Banco. Verificar solo el comportamiento ante indisponibilidad del gateway |
| Los Service Principals de Entra ID tardan más de lo previsto  | Avanzar el *resource server* y el perfil `local` sin ellos (§3.3). Que el bloqueo externo no detenga la Fase B                                 |
| "Tenant" sin definir bloquea el aislamiento de datos          | Decisión de producto pendiente. Resolver **antes** de implementar el aislamiento, no después                                                   |
| Perseguir el alcance completo de Wynxx                        | Decisión explícita de foco en una vertical                                                                                                     |

