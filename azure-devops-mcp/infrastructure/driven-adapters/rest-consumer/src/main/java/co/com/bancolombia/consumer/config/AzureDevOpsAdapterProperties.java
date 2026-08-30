package co.com.bancolombia.consumer.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros operativos del adaptador de Azure DevOps: <b>versiones de API</b> (D-18) y
 * <b>timeouts por operación</b> (D-24). <b>Fase 06</b>, decisión <b>B-09</b>.
 *
 * <h2>Por qué las versiones son configuración y no dominio</h2>
 *
 * <p><b>B-09</b> zanjó la duda que dejó abierta la Fase 05: las versiones pasan a
 * {@code @ConfigurationProperties}, <b>no</b> a un objeto de valor de dominio. DP-04 sentó el
 * precedente de que las <i>reglas</i> de Azure DevOps —los tipos por defecto, la traducción
 * {@code User Story → Historia de Usuario}— son dominio; pero una versión de API no es una regla de
 * negocio: es un detalle del proveedor que cambia por motivos ajenos al negocio y que un operador
 * debe poder mover sin recompilar. Meterla en {@code domain/model} habría acoplado el núcleo puro a
 * la cadencia de publicación de Microsoft.
 *
 * <p><b>Los literales no cambian.</b> {@code 7.1} para leer, crear, actualizar y consultar en lote;
 * {@code 7.0} para la consulta WIQL. Son exactamente los que {@code ApiVersions} centralizó en la
 * Fase 05 y los que estaban repetidos siete veces antes de ella. Al declararlos como valores por
 * defecto de este record, <b>un {@code application.yaml} que no los mencione produce las mismas
 * llamadas HTTP de siempre</b>.
 *
 * <p>⚠️ <b>B-10:</b> las dos consultas de ámbito de equipo siguen con {@code api-version=7.0}
 * <b>escrito literalmente en la ruta</b>. No se parametrizan: hacerlo habría cambiado una llamada
 * HTTP, y la regla innegociable nº 2 de esta fase es que no cambia ninguna.
 *
 * <h2>Por qué hacían falta timeouts por operación</h2>
 *
 * <p>Hasta esta fase el único límite temporal eran los {@code ReadTimeoutHandler} y
 * {@code WriteTimeoutHandler} de Netty configurados en {@code RestConsumerConfig} con
 * {@code adapter.restconsumer.timeout} — <b>5 segundos compartidos por las siete llamadas</b>, la de
 * lote incluida, que es justamente la que más tarda. Un lote de doscientos identificadores y una
 * consulta de un único work item tenían el mismo presupuesto de tiempo, elegido por nadie.
 *
 * <p>Estos cuatro valores son el timeout <b>reactivo</b> ({@code Mono.timeout}) que D-24 pedía. No
 * sustituyen a los de Netty, que siguen protegiendo la conexión: los complementan poniendo un techo
 * <b>por operación</b>. Al vencer, el {@code AzureDevOpsErrorTranslator} los convierte en
 * {@code AZDO_UNAVAILABLE}, no en un {@code TimeoutException} crudo.
 *
 * @param apiVersion       versiones por defecto cuando el cliente MCP no indica ninguna
 * @param operationTimeout techo de tiempo por tipo de operación
 */
@ConfigurationProperties(prefix = "adapter.restconsumer")
public record AzureDevOpsAdapterProperties(ApiVersion apiVersion, OperationTimeout operationTimeout) {

    public AzureDevOpsAdapterProperties {
        if (apiVersion == null) {
            apiVersion = ApiVersion.defaults();
        }
        if (operationTimeout == null) {
            operationTimeout = OperationTimeout.defaults();
        }
    }

    /**
     * Valores por defecto completos. Es el constructor que usan las pruebas de adaptador, para que
     * no dependan del contexto de Spring ni del {@code application.yaml}.
     */
    public static AzureDevOpsAdapterProperties defaults() {
        return new AzureDevOpsAdapterProperties(ApiVersion.defaults(), OperationTimeout.defaults());
    }

    /**
     * Versiones por defecto de la API de Azure DevOps. <b>Los dos literales son los de siempre.</b>
     *
     * @param workItem para leer, crear, actualizar y consultar en lote
     * @param wiql     para la consulta WIQL; es distinta desde antes de este plan
     */
    public record ApiVersion(String workItem, String wiql) {

        private static final String WORK_ITEM_DEFAULT = "7.1";
        private static final String WIQL_DEFAULT = "7.0";

        public ApiVersion {
            workItem = orDefault(workItem, WORK_ITEM_DEFAULT);
            wiql = orDefault(wiql, WIQL_DEFAULT);
        }

        public static ApiVersion defaults() {
            return new ApiVersion(WORK_ITEM_DEFAULT, WIQL_DEFAULT);
        }

        private static String orDefault(String value, String fallback) {
            return (value != null && !value.isBlank()) ? value : fallback;
        }
    }

    /**
     * Techo de tiempo por tipo de operación (D-24, valores elegidos en <b>DP-06 §0.2</b>).
     *
     * @param query     consulta de un work item y consulta WIQL
     * @param batch     consulta en lote; <b>es el caso crítico</b>, el que más tarda y el que hasta
     *                  ahora compartía los 5 s de todos los demás
     * @param command   creación y actualización
     * @param teamScope resolución de {@code AreaPath} e iteraciones. Es el más corto a propósito:
     *                  su fallo <b>no rompe nada</b>, cae al repliegue por concatenación que DP-04
     *                  conservó, así que esperar más solo retrasaría el repliegue
     */
    public record OperationTimeout(Duration query, Duration batch, Duration command,
                                   Duration teamScope) {

        private static final Duration QUERY_DEFAULT = Duration.ofSeconds(10);
        private static final Duration BATCH_DEFAULT = Duration.ofSeconds(30);
        private static final Duration COMMAND_DEFAULT = Duration.ofSeconds(10);
        private static final Duration TEAM_SCOPE_DEFAULT = Duration.ofSeconds(5);

        public OperationTimeout {
            query = orDefault(query, QUERY_DEFAULT);
            batch = orDefault(batch, BATCH_DEFAULT);
            command = orDefault(command, COMMAND_DEFAULT);
            teamScope = orDefault(teamScope, TEAM_SCOPE_DEFAULT);
        }

        public static OperationTimeout defaults() {
            return new OperationTimeout(QUERY_DEFAULT, BATCH_DEFAULT, COMMAND_DEFAULT,
                    TEAM_SCOPE_DEFAULT);
        }

        private static Duration orDefault(Duration value, Duration fallback) {
            return value != null ? value : fallback;
        }
    }
}

