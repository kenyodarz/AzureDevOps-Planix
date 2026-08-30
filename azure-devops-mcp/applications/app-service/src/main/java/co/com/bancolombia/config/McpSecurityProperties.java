package co.com.bancolombia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Postura de acceso del servidor MCP, expresada como <b>configuración</b> y no como código.
 *
 * <p>Antes de la Fase 02 la postura estaba cableada en {@code McpSecurityConfig} con un
 * {@code .anyExchange().permitAll()} literal, de modo que endurecerla exigía <b>recompilar</b>: el
 * binario que llegaba a producción no era el que se había probado en la POC. Esta propiedad existe
 * para que el mismo {@code .jar} sirva en los dos escenarios y el salto sea una variable de entorno.
 *
 * <p>Se resuelve desde {@code MCP_SECURITY_MODE} (ver {@code application.yaml}). Se eligió una
 * propiedad y no un perfil de Spring (decisión <b>B-02</b>) porque el modo debe ser visible y
 * explícito en el log de arranque, no un efecto lateral de qué perfil esté activo.
 *
 * @param mode postura de acceso; {@link SecurityMode#PERMISSIVE} si no se indica nada.
 */
@ConfigurationProperties(prefix = "mcp.security")
public record McpSecurityProperties(SecurityMode mode) {

    /**
     * Postura de acceso del servidor MCP.
     */
    public enum SecurityMode {
        /**
         * Postura de la POC: no se exige token. Es el comportamiento vigente desde el origen del
         * proyecto y el valor por defecto mientras no exista el Service Principal (DP-01).
         *
         * <p>Para que las anotaciones {@code @PreAuthorize} de las tools se compilen, se ejecuten y
         * se prueben <b>también aquí</b> sin alterar el comportamiento observable, en este modo se
         * concede a la identidad anónima los roles de lectura y escritura.
         */
        PERMISSIVE,

        /**
         * Postura de pre y producción: toda petición ajena a las sondas del actuator exige un token
         * válido, y cada tool exige además su rol. Construido y probado, pero <b>apagado</b> hasta
         * que exista el Service Principal.
         */
        ENFORCED
    }

    /**
     * Normaliza la ausencia de valor al modo laxo, que es el comportamiento histórico.
     */
    public McpSecurityProperties {
        if (mode == null) {
            mode = SecurityMode.PERMISSIVE;
        }
    }

    /**
     * @return {@code true} si la postura exige autenticación.
     */
    public boolean isEnforced() {
        return mode == SecurityMode.ENFORCED;
    }
}

