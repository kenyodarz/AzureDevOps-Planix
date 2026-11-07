# Reglas Técnicas y Gobernanza de Arquitectura — <Nombre del Stack / Módulo>

> **Propósito:** Definir los límites arquitectónicos, directivas de codificación y restricciones técnicas que todo Agente de IA y desarrollador debe cumplir obligatoriamente al modificar código en este componente.

---

## 1. Principios de Arquitectura Obligatorios

* **Separación de Capas:** Mantener la pureza del dominio (Clean Architecture / Hexagonal). Cero dependencias de infraestructura o frameworks en modelos de negocio.
* **Inmutabilidad y Robustez:** Favorecer objetos de valor inmutables, records o clases selladas. Validación estricta de invariantes en el constructor.
* **Gestión de Errores:** Prohibido atrapar excepciones silenciosamente o retornar valores por defecto mágicos sin registrar el error.

---

## 2. Restricciones Técnicas y Prohibiciones (Qué NO Hacer)

1. **Dependencias no autorizadas:** Queda prohibido instalar librerías externas sin justificación y aprobación explícita.
2. **Secretos y Seguridad:** Cero credenciales, tokens o URLs sensibles hardcodeadas en código o tests (detección Sonar S2068).
3. **Complejidad Cognitiva:** Ningún método debe superar el límite de complejidad cognitiva estipulado (máximo 10 a 15 según linters).
4. **Acoplamiento Indebido:** Clases controladoras o entry-points no deben contener lógica de negocio; solo validan entrada y delegan al caso de uso.

---

## 3. Estándar de Testing y Cobertura

* **Cobertura Mínima:** Cobertura de líneas $\ge 90\%$ en capas de lógica de negocio (`domain/usecase` y `domain/model`).
* **Patrón de Naming:** Pruebas unitarias estructuradas bajo el patrón `Given-When-Then` o nomenclatura descriptiva en español.
* **Aislamiento:** Pruebas unitarias aisladas sin dependencias de red ni bases de datos activas (usar mocks, stubs o bases en memoria).
