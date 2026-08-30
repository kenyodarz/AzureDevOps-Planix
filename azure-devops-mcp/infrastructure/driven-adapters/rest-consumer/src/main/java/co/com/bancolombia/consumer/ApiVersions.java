package co.com.bancolombia.consumer;

/**
 * Versiones por defecto de la API de Azure DevOps — punto único (decisión <b>B-07</b>).
 *
 * <p><b>Qué hacía falta arreglar.</b> El mismo ternario
 * {@code (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.x"} estaba copiado
 * <b>cinco veces</b> dentro de {@code RestConsumer}, con dos literales distintos y sin nada que
 * explicara por qué la consulta WIQL usa {@code 7.0} y el resto {@code 7.1}. Al partir el adaptador
 * en tres, esa duplicación se habría triplicado. B-07 decidió compartir un helper.
 *
 * <p><b>Lo que NO hace este helper.</b> No cambia ni un valor, ni tipa las versiones, ni las lleva
 * a configuración: los dos literales son <b>exactamente</b> los que había y siguen resolviéndose
 * con la misma condición. Convertirlos en un objeto de valor y sacarlos a
 * {@code @ConfigurationProperties} es <b>D-18</b>, material de la <b>Fase 06</b>. Lo que esta clase
 * consigue hoy es que cuando llegue esa fase haya <b>un solo sitio</b> que tocar.
 *
 * <p>Las dos consultas de ámbito de equipo no aparecen aquí: sus URLs llevan
 * {@code api-version=7.0} escrito <b>literalmente en la ruta</b> y no admiten sobreescritura. Se
 * dejan intactas porque esta fase no cambia ni una llamada HTTP.
 */
final class ApiVersions {

    /** Por defecto para leer, crear, actualizar y consultar en lote. */
    static final String WORK_ITEM_DEFAULT = "7.1";

    /** Por defecto para la consulta WIQL. Es distinta desde antes de este plan. */
    static final String WIQL_DEFAULT = "7.0";

    private ApiVersions() {
    }

    /**
     * Devuelve la versión recibida, o la que corresponda por defecto si viene ausente o en blanco.
     * La condición es literalmente la que había repetida en cada método.
     */
    static String orDefault(String apiVersion, String fallback) {
        return (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : fallback;
    }
}

