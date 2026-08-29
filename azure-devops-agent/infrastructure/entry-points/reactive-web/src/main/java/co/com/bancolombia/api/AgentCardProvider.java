package co.com.bancolombia.api;

import co.com.bancolombia.model.a2a.AgentCapabilities;
import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.AgentInterface;
import co.com.bancolombia.model.a2a.AgentProvider;
import co.com.bancolombia.model.a2a.AgentSkill;
import java.util.List;
import java.util.Map;

/**
 * Provee la Agent Card A2A publicada en {@code /.well-known/agent-card.json}.
 * <p>
 * La tarjeta es contenido estático de descubrimiento: no depende de la petición ni del estado
 * del agente, por lo que se construye una sola vez y se reutiliza.
 */
public final class AgentCardProvider {

    private static final String SCHEMA_TYPE = "type";
    private static final String SCHEMA_TYPE_STRING = "string";
    private static final String SCHEMA_DESCRIPTION = "description";
    private static final String MODE_TEXT = "text";
    private static final String MODE_DATA = "data";

    private static final AgentCard AGENT_CARD = buildAgentCard();

    private AgentCardProvider() {
        // Utilidad sin estado: no se instancia.
    }

    /**
     * @return la tarjeta de capacidades del agente
     */
    public static AgentCard agentCard() {
        return AGENT_CARD;
    }

    private static AgentCard buildAgentCard() {
        return AgentCard.builder()
                .protocolVersion("1.0")
                .name("Financial Consumer Agent")
                .version("1.0.0")
                .description(
                        "Agente integrador A2A para consulta de clientes y términos financieros")
                .provider(AgentProvider.builder()
                        .organization("Bancolombia")
                        .url("https://www.bancolombia.com")
                        .build())
                .supportedInterfaces(List.of(
                        AgentInterface.builder()
                                .protocol("jsonrpc")
                                .url("http://localhost:8082")
                                .build()))
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of(MODE_TEXT, MODE_DATA))
                .defaultOutputModes(List.of(MODE_TEXT, MODE_DATA))
                .skills(List.of(searchClientsTermsSkill()))
                .securitySchemes(securitySchemes())
                .tags(List.of("finance", "a2a", "clients", "terms"))
                .build();
    }

    private static AgentSkill searchClientsTermsSkill() {
        return AgentSkill.builder()
                .id("search-clients-terms")
                .name("Búsqueda de clientes y términos")
                .description("Busca información de clientes y términos por nacionalidad")
                .tags(List.of("finance", "clients", "terms"))
                .inputModes(List.of(MODE_TEXT, MODE_DATA))
                .outputModes(List.of(MODE_TEXT, MODE_DATA))
                .schema(Map.of(
                        SCHEMA_TYPE, "object",
                        "properties", Map.of(
                                "name", stringField("Nombre o parte del nombre del cliente"),
                                "documentType", stringField(
                                        "Tipo de documento (CC, NIT, CIP, SSN, etc.)"),
                                "documentNumber", stringField(
                                        "Número de documento de identidad")),
                        "required", List.of("name", "documentType", "documentNumber")))
                .examples(List.of(
                        "Busca el cliente con CC 12345 y dame sus términos",
                        "Cuales son las condiciones para clientes colombianos?"))
                .build();
    }

    private static Map<String, String> stringField(String description) {
        return Map.of(SCHEMA_TYPE, SCHEMA_TYPE_STRING, SCHEMA_DESCRIPTION, description);
    }

    private static Map<String, Object> securitySchemes() {
        return Map.of("oauth2", Map.of(
                SCHEMA_TYPE, "oauth2",
                "flows", Map.of(
                        "clientCredentials", Map.of(
                                "tokenUrl",
                                "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token",
                                "scopes", Map.of("api://{client-id}/.default",
                                        "Access to agent")))));
    }
}

