package co.com.bancolombia.api;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        RouterFunction<ServerResponse> routes = route(POST("/api/planning/ingest"), handler::handleIngestPlanning)
                .andRoute(GET("/api/planning/search"), handler::handleSearchPlanning)
                .andRoute(GET("/api/planning/initiatives"),
                        request -> handler.handleListInitiatives())
                .andRoute(GET("/api/planning/initiatives/{id}/chunks"),
                        handler::handleGetInitiativeChunks)
                .andRoute(DELETE("/api/planning/initiatives/{id}"), handler::handleDeleteInitiative)
                .andRoute(PUT("/api/planning/initiatives/{id}/cell"), handler::handleUpdateCell)
                .andRoute(GET("/api/devops/dashboard"), handler::handleDevOpsDashboard)
                .andRoute(GET("/api/devops/dashboard/stream"), handler::handleDevOpsDashboardStream)
                .andRoute(GET("/api/tasks"), request -> handler.handleListTasks());

        // SPA (Single Page Application) fallback for Angular router
        RequestPredicate isSpaRoute = request -> {
            String path = request.path();
            return !path.startsWith("/api") &&
                    !path.equals("/") &&
                    !path.contains(".");
        };

        routes = routes.andRoute(GET("/**").and(isSpaRoute), request ->
                ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .bodyValue(new ClassPathResource("static/index.html"))
        );

        return routes;
    }
}
