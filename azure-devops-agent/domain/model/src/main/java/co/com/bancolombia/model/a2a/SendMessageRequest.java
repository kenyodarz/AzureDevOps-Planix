package co.com.bancolombia.model.a2a;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - SendMessageRequest Params object for the JSON-RPC "SendMessage" method. The
 * task ID is server-generated; clients do NOT set it here.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * The message to send to the agent. Must contain at least one Part.
     */
    private Message message;

    /**
     * Optional configuration hints (accepted output modes, history length, etc.). Represented as a
     * generic object to keep domain layer free of web concerns.
     */
    private MessageSendConfiguration configuration;
}
