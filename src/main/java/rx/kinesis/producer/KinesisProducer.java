package rx.kinesis.producer;

import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.kinesis.producer.metrics.KinesisMetrics;
import rx.kinesis.producer.metrics.RecordFailed;
import rx.kinesis.producer.metrics.RecordSent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KinesisProducer {

    private final Logger log = LoggerFactory.getLogger(KinesisProducer.class);

    private final KinesisProducerConfiguration configuration;

    private final AmazonKinesisAsync kinesis;

    private final KinesisMetrics metrics;

    private final PublishProcessor<RecordInTransit> buffer = PublishProcessor.create();

    private final Disposable subscription;

    KinesisProducer(KinesisProducerConfiguration configuration, AmazonKinesisAsync kinesis, KinesisMetrics metrics) {
        this.configuration = configuration;
        this.kinesis = kinesis;
        this.metrics = metrics;

        this.subscription = buffer
                .compose(source -> configuration.getBufferingPolicy().attach(source))
                .flatMapSingle(Flowable::toList)
                .filter(buffer -> !buffer.isEmpty())
                .flatMap(this::sendRecords)
                .subscribe(this::acknowledge);
    }

    private Flowable<CompletedRecord> sendRecords(List<RecordInTransit> records) {

        List<PutRecordsRequestEntry> entries = records.stream()
                .map(transit -> new PutRecordsRequestEntry()
                        .withData(transit.record.getBuffer())
                        .withPartitionKey(transit.record.getPartitionKey()))
                .collect(Collectors.toCollection(() -> new ArrayList<>(records.size())));

        PutRecordsRequest request = new PutRecordsRequest()
                .withStreamName(configuration.getStreamName())
                .withRecords(entries);

        Maybe<PutRecordsResult> future = Maybe.fromFuture(kinesis.putRecordsAsync(request));

        return future
                .subscribeOn(Schedulers.io())
                .doOnError(throwable -> records.forEach(record -> record.callback.onError(throwable)))
                .onErrorComplete()
                .flatMapPublisher(result -> Flowable.fromIterable(result.getRecords()))
                .zipWith(Flowable.fromIterable(records), KinesisProducer::merge);
    }

    private static CompletedRecord merge(PutRecordsResultEntry result,  RecordInTransit transit) {
        return new CompletedRecord(transit.callback, result, transit.record);
    }

    private void acknowledge(CompletedRecord completedRecord) {

        if (completedRecord.resultEntry.getErrorCode() != null && !completedRecord.resultEntry.getErrorCode().isEmpty()) {
            completedRecord.callback.onError(new KinesisProducerException(
                    completedRecord.resultEntry.getErrorMessage(),
                    completedRecord.resultEntry.getErrorCode(),
                    configuration.getStreamName()));

            log.info("Failed to send {}, {}", completedRecord.resultEntry.getErrorCode(), completedRecord.resultEntry.getErrorMessage());

            RecordFailed recordFailed = new RecordFailed(
                    completedRecord.resultEntry.getShardId(),
                    completedRecord.resultEntry.getErrorCode());

            metrics.reporter().recordFailed(recordFailed);

            return;
        }

        RecordSent recordSent = new RecordSent(
                completedRecord.record.getBuffer().position(),
                completedRecord.resultEntry.getShardId());

        metrics.reporter().recordSent(recordSent);

        KinesisRecordResult result = new KinesisRecordResult(
                completedRecord.resultEntry.getShardId(),
                completedRecord.resultEntry.getSequenceNumber());

        completedRecord.callback.onSuccess(result);
    }

    public Single<KinesisRecordResult> send(KinesisRecord record) {
        return Single.create(subscriber -> {
            RecordInTransit transitRecord = new RecordInTransit(record, subscriber);
            buffer.onNext(transitRecord);
        });
    }

    private static class RecordInTransit {

        private final KinesisRecord record;

        private final SingleEmitter<KinesisRecordResult> callback;

        private RecordInTransit(KinesisRecord record, SingleEmitter<KinesisRecordResult> returnSubject) {
            this.record = record;
            this.callback = returnSubject;
        }
    }

    private static class CompletedRecord {

        private final SingleEmitter<KinesisRecordResult> callback;

        private final PutRecordsResultEntry resultEntry;

        private final KinesisRecord record;

        private CompletedRecord(SingleEmitter<KinesisRecordResult> callback, PutRecordsResultEntry resultEntry, KinesisRecord record) {
            this.callback = callback;
            this.resultEntry = resultEntry;
            this.record = record;
        }
    }

    public KinesisMetrics metrics() {
        return metrics;
    }

    public void shutdown() {
        subscription.dispose();
        metrics.reporter().shutdown();
    }
}