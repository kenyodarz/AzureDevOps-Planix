package co.com.bancolombia.model.overview;

import lombok.Builder;

/**
 * Entidad de dominio inmutable que representa la visión general y metadatos de un proyecto en Azure DevOps.
 *
 * @param id          identificador único (UUID) del proyecto
 * @param name        nombre descriptivo del proyecto
 * @param description descripción del proyecto
 * @param state       estado del proyecto (ej. "wellFormed", "deleting", "createPending")
 * @param visibility  visibilidad del proyecto (ej. "private", "public")
 * @param url         URL canónica de la API del proyecto
 */
@Builder(toBuilder = true)
public record ProjectOverview(
        String id,
        String name,
        String description,
        String state,
        String visibility,
        String url) {
}
