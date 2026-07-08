package co.com.bancolombia.api;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler,
            @Value("${a2a.legacy-message-send-enabled:true}") boolean legacyMessageSendEnabled,
            @Value("${a2a.legacy.disable-after-sunset:false}") boolean disableAfterSunset,
            @Value("${a2a.legacy.sunset-date:2026-06-30}") String sunsetDateValue) {
        RouterFunction<ServerResponse> routes = route(POST("/"), handler::handleJsonRpc)
                .andRoute(GET("/.well-known/agent-card.json"), request -> handler.handleAgentCard())
                .andRoute(GET("/.well-known/agent.json"), request -> handler.handleAgentCard())
                .andRoute(GET("/card"), request -> handler.handleAgentCard())
                .andRoute(GET("/api/tasks"), request -> handler.handleListTasks());

        boolean legacyEnabledByDate = isLegacyEnabledByDate(disableAfterSunset, sunsetDateValue);
        if (legacyMessageSendEnabled && legacyEnabledByDate) {
            routes = routes.andRoute(POST("/message:send"), handler::handleSendMessage);
        }

        return routes;
    }

    private boolean isLegacyEnabledByDate(boolean disableAfterSunset, String sunsetDateValue) {
        if (!disableAfterSunset) {
            return true;
        }
        try {
            LocalDate sunsetDate = LocalDate.parse(sunsetDateValue);
            return !LocalDate.now(ZoneId.systemDefault()).isAfter(sunsetDate);
        } catch (RuntimeException _) {
            return true;
        }
    }
}
