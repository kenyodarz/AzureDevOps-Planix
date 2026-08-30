package co.com.bancolombia.consumer;

/**
 * Resolución de la versión de API a usar en cada llamada — punto único (decisión <b>B-07</b>,
 * Fase 05; <b>B-09</b>, Fase 06).
 *
 * <h2>De dónde viene</h2>
 *
 * <p>El mismo ternario {@code (apiVersion != null && !apiVersion.isBlank()) ? apiVersion : "7.x"}
 * estaba copiado <b>cinco veces</b> dentro de {@code RestConsumer}, con dos literales distintos y
 * sin nada que explicara por qué la consulta WIQL usa {@code 7.0} y el resto {@code 7.1}. Al partir
 * el adaptador en tres, esa duplicación se habría triplicado. B-07 decidió compartir el helper.
 *
 * <h2>Qué cambió en la Fase 06</h2>
 *
 * <p><b>B-09</b> sacó los dos literales de aquí y los llevó a
 * {@code AzureDevOpsAdapterProperties.ApiVersion}, es decir, a <b>configuración</b> — no a un objeto
 * de valor de dominio: una versión de API es un detalle del proveedor, no una regla de negocio, y
 * meterla en {@code domain/model} habría acoplado el núcleo puro a la cadencia de publicación de
 * Microsoft. Lo que queda en esta clase es exactamente lo que siempre fue suyo: <b>la condición</b>.
 *
 * <p><b>Los valores por defecto no cambiaron</b>, solo cambiaron de sitio. Un
 * {@code application.yaml} que no mencione las versiones produce las mismas llamadas HTTP de
 * siempre.
 *
 * <p>Las dos consultas de ámbito de equipo siguen sin aparecer aquí: sus URLs llevan
 * {@code api-version=7.0} escrito <b>literalmente en la ruta</b> y no admiten sobreescritura. Se
 * dejan intactas por decisión expresa de <b>B-10</b>, porque parametrizarlas habría cambiado una
 * llamada HTTP.
 */
final class ApiVersions {

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
