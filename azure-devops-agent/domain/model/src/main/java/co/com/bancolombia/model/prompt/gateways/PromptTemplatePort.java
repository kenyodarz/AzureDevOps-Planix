package co.com.bancolombia.model.prompt.gateways;

import co.com.bancolombia.model.prompt.PromptTemplateId;
import java.util.Map;

/**
 * Puerto de salida para obtener las plantillas de prompt ya renderizadas.
 *
 * <p>El dominio declara <b>qué</b> plantilla necesita y con qué variables, sin conocer dónde ni
 * cómo se almacenan (classpath, base de datos, servicio remoto). Esa decisión pertenece al
 * adaptador.
 *
 * <p>Las variables se resuelven <b>por nombre</b>, no por posición, para eliminar la clase de
 * errores en la que dos argumentos del mismo tipo se intercambian silenciosamente.
 */
public interface PromptTemplatePort {

    /**
     * Renderiza la plantilla indicada sustituyendo sus marcadores {@code {{nombre}}} por los
     * valores suministrados.
     *
     * @param id        identificador de la plantilla, no nulo
     * @param variables valores a sustituir, indexados por nombre de marcador; no nulo
     * @return el prompt listo para enviar al modelo, sin marcadores pendientes
     * @throws co.com.bancolombia.model.prompt.exceptions.PromptTemplateException si la plantilla no
     *                                                                           existe o quedan
     *                                                                           marcadores sin
     *                                                                           resolver
     */
    String render(PromptTemplateId id, Map<String, Object> variables);
}

