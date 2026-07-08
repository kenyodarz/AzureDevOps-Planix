package co.com.bancolombia.model.a2a;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A Protocol v1.0 - Part
 * <p>
 * Fundamental content container used within Messages and Artifacts. A Part holds one of: text
 * content, a file reference (URL or inline bytes), or structured data. Only one content field
 * should be set per Part instance.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Part {

    /**
     * Plain textual content.
     */
    private String text;

    /**
     * Structured JSON value for machine-readable data (e.g., query parameters, results).
     */
    @SuppressWarnings("java:S1948")
    private Map<String, Object> data;

    /**
     * URI referencing external file content.
     */
    private String url;

    /**
     * MIME type of the content (e.g., "text/plain", "application/json").
     */
    @SuppressWarnings("java:S116")
    private String mediaType;

    /**
     * Optional name for the file or content.
     */
    private String filename;

    /**
     * Additional context metadata.
     */
    @SuppressWarnings("java:S1948")
    private Map<String, Object> metadata;

    // ─── Factory helpers ───────────────────────────────────────────────────────

    public static Part ofText(String text) {
        return Part.builder().text(text).build();
    }

    public static Part ofData(Map<String, Object> data) {
        return Part.builder().data(data).build();
    }

    public static Part ofUrl(String url, String mediaType) {
        return Part.builder().url(url).mediaType(mediaType).build();
    }
}
