package co.com.bancolombia.model.workitem;

/**
 * Sentencia WIQL lista para enviarse a Azure DevOps.
 *
 * <p><b>Value Object inmutable y única fábrica de sentencias WIQL del sistema.</b> Hasta la Fase 04
 * la sentencia se redactaba con un {@code String.format} dentro del entry-point (deuda
 * <b>D-07</b>), en contra de la regla «cero lógica de negocio en {@code entry-points}» de
 * {@code spring-rules.md}.
 *
 * <p>⚠️ <b>La plantilla es intocable.</b> Está congelada carácter a carácter en
 * {@code BASELINE.md} §7.1 y verificada en sus <b>8 ramas</b> por
 * {@code ListWorkItemsByTeamAndSprintUseCaseTest}. Un espacio de más no produce un error: produce
 * una consulta válida que devuelve cero elementos, y el tablero sale vacío sin que nadie se entere.
 */
public record WiqlStatement(String value) {

    /**
     * Plantilla vigente, copiada literalmente del entry-point del que procede.
     *
     * <p>El orden de los argumentos es <b>IterationPath, AreaPath, tipos</b>: invertir los dos
     * primeros produciría una sentencia sintácticamente válida y semánticamente equivocada.
     */
    private static final String TEMPLATE =
            "SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project AND [System.IterationPath] = '%s' AND [System.AreaPath] = '%s' AND [System.WorkItemType] IN (%s) ORDER BY [System.Id]";

    public WiqlStatement {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "La sentencia WIQL no puede ser nula ni estar en blanco");
        }
    }

    /**
     * Redacta la consulta que lista los elementos de trabajo de una célula en un sprint.
     */
    public static WiqlStatement forTeamAndSprint(TeamScope scope, WorkItemTypes types) {
        return new WiqlStatement(String.format(TEMPLATE,
                scope.iterationPath(), scope.areaPath(), types.toWiqlList()));
    }
}

