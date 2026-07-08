# ¿Cómo estructurar el mensaje de un commit?

La forma de un commit es: `COMMIT_TYPE(SCOPE): DESCRIPTION`

Ejemplo: `feat(button): Se crea componente botón.`

- **COMMIT_TYPE**: es obligatorio. Más abajo podrás encontrar el estándar de sus posibles opciones y usos.
- **SCOPE**: la forma correcta de escribirlo es usando `snake_camel_case`. El alcance del commit puede ser el nombre del componente o archivo modificado.
- **DESCRIPTION**: es obligatorio. Es una descripción concisa de los cambios.

# Listado de COMMIT_TYPE

| Type       | Description                                                                                     |
|------------|-------------------------------------------------------------------------------------------------|
| feat       | **Feature**: Utilizar cuando creamos nuevas funciones o nuevas características                  |
| fix        | **Bug Fixed**: Aplica cuando resolvemos errores                                                 |
| docs       | **Documentation**: Usar cuando realizamos cambios en la documentación                           |
| style      | **Styles**: Cuando se aplican cambios en formato, comas y puntos faltantes, etc; sin cambios en el código |
| refactor   | **Code Refactoring**: refactorización del código, sin cambios en la funcionalidad               |
| perf       | **Performance Improvements**: Cuando hacemos mejoras de rendimiento                             |
| test       | **Tests**: Usar cuando aplicamos pruebas, refactorización de pruebas; sin cambios en el código  |
| build      | **Build System**: cambios que afectan el sistema de compilación o las dependencias externas     |
| ci         | **Continuous Integration**: Aplica al hacer cambios en scripts y archivos de configuración de la integración continua CI |
| removed    | **Removed Feature**: Eliminación de caractrísticas obsoletas                                    |
| deprecated | **Deprecated Feature**: Usar cuando marcamos características como obsoletas                     |
| security   | **Security Fixed**: Usar para identificar correcciones de seguridad                             |
| chore      | **Chore**: Aplica para actualización de tareas de build, configuraciones, subir cambios sin generar releases o tags|

## Ejemplos de commits

- `feat(user_module): agregar función de registro de usuarios`
- `fix(auth_service): corregir error en la validación de tokens`
- `docs(api_reference): actualizar documentación de la API`
- `style(main.css): aplicar formato a los estilos`
- `refactor(user_controller): mejorar legibilidad del código`
- `perf(database_queries): optimizar consultas a la base de datos`
- `test(auth_tests): añadir pruebas unitarias para el servicio de autenticación`
- `build(dependencies): actualizar librerías externas`
- `ci(github_actions): modificar workflow para integración continua`
- `removed(old_feature): eliminar característica obsoleta`
- `deprecated(api_v1): marcar la versión 1 de la API como obsoleta`
- `security(xss_fix): arreglar vulnerabilidad de cross-site scripting`
- `chore(build_script): actualizar script de build`