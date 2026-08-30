package co.com.bancolombia.usecase.ingestplanning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.bancolombia.model.planning.PlanningChunk;
import co.com.bancolombia.model.planning.gateways.PlanningVectorStorePort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Pruebas de caracterización del troceo de Markdown (Fase 01).
 * <p>
 * Congelan el comportamiento actual del caso de uso <b>tal como es hoy</b>, incluidas sus rarezas,
 * para que las fases posteriores puedan refactorizar con red. Donde el comportamiento actual es
 * discutible se deja constancia explícita en el nombre de la prueba y en su Javadoc.
 */
@ExtendWith(MockitoExtension.class)
class IngestPlanningSpecUseCaseTest {

    private static final String INITIATIVE_ID = "INI-001";
    private static final String TITLE = "Plan de la iniciativa";
    private static final String DEFAULT_SECTION_NAME = "Introducción / Contexto";

    @Mock
    private PlanningVectorStorePort vectorStorePort;

    @Captor
    private ArgumentCaptor<List<PlanningChunk>> chunksCaptor;

    private IngestPlanningSpecUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new IngestPlanningSpecUseCase(vectorStorePort);
    }

    private void givenStoreAcceptsEverything() {
        when(vectorStorePort.deleteInitiative(anyString())).thenReturn(Mono.empty());
        when(vectorStorePort.saveChunks(anyList())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("GIVEN markdown con dos secciones de nivel 2 WHEN ingest THEN genera un chunk por sección")
    void givenMarkdownWithTwoSections_whenIngest_thenOneChunkPerSection() {
        // GIVEN
        givenStoreAcceptsEverything();
        String markdown = """
                # Iniciativa
                
                Contexto general.
                
                ## Alcance
                
                Lo que entra.
                
                ## Riesgos
                
                Lo que puede fallar.
                """;

        // WHEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, markdown))
                .verifyComplete();

        // THEN
        verify(vectorStorePort).saveChunks(chunksCaptor.capture());
        List<PlanningChunk> chunks = chunksCaptor.getValue();

        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(PlanningChunk::sectionName)
                .containsExactly("Iniciativa", "Alcance", "Riesgos");
        assertThat(chunks).allSatisfy(
                chunk -> assertThat(chunk.initiativeId()).isEqualTo(INITIATIVE_ID));
    }

    @Test
    @DisplayName("GIVEN markdown sin encabezados WHEN ingest THEN genera un único chunk con la sección por defecto")
    void givenMarkdownWithoutHeadings_whenIngest_thenSingleChunkWithDefaultSection() {
        // GIVEN
        givenStoreAcceptsEverything();
        String markdown = "Solo texto plano, sin ningún encabezado.";

        // WHEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, markdown))
                .verifyComplete();

        // THEN
        verify(vectorStorePort).saveChunks(chunksCaptor.capture());
        List<PlanningChunk> chunks = chunksCaptor.getValue();

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().sectionName()).isEqualTo(DEFAULT_SECTION_NAME);
        assertThat(chunks.getFirst().content()).isEqualTo(markdown);
    }

    @Test
    @DisplayName("GIVEN la misma iniciativa y sección WHEN ingest dos veces THEN el id del chunk es determinista")
    void givenSameInitiativeAndSection_whenIngestTwice_thenChunkIdIsDeterministic() {
        // GIVEN
        givenStoreAcceptsEverything();
        String markdown = """
                ## Alcance
                
                Contenido.
                """;

        // WHEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, markdown))
                .verifyComplete();
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, markdown))
                .verifyComplete();

        // THEN
        verify(vectorStorePort, times(2)).saveChunks(chunksCaptor.capture());
        List<List<PlanningChunk>> allInvocations = chunksCaptor.getAllValues();

        String firstId = allInvocations.get(0).getFirst().id();
        String secondId = allInvocations.get(1).getFirst().id();
        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    @DisplayName("GIVEN un chunk WHEN ingest THEN la metadata lleva iniciativa, sección, título e índice")
    void givenChunk_whenIngest_thenMetadataCarriesInitiativeSectionTitleAndIndex() {
        // GIVEN
        givenStoreAcceptsEverything();
        String markdown = """
                # Cabecera
                
                Texto.
                
                ## Segunda
                
                Más texto.
                """;

        // WHEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, markdown))
                .verifyComplete();

        // THEN
        verify(vectorStorePort).saveChunks(chunksCaptor.capture());
        List<PlanningChunk> chunks = chunksCaptor.getValue();

        assertThat(chunks.get(0).metadata())
                .containsEntry("initiative_id", INITIATIVE_ID)
                .containsEntry("section_name", "Cabecera")
                .containsEntry("initiative_title", TITLE)
                .containsEntry("chunk_index", 0);
        assertThat(chunks.get(1).metadata()).containsEntry("chunk_index", 1);
    }

    @Test
    @DisplayName("GIVEN ingesta válida WHEN ingest THEN borra la iniciativa antes de guardar")
    void givenValidIngest_whenIngest_thenDeletesInitiativeBeforeSaving() {
        // GIVEN
        givenStoreAcceptsEverything();

        // WHEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, "## Sección\n\ntexto"))
                .verifyComplete();

        // THEN
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(vectorStorePort);
        inOrder.verify(vectorStorePort).deleteInitiative(INITIATIVE_ID);
        inOrder.verify(vectorStorePort).saveChunks(anyList());
    }

    @Test
    @DisplayName("GIVEN contenido vacío o nulo WHEN ingest THEN falla y no toca el almacén")
    void givenEmptyOrNullContent_whenIngest_thenFailsWithoutTouchingStore() {
        // GIVEN / WHEN / THEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, null))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, "   "))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(vectorStorePort, never()).deleteInitiative(anyString());
        verify(vectorStorePort, never()).saveChunks(anyList());
    }

    @Test
    @DisplayName("GIVEN el almacén falla al guardar WHEN ingest THEN el error se propaga")
    void givenStoreFailsOnSave_whenIngest_thenErrorIsPropagated() {
        // GIVEN
        when(vectorStorePort.deleteInitiative(anyString())).thenReturn(Mono.empty());
        when(vectorStorePort.saveChunks(anyList()))
                .thenReturn(Mono.error(new IllegalStateException("pgvector caído")));

        // WHEN / THEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, "## S\n\ntexto"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    /**
     * Caracteriza una rareza del troceo actual: el {@code split} solo corta ante {@code \n##}, de
     * modo que un encabezado de nivel 3 no abre sección propia y queda absorbido por la anterior.
     * No es necesariamente un defecto, pero conviene tenerlo congelado antes de tocar el
     * algoritmo.
     */
    @Test
    @DisplayName("GIVEN encabezados de nivel 3 WHEN ingest THEN NO abren sección propia (comportamiento actual)")
    void givenLevelThreeHeadings_whenIngest_thenTheyDoNotOpenTheirOwnSection() {
        // GIVEN
        givenStoreAcceptsEverything();
        String markdown = """
                ## Principal
                
                Texto.
                
                ### Subsección
                
                Más texto.
                """;

        // WHEN
        StepVerifier.create(useCase.ingestMarkdown(INITIATIVE_ID, TITLE, markdown))
                .verifyComplete();

        // THEN
        verify(vectorStorePort).saveChunks(chunksCaptor.capture());
        assertThat(chunksCaptor.getValue()).hasSize(1);
        assertThat(chunksCaptor.getValue().getFirst().sectionName()).isEqualTo("Principal");
    }
}

