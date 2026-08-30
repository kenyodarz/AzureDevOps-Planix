package co.com.bancolombia.model.dashboard;

/**
 * Nombre de la célula o equipo cuyo backlog se audita, por ejemplo {@code "EQU1096 - EXODIA"}.
 *
 * <p><b>Fase 05 (D-09, B-01).</b> Hasta ahora el BFF recibía un {@code String} desnudo y lo
 * convertía en un {@code AreaPath} de Azure DevOps concatenando el proyecto y una barra invertida.
 * Esa traducción no era asunto suyo: el MCP ya la hace —y mejor, porque le pregunta a Azure DevOps
 * por el {@code AreaPath} real del equipo y solo concatena como último recurso—. El BFF duplicaba
 * el trabajo y, al enviar la ruta ya montada, dejaba muerta la resolución dinámica del MCP.
 *
 * <p>Lo que sí es del dominio del BFF es el <b>nombre</b>: que exista, que no venga en blanco y
 * que viaje sin espacios sobrantes. Eso es lo que este tipo garantiza, y por eso sustituye al
 * {@code AreaPath} que la Fase 05 planteaba en su borrador (T-01).
 */
public record TeamName(String value) {

    private static final String BLANK_MESSAGE = "El nombre de la célula no puede estar vacío";

    public TeamName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(BLANK_MESSAGE);
        }
        value = value.trim();
    }

    public static TeamName of(String raw) {
        return new TeamName(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}

