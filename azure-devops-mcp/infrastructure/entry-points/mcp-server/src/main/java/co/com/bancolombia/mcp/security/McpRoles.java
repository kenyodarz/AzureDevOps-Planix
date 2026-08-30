package co.com.bancolombia.mcp.security;

/**
 * Nombres de rol del servidor MCP, en un único sitio.
 *
 * <p>Los literales <b>se conservan exactamente</b> como estaban repartidos por las anotaciones
 * (decisión <b>B-03</b>): al no existir todavía la App Registration en Entra ID, nadie los ha
 * validado contra el proveedor de identidad, y renombrarlos sería asumir un contrato inexistente
 * ({@code rules/spring-rules.md} §6, Política de No-Asunción). Lo que sí cambia es que dejan de
 * estar duplicados como cadenas sueltas dentro de cada {@code @PreAuthorize}.
 *
 * <p>Las expresiones son constantes de compilación precisamente para poder usarse como argumento de
 * una anotación.
 */
public final class McpRoles {

    /** Prefijo que Spring Security antepone a los roles al evaluar {@code hasRole(...)}. */
    public static final String ROLE_PREFIX = "ROLE_";

    /** Rol de lectura sobre Azure DevOps. */
    public static final String READ = "MCP.AZURE_DEVOPS.READ";

    /** Rol de escritura sobre Azure DevOps. */
    public static final String WRITE = "MCP.AZURE_DEVOPS.WRITE";

    /** Autoridad concedida equivalente a {@link #READ}. */
    public static final String ROLE_READ = ROLE_PREFIX + READ;

    /** Autoridad concedida equivalente a {@link #WRITE}. */
    public static final String ROLE_WRITE = ROLE_PREFIX + WRITE;

    /** Expresión de autorización para las operaciones de consulta. */
    public static final String HAS_READ = "hasRole('" + READ + "')";

    /** Expresión de autorización para las operaciones de mutación. */
    public static final String HAS_WRITE = "hasRole('" + WRITE + "')";

    /**
     * Expresión para las tools deliberadamente públicas (sondas de vida). Se declara de forma
     * explícita en lugar de dejarlas sin anotación: una tool sin anotación es indistinguible de un
     * olvido, que es exactamente el problema que esta fase corrige.
     */
    public static final String PUBLIC = "permitAll()";

    private McpRoles() {
        throw new IllegalStateException("Clase de constantes, no instanciable");
    }
}

