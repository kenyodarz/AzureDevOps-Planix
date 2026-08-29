package co.com.bancolombia.usecase.chat.handler;

/**
 * Nombres de las variables que declaran las plantillas de prompt.
 *
 * <p>Fuente única de verdad compartida por los handlers: el nombre de cada variable aparece una
 * sola vez en todo el dominio.
 */
final class PromptVariables {

    static final String WORK_ITEM_ID = "workItemId";
    static final String ORGANIZATION = "organizacion";
    static final String PROJECT = "proyecto";
    static final String AGILE_GUIDE = "guiaAgilidad";
    static final String STANDARDS = "estandares";
    static final String ORIGINAL_IDEA = "ideaOriginal";
    static final String RAG_CONTEXT = "contextoRag";
    static final String CORPORATE_TEMPLATE = "plantillaCorporativa";

    private PromptVariables() {
        // Constantes sin estado
    }
}

