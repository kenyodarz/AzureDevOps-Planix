package co.com.bancolombia.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonRpcRequest {

    private String jsonrpc;
    private String id;
    private String method;
    private Object params;
}

