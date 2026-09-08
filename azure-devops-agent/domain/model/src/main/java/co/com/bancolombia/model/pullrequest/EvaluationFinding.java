package co.com.bancolombia.model.pullrequest;

import lombok.Builder;
import lombok.Getter;

/**
 * Hallazgo o desviación detectada durante la evaluación de un Pull Request.
 */
@Getter
@Builder(toBuilder = true)
public class EvaluationFinding {

    private final String filePath;
    private final String severity;
    private final String category;
    private final String message;
    private final String suggestion;
}
