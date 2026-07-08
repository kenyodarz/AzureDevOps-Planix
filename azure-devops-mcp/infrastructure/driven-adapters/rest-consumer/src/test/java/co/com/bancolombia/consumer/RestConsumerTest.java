package co.com.bancolombia.consumer;

import co.com.bancolombia.model.workitem.JsonPatchOperation;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WorkItemsBatchRequest;
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

class RestConsumerTest {

    private static RestConsumer restConsumer;
    private static MockWebServer mockBackEnd;

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

    @Test
    @DisplayName("Validate the function getWorkItem.")
    void validateGetWorkItem() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"id\": 7539457, \"rev\": 1, \"url\": \"https://dev.azure.com\", \"fields\": {\"System.Title\": \"Test Title\"}}"));

        var response = restConsumer.getWorkItem("Org", "Proj", 7539457, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(workItem -> workItem.getId() == 7539457 && "Test Title".equals(workItem.getFields().get("System.Title")))
                .verifyComplete();
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

        var response = restConsumer.createWorkItem("Org", "Proj", "User Story", patch, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(workItem -> workItem.getId() == 7539458 && "New Story".equals(workItem.getFields().get("System.Title")))
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

        var response = restConsumer.updateWorkItem("Org", "Proj", 7539458, patch, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(workItem -> workItem.getId() == 7539458 && "Updated Title".equals(workItem.getFields().get("System.Title")))
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
        var response = restConsumer.queryByWiql("Org", "Proj", query, "7.0");

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

        WorkItemsBatchRequest request = WorkItemsBatchRequest.builder()
                .ids(List.of(7539457))
                .fields(List.of("System.Id", "System.Title"))
                .build();

        var response = restConsumer.getWorkItemsBatch("Org", "Proj", request, "7.1");

        StepVerifier.create(response)
                .expectNextMatches(list -> list.size() == 1 && list.get(0).getId() == 7539457 && "Batch Story".equals(list.get(0).getFields().get("System.Title")))
                .verifyComplete();
    }
}