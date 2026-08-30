package co.com.bancolombia.consumer.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Verifica que la credencial de Azure DevOps se valide <b>al arranque</b> y no varias llamadas
 * después, en forma de 401 opaco.
 *
 * <p>Ningún valor de estas pruebas es un token real: se construyen en el momento a partir de un
 * literal evidente, conforme a DP-02.
 */
class RestConsumerConfigTokenTest {

    private static final String URL = "https://dev.azure.com";
    private static final int TIMEOUT = 5000;
    private static final String PERMISSIVE = "PERMISSIVE";
    private static final String ENFORCED = "ENFORCED";

    /**
     * Formato correcto: Base64(":" + PAT). El PAT es un valor de relleno, no una credencial.
     */
    private static String validCredential() {
        return Base64.getEncoder()
                .encodeToString(":pat-de-prueba-no-es-un-secreto".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("GIVEN credencial con formato correcto WHEN arranca THEN no falla en ningun modo")
    void givenWellFormedCredential_whenStartup_thenSucceeds() {
        assertDoesNotThrow(
                () -> new RestConsumerConfig(URL, TIMEOUT, validCredential(), PERMISSIVE));
        assertDoesNotThrow(
                () -> new RestConsumerConfig(URL, TIMEOUT, validCredential(), ENFORCED));
    }

    @Test
    @DisplayName("GIVEN modo ENFORCED y credencial ausente WHEN arranca THEN falla con mensaje explicito")
    void givenEnforcedAndMissingCredential_whenStartup_thenFails() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new RestConsumerConfig(URL, TIMEOUT, "", ENFORCED));

        assertTrue(error.getMessage().contains("adapter.restconsumer.token"));
        assertTrue(error.getMessage().contains("Base64"));
    }

    @Test
    @DisplayName("GIVEN modo ENFORCED y PAT crudo sin codificar WHEN arranca THEN falla en vez de producir un 401 mudo")
    void givenEnforcedAndRawPat_whenStartup_thenFails() {
        assertThrows(IllegalStateException.class,
                () -> new RestConsumerConfig(URL, TIMEOUT, "pat crudo sin codificar!!", ENFORCED));
    }

    @Test
    @DisplayName("GIVEN modo ENFORCED y Base64 valido sin ':' WHEN arranca THEN falla")
    void givenEnforcedAndBase64WithoutColon_whenStartup_thenFails() {
        String withoutColon = Base64.getEncoder()
                .encodeToString("sin-separador".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new RestConsumerConfig(URL, TIMEOUT, withoutColon, ENFORCED));
    }

    @Test
    @DisplayName("GIVEN modo PERMISSIVE y credencial invalida WHEN arranca THEN solo avisa y continua")
    void givenPermissiveAndInvalidCredential_whenStartup_thenOnlyWarns() {
        assertDoesNotThrow(() -> new RestConsumerConfig(URL, TIMEOUT, "", PERMISSIVE));
        assertDoesNotThrow(
                () -> new RestConsumerConfig(URL, TIMEOUT, "pat crudo sin codificar!!", PERMISSIVE));
    }

    @Test
    @DisplayName("GIVEN credencial ya prefijada con 'Basic ' WHEN arranca THEN se acepta igualmente")
    void givenCredentialAlreadyPrefixed_whenStartup_thenAccepted() {
        assertDoesNotThrow(() -> new RestConsumerConfig(URL, TIMEOUT,
                "Basic " + validCredential(), ENFORCED));
    }

    @Test
    @DisplayName("GIVEN configuracion valida WHEN se construye el WebClient THEN se obtiene una instancia")
    void givenValidConfig_whenBuildWebClient_thenInstanceIsCreated() {
        RestConsumerConfig config = new RestConsumerConfig(URL, TIMEOUT, validCredential(),
                PERMISSIVE);

        @SuppressWarnings("unchecked")
        ObjectProvider<WebClient.Builder> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable(org.mockito.ArgumentMatchers.any()))
                .thenReturn(WebClient.builder());

        assertNotNull(config.getWebClient(provider));
    }
}



