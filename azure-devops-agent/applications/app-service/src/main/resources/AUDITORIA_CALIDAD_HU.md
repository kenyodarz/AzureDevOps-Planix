# PROTOCOLO DE AUDITORÍA DE CALIDAD DE HISTORIAS DE USUARIO Y HABILITADORES (HU/HA)
## Estándares Corporativos de Bancolombia - Fase 2 de Análisis de Equipo

Este protocolo establece los lineamientos detallados, criterios de puntuación, ejemplos y casos de borde para evaluar la calidad interna de los ítems de trabajo (Work Items) en Azure DevOps.

---

## 1. CRITERIOS DE EVALUACIÓN Y PUNTUACIÓN (Total: 100 puntos)

La evaluación se realiza de manera cuantitativa y cualitativa distribuida en las siguientes 6 dimensiones:

| Dimensión | Puntos Máx. | Criterios de Validación Corporativos |
| :--- | :---: | :--- |
| **Título** | **10** | • Formato estricto: `<Equipo/Frente> \| <Nombre de la HU/HA>` (Ej: `Plataforma Colectivos \| Cambiar Contraseña`).<br>• Si no se define el frente u organización, se evaluará como "Parcial". |
| **Descripción** | **20** | • Uso estructurado del formato oficial en primera persona de cara al usuario:<br>  `Yo como <rol>, requiero <necesidad/funcionalidad> para <beneficio/finalidad>`. <br>• Redacción fluida en prosa, sin ambigüedades técnicas y comprensible por cualquier perfil de la célula ágil. |
| **Criterios de Aceptación** | **25** | • Estructurados en forma de checklist o lista medible con casillas `[ ]`. <br>• Deben ser condiciones específicas y de prueba para decidir objetivamente su aprobación/rechazo por parte del Product Owner (PO). |
| **Definition of Done (DoD)** | **25** | • Incorporación y completitud de los **7 ítems obligatorios corporativos**:<br>  1. `[ ] Desarrollo completado`<br>  2. `[ ] Pruebas unitarias ejecutadas`<br>  3. `[ ] Pruebas de integración exitosas`<br>  4. `[ ] Documentación actualizada`<br>  5. `[ ] Validaciones de seguridad aplicadas`<br>  6. `[ ] Cumple todos los criterios de aceptación`<br>  7. `[ ] Código mergeado en rama principal` |
| **Estimación (Story Points)** | **10** | • Coherencia con la escala de Fibonacci (1, 2, 3, 5, 8). Se debe evaluar si la estimación es realista según la descripción y alcance del Work Item. La guía no impone un puntaje mínimo obligatorio por tipo de tarea técnica, por lo cual estimar 1, 2 o 3 SP es perfectamente válido si el esfuerzo y la complejidad lo ameritan.<br>• Si la estimación supera los 8 SP, debe recomendarse su fragmentación. |
| **Estado y Asignación** | **10** | • Estados válidos y coherentes: `New`, `Active`, `Impedimento`, `Closed`.<br>• Debe tener un responsable asignado y la prioridad establecida apropiadamente (1 al 4). |

---

## 2. EJEMPLOS DE EVALUACIÓN

### A. HISTORIA DE USUARIO CORRECTA (100/100)
*   **Título:** `Canales Digitales | Recuperación de contraseña por SMS`
*   **Descripción:** `Yo como cliente de Bancolombia, requiero una opción de recuperación de clave vía SMS en la app personas para poder acceder a mi cuenta de manera segura en caso de olvido.`
*   **Criterios de Aceptación:**
    *   `[ ] Validar que el botón 'Olvidé contraseña' esté accesible en el login.`
    *   `[ ] Verificar que el SMS se envíe al teléfono celular registrado en un lapso de máximo 60 segundos.`
    *   `[ ] Confirmar que el código OTP expire después de 5 minutos.`
*   **DoD:** Contiene exactamente los 7 ítems obligatorios.
*   **Story Points:** `5` (Estimación realista considerando la integración del SMS y las pruebas de timeout de OTP).

### B. HISTORIA DE USUARIO DEFICIENTE (40/100)
*   **Título:** `Backend - Fix OTP` *(Defecto: No usa nomenclatura de Equipo/Frente)*
*   **Descripción:** `Cambiar el endpoint de validación de OTP para que funcione con el nuevo proveedor.` *(Defecto: Redacción meramente técnica, sin estructura de rol, necesidad y beneficio)*
*   **Criterios de Aceptación:** No cuenta con ningún listado explícito de validación del PO.
*   **DoD:** Ausente en su descripción.
*   **Story Points:** `13` *(Defecto: Supera los 8 puntos sin proponer división)*

---

## 3. MANEJO DE CASOS DE BORDE (EDGE CASES)
1.  **Descripciones con Etiquetas HTML:** Azure DevOps guarda el detalle con tags (`<div>`, `<br>`, etc.). El proceso de auditoría debe ignorar o sanear el HTML para centrarse puramente en la cohesión de la narrativa y la semántica ágil.
2.  **Habilitadores de Arquitectura o Técnicos (HA):** Los habilitadores técnicos (p. ej., actualización de infraestructura, migraciones o análisis de seguridad) no siempre siguen la plantilla de usuario de manera fluida en la descripción, pero sí deben cumplir el título estructurado `<Equipo/Frente> | <Nombre de la HA>` y responder a un rol técnico (ej: `Yo como arquitecto de software, requiero configurar el pipeline de CI/CD para...`).

