package co.com.bancolombia.metrics.aws;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

@Component
@RequiredArgsConstructor
public class MicrometerMetricPublisher implements MetricPublisher {

    private final ExecutorService service = Executors.newFixedThreadPool(10);
    private final MeterRegistry registry;

    @Override
    public void publish(MetricCollection metricCollection) {
        service.submit(() -> {
            List<Tag> tags = buildTags(metricCollection);
            metricCollection.stream()
                    .filter(rec -> rec.value() instanceof Duration
                            || rec.value() instanceof Integer)
                    .forEach(rec -> {
                        if (rec.value() instanceof Duration) {
                            registry.timer(rec.metric().name(), tags)
                                    .record((Duration) rec.value());
                        } else if (rec.value() instanceof Integer) {
                            registry.counter(rec.metric().name(), tags)
                                    .increment((Integer) rec.value());
                        }
                    });
        });
    }

    @Override
    public void close() {
        // No-op close implementation suitable for micrometer publisher
    }

    private List<Tag> buildTags(MetricCollection metricCollection) {
        return metricCollection.stream()
                .filter(rec -> rec.value() instanceof String
                        || rec.value() instanceof Boolean)
                .map(rec -> Tag.of(rec.metric().name(), rec.value().toString()))
                .toList();
    }
}
