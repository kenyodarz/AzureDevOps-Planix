package co.com.bancolombia.model.agent;

/**
 * Ámbito de Azure DevOps sobre el que trabaja el agente: dónde vive el Work Item.
 *
 * <p>Existe para que la organización y el proyecto dejen de ser dos {@code String} intercambiables.
 * Antes nada impedía construir un flujo con ambos valores invertidos: el compilador callaba y el
 * error solo aparecía al llamar a Azure DevOps. Ahora el tipo lo impide.
 *
 * @param organization organización de Azure DevOps; obligatoria
 * @param project      proyecto dentro de la organización; obligatorio
 */
public record AzureDevOpsScope(String organization, String project) {

    public AzureDevOpsScope {
        if (organization == null || organization.isBlank()) {
            throw new IllegalArgumentException("La organización de Azure DevOps es obligatoria");
        }
        if (project == null || project.isBlank()) {
            throw new IllegalArgumentException("El proyecto de Azure DevOps es obligatorio");
        }
    }
}

