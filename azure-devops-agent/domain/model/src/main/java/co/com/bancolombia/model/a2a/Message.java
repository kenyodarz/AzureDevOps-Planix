package co.com.bancolombia.model.a2a;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - Message A single turn of communication between a client and an agent. role
 * must be "user" or "agent" (lowercase, per spec).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * "user" or "agent" (A2A spec uses lowercase).
     */
    private String role;

    /**
     * Content parts of the message (text, data, file). At least one required.
     */
    private List<Part> parts;

    /**
     * Unique identifier for this message.
     */
    private String messageId;

    /**
     * Groups related messages within the same session/conversation.
     */
    private String contextId;

    /**
     * IDs of previous tasks this message is refining or continuing.
     */
    private List<String> referenceTaskIds;

    // ─── Factory helpers ───────────────────────────────────────────────────────

    /**
     * Creates a simple user message with plain text content.
     */
    public static Message userText(String text) {
        return Message.builder()
                .role("user")
                .parts(List.of(Part.ofText(text)))
                .build();
    }

    /**
     * Creates a simple agent message with plain text content.
     */
    public static Message agentText(String text) {
        return Message.builder()
                .role("agent")
                .parts(List.of(Part.ofText(text)))
                .build();
    }

    /**
     * Extracts the first text content from parts, or empty string if none.
     */
    public String extractText() {
        if (parts == null) {
            return "";
        }
        return parts.stream()
                .filter(p -> p.getText() != null)
                .map(Part::getText)
                .findFirst()
                .orElse("");
    }
}
