package co.com.bancolombia.usecase.spec;

import co.com.bancolombia.model.spec.SpecDocument;
import co.com.bancolombia.model.spec.gateways.SpecStoragePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso responsable de obtener un documento de especificación por su nombre.
 *
 * <p>Consume el puerto de dominio {@link SpecStoragePort}, asegurando la validación del nombre
 * solicitado y la propagación de excepciones de negocio correspondientes (como
 * {@link co.com.bancolombia.model.spec.SpecNotFoundException}).
 */
@RequiredArgsConstructor
public class GetSpecDocumentUseCase {

    private final SpecStoragePort specStoragePort;

    /**
     * Obtiene el documento de especificación solicitado.
     *
     * @param specName nombre del archivo o identificador del spec (ej. "ideas_planning_q3.md")
     * @return Mono con el {@link SpecDocument}, o Mono de error si el nombre es inválido o no
     * existe
     */
    public Mono<SpecDocument> execute(String specName) {
        if (specName == null || specName.isBlank()) {
            return Mono.error(new IllegalArgumentException(
                    "El nombre de la especificación no puede ser nulo o vacío"));
        }
        return specStoragePort.getSpec(specName.trim());
    }
}
