package co.com.bancolombia.model.workitem;

/**
 * Intención completa de «listar los elementos de trabajo de una célula en un sprint».
 *
 * <p><b>Objeto comando inmutable</b> (sub-decisión <b>B-04</b>, resuelta por el propietario a favor
 * del comando frente a los seis parámetros sueltos, tal y como anticipaba §4.1 del plan maestro).
 *
 * <p>⚠️ <b>No es un DTO de cable y no debe convertirse en uno.</b> No cruza la frontera MCP —el
 * entry-point lo construye a partir de los seis {@code @McpToolParam} que sí son contrato— ni lo
 * serializa el {@code WebClient}. La frontera que estableció la Fase 03 (DP-03) queda intacta.
 */
public record ListWorkItemsCommand(
        String organization,
        String project,
        TeamName team,
        SprintName sprint,
        WorkItemTypes workItemTypes,
        String apiVersion) {

    public ListWorkItemsCommand {
        if (organization == null || organization.isBlank()) {
            throw new IllegalArgumentException("La organización no puede ser nula ni estar en blanco");
        }
        if (project == null || project.isBlank()) {
            throw new IllegalArgumentException("El proyecto no puede ser nulo ni estar en blanco");
        }
        if (team == null || sprint == null || workItemTypes == null) {
            throw new IllegalArgumentException(
                    "La célula, el sprint y los tipos de elemento de trabajo son obligatorios");
        }
    }

    /**
     * Fábrica desde los valores crudos que llegan por el protocolo MCP.
     *
     * <p>Aquí es donde el texto del cable se convierte en dominio: las barras dobles se colapsan,
     * la lista de tipos se interpreta y se aplican los valores por defecto. El entry-point se
     * limita a invocar este método, de modo que no le queda ni una regla de negocio.
     */
    public static ListWorkItemsCommand of(String organization, String project, String teamName,
            String sprintName, String workItemTypes, String apiVersion) {
        return new ListWorkItemsCommand(organization, project,
                TeamName.of(teamName),
                SprintName.of(sprintName),
                WorkItemTypes.parse(workItemTypes),
                apiVersion);
    }
}

