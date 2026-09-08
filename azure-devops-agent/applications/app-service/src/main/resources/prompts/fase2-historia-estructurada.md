Genera el borrador estructurado en Markdown de la Historia de Usuario (HU) o Historia Habilitadora
(HA) siguiendo estrictamente la Plantilla Corporativa, la Guía de Agilidad y las directivas
corporativas DP-PL-02.

Usa esta plantilla:
{{plantillaCorporativa}}

Guía de agilidad:
{{guiaAgilidad}}

Directivas de Alcance y Delimitación de Negocio (DP-PL-02):
Debes identificar con precisión la naturaleza de la historia y delimitar estrictamente sus criterios
de aceptación y tareas:

1. **Historia de Usuario (HU - USER_STORY):**
    - **Alcance estricto:** Comprende diseño, construcción/desarrollo funcional, integración,
      pruebas unitarias, pruebas integradas y certificación/aprobación en ambiente **QA**.
    - **PROHIBICIÓN TAXATIVA:** Está estrictamente prohibido incluir tareas, actividades o criterios
      de aceptación relacionados con despliegue a producción, infraestructura productiva o procesos
      operativos de paso a producción.
2. **Historia Habilitadora (HA - ENABLER):**
    - **Alcance técnico u operativo:** Comprende setups técnicos previos de
      arquitectura/infraestructura, o la habilitación operativa formal bajo el **marco corporativo HyMS**
      (construcción y validación de Runbook de operación,
      observabilidad y telemetría, ciberseguridad, mesas de control y soporte post-producción
      inicial).
    - **Delimitación HyMS:** Si la HA corresponde a habilitación/paso a producción, todos los
      criterios de aceptación y tareas deben enfocarse exclusivamente en el cumplimiento riguroso de
      los artefactos y aprobaciones del marco HyMS.
3. **Separación Estricta de Responsabilidades:** Jamás mezcles la construcción funcional de la
   solución con la habilitación operativa o el paso a producción bajo HyMS en una misma historia.

Reglas de Formato e Inyección de Estimación:
1. Presenta la HU/HA completa en formato Markdown encerrada estrictamente dentro de una única caja de código de formato Markdown (delimitada por ```markdown y ```).
2. En el listado de "Definition of Done (DoD)" de la plantilla, incluye obligatoriamente: `- [ ] Cumple todos los criterios de aceptación`. Asegúrate de no duplicar corchetes ni el formato de lista.
3. Evalúa la complejidad en Story Points (Fibonacci) y el nivel de incertidumbre de forma REALISTA.
   - **No juegues a ser 'The Wizard of Master Code'**. Estima pensando en el esfuerzo real de un desarrollador humano (escribir código, pruebas unitarias y de integración, documentar, pasar pipelines).
   - **Criterio único de estimación**: usa la siguiente tabla de equivalencia. La estimación debe corresponder al rango de horas que realmente tomará completar el trabajo.

{{tablaEstimacion}}

   - **No apliques mínimos por tipo de tecnología.** Una tarea de base de datos, API o seguridad puede estimarse en 1 o 2 puntos si el esfuerzo real está por debajo de 4 horas. Lo que manda es el esfuerzo en horas, no la etiqueta tecnológica.
   - Si la estimación supera los **8 puntos** (más de 30 horas), la historia debe dividirse.
4. Al final de tu respuesta (FUERA de la caja de código), debes retornar obligatoriamente un bloque JSON estructurado con el siguiente formato exacto:
```json
{
  "puntos": X,
  "incertidumbre_nivel": "Nula/Baja/Media/Alta/Critica",
  "incertidumbre_justificacion": "..."
}
```
No agregues texto explicativo después del bloque JSON.

