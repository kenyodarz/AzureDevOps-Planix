package co.com.bancolombia.model.workitem;

/**
 * Ámbito resuelto de una consulta: la ruta de área de la célula y la ruta de iteración del sprint.
 *
 * <p><b>Value Object inmutable.</b> Es lo que {@code ResolveTeamScopeUseCase} produce y lo único
 * que {@link WiqlStatement} necesita para redactar la sentencia.
 *
 * <p>⚠️ <b>Por qué solo se rechazan los nulos y no las cadenas en blanco.</b> Azure DevOps puede
 * devolver un {@code defaultValue} vacío para el campo de área de una célula mal configurada. El
 * comportamiento heredado en ese caso era emitir la consulta igualmente, con
 * {@code [System.AreaPath] = ''}. Rechazar aquí las cadenas en blanco convertiría ese tablero vacío
 * en un error, que es <b>exactamente</b> el cambio de comportamiento observable que DP-04 §0.1
 * descartó al elegir la opción (a). La invariante se endurecerá cuando se decida qué debe ver el
 * cliente ante un fallo (DP-06, Fase 06).
 */
public record TeamScope(String areaPath, String iterationPath) {

    public TeamScope {
        if (areaPath == null) {
            throw new IllegalArgumentException("El AreaPath no puede ser nulo");
        }
        if (iterationPath == null) {
            throw new IllegalArgumentException("El IterationPath no puede ser nulo");
        }
    }
}

