package co.com.bancolombia.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/**
 * Caracterización del <b>cuerpo HTTP que sale hacia Azure DevOps</b> — Fase 03, D-11.
 *
 * <p><b>Por qué existe.</b> La Fase 03 interpone DTOs y mappers en la frontera de salida: donde
 * antes había {@code .bodyValue(patch)}, {@code .bodyValue(query)} y {@code .bodyValue(request)}
 * sobre <b>modelos de dominio</b>, ahora hay {@code JsonPatchMapper}, {@code WiqlQueryMapper} y
 * {@code WorkItemBatchMapper}. Eso es exactamente el tipo de cambio que puede alterar el JSON
 * emitido sin que nada se ponga rojo: un campo que desaparece, un nulo que deja de incluirse, un
 * orden que cambia. Azure DevOps no avisaría; devolvería un error o, peor, un resultado distinto.
 *
 * <p>{@code RestConsumerTest} —la red de seguridad heredada— verifica URIs y respuestas, pero
 * <b>nunca ha comparado un cuerpo de petición</b>. Esta clase cubre ese hueco.
 *
 * <p><b>Cómo demuestra la equivalencia.</b> No compara contra una cadena literal escrita a mano
 * (eso solo congelaría «lo que salga hoy»). Compara el cuerpo <b>realmente enviado</b> con el
 * resultado de serializar el <b>modelo de dominio</b>, que es literalmente lo que el
 * {@code WebClient} hacía antes de esta fase. Si un mapper pierde, renombra o reordena un campo, la
 * igualdad de cadenas falla.
 *
 * <p>Para el lote se serializa {@code WorkItemBatchCriteria}, que es el antiguo
 * {@code WorkItemsBatchRequest} con el <b>mismo</b> conjunto y orden de campos: por DP-03 solo
 * cambió el nombre de la clase, que no viaja por el cable.
 */
class OutboundPayloadCharacterizationTest {

    private static RestConsumer restConsumer;
    private static MockWebServer mockBackEnd;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        restConsumer = new RestConsumer(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    private static void enqueueOk(String body) {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody(body));
    }

    private static String takeBody() throws InterruptedException {
        RecordedRequest recorded = mockBackEnd.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recorded, "No se registró ninguna petición contra el servidor simulado");
        return recorded.getBody().readUtf8();
    }

    @Test
    @DisplayName("GIVEN un patch de dominio WHEN createWorkItem THEN el cuerpo enviado es identico al que producia el modelo de dominio")
    void givenPatch_whenCreateWorkItem_thenBodyIsByteForByteTheSame() throws InterruptedException {
        // Arrange (GIVEN)
        List<JsonPatchOperation> patch = List.of(
                JsonPatchOperation.builder()
                        .op("add").path("/fields/System.Title").value("New Story").build(),
                JsonPatchOperation.builder()
                        .op("add").path("/relations/-").value("algo").from("origen").build());
        enqueueOk("{\"id\": 1}");

        // Act (WHEN)
        StepVerifier.create(restConsumer.createWorkItem("Org", "Proj", "User Story", patch, "7.1"))
                .expectNextCount(1)
                .verifyComplete();

        // Assert (THEN) — el cuerpo real contra la serialización del dominio de antes de la Fase 03
        assertEquals(MAPPER.writeValueAsString(patch), takeBody());
    }

    @Test
    @DisplayName("GIVEN un patch de dominio WHEN updateWorkItem THEN el cuerpo enviado es identico al que producia el modelo de dominio")
    void givenPatch_whenUpdateWorkItem_thenBodyIsByteForByteTheSame() throws InterruptedException {
        // Arrange (GIVEN)
        List<JsonPatchOperation> patch = List.of(
                JsonPatchOperation.builder()
                        .op("replace").path("/fields/System.Title").value("Updated Title").build());
        enqueueOk("{\"id\": 1}");

        // Act (WHEN)
        StepVerifier.create(restConsumer.updateWorkItem("Org", "Proj", 1, patch, "7.1"))
                .expectNextCount(1)
                .verifyComplete();

        // Assert (THEN)
        assertEquals(MAPPER.writeValueAsString(patch), takeBody());
    }

    @Test
    @DisplayName("GIVEN una sentencia WIQL WHEN queryByWiql THEN el cuerpo enviado es identico al que producia WiqlQuery")
    void givenWiqlQuery_whenQueryByWiql_thenBodyIsByteForByteTheSame() throws InterruptedException {
        // Arrange (GIVEN) — la sentencia congelada carácter a carácter en la Fase 01
        WiqlQuery query = WiqlQuery.builder()
                .query("SELECT [System.Id] FROM workitems WHERE [System.TeamProject] = @project"
                        + " AND [System.IterationPath] = 'Proj\\2025\\Sprint 247'"
                        + " AND [System.AreaPath] = 'Proj\\EQU1096 - EXODIA'"
                        + " AND [System.WorkItemType] IN ('Historia de Usuario','Habilitador')"
                        + " ORDER BY [System.Id]")
                .build();
        enqueueOk("{\"queryType\": \"flat\"}");

        // Act (WHEN)
        StepVerifier.create(restConsumer.queryByWiql("Org", "Proj", query, "7.0"))
                .expectNextCount(1)
                .verifyComplete();

        // Assert (THEN)
        assertEquals(MAPPER.writeValueAsString(query), takeBody());
    }

    @Test
    @DisplayName("GIVEN un criterio de lote WHEN getWorkItemsBatch THEN el cuerpo enviado conserva ids, fields, expand y errorPolicy")
    void givenCriteria_whenGetWorkItemsBatch_thenBodyIsByteForByteTheSame() throws InterruptedException {
        // Arrange (GIVEN)
        WorkItemBatchCriteria criteria = WorkItemBatchCriteria.builder()
                .ids(List.of(7539457, 7539458))
                .fields(List.of("System.Id", "System.Title"))
                .expand("None")
                .errorPolicy("Omit")
                .build();
        enqueueOk("{\"count\": 0, \"value\": []}");

        // Act (WHEN)
        StepVerifier.create(restConsumer.getWorkItemsBatch("Org", "Proj", criteria, "7.1"))
                .expectNextCount(1)
                .verifyComplete();

        // Assert (THEN) — los cuatro nombres de campo del cable son contrato público
        String body = takeBody();
        assertEquals(MAPPER.writeValueAsString(criteria), body);
    }
}

