# Convención de Commits y Semantic Release

Este repositorio utiliza el estándar de **Conventional Commits** adaptado para **Semantic Release**. Todo commit debe seguir una estructura estricta para garantizar la generación automática y confiable de versiones (SemVer) y del CHANGELOG.

---

## 1. Estructura del Mensaje

```text
COMMIT_TYPE(SCOPE): DESCRIPTION
```
O para cambios que rompen la compatibilidad (*Breaking Changes*):
```text
COMMIT_TYPE(SCOPE)!: DESCRIPTION
```

### Componentes obligatorios:
- **`COMMIT_TYPE`**: Tipo de cambio en minúsculas (ver tabla de tipos).
- **`SCOPE`**: Módulo, componente o archivo afectado, delimitado entre paréntesis `()` y escrito
  estrictamente en **`snake_case`** (minúsculas, números y guiones bajos, ej. `auth_service`,
  `user_module`, `database_client`, `commit_hooks`).
- **`!` (Opcional)**: Agregado inmediatamente después del cierre del paréntesis para indicar un **Breaking Change** (dispara versión **MAJOR**).
- **`: `**: Dos puntos obligatorios seguidos de un espacio.
- **`DESCRIPTION`**: Breve descripción concisa de los cambios **estrictamente en español**
  (PROHIBIDO usar inglés como `add...`, `update...`, `fix...`, `implement...`, `create...`),
  iniciando en minúscula y sin punto final.

---

## 2. Tipos de Commit y su Impacto en Semantic Release

| Tipo (`COMMIT_TYPE`) | Impacto SemVer | Descripción |
| :--- | :--- | :--- |
| `feat` | **MINOR** (`x.Y.0`) | Nuevas funciones o características para el usuario o sistema |
| `fix` | **PATCH** (`x.x.Z`) | Corrección de errores (bugs) |
| `perf` | **PATCH** (`x.x.Z`) | Mejoras directas de rendimiento y consumo de recursos |
| `security` | **PATCH** (`x.x.Z`) | Correcciones de seguridad y parches de vulnerabilidades |
| `docs` | **NO RELEASE** | Cambios exclusivos en documentación |
| `style` | **NO RELEASE** | Formato, comas, espacios o linteo sin cambios en lógica de código |
| `refactor` | **NO RELEASE** | Refactorización de código sin alterar el comportamiento funcional |
| `test` | **NO RELEASE** | Añadir, corregir o refactorizar pruebas unitarias / integración |
| `build` | **NO RELEASE** | Cambios en el sistema de compilación o dependencias externas |
| `ci` | **NO RELEASE** | Modificación en pipelines y scripts de Integración Continua |
| `chore` | **NO RELEASE** | Tareas misceláneas, herramientas de desarrollo o configuraciones |
| `deprecated` | **NO RELEASE** | Marcado de funcionalidades o contratos como obsoletos |
| `removed` | **NO RELEASE** | Eliminación de código o componentes obsoletos |

> 💥 **BREAKING CHANGES (Versión MAJOR `X.0.0`):**  
> Cualquier tipo con `!` (ej. `feat(api)!: ...` o `fix(auth)!: ...`) o que incluya en el pie del mensaje `BREAKING CHANGE: <descripción>` generará automáticamente una versión **MAJOR**.
