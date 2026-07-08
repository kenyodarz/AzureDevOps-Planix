# Azure DevOps Monorepo

Este directorio ahora funciona como un monorepo de Git que agrupa los módulos:

- `azure-devops-agent`
- `azure-devops-backend`
- `azure-devops-frontend`
- `azure-devops-mcp`
- `reports`

## Estructura

- Cada módulo conserva su propia estructura interna.
- Las reglas globales del repositorio viven en la raíz.
- Los secretos nunca deben subirse al repositorio; usa variables de entorno o un gestor de secretos.

## Reglas globales

- Revisa `AGENTS.md` para las reglas de desarrollo del monorepo.
- Revisa `COMMIT_RULES.md` para el formato de commits.
- Revisa `.github/copilot-instructions.md` para las reglas aplicables a GitHub Copilot.

## Seguridad

- Copia `.env.example` a `.env` solo localmente.
- No subas archivos `.env`, claves, tokens ni certificados.
- Si necesitas un secret para desarrollo, configúralo en GitHub Actions, Azure DevOps o tu gestor de secretos.

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

