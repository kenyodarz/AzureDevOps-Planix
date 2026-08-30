package co.com.bancolombia.model.team;

import lombok.Builder;

/**
 * Iteración (sprint) tal y como la define Azure DevOps para un equipo.
 *
 * <p>El campo que importa es {@code path}: es el {@code IterationPath} <b>real</b>, el que Azure
 * DevOps usa en las consultas WIQL. Hasta la Fase 04 nadie lo pedía: se fabricaba concatenando el
 * proyecto, el año en curso y el nombre del sprint. Ese año calculado era el fallo: un sprint que
 * va de diciembre a enero pertenece al año en que empezó, no al año en que se consulta, así que en
 * enero la ruta apuntaba a una iteración inexistente y la consulta devolvía cero ítems sin error.
 *
 * <p>Preguntar en vez de calcular no es un invento: es lo que este mismo servidor ya hacía para el
 * {@code AreaPath} mediante {@code teamfieldvalues}. Aquí se completa la simetría que quedó a
 * medias.
 *
 * <p><b>Cambio de la Fase 05 (D-14).</b> Esta clase vivía en el paquete {@code model.iteration},
 * nombrado por la operación que la devolvía y no por el agregado al que pertenece. Convive ahora
 * con {@link TeamFieldValues} en {@code model.team}, porque las dos describen lo mismo: el ámbito
 * de un equipo. <b>Ni un campo ni una anotación han cambiado</b>; solo el paquete.
 *
 * <p><b>Cambio de la Fase 07 (D-16).</b> Deja de ser una clase {@code @Data} mutable y pasa a ser un
 * objeto de valor inmutable. <b>Los tres componentes conservan su nombre.</b> No se rechaza ningún
 * valor: {@code GetTeamIterationsUseCase} ya tolera expresamente un {@code name} nulo al buscar el
 * sprint, y endurecerlo aquí convertiría en error una respuesta que hoy simplemente no casa
 * (DP-07 §0.2(c), precedente de {@code TeamScope}).
 */
@Builder(toBuilder = true)
public record TeamIteration(String id, String name, String path) {
}
