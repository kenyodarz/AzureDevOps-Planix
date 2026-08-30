package co.com.bancolombia.model.prompt.gateways;

import java.util.Map;

/**
 * Origen de las plantillas de prompt.
 *
 * <p>El dominio conoce el <b>nombre lógico</b> de la plantilla, nunca su ubicación. Esa
 * indirección es lo que permitirá que la deuda <b>D-35</b> cambie el origen —hoy el classpath del
 * BFF, mañana el MCP o el agente— sin volver a tocar los casos de uso.
 *
 * <p><b>Formato de las plantillas.</b> Texto plano con dos únicas reglas:
 * <ul>
 *   <li>Las variables se escriben {@code ${nombre}} y son <b>obligatorias</b>: si el llamador no
 *       aporta valor para una de ellas, la implementación debe fallar. Un prompt a medio rellenar
 *       produce respuestas del LLM que parecen válidas y no lo son.</li>
 *   <li>Toda línea que empiece por {@code #~} es <b>metadato</b> —procedencia, reparto por dueño
 *       para D-35— y se descarta antes de renderizar. No llega nunca al modelo.</li>
 * </ul>
 */
public interface PromptTemplatePort {

    /**
     * Renderiza la plantilla indicada sustituyendo sus variables.
     *
     * @param name      nombre lógico de la plantilla, sin ruta ni extensión
     * @param variables valores de las variables declaradas en la plantilla
     * @return el prompt listo para enviar al agente
     * @throws co.com.bancolombia.model.prompt.exceptions.PromptTemplateException si la plantilla no
     *                                                                            existe, no puede
     *                                                                            leerse o falta el
     *                                                                            valor de alguna
     *                                                                            variable
     */
    String render(String name, Map<String, String> variables);
}

