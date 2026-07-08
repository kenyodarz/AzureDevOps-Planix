# Reglas globales del monorepo

Este repositorio es un monorepo. Cada subproyecto mantiene su propia estructura y reglas internas, pero las reglas de operación del repositorio deben aplicarse de forma global.

## Reglas obligatorias

- No subas secretos, tokens, claves ni archivos `.env` al repositorio.
- Usa variables de entorno y gestores de secretos para valores sensibles.
- Mantén las dependencias y configuraciones compatibles con el módulo correspondiente.
- Si cambias varios módulos, revisa que la coherencia del monorepo se mantenga.
- Antes de terminar un cambio, verifica que no se hayan agregado archivos locales ni artefactos generados.

## Referencias

- Para reglas de commits, consulta `COMMIT_RULES.md`.
- Para instrucciones específicas para GitHub Copilot, consulta `.github/copilot-instructions.md`.
- Si un módulo tiene su propio `AGENTS.md`, respétalo y compleméntalo, no lo contradigas.

