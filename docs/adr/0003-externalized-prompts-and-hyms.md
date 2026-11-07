# ADR-0003: Externalización de Prompts y Delimitación Normativa HyMS (DP-PL-02)

* **Fecha:** 2026-08-25
* **Estado:** Aceptado
* **Autor(es):** Architecture & Governance Team
* **Aprobadores:** Lead Architect

---

## 1. Contexto y Planteamiento del Problema

Los prompts utilizados por los agentes contenían directivas quemadas dentro de clases de Java,
dificultando su versionamiento, revisión por pares y auditoría de seguridad. Adicionalmente, existía
una inconsistencia recurrente en las historias de usuario generadas: los criterios de aceptación
mezclaban tareas de desarrollo de software con actividades de infraestructura de despliegue a
producción y habilitación operativa (marcos HyMS).

---

## 2. Opciones Evaluadas

| Opción                                                                                                                   | Ventajas                                                                                         | Desventajas                                                                                     |
|--------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| **Opción A:** Prompts en Strings Java dentro de los casos de uso                                                         | Cero infraestructura adicional.                                                                  | Código sucio, no auditable por no-desarrolladores, mezcla de responsabilidades en el dominio.   |
| **Opción B:** Prompts dinámicos en base de datos remota                                                                  | Edición en tiempo de ejecución sin redespliegue.                                                 | Introduce dependencia de red, latencia de consulta y riesgo de inyección no controlada por Git. |
| **Opción C:** Externalización en archivos Markdown versionados bajo `resources/prompts/` con puerto `PromptTemplatePort` | Prompts auditables en Git; versionamiento seguro; directiva corporativa taxativa (**DP-PL-02**). | Requiere compilar y desplegar para actualizar una plantilla.                                    |

---

## 3. Decisión Adoptada

**Adoptamos la Opción C:**

1. **Externalización en `applications/app-service/src/main/resources/prompts/`:**  
   El dominio interactúa exclusivamente mediante el puerto `PromptTemplatePort`, implementado por
   `ClasspathPromptTemplateAdapter`. El código Java no contiene texto de prompts quemados.
2. **Directiva Normativa DP-PL-02 (Delimitación de Historias):**  
   Se consagra taxativamente en las plantillas `program-planning-roadmap.md` y
   `fase2-historia-estructurada.md` la separación:
    * **Historias de Usuario (HU):** Alcance delimitado a análisis, diseño, construcción y pruebas
      certificadas hasta el ambiente de QA.
    * **Historias de Acompañamiento (HA):** Cubren la habilitación operativa, validación de marcos
      normativos HyMS, despliegue a preproducción/producción y soporte post-lanzamiento.

---

## 4. Consecuencias y Compromisos

### Positivas:

* Eliminación total de mezclas normativas en las historias de usuario generadas por IA.
* Pruebas automáticas (`PromptTemplateContentTest`) validan que ninguna plantilla contenga variables
  residuales sin resolver ni ambigüedades.
* Auditoría de cambios en prompts mediante Pull Requests de Git convencionales.

### Negativas / Riesgos Aceptados:

* Modificar una instrucción o directiva de un prompt requiere ejecutar el pipeline de compilación y
  despliegue del microservicio.

---

## 5. Referencias y Enlaces Relacionados

* `azure-devops-agent/docs/fases/FASE-07-prompts-story-creator-hyms.md`
* `azure-devops-agent/docs/plan/DECISIONES_PENDIENTES.md` (DP-PL-02)
