package co.com.bancolombia.pgvector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del almacén vectorial, tipada y en un solo sitio.
 *
 * <p><b>Fase 08 (D-12/T-05).</b> Estos dos valores se leían con {@code @Value} <b>sobre
 * campos</b>, y además duplicados: {@code table-name} aparecía tanto en {@code PgVectorConfig}
 * —para construir el {@code PgVectorStore}— como en {@code PgVectorPlanningAdapter} —para escribir
 * el SQL de borrado y de consulta—. Dos lecturas independientes de la misma clave es una invitación
 * a que un día digan cosas distintas: bastaba cambiar el valor por defecto en uno de los dos sitios
 * para que el adaptador consultase una tabla y el store escribiese en otra, sin ningún error
 * visible.
 *
 * <p>Es un {@code record} y no una clase con <i>setters</i> a propósito: <i>Rule_2.7</i> de
 * ArchUnit se cuenta <b>por campo</b>, así que una versión con campos mutables habría subido el
 * contador en lugar de bajarlo. Al ser componentes de un {@code record} son finales y se inyectan
 * por constructor, que es justo lo que la regla pide.
 *
 * @param tableName           tabla donde viven los fragmentos vectorizados.
 * @param similarityThreshold umbral de similitud por debajo del cual un resultado se descarta.
 */
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public record PgVectorProperties(String tableName, Double similarityThreshold) {

    private static final String DEFAULT_TABLE_NAME = "planning_chunks";
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.75;

    /**
     * Los valores por defecto viven aquí y no repartidos en cada {@code @Value}, que es lo que
     * permitía que divergieran.
     */
    public PgVectorProperties {
        tableName = (tableName == null || tableName.isBlank()) ? DEFAULT_TABLE_NAME : tableName;
        similarityThreshold =
                similarityThreshold == null ? DEFAULT_SIMILARITY_THRESHOLD : similarityThreshold;
    }
}

