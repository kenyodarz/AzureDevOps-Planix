package co.com.bancolombia.model.spec.gateways;

import co.com.bancolombia.model.spec.SpecDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de dominio que desacopla la persistencia y lectura íntegra de documentos de
 * especificación.
 */
public interface SpecStoragePort {

    /**
     * Obtiene un documento de especificación por su nombre.
     *
     * @param specName Nombre del archivo o identificador del spec (ej. "ideas_planning_q3.md").
     * @return Mono emitiendo el {@link SpecDocument}, o Mono de error con
     * {@link co.com.bancolombia.model.spec.SpecNotFoundException} si no existe.
     */
    Mono<SpecDocument> getSpec(String specName);

    /**
     * Lista todos los nombres de especificaciones disponibles en el repositorio de specs.
     *
     * @return Flux emitiendo los nombres de las especificaciones encontradas.
     */
    Flux<String> listAvailableSpecs();

    /**
     * Guarda o sobrescribe un documento de especificación en formato Markdown.
     *
     * @param specName        Nombre identificador del spec.
     * @param markdownContent Contenido en formato Markdown.
     * @return Mono señalando la finalización de la operación de guardado.
     */
    Mono<Void> saveSpec(String specName, String markdownContent);
}
