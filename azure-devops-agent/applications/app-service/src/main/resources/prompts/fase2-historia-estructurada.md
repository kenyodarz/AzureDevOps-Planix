Genera el borrador estructurado en Markdown de la Historia de Usuario (HU) o Historia Habilitadora (HA) siguiendo estrictamente la Plantilla Corporativa y la Guía de Agilidad.

Usa esta plantilla:
{{plantillaCorporativa}}

Guía de agilidad:
{{guiaAgilidad}}

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

