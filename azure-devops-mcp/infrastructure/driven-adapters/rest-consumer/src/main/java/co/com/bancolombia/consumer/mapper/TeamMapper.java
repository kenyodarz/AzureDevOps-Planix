package co.com.bancolombia.consumer.mapper;

import co.com.bancolombia.consumer.dto.TeamFieldValueDTO;
import co.com.bancolombia.consumer.dto.TeamFieldValuesDTO;
import co.com.bancolombia.consumer.dto.TeamIterationDTO;
import co.com.bancolombia.consumer.dto.TeamIterationsDTO;
import co.com.bancolombia.model.iteration.TeamIteration;
import co.com.bancolombia.model.team.TeamFieldValues;
import java.util.List;

/**
 * Mapper de respuesta: DTOs de Azure DevOps → modelos de dominio de equipo.
 *
 * <p>Cubre las dos consultas que sostienen la resolución del {@code AreaPath} y del
 * {@code IterationPath}. Su contenido es <b>exactamente</b> el de los métodos privados
 * {@code toDomain(...)} que vivían dentro de {@code RestConsumer}: la Fase 03 los mueve, no los
 * cambia.
 */
public final class TeamMapper {

    private TeamMapper() {
        // Mapper estático: no se instancia.
    }

    public static TeamFieldValues toDomain(TeamFieldValuesDTO dto) {
        if (dto == null) {
            return null;
        }
        return TeamFieldValues.builder()
                .defaultValue(dto.getDefaultValue())
                .values(dto.getValues() != null
                        ? dto.getValues().stream().map(TeamFieldValueDTO::getValue).toList()
                        : List.of())
                .build();
    }

    public static List<TeamIteration> toDomain(TeamIterationsDTO dto) {
        if (dto == null || dto.getValue() == null) {
            return List.of();
        }
        return dto.getValue().stream().map(TeamMapper::toDomain).toList();
    }

    public static TeamIteration toDomain(TeamIterationDTO dto) {
        if (dto == null) {
            return null;
        }
        return TeamIteration.builder()
                .id(dto.getId())
                .name(dto.getName())
                .path(dto.getPath())
                .build();
    }
}

