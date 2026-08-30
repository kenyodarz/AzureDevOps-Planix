package co.com.bancolombia.model.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copias defensivas para los objetos de valor del dominio — Fase 07, D-16.
 *
 * <p><b>Por qué existe.</b> Un {@code record} garantiza que la <i>referencia</i> a una colección no
 * cambie, pero no que la colección no se modifique por detrás. Sin esta copia, un
 * {@code workItem.fields().put(...)} desde cualquier punto de un flujo reactivo seguiría siendo
 * posible, y el objetivo de la fase es precisamente que <b>un {@code WorkItem} válido no pueda dejar
 * de serlo a mitad de un flujo</b>.
 *
 * <p>⚠️ <b>Dos decisiones deliberadas, y las dos existen para no cambiar el JSON público.</b>
 *
 * <ol>
 *   <li><b>Un nulo se conserva como nulo.</b> No se sustituye por una colección vacía. Convertir
 *       {@code null} en {@code []} o {@code {}} cambiaría el cuerpo que ve el cliente MCP, y eso no
 *       lo autorizó DP-07 §0.2(c). Es el mismo criterio que el javadoc de {@code TeamScope} aplicó a
 *       las cadenas en blanco: la invariante se endurece cuando el propietario lo decida, no cuando
 *       quede más limpio.</li>
 *   <li><b>Se usa {@link LinkedHashMap} y {@link ArrayList}, no {@code Map.copyOf} ni
 *       {@code List.copyOf}.</b> Los métodos de fábrica de Java <b>no preservan el orden de
 *       iteración</b> y <b>rechazan los nulos</b>. El orden de las claves de {@code fields} es lo
 *       que determina el orden de los campos del JSON emitido —contrato congelado en
 *       {@code McpResponsePayloadCharacterizationTest}— y los elementos nulos son posibles porque
 *       los mappers del adaptador los propagan.</li>
 * </ol>
 */
public final class DomainCollections {

    private DomainCollections() {
        // Utilidad de dominio: no se instancia.
    }

    /**
     * Devuelve una vista inmodificable de la lista, conservando su orden y admitiendo elementos
     * nulos. Una lista nula devuelve {@code null}.
     */
    public static <T> List<T> immutableCopy(List<T> source) {
        if (source == null) {
            return null;
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    /**
     * Devuelve una vista inmodificable del mapa, conservando su <b>orden de iteración</b> y
     * admitiendo valores nulos. Un mapa nulo devuelve {@code null}.
     */
    public static <K, V> Map<K, V> immutableCopy(Map<K, V> source) {
        if (source == null) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}

