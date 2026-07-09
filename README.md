# Azure DevOps Monorepo

Este repositorio funciona como un monorepo para una plataforma de integración inteligente con Azure DevOps. Agrupa distintos módulos especializados para cubrir la experiencia de usuario, la lógica de negocio, la automatización y la integración con herramientas de IA.

## Qué incluye este proyecto

- `azure-devops-frontend`: interfaz web en Angular para interactuar con la plataforma.
- `azure-devops-backend`: API y servicios de negocio en Java/Spring.
- `azure-devops-agent`: módulo de orquestación, chat y flujos autónomos.
- `azure-devops-mcp`: servidor MCP para exponer herramientas a modelos y agentes.
- `reports`: informes y documentación de seguimiento del proyecto.

## Estructura del repositorio

- Cada módulo conserva su propia estructura interna y su propio README.
- Las reglas globales del repositorio viven en la raíz.
- Los cambios deben mantenerse coherentes si afectan más de un módulo.

## Reglas de desarrollo

- Revisa `AGENTS.md` para las reglas operativas del monorepo.
- Revisa `AI_WORK_PLAN.md` para el plan de trabajo por fases.
- Revisa `COMMIT_RULES.md` para el formato de commits.
- Revisa `.github/copilot-instructions.md` para las reglas aplicables a GitHub Copilot.

## Seguridad

- No subas secretos, tokens, claves ni archivos `.env`.
- Copia `.env.example` a `.env` solo localmente si aplica.
- Usa variables de entorno o un gestor de secretos para cualquier valor sensible.

## Cómo trabajar en el monorepo

1. Identifica la fase de trabajo: frontend, backend, agente o MCP.
2. Trabaja únicamente en el módulo correspondiente para mantener el cambio acotado.
3. Mantén la documentación y los contratos actualizados si el cambio afecta más de un módulo.
4. Valida el cambio antes de cerrar la tarea.

## Publicar en GitHub

1. Inicializa el repositorio local:
   ```powershell
   git init
   git branch -M main
   git add .
   git commit -m "chore(repo): inicializar monorepo"
   ```
2. Crea el repositorio en GitHub y añade el remoto:
   ```powershell
   git remote add origin https://github.com/<usuario>/<repo>.git
   git push -u origin main
   ```
