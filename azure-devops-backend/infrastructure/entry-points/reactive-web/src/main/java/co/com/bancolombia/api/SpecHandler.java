package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.SpecDocumentDTO;
import co.com.bancolombia.api.dto.SpecListDTO;
import co.com.bancolombia.api.error.ApiErrorTranslator;
import co.com.bancolombia.usecase.spec.GetSpecDocumentUseCase;
import co.com.bancolombia.usecase.spec.ListAvailableSpecsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler reactivo funcional para la gestión documental de especificaciones (specs).
 *
 * <p>Expone endpoints para listar las especificaciones disponibles y obtener el contenido
 * de un documento específico en Markdown.
 */
@Slf4j
@Component
public class SpecHandler {

    private final GetSpecDocumentUseCase getSpecDocumentUseCase;
    private final ListAvailableSpecsUseCase listAvailableSpecsUseCase;

    public SpecHandler(GetSpecDocumentUseCase getSpecDocumentUseCase,
            ListAvailableSpecsUseCase listAvailableSpecsUseCase) {
        this.getSpecDocumentUseCase = getSpecDocumentUseCase;
        this.listAvailableSpecsUseCase = listAvailableSpecsUseCase;
    }

    /**
     * {@code GET /api/planning/specs} — lista los identificadores de todas las especificaciones
     * disponibles.
     *
     * @param request petición entrante
     * @return respuesta HTTP 200 OK con {@link SpecListDTO}
     */
    public Mono<ServerResponse> listSpecs(ServerRequest request) {
        return listSpecs();
    }

    /**
     * Sobrecarga de conveniencia para invocar sin ServerRequest.
     *
     * @return respuesta HTTP 200 OK con {@link SpecListDTO}
     */
    public Mono<ServerResponse> listSpecs() {
        return listAvailableSpecsUseCase.execute()
                .collectList()
                .map(SpecListDTO::new)
                .flatMap(dto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }

    /**
     * {@code GET /api/planning/specs/{specName}} — obtiene el contenido del documento solicitado.
     *
     * @param request petición entrante con el path variable {@code specName}
     * @return respuesta HTTP 200 OK con {@link SpecDocumentDTO} o 404 Not Found si no existe
     */
    public Mono<ServerResponse> getSpec(ServerRequest request) {
        return Mono.fromCallable(() -> RequestValidator.requireText(
                        request.pathVariable("specName"), "nombre de la especificación"))
                .flatMap(getSpecDocumentUseCase::execute)
                .map(SpecDocumentDTO::fromDomain)
                .flatMap(dto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .onErrorResume(ApiErrorTranslator::toResponse);
    }
}
