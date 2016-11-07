package rx.kinesis.producer.metrics;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class MetricsReporter {

    private final PublishSubject<RecordSent> sentRecords = PublishSubject.create();

    private final PublishSubject<RecordFailed> failedRecords = PublishSubject.create();

    public void recordSent(RecordSent recordSent) {
        sentRecords.onNext(recordSent);
    }

    public void recordFailed(RecordFailed failedRecord) {
        failedRecords.onNext(failedRecord);
    }

    public void shutdown() {
        sentRecords.onComplete();
        failedRecords.onComplete();
    }

    Observable<RecordSent> getSentRecords() {
        return sentRecords;
    }

    Observable<RecordFailed> getFailedRecords() {
        return failedRecords;

    }
}
