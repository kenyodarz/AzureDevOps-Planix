# GitHub Copilot Instructions — Bancolombia InnerSource

Estas instrucciones guían el comportamiento de GitHub Copilot y los asistentes de IA que operan en
este repositorio.

---

## 1. Convención Estricta de Mensajes de Commit

Al generar sugerencias de commits o mensajes de Pull Request:

* Utilizar la convención institucional definida en `COMMIT_MESSAGE_CONVENTION.md`.
* **Estructura:** `<tipo>(<scope>): <descripción en minúscula y español sin punto final>`.
* **Tipos Permitidos:** `feat`, `fix`, `perf`, `security`, `docs`, `style`, `refactor`, `test`,
  `build`, `ci`, `chore`.
* **Scope:** En minúscula y snake_case (ej. `auth_service`, `account_balance`).
* **Idioma:** Descripción y explicaciones obligatoriamente en **español**.

---

## 2. Principios de Código y Arquitectura

* Seguir rigurosamente la arquitectura del proyecto (Clean Architecture, Standalone Components,
  BLoC, OTP, etc.).
* Prohibido introducir credenciales, secretos, API keys o URLs con datos reales en el código.
* Aplicar política "Fail-Closed" en aspectos de seguridad y validación de permisos.
* En casos de ambigüedad técnica o de negocio, detenerse y consultar al desarrollador.
