package co.com.bancolombia.consumer;

import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WorkItemBatchCriteria;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

/**
 * Red de seguridad del adaptador de <b>consulta</b> — Fase 05, D-10.
 *
 * <p><b>Procedencia.</b> Los tres escenarios de esta clase vienen de {@code RestConsumerTest}, que
 * probaba los siete gateways contra la misma instancia porque los siete vivían en la misma clase.
 * Ése era exactamente el síntoma de D-10: <b>no había forma de probar un flujo sin arrastrar los
 * otros seis</b>. Al partir el adaptador, las pruebas se reparten con él.
 *
 * <p><b>Ni un escenario, ni una aserción, ni una URL, ni un cuerpo, ni un código de estado ha
 * cambiado</b> respecto de la clase original: solo cambia sobre qué objeto se invocan. Es la
 * adaptación mecánica que el propietario autorizó al plantearse esta fase, con el mismo criterio
 * que en las Fases 03 y 04.
 */
class WorkItemQueryAdapterTest {

    private static WorkItemQueryAdapter adapter;
    private static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        adapter = new WorkItemQueryAdapter(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    @DisplayName("Validate the function getWorkItem.")
    void validateGetWorkItem() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"id\": 7539457, \"rev\": 1, \"url\": \"https://dev.azure.com\", \"fields\": {\"System.Title\": \"Test Title\"}}"));

        var response = adapter.getWorkItem("Org", "Proj", 7539457, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(workItem -> workItem.getId() == 7539457 && "Test Title".equals(workItem.getFields().get("System.Title")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Validate the function queryByWiql.")
    void validateQueryByWiql() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"queryType\": \"flat\", \"queryResultType\": \"workItem\", \"workItems\": [{\"id\": 7539457, \"url\": \"https://dev.azure.com\"}]}"));

        WiqlQuery query = WiqlQuery.builder().query("SELECT ...").build();
        var response = adapter.queryByWiql("Org", "Proj", query, "7.0");

        StepVerifier.create(response)
                .expectNextMatches(result -> "flat".equals(result.getQueryType()) && result.getWorkItems().size() == 1 && result.getWorkItems().get(0).getId() == 7539457)
                .verifyComplete();
    }

    @Test
    @DisplayName("Validate the function getWorkItemsBatch.")
    void validateGetWorkItemsBatch() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"count\": 1, \"value\": [{\"id\": 7539457, \"fields\": {\"System.Title\": \"Batch Story\"}}]}"));

        WorkItemBatchCriteria request = WorkItemBatchCriteria.builder()
                .ids(List.of(7539457))
                .fields(List.of("System.Id", "System.Title"))
                .build();

        var response = adapter.getWorkItemsBatch("Org", "Proj", request, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(list -> list.size() == 1 && list.get(0).getId() == 7539457 && "Batch Story".equals(list.get(0).getFields().get("System.Title")))
                .verifyComplete();
    }
}

