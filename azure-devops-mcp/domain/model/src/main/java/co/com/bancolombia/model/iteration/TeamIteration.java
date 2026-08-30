package co.com.bancolombia.model.iteration;

import lombok.Builder;
import lombok.Data;

/**
 * Iteración (sprint) tal y como la define Azure DevOps para un equipo.
 *
 * <p>El campo que importa es {@link #path}: es el {@code IterationPath} <b>real</b>, el que Azure
 * DevOps usa en las consultas WIQL. Hasta ahora nadie lo pedía: se fabricaba concatenando el
 * proyecto, el año en curso y el nombre del sprint. Ese año calculado era el fallo: un sprint que
 * va de diciembre a enero pertenece al año en que empezó, no al año en que se consulta, así que en
 * enero la ruta apuntaba a una iteración inexistente y la consulta devolvía cero ítems sin error.
 *
 * <p>Preguntar en vez de calcular no es un invento: es lo que este mismo servidor ya hacía para el
 * {@code AreaPath} mediante {@code teamfieldvalues}. Aquí se completa la simetría que quedó a
 * medias.
 */
@Data
@Builder(toBuilder = true)
public class TeamIteration {

    private String id;
    private String name;
    private String path;
}

