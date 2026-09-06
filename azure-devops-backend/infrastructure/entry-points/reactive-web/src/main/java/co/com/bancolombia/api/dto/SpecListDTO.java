package co.com.bancolombia.api.dto;

import java.util.List;

/**
 * Representación HTTP del listado de especificaciones disponibles en el repositorio documental.
 *
 * @param specs Lista de identificadores o nombres de archivo de las especificaciones
 * @param count Total de documentos encontrados
 */
public record SpecListDTO(List<String> specs, int count) {

    public SpecListDTO {
        specs = specs == null ? List.of() : List.copyOf(specs);
    }

    public SpecListDTO(List<String> specs) {
        this(specs, specs != null ? specs.size() : 0);
    }
}
