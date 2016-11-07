package rx.kinesis.producer.metrics;

import io.reactivex.Observable;

import java.util.concurrent.TimeUnit;

public class KinesisMetrics {

    private final MetricsReporter reporter;

    public KinesisMetrics(MetricsReporter reporter) {
        this.reporter = reporter;
    }

    public Observable<RecordSent> sentRecords() {
        return reporter.getSentRecords();
    }

    public Observable<RecordFailed> failedRecords() {
        return reporter.getFailedRecords();
    }

    public MetricsReporter reporter() {
        return reporter;
    }

    @SuppressWarnings("unchecked")
    public Observable<Integer> bytesPerSecond() {
        return sentRecords()
                .window(1, TimeUnit.SECONDS)
                .flatMapSingle(window -> window
                        .map(RecordSent::getSize)
                        .reduce((left, right) -> left + right)
                        .toSingle(0));
    }
}
