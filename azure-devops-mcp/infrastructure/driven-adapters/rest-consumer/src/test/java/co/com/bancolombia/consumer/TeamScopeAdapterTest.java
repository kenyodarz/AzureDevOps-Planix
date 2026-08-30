package co.com.bancolombia.consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Red de seguridad del adaptador de <b>ámbito de equipo</b> — Fase 05, D-10.
 *
 * <p>Los seis escenarios vienen de {@code RestConsumerTest} y se conservan <b>literalmente</b>.
 * Cubren las dos consultas que resuelven el {@code AreaPath} y el {@code IterationPath}, es decir,
 * justo las que sostienen el arreglo del fallo del año (D-21, heredada como D-36 del plan del BFF),
 * más la caracterización del manejo de errores de D-13.
 *
 * <p>⚠️ Las dos últimas pruebas <b>no validan un buen comportamiento: fijan el ACTUAL</b>. Hoy el
 * adaptador no traduce nada, así que un fallo de Azure DevOps viaja crudo hasta el cliente MCP como
 * {@code WebClientResponseException}, una excepción de Spring, en un flujo cuyo contrato debería ser
 * de dominio. La <b>Fase 06</b> introducirá excepciones de dominio y estas aserciones deberán
 * cambiar de tipo esperado. Cuando eso ocurra será un cambio DELIBERADO y visible, que es justo lo
 * que estas pruebas existen para garantizar.
 */
class TeamScopeAdapterTest {

    private static TeamScopeAdapter adapter;
    private static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        adapter = new TeamScopeAdapter(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    /**
     * Descarta las peticiones registradas por pruebas anteriores. El {@link MockWebServer} es
     * estático y compartido por toda la clase, y nadie drena su cola: sin esto, {@code takeRequest}
     * devolvería la petición de otra prueba y la aserción sobre la URI sería falsa.
     */
    private static void drainRecordedRequests() throws InterruptedException {
        while (mockBackEnd.takeRequest(50, TimeUnit.MILLISECONDS) != null) {
            // descartar
        }
    }

    @Test
    @DisplayName("GIVEN Azure DevOps devuelve el AreaPath WHEN getTeamFieldValues THEN se consulta la URI correcta y se mapea al dominio")
    void givenTeamFieldValues_whenGetTeamFieldValues_thenMapsToDomain() throws InterruptedException {
        // Arrange (GIVEN)
        drainRecordedRequests();
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"field\": {\"referenceName\": \"System.AreaPath\"},"
                        + " \"defaultValue\": \"Proj\\\\Team\","
                        + " \"values\": [{\"value\": \"Proj\\\\Team\", \"includeChildren\": true}]}"));

        // Act (WHEN)
        var response = adapter.getTeamFieldValues("Org", "Proj", "Team");

        // Assert (THEN)
        StepVerifier.create(response)
                .expectNextMatches(values -> "Proj\\Team".equals(values.getDefaultValue())
                        && values.getValues().size() == 1
                        && "Proj\\Team".equals(values.getValues().get(0)))
                .verifyComplete();

        RecordedRequest recorded = mockBackEnd.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recorded, "No se registró ninguna petición contra el servidor simulado");
        assertEquals("/Org/Proj/Team/_apis/work/teamsettings/teamfieldvalues?api-version=7.0",
                recorded.getPath());
    }

    @Test
    @DisplayName("GIVEN la respuesta no trae 'values' WHEN getTeamFieldValues THEN se devuelve una lista vacia y no null")
    void givenNullValues_whenGetTeamFieldValues_thenReturnsEmptyList() {
        // Arrange (GIVEN)
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"defaultValue\": \"Proj\\\\Team\"}"));

        // Act (WHEN)
        var response = adapter.getTeamFieldValues("Org", "Proj", "Team");

        // Assert (THEN)
        StepVerifier.create(response)
                .expectNextMatches(values -> values.getValues() != null && values.getValues().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN Azure DevOps devuelve las iteraciones WHEN getTeamIterations THEN se consulta la URI correcta y se mapea el path real")
    void givenTeamIterations_whenGetTeamIterations_thenMapsRealPath() throws InterruptedException {
        // Arrange (GIVEN)
        drainRecordedRequests();
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"count\": 2, \"value\": ["
                        + "{\"id\": \"a1\", \"name\": \"Sprint 247\", \"path\": \"Proj\\\\2025\\\\Sprint 247\"},"
                        + "{\"id\": \"b2\", \"name\": \"Sprint 248\", \"path\": \"Proj\\\\2026\\\\Sprint 248\"}]}"));

        // Act (WHEN)
        var response = adapter.getTeamIterations("Org", "Proj", "Team");

        // Assert (THEN) — el path llega tal cual lo declara Azure DevOps, con SU año, no con el del
        // calendario. Es la razón de ser de esta consulta.
        StepVerifier.create(response)
                .expectNextMatches(iterations -> iterations.size() == 2
                        && "Sprint 247".equals(iterations.get(0).getName())
                        && "Proj\\2025\\Sprint 247".equals(iterations.get(0).getPath())
                        && "Proj\\2026\\Sprint 248".equals(iterations.get(1).getPath()))
                .verifyComplete();

        RecordedRequest recorded = mockBackEnd.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recorded, "No se registró ninguna petición contra el servidor simulado");
        assertEquals("/Org/Proj/Team/_apis/work/teamsettings/iterations?api-version=7.0",
                recorded.getPath());
    }

    @Test
    @DisplayName("GIVEN la respuesta no trae 'value' WHEN getTeamIterations THEN se devuelve una lista vacia y no null")
    void givenNullValue_whenGetTeamIterations_thenReturnsEmptyList() {
        // Arrange (GIVEN)
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"count\": 0}"));

        // Act (WHEN)
        var response = adapter.getTeamIterations("Org", "Proj", "Team");

        // Assert (THEN)
        StepVerifier.create(response)
                .expectNextMatches(iterations -> iterations != null && iterations.isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("GIVEN Azure DevOps responde 401 WHEN getTeamFieldValues THEN el error tecnico llega crudo, sin traducir (D-13)")
    void givenUnauthorized_whenGetTeamFieldValues_thenRawTechnicalErrorIsPropagated() {
        // Arrange (GIVEN)
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .setBody("{\"message\": \"TF400813: El usuario no esta autorizado\"}"));

        // Act (WHEN)
        var response = adapter.getTeamFieldValues("Org", "Proj", "Team");

        // Assert (THEN)
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof WebClientResponseException ex
                        && ex.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }

    @Test
    @DisplayName("GIVEN Azure DevOps responde 500 WHEN getTeamIterations THEN el error tecnico llega crudo, sin traducir (D-13)")
    void givenServerError_whenGetTeamIterations_thenRawTechnicalErrorIsPropagated() {
        // Arrange (GIVEN)
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody("{\"message\": \"Internal Server Error\"}"));

        // Act (WHEN)
        var response = adapter.getTeamIterations("Org", "Proj", "Team");

        // Assert (THEN)
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof WebClientResponseException ex
                        && ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                .verify();
    }
}

