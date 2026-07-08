package co.com.bancolombia.api.dto.event;

import co.com.bancolombia.model.dashboard.DashboardResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUpdateEvent {

    private String event;
    private DashboardResponse data;

}

