package co.com.bancolombia.model.pullrequest;

import lombok.Builder;
import lombok.Getter;

/**
 * Representa un archivo afectado (modificado, agregado o eliminado) dentro de un Pull Request.
 */
@Getter
@Builder(toBuilder = true)
public class PullRequestChangeInfo {

    private final String path;
    private final String changeType;
}
