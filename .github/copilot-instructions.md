# Instrucciones globales para GitHub Copilot

Este repositorio es un monorepo. Al modificar código, respeta las reglas de cada módulo y las reglas del monorepo.

## Reglas obligatorias

- No introducir secretos, tokens, API keys ni contraseñas en código, configuraciones, ejemplos ni documentación.
- Usar variables de entorno, GitHub Secrets, Azure DevOps Secrets o un gestor de secretos para cualquier valor sensible.
- Si un archivo debe contener valores locales, usar `.env.example` o placeholders y no subir el archivo real.
- Mantener los cambios coherentes entre módulos cuando se tocan interfaces compartidas.
- Evitar subir artefactos generados (`build/`, `dist/`, `node_modules/`, `.gradle/`, `coverage/`).

## Estilo

- Preferir cambios pequeños y claros.
- Mantener la arquitectura y los límites de cada módulo.
- Si el cambio afecta varios módulos, documentarlo en la raíz del monorepo.

