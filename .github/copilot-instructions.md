# Instrucciones globales para GitHub Copilot y Agentes de IA

Este repositorio es un monorepo. Al modificar código, respeta las reglas de cada módulo, la arquitectura global y las directrices de este archivo.

## Reglas obligatorias de Seguridad e Infraestructura

- **No introducir secretos**, tokens, API keys ni contraseñas en código, configuraciones, ejemplos ni documentación.
- Usar variables de entorno, GitHub Secrets, Azure DevOps Secrets o un gestor de secretos para cualquier valor sensible.
- Si un archivo debe contener valores locales, usar `.env.example` o placeholders y no subir el archivo real.
- Mantener los cambios coherentes entre módulos cuando se tocan interfaces compartidas.
- Evitar subir artefactos generados (`build/`, `dist/`, `node_modules/`, `.gradle/`, `coverage/`, `reports/`).

## Estándares de Arquitectura y Código (Rules Globales)

Para modificaciones en componentes específicos, debes seguir estrictamente los manuales de arquitectura global ubicados en la carpeta `/rules`:

1. **Backend (Java Spring Boot)**:
   - Sigue los principios de **Clean Architecture & DDD** de Bancolombia.
   - El dominio (`domain/model`) y los casos de uso (`domain/usecase`) deben permanecer puros, sin anotaciones o dependencias técnicas de Spring.
   - Consulta el manual completo y el checklist de entrega en: `/rules/spring-rules.md`.

2. **Frontend (Angular)**:
   - Sigue una arquitectura modular basada en **features** con desacoplamiento estricto.
   - Todos los componentes deben ser standalone (`standalone: true`) y utilizar la nueva sintaxis de control de flujo (`@if`, `@for`).
   - El estado se gestiona de manera reactiva mediante **Signals & RxJS**, previniendo fugas de memoria con `takeUntilDestroyed()` o `AsyncPipe`.
   - Consulta el manual completo y el checklist en: `/rules/angular-rules.md`.

## Estilo y Desarrollo

- Preferir cambios pequeños, enfocados y claros.
- Mantener la arquitectura y los límites bien definidos de cada módulo.
- Si el cambio afecta varios módulos, documentarlo en la raíz del monorepo.
- **Política de No-Asunción**: Nunca asumas información que no esté explícita en los requerimientos. Ante cualquier duda contractual, visual, de nombres o técnica, detén el desarrollo y consulta al usuario.

## Reglas Obligatorias para Mensajes de Commit

Cualquier commit realizado en este workspace por agentes de IA, automatizaciones o desarrolladores **debe** cumplir con el formato estandarizado definido de manera global en `COMMIT_RULES.md`:

`COMMIT_TYPE(SCOPE): DESCRIPTION`

- **COMMIT_TYPE**: En minúscula, debe ser uno de: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `removed`, `deprecated`, `security`, `chore`.
- **SCOPE**: En minúsculas y entre paréntesis, utilizando `snake_case` (ej. `auth_service`, `user_module`).
- **DESCRIPTION**: Mensaje corto, conciso, claro y en español, comenzando con minúscula.

Ejemplos válidos:
- `feat(user_module): agregar función de registro de usuarios`
- `fix(auth_service): corregir error en la validación de tokens`

