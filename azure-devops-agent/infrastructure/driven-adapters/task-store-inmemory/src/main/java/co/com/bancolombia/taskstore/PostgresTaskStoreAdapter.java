package co.com.bancolombia.taskstore;

import co.com.bancolombia.model.a2a.Task;
import co.com.bancolombia.model.chat.gateways.TaskStoreGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresTaskStoreAdapter implements TaskStoreGateway {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public Mono<Task> save(Task task) {
        if (task == null || task.getId() == null) {
            return Mono.error(new IllegalArgumentException("Task and Task ID must not be null"));
        }
        log.debug("Saving Task {} to PostgreSQL store | State: {}", task.getId(),
                task.getStatus() != null ? task.getStatus().getState() : "UNKNOWN");

        return Mono.fromCallable(() -> {
            String payload;
            try {
                payload = jsonMapper.writeValueAsString(task);
            } catch (Exception e) {
                log.error("Error serializing Task to JSON", e);
                throw new IllegalArgumentException("Error serializing Task to JSON", e);
            }

            String contextId = task.getContextId();
            String state = task.getStatus() != null && task.getStatus().getState() != null
                    ? task.getStatus().getState().name()
                    : null;

            String sql = "INSERT INTO agent_tasks (id, context_id, state, payload, updated_at) " +
                    "VALUES (?, ?, ?, ?::jsonb, NOW()) " +
                    "ON CONFLICT (id) DO UPDATE " +
                    "SET context_id = EXCLUDED.context_id, " +
                    "    state = EXCLUDED.state, " +
                    "    payload = EXCLUDED.payload, " +
                    "    updated_at = NOW()";

            jdbcTemplate.update(sql, task.getId(), contextId, state, payload);
            return task;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Task> findById(String taskId) {
        if (taskId == null) {
            return Mono.empty();
        }
        log.debug("Retrieving Task {} from PostgreSQL store", taskId);

        return Mono.fromCallable(() -> {
            String sql = "SELECT payload FROM agent_tasks WHERE id = ?";
            List<Task> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                String payload = rs.getString("payload");
                try {
                    return jsonMapper.readValue(payload, Task.class);
                } catch (Exception e) {
                    log.error("Error deserializing Task from JSON", e);
                    return null;
                }
            }, taskId);

            if (results.isEmpty() || results.get(0) == null) {
                return null;
            }
            return results.get(0);
        }).subscribeOn(Schedulers.boundedElastic()).flatMap(Mono::justOrEmpty);
    }

    @Override
    public Flux<Task> findAll() {
        log.debug("Retrieving all Tasks from PostgreSQL store");

        return Mono.fromCallable(() -> {
            String sql = "SELECT payload FROM agent_tasks ORDER BY updated_at DESC";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String payload = rs.getString("payload");
                try {
                    return jsonMapper.readValue(payload, Task.class);
                } catch (Exception e) {
                    log.error("Error deserializing Task from JSON", e);
                    return null;
                }
            });
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable)
        .filter(t -> t != null);
    }
}

