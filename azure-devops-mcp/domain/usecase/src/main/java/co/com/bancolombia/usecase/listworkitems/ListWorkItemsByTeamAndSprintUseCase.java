package co.com.bancolombia.usecase.listworkitems;

import co.com.bancolombia.model.workitem.ListWorkItemsCommand;
import co.com.bancolombia.model.workitem.TeamScope;
import co.com.bancolombia.model.workitem.WiqlQuery;
import co.com.bancolombia.model.workitem.WiqlResult;
import co.com.bancolombia.model.workitem.WiqlStatement;
import co.com.bancolombia.usecase.querybywiql.QueryByWiqlUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * El flujo compuesto del sistema: listar los elementos de trabajo de una célula en un sprint.
 *
 * <p>Es <b>el</b> caso de uso de este repositorio. Hasta la Fase 04 sus 44 líneas vivían dentro de
 * {@code AzureDevOpsTools#listWorkItemsByTeamAndSprint}, es decir, en el entry-point: allí se
 * limpiaban las cadenas, se resolvían las rutas, se normalizaban los tipos y se <b>redactaba la
 * sentencia WIQL con un {@code String.format}</b>. Las otras cinco herramientas eran pasamanos y
 * seis de los siete casos de uso, delegantes vacíos: la pirámide estaba invertida porque la lógica
 * se había quedado arriba (deudas <b>D-07</b>, <b>D-08</b>, <b>D-09</b> y <b>D-12</b>).
 *
 * <p>Ahora el entry-point solo traduce protocolo y delega aquí. La normalización de las entradas
 * vive en los objetos de valor ({@link ListWorkItemsCommand}), la resolución de rutas en
 * {@link ResolveTeamScopeUseCase} y la redacción de la consulta en {@link WiqlStatement}.
 *
 * <p>⚠️ <b>La sentencia resultante debe ser idéntica carácter a carácter</b> a la que producía el
 * entry-point, en sus <b>ocho ramas</b>. Está congelada en {@code BASELINE.md} §7.1 y verificada
 * por {@code WiqlCharacterizationTest}. Un espacio de diferencia no produce un error: produce un
 * tablero vacío que nadie sabe explicar.
 *
 * <p><b>Sin logs, y no por olvido.</b> El módulo {@code domain/usecase} solo puede depender de
 * {@code :model}: la tarea {@code validateStructure} del plugin de Clean Architecture rechaza el
 * módulo si se le añade cualquier otra dependencia, SLF4J incluido. Las trazas del flujo se emiten
 * en las capas que sí pueden hacerlo —el entry-point y el adaptador—, sin que se haya perdido
 * ninguna de las que había.
 */
@RequiredArgsConstructor
public class ListWorkItemsByTeamAndSprintUseCase {

    private final ResolveTeamScopeUseCase resolveTeamScopeUseCase;
    private final QueryByWiqlUseCase queryByWiqlUseCase;

    public Mono<WiqlResult> execute(ListWorkItemsCommand command) {
        return resolveTeamScopeUseCase.resolve(command.organization(), command.project(),
                        command.team(), command.sprint())
                .flatMap(scope -> query(command, scope));
    }

    private Mono<WiqlResult> query(ListWorkItemsCommand command, TeamScope scope) {
        WiqlStatement statement = WiqlStatement.forTeamAndSprint(scope, command.workItemTypes());

        WiqlQuery wiqlQuery = WiqlQuery.builder().query(statement.value()).build();
        return queryByWiqlUseCase.queryByWiql(command.organization(), command.project(), wiqlQuery,
                command.apiVersion());
    }
}

