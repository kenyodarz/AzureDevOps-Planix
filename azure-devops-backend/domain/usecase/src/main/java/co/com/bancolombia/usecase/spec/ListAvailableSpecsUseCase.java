package co.com.bancolombia.usecase.spec;

import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Caso de uso responsable de listar los nombres de todas las especificaciones disponibles en el
 * repositorio de specs.
 *
 * <p>Consume el puerto de dominio {@link SpecStoragePort}.
 */
@RequiredArgsConstructor
public class ListAvailableSpecsUseCase {

    private final SpecStoragePort specStoragePort;

    /**
     * Lista los nombres de los documentos de especificación disponibles.
     *
     * @return Flux emitiendo los identificadores de especificaciones encontradas
     */
    public Flux<String> execute() {
        return specStoragePort.listAvailableSpecs();
    }
}
