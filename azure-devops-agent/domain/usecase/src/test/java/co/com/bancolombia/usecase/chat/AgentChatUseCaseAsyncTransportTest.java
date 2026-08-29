package co.com.bancolombia.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.a2a.Message;
import co.com.bancolombia.model.a2a.Part;
import co.com.bancolombia.model.a2a.SendMessageRequest;
import co.com.bancolombia.model.a2a.SendMessageResponse;
import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.a2a.TaskState;
import co.com.bancolombia.model.agent.IntentResolver;
import co.com.bancolombia.model.chat.gateways.AgentResponseGateway;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import co.com.bancolombia.usecase.chat.handler.ChatFlowContext;
import co.com.bancolombia.usecase.chat.handler.ChatFlowDispatcher;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Cubre el <b>transporte asíncrono</b> {@link AgentChatUseCase#chat(SendMessageRequest)}.
 *
 * <p><b>Por qué existe esta suite (DP-05, Opción A).</b> En la plataforma del Banco el protocolo
 * A2A viaja sobre Kafka, así que este es el camino real de producción cuando la mensajería esté
 * cableada. Hoy el puerto lo resuelve un <i>Null Object</i> que devuelve {@link Mono#empty()}, de
 * modo que el flujo no se ejercita en ninguna petición: era el hueco de cobertura de la clase.
 *
 * <p>Sin estas pruebas, una regresión en el camino asíncrono solo se descubriría el día que se
 * active Kafka. Con ellas, se descubre en el build.
 *
 * <p>El {@link ChatFlowDispatcher} sí se simula aquí: lo que se verifica es el <b>transporte</b>
 * (persistir y publicar), no el enrutamiento, que caracteriza
 * {@code AgentChatUseCaseCharacterizationTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentChatUseCase - Transporte asíncrono A2A/Kafka (DP-05)")
class AgentChatUseCaseAsyncTransportTest {

    private static final String LLM_RESPONSE = "Respuesta del modelo";
    private static final String NO_CONTENT = "No content provided";

    @Mock
    private AgentResponseGateway agentResponseGateway;

    @Mock
    private TaskStoreGateway taskStoreGateway;

    @Mock
    private ChatFlowDispatcher chatFlowDispatcher;

    private AgentChatUseCase useCase;

    private static SendMessageRequest requestWith(String text) {
        return SendMessageRequest.builder()
                .message(Message.builder()
                        .role("user")
                        .contextId("ctx-async")
                        .parts(List.of(Part.ofText(text)))
                        .build())
                .build();
    }

    @BeforeEach
    void setUp() {
        useCase = new AgentChatUseCase(agentResponseGateway, taskStoreGateway,
                new IntentResolver(), chatFlowDispatcher);
    }

    @Test
    @DisplayName("GIVEN un flujo exitoso WHEN se invoca chat THEN persiste la tarea y publica la respuesta")
    void givenSuccessfulFlow_whenChat_thenPersistsTaskAndPublishesResponse() {
        // GIVEN
        when(chatFlowDispatcher.dispatch(any(ChatFlowContext.class)))
                .thenReturn(Mono.just(LLM_RESPONSE));
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
        when(agentResponseGateway.sendResponse(any(SendMessageResponse.class)))
                .thenReturn(Mono.empty());

        // WHEN / THEN: el Mono<Void> completa sin emitir
        StepVerifier.create(useCase.chat(requestWith("Dame la lista de historias del sprint")))
                .verifyComplete();

        // THEN: la tarea se persistió antes de publicarse
        ArgumentCaptor<Task> savedTask = ArgumentCaptor.forClass(Task.class);
        verify(taskStoreGateway).save(savedTask.capture());
        assertThat(savedTask.getValue().getStatus().getState()).isEqualTo(TaskState.COMPLETED);

        // THEN: la respuesta publicada lleva el texto del modelo
        ArgumentCaptor<SendMessageResponse> published =
                ArgumentCaptor.forClass(SendMessageResponse.class);
        verify(agentResponseGateway).sendResponse(published.capture());
        assertThat(published.getValue().getMessage().extractText()).isEqualTo(LLM_RESPONSE);
        assertThat(published.getValue().getTask()).isEqualTo(savedTask.getValue());
    }

    @Test
    @DisplayName("GIVEN un flujo que falla WHEN se invoca chat THEN publica igualmente la tarea fallida")
    void givenFailingFlow_whenChat_thenPublishesFailedTask() {
        // GIVEN
        when(chatFlowDispatcher.dispatch(any(ChatFlowContext.class)))
                .thenReturn(Mono.error(new IllegalStateException("boom")));
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
        when(agentResponseGateway.sendResponse(any(SendMessageResponse.class)))
                .thenReturn(Mono.empty());

        // WHEN / THEN: el error no se propaga, se convierte en respuesta
        StepVerifier.create(useCase.chat(requestWith("Audita la historia (ID: 12345)")))
                .verifyComplete();

        // THEN
        ArgumentCaptor<SendMessageResponse> published =
                ArgumentCaptor.forClass(SendMessageResponse.class);
        verify(agentResponseGateway).sendResponse(published.capture());
        assertThat(published.getValue().getTask().getStatus().getState())
                .isEqualTo(TaskState.FAILED);
        assertThat(published.getValue().getMessage().extractText()).contains("boom");
    }

    @Test
    @DisplayName("GIVEN un texto vacío WHEN se invoca chat THEN publica el aviso sin consultar al modelo")
    void givenEmptyText_whenChat_thenPublishesNoContentWithoutDispatching() {
        // GIVEN
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
        when(agentResponseGateway.sendResponse(any(SendMessageResponse.class)))
                .thenReturn(Mono.empty());

        // WHEN
        StepVerifier.create(useCase.chat(requestWith("   ")))
                .verifyComplete();

        // THEN
        verify(chatFlowDispatcher, never()).dispatch(any(ChatFlowContext.class));
        ArgumentCaptor<SendMessageResponse> published =
                ArgumentCaptor.forClass(SendMessageResponse.class);
        verify(agentResponseGateway).sendResponse(published.capture());
        assertThat(published.getValue().getMessage().extractText()).isEqualTo(NO_CONTENT);
    }

    @Test
    @DisplayName("GIVEN el Null Object del puerto WHEN se invoca chat THEN el camino queda apagado sin efectos")
    void givenNullObjectGateway_whenChat_thenPathIsSilentlyDisabled() {
        // GIVEN: exactamente lo que hace NoOpAgentResponseAdapter hoy en producción
        when(chatFlowDispatcher.dispatch(any(ChatFlowContext.class)))
                .thenReturn(Mono.just(LLM_RESPONSE));
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
        when(agentResponseGateway.sendResponse(any(SendMessageResponse.class)))
                .thenReturn(Mono.empty());

        // WHEN / THEN: completa sin emitir y sin propagar error alguno
        StepVerifier.create(useCase.chat(requestWith("Dame la lista de historias")))
                .verifyComplete();

        // THEN: la tarea sí se persiste; lo único que no ocurre es la publicación real
        verify(taskStoreGateway).save(any(Task.class));
        verify(agentResponseGateway).sendResponse(any(SendMessageResponse.class));
    }

    @Test
    @DisplayName("GIVEN un fallo al publicar WHEN se invoca chat THEN el error se propaga al llamador")
    void givenPublishFailure_whenChat_thenErrorPropagates() {
        // GIVEN
        when(chatFlowDispatcher.dispatch(any(ChatFlowContext.class)))
                .thenReturn(Mono.just(LLM_RESPONSE));
        when(taskStoreGateway.save(any(Task.class)))
                .thenAnswer(invocation -> Mono.just((Task) invocation.getArgument(0)));
        when(agentResponseGateway.sendResponse(any(SendMessageResponse.class)))
                .thenReturn(Mono.error(new IllegalStateException("kafka caído")));

        // WHEN / THEN: el fallo de transporte NO se enmascara como respuesta exitosa
        StepVerifier.create(useCase.chat(requestWith("Dame la lista de historias")))
                .expectErrorMessage("kafka caído")
                .verify();
    }
}

