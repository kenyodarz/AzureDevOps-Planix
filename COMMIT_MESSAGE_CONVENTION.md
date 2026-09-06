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
  iniciando en minúscula y sin punto final. El hook `commit-msg` y el pipeline de Azure rechazan
  descripciones que empiecen con esos verbos en inglés.

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

---

## 3. Ejemplos de Commits Válidos

### Nuevas características (MINOR):
- `feat(user_module): agregar función de registro de usuarios reactivo`
- `feat(auth_service): integrar soporte para autenticación biométrica`

### Corrección de errores y parches (PATCH):
- `fix(auth_service): corregir error en la validación de tokens jwt`
- `perf(database_queries): optimizar consulta reactiva de empleados`
- `security(xss_fix): corregir sanitización de entradas en headers http`

### Cambios sin versión (NO RELEASE):
- `docs(api_reference): actualizar documentación de endpoints de rrhh`
- `style(main_controller): aplicar formato de indentación y reglas sonar`
- `refactor(user_controller): desacoplar lógica de presentación hacia el caso de uso`
- `test(auth_tests): agregar pruebas unitarias con convención given-when-then`
- `build(dependencies): actualizar versión de spring boot a última estable`
- `ci(azure_pipeline): agregar validación estricta de commits en pull requests`
- `chore(build_script): limpiar dependencias en desuso del gradle`

### Cambios que rompen compatibilidad (MAJOR):
- `feat(auth_api)!: cambiar contrato de autenticación eliminando soporte básico`
- `fix(employee_model)!: renombrar campo id_empleado a empleado_id en modelo de dominio`

---

## 4. Compatibilidad con Semantic Release (configuración obligatoria)

La tabla de la sección 2 **no funciona con la configuración por defecto** de `semantic-release`.
El plugin `@semantic-release/commit-analyzer` solo reconoce de fábrica `feat`, `fix` y `perf`.
Sin la configuración correcta ocurren tres fallos silenciosos:

| Riesgo | Consecuencia si no se configura |
| :--- | :--- |
| Tipo `security` desconocido | Un parche de seguridad **no publica versión**; el commit se ignora |
| Tipos `deprecated` y `removed` desconocidos | No aparecen en el CHANGELOG; los cambios quedan invisibles |
| Preset `angular` con `!` | El *breaking change* del header puede no detectarse y salir como MINOR en vez de MAJOR |

Para evitarlo se usa el preset **`conventionalcommits`** (no `angular`) y `releaseRules` explícitas.
La configuración validada está en **`.releaserc.json`** en la raíz del repositorio y debe migrarse
tal cual al repositorio de destino.

Dependencias requeridas en el pipeline:

```bash
npm install --no-save \
  semantic-release \
  @semantic-release/commit-analyzer \
  @semantic-release/release-notes-generator \
  @semantic-release/changelog \
  @semantic-release/git \
  conventional-changelog-conventionalcommits
```

> ⚠️ **Squash merge en Azure DevOps:** al completar un Pull Request con *squash*, el mensaje final
> se genera a partir del **título del PR**, no de los commits individuales. El hook `commit-msg`
> local **no** se ejecuta en ese punto. El título del PR debe cumplir esta misma convención, y la
> validación debe reforzarse con una *branch policy* o con la validación de mensajes en
> `azure_build.yaml`.

---

## 5. Activación de las validaciones

### Local (obligatorio al clonar)

```bash
bash tools/setup-hooks.sh
```

Configura `core.hooksPath=.githooks`, otorga permisos de ejecución y fuerza codificación UTF-8
para evitar acentos corruptos en los mensajes.

### Integración continua

El paso *Validate Commit Messages* de `deployment/azure_build.yaml` ejecuta
`tools/commit-lint/validate-range.sh`, que valida los commits del PR **y el título del PR**.

Requiere que el pipeline tenga habilitado *Allow scripts to access the OAuth token*, necesario
para consultar el título del PR vía API REST.

### Fuente única de verdad

Las expresiones regulares viven **solo** en `tools/commit-lint/commit-rules.sh`. El hook local y
el pipeline la consumen desde ahí.

| Archivo | Rol |
| :--- | :--- |
| `tools/commit-lint/commit-rules.sh` | Regex y funciones de validación (única fuente) |
| `tools/commit-lint/validate-range.sh` | Valida un rango de commits + título de PR (CI) |
| `.githooks/commit-msg` | Hook local; delega en `commit-rules.sh` |
| `tools/setup-hooks.sh` | Activa los hooks en un clon nuevo |
| `.releaserc.json` | Configuración de semantic-release alineada con la sección 2 |

> ❌ **Nunca dupliques el regex** en otro archivo. Si cambia un tipo de commit, se modifica
> únicamente `commit-rules.sh` y `.releaserc.json`.
