package co.com.bancolombia.consumer;

import co.com.bancolombia.model.workitem.JsonPatchOperation;
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
 * Red de seguridad del adaptador de <b>mutación</b> — Fase 05, D-10.
 *
 * <p>Los dos escenarios vienen de {@code RestConsumerTest} y se conservan <b>literalmente</b>: ni
 * una aserción, ni una URL, ni un cuerpo, ni un código de estado ha cambiado. Lo único distinto es
 * que ahora se invocan sobre el adaptador que solo escribe, sin arrastrar los cinco flujos de
 * lectura con los que compartían clase.
 */
class WorkItemCommandAdapterTest {

    private static WorkItemCommandAdapter adapter;
    private static MockWebServer mockBackEnd;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        adapter = new WorkItemCommandAdapter(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    @DisplayName("Validate the function createWorkItem.")
    void validateCreateWorkItem() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"id\": 7539458, \"rev\": 1, \"url\": \"https://dev.azure.com\", \"fields\": {\"System.Title\": \"New Story\"}}"));

        List<JsonPatchOperation> patch = List.of(
                JsonPatchOperation.builder().op("add").path("/fields/System.Title").value("New Story").build()
        );

        var response = adapter.createWorkItem("Org", "Proj", "User Story", patch, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(workItem -> workItem.id() == 7539458 && "New Story".equals(workItem.fields().get("System.Title")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Validate the function updateWorkItem.")
    void validateUpdateWorkItem() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"id\": 7539458, \"rev\": 2, \"url\": \"https://dev.azure.com\", \"fields\": {\"System.Title\": \"Updated Title\"}}"));

        List<JsonPatchOperation> patch = List.of(
                JsonPatchOperation.builder().op("replace").path("/fields/System.Title").value("Updated Title").build()
        );

        var response = adapter.updateWorkItem("Org", "Proj", 7539458, patch, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(workItem -> workItem.id() == 7539458 && "Updated Title".equals(workItem.fields().get("System.Title")))
                .verifyComplete();
    }
}

