package co.com.bancolombia.model.dashboard.gateways;

import reactor.core.publisher.Mono;

/**
 * Salida de los reportes del tablero.
 *
 * <p>Existe para que el caso de uso deje de escribir en disco por su cuenta (D-10). Hasta la Fase
 * 05, {@code DevOpsDashboardUseCase.saveDashboardReport} era un método {@code void} que hacía
 * {@code new File(...)}, {@code mkdirs()} y {@code Files.writeString(...)} en mitad de una cadena
 * reactiva: bloqueaba el hilo del bucle de eventos y se tragaba las excepciones en un {@code catch}
 * que solo registraba.
 *
 * <p><b>Por qué es reactivo y el hermano {@code DashboardFallbackPort} no.</b> Aquí sí hay
 * entrada/salida real en tiempo de petición, así que el resultado tiene que poder esperarse,
 * componerse y fallar como cualquier otro paso de la cadena.
 *
 * <p><b>DP-04, resuelta como opción A (2026-08-29):</b> el destino es el disco, detrás de este
 * puerto. Añadir S3 más adelante es escribir otro adaptador; el dominio no se entera.
 */
public interface ReportStoragePort {

    /**
     * Guarda el contenido de un reporte.
     *
     * @param reportName nombre del reporte, ya saneado por quien lo compone; el adaptador decide
     *                   dónde acaba
     * @param content    contenido completo del reporte
     * @return señal de finalización; el error se propaga en lugar de perderse
     */
    Mono<Void> save(String reportName, String content);
}

