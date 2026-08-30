package co.com.bancolombia.api.dto.event;

import co.com.bancolombia.api.dto.dashboard.DashboardResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento que viaja por el stream SSE del tablero.
 *
 * <p><b>Fase 06 (B-06, D-31).</b> Se añade {@code message} y el tipo {@code ERROR}. Hasta ahora,
 * cuando el agente fallaba antes del primer elemento, WebFlux no llegaba a escribir la cabecera
 * {@code text/event-stream} y el cliente recibía un 500 con cuerpo JSON: el frontend cerraba la
 * conexión en silencio y el usuario no distinguía «no hay datos» de «el agente se cayó».
 *
 * <p>El cambio es <b>aditivo</b>: los eventos {@code INITIAL} y {@code BATCH_UPDATE} conservan
 * exactamente los mismos campos que antes, con {@code message} a {@code null}.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUpdateEvent {

    /**
     * Tipo de evento que indica que la auditoría no pudo completarse.
     */
    public static final String ERROR = "ERROR";

    private String event;
    private DashboardResponse data;

    /**
     * Detalle legible del fallo. Solo viaja relleno en los eventos {@link #ERROR}.
     */
    private String message;

    public DashboardUpdateEvent(String event, DashboardResponse data) {
        this(event, data, null);
    }

    /**
     * Evento de fallo con su mensaje.
     *
     * <p>Se llama {@code ofError} y no {@code error} porque esta clase ya tiene una constante
     * {@link #ERROR}: en Java, {@code DashboardUpdateEvent.error(...)} y
     * {@code DashboardUpdateEvent.ERROR} solo se distinguen por las mayúsculas, y quien lee el
     * código a velocidad de lectura no distingue si está invocando la fábrica o leyendo el nombre
     * del tipo de evento. El prefijo {@code of} deja claro que esto construye algo.
     */
    public static DashboardUpdateEvent ofError(String message) {
        return new DashboardUpdateEvent(ERROR, null, message);
    }
}

