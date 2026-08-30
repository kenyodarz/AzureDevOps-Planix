package co.com.bancolombia.model.workitem;

import java.util.regex.Pattern;

/**
 * Repliegue por concatenación: fabrica las rutas de área e iteración cuando Azure DevOps no puede
 * resolverlas.
 *
 * <p><b>Servicio de dominio.</b> Hasta la Fase 04 estas dos reglas vivían como métodos privados del
 * entry-point (deuda <b>D-09</b>, heredada como D-37 del plan del BFF). <b>DP-04 §0.1 decidió la
 * opción (a): el repliegue se conserva —incluido el año del calendario— pero deja de ser
 * invisible</b>, porque quien lo invoca está obligado a contarlo
 * ({@link co.com.bancolombia.model.workitem.gateways.TeamScopeFallbackMetrics}).
 *
 * <p>⚠️ <b>Por qué el año se recibe como parámetro y no se calcula aquí.</b> El código original
 * hacía {@code LocalDate.now(ZoneId.systemDefault()).getYear()} en mitad de la construcción de la
 * ruta, lo que convertía el resultado en dependiente del reloj de la máquina y por tanto imposible
 * de probar de forma determinista. Recibirlo como argumento mantiene el dominio puro y deja la
 * elección del reloj en manos del caso de uso.
 *
 * <p>⚠️ <b>Este repliegue es defectuoso y se conserva a propósito.</b> Un sprint que va de
 * diciembre a enero pertenece al año en que <b>empezó</b>: consultado en enero, la ruta apunta a
 * una iteración que no existe y el tablero sale vacío sin error. Conservarlo es la única opción sin
 * regresión posible (los puntos 5 y 6 de {@code CONTRATO-MCP.md} §2.4 lo declaran contrato de
 * facto); medirlo es lo que permitirá retirarlo con datos.
 */
public final class TeamPathFallback {

    private static final char PATH_SEPARATOR = '\\';

    /** Sprints con la forma {@code Sprint 247}, los únicos a los que se les intercala el año. */
    private static final Pattern NUMBERED_SPRINT = Pattern.compile("Sprint \\d+");

    private TeamPathFallback() {
        // Servicio de dominio sin estado
    }

    /**
     * Ruta de área fabricada: se antepone el proyecto salvo que la célula ya empiece por él.
     */
    public static String areaPath(String project, TeamName team) {
        String value = team.value();
        if (!value.startsWith(project)) {
            return project + PATH_SEPARATOR + value;
        }
        return value;
    }

    /**
     * Ruta de iteración fabricada. Si el sprint tiene la forma {@code Sprint N} se intercala el
     * año indicado; en cualquier otro caso solo se antepone el proyecto.
     *
     * @param year año del calendario, inyectado por el caso de uso
     */
    public static String iterationPath(String project, SprintName sprint, int year) {
        String value = sprint.value();
        if (value.startsWith(project)) {
            return value;
        }
        if (interleavesCalendarYear(sprint)) {
            return project + PATH_SEPARATOR + year + PATH_SEPARATOR + value;
        }
        return project + PATH_SEPARATOR + value;
    }

    /**
     * Indica si el repliegue va a intercalar el año del calendario, para que la métrica pueda
     * distinguir el caso realmente peligroso del inocuo.
     */
    public static boolean interleavesCalendarYear(SprintName sprint) {
        return NUMBERED_SPRINT.matcher(sprint.value()).matches();
    }
}

