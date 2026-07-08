package co.com.bancolombia.model.chat.gateways;

import co.com.bancolombia.model.a2a.Task;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskStoreGateway {

    Mono<Task> save(Task task);

    Mono<Task> findById(String taskId);

    Flux<Task> findAll();
}
