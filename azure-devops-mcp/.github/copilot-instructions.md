# Configuración de Copilot e Instrucciones para Agentes de IA

Este repositorio requiere que todos los mensajes de commit sigan un estándar estricto estructurado y definido en `COMMIT_RULES.md`. Cualquier agente de IA, Copilot u automatización que trabaje en este workspace debe seguir esta regla de mensajería sin excepciones.

## Reglas para mensajes de commit

Cada mensaje de commit debe tener la siguiente estructura exacta:
`COMMIT_TYPE(SCOPE): DESCRIPTION`

### Especificaciones técnicas:
1. **COMMIT_TYPE**: Debe ser uno de los siguientes tipos admitidos (en minúscula):
   - `feat`: Nuevas funciones o nuevas características.
   - `fix`: Resolución de errores.
   - `docs`: Cambios en la documentación.
   - `style`: Cambios de formato (como comas, espacios, etc.) sin impacto funcional.
   - `refactor`: Refactorización del código sin alterar su comportamiento externo.
   - `perf`: Mejoras de rendimiento.
   - `test`: Añadir o refactorizar pruebas (sin alterar código de dominio/aplicación).
   - `build`: Cambios que afectan el sistema de compilación o dependencias externas.
   - `ci`: Archivos de configuración y scripts de integración continua.
   - `removed`: Eliminación de características obsoletas.
   - `deprecated`: Marcado de características como obsoletas.
   - `security`: Correcciones relacionadas con la seguridad.
   - `chore`: Tareas de construcción, configuraciones, subir cambios sin tag de release, etc.

2. **SCOPE**: Representa el contexto o módulo afectado por el cambio, escrito en minúsculas y utilizando guiones bajos para múltiples palabras (`snake_case` o `snake_camel_case` como se menciona en `COMMIT_RULES.md`). Ejemplo: `user_module`, `auth_service`, `main.css`.

3. **DESCRIPTION**: Una descripción breve, concisa y clara en español de los cambios introducidos, en minúscula.

### Ejemplos correctos:
- `feat(user_module): agregar función de registro de usuarios`
- `fix(auth_service): corregir error en la validación de tokens`
- `docs(api_reference): actualizar documentación de la API`
- `style(main.css): aplicar formato a los estilos`
- `refactor(user_controller): mejorar legibilidad del código`

---
**IMPORTANTE:** Si utilizas Git CLI o herramientas virtuales de Agente para realizar commits de manera directa, asegúrate de aplicar siempre este formato o de que las validaciones del hook local de Git (`.git/hooks/commit-msg`) no rechacen tu contribución.

