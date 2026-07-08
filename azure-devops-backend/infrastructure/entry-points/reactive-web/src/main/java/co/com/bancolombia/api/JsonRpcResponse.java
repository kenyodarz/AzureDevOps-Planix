package co.com.bancolombia.api;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonRpcResponse {

    private String jsonrpc;
    private String id;
    @SuppressWarnings("java:S1948")
    private Map<String, Object> result;
    private JsonRpcError error;
}

