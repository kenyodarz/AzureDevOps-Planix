# Reglas globales de commits

Todos los commits del monorepo deben seguir este formato:

`COMMIT_TYPE(SCOPE): DESCRIPCIÓN`

## Reglas

- `COMMIT_TYPE` en minúsculas: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `security`.
- `SCOPE` en `snake_case` y breve.
- `DESCRIPCIÓN` en español, con minúscula inicial y claro.

## Ejemplos

- `chore(repo): inicializar monorepo`
- `docs(security): agregar instrucciones globales de secretos`
- `fix(agent): corregir referencias de use cases en tests`

## Reglas de seguridad

- No incluir tokens, passwords, claves ni URLs con credenciales en el mensaje ni en los cambios.
- Si un cambio toca secrets, usa nombres genéricos o placeholders.

