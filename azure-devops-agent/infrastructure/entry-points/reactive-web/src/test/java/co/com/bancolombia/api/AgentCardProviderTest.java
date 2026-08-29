package co.com.bancolombia.api;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.bancolombia.model.a2a.AgentCard;
import co.com.bancolombia.model.a2a.AgentSkill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica que la tarjeta de capacidades publicada mantiene los campos obligatorios del
 * descubrimiento A2A.
 */
class AgentCardProviderTest {

    @Test
    @DisplayName("GIVEN la tarjeta WHEN se consulta THEN los campos de identidad están presentes")
    void givenCard_whenRead_thenIdentityFieldsArePresent() {
        AgentCard card = AgentCardProvider.agentCard();

        assertThat(card.getProtocolVersion()).isEqualTo("1.0");
        assertThat(card.getName()).isEqualTo("Financial Consumer Agent");
        assertThat(card.getVersion()).isEqualTo("1.0.0");
        assertThat(card.getDescription()).isNotBlank();
        assertThat(card.getProvider().getOrganization()).isEqualTo("Bancolombia");
        assertThat(card.getProvider().getUrl()).isNotBlank();
    }

    @Test
    @DisplayName("GIVEN la tarjeta WHEN se consulta THEN declara interfaz y capacidades")
    void givenCard_whenRead_thenInterfacesAndCapabilitiesAreDeclared() {
        AgentCard card = AgentCardProvider.agentCard();

        assertThat(card.getSupportedInterfaces()).hasSize(1);
        assertThat(card.getSupportedInterfaces().getFirst().getProtocol()).isEqualTo("jsonrpc");
        assertThat(card.getSupportedInterfaces().getFirst().getUrl()).isNotBlank();
        assertThat(card.getCapabilities().isStreaming()).isFalse();
        assertThat(card.getCapabilities().isPushNotifications()).isFalse();
        assertThat(card.getDefaultInputModes()).containsExactly("text", "data");
        assertThat(card.getDefaultOutputModes()).containsExactly("text", "data");
        assertThat(card.getTags()).contains("a2a");
    }

    @Test
    @DisplayName("GIVEN la tarjeta WHEN se consulta la habilidad THEN conserva su esquema")
    void givenCard_whenReadSkill_thenSchemaIsPreserved() {
        AgentSkill skill = AgentCardProvider.agentCard().getSkills().getFirst();

        assertThat(skill.getId()).isEqualTo("search-clients-terms");
        assertThat(skill.getName()).isNotBlank();
        assertThat(skill.getDescription()).isNotBlank();
        assertThat(skill.getTags()).contains("finance", "clients", "terms");
        assertThat(skill.getInputModes()).containsExactly("text", "data");
        assertThat(skill.getOutputModes()).containsExactly("text", "data");
        assertThat(skill.getExamples()).hasSize(2);
        assertThat(skill.getSchema()).containsEntry("type", "object");
        assertThat(skill.getSchema()).containsKeys("properties", "required");
    }

    @Test
    @DisplayName("GIVEN la tarjeta WHEN se consulta la seguridad THEN declara oauth2")
    void givenCard_whenReadSecurity_thenOauth2IsDeclared() {
        AgentCard card = AgentCardProvider.agentCard();

        assertThat(card.getSecuritySchemes()).containsKey("oauth2");
    }

    @Test
    @DisplayName("GIVEN dos consultas WHEN se comparan THEN se reutiliza la misma tarjeta")
    void givenTwoReads_whenCompared_thenSameInstanceIsReused() {
        assertThat(AgentCardProvider.agentCard()).isSameAs(AgentCardProvider.agentCard());
    }
}

