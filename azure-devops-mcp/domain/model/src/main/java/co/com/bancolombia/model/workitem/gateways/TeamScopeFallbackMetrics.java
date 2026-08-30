package co.com.bancolombia.model.workitem.gateways;

/**
 * Puerto de observabilidad del repliegue por concatenación.
 *
 * <p>Existe por la deuda <b>D-09</b>: hasta la Fase 04, cuando Azure DevOps no resolvía el
 * {@code AreaPath} o el {@code IterationPath} el sistema fabricaba la ruta por concatenación
 * <b>y nadie medía cuántas veces ocurría</b>. Sin ese dato es imposible decidir con fundamento si
 * el repliegue puede retirarse: solo se sabía que existía, no si se usaba.
 *
 * <p><b>DP-04 §0.1 eligió la opción (a)</b> —conservar el repliegue con métrica y log de aviso—,
 * de modo que este puerto es lo que convierte esa decisión en un hecho verificable.
 *
 * <p>Es un puerto de dominio: la implementación con Micrometer vive en {@code app-service} y el
 * dominio no sabe que existe Micrometer.
 *
 * <p><b>Por qué el puerto recibe también la causa.</b> El módulo {@code domain/usecase} no puede
 * declarar ninguna dependencia más allá de {@code :model} —lo verifica la tarea
 * {@code validateStructure} del plugin de Clean Architecture de Bancolombia, que falla con «Use
 * case module is invalid»—, así que <b>ni siquiera SLF4J puede entrar en la capa de dominio</b>.
 * Pasar la excepción original a través de este puerto permite que el aviso se emita en el
 * adaptador con toda la información de diagnóstico, sin ensuciar el dominio.
 */
public interface TeamScopeFallbackMetrics {

    /**
     * Se invoca cada vez que el {@code AreaPath} se fabrica por concatenación.
     *
     * @param team  nombre de la célula cuya ruta no pudo resolverse
     * @param cause error que impidió resolverla
     */
    void areaPathFallbackUsed(String team, Throwable cause);

    /**
     * Se invoca cada vez que el {@code IterationPath} se fabrica por concatenación.
     *
     * @param sprint                  nombre del sprint cuya ruta no pudo resolverse
     * @param calendarYearInterleaved {@code true} si además se intercaló el año del calendario,
     *                                que es el caso que produce el tablero vacío
     * @param cause                   error que impidió resolverla
     */
    void iterationPathFallbackUsed(String sprint, boolean calendarYearInterleaved, Throwable cause);

    /**
     * Implementación inerte, para pruebas y para cualquier contexto donde no haya registro de
     * métricas. Evita tener que comprobar nulos en el caso de uso.
     */
    static TeamScopeFallbackMetrics noOp() {
        return new TeamScopeFallbackMetrics() {
            @Override
            public void areaPathFallbackUsed(String team, Throwable cause) {
                // Sin efecto
            }

            @Override
            public void iterationPathFallbackUsed(String sprint, boolean calendarYearInterleaved,
                    Throwable cause) {
                // Sin efecto
            }
        };
    }
}

