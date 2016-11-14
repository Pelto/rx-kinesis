package rx.kinesis.producer;

import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import io.reactivex.Flowable;
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

    private final PublishProcessor<BufferedRecord> buffer = PublishProcessor.create();

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

    private Flowable<CompletedRecord> sendRecords(List<BufferedRecord> records) {

        List<PutRecordsRequestEntry> entries = records.stream()
                .map(transit -> new PutRecordsRequestEntry()
                        .withData(transit.record.getBuffer())
                        .withPartitionKey(transit.record.getPartitionKey()))
                .collect(Collectors.toCollection(() -> new ArrayList<>(records.size())));

        PutRecordsRequest request = new PutRecordsRequest()
                .withStreamName(configuration.getStreamName())
                .withRecords(entries);

        return Single.fromCallable(() -> kinesis.putRecordsAsync(request))
                .flatMap(future -> Single.fromFuture(future).subscribeOn(Schedulers.io()))
                .compose(source -> configuration.getRetryPolicy().attach(source))
                .doOnError(throwable -> records.forEach(record -> record.callback.onError(throwable)))
                .toMaybe()
                .onErrorComplete()
                .flatMapPublisher(result -> Flowable.fromIterable(result.getRecords()))
                .zipWith(Flowable.fromIterable(records), CompletedRecord::new);
    }

    private void acknowledge(CompletedRecord completedRecord) {

        if (recordFailed(completedRecord)) {
            completedRecord.callback.onError(new KinesisProducerException(
                    completedRecord.resultEntry.getErrorMessage(),
                    completedRecord.resultEntry.getErrorCode(),
                    configuration.getStreamName()));

            log.info("Failed to send {}, {}",
                     completedRecord.resultEntry.getErrorCode(),
                     completedRecord.resultEntry.getErrorMessage());

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

    private static boolean recordFailed(CompletedRecord record) {
        return record.resultEntry.getErrorCode() != null && ! record.resultEntry.getErrorCode().isEmpty();
    }

    public Single<KinesisRecordResult> send(KinesisRecord record) {
        Single<KinesisRecordResult> single = Single.create(subscriber -> {
            BufferedRecord transitRecord = new BufferedRecord(record, subscriber);
            buffer.onNext(transitRecord);
        });

        return single;
    }

    private static class BufferedRecord {

        private final KinesisRecord record;

        private final SingleEmitter<KinesisRecordResult> callback;

        private BufferedRecord(KinesisRecord record, SingleEmitter<KinesisRecordResult> returnSubject) {
            this.record = record;
            this.callback = returnSubject;
        }
    }

    private static class CompletedRecord {

        private final SingleEmitter<KinesisRecordResult> callback;

        private final PutRecordsResultEntry resultEntry;

        private final KinesisRecord record;

        private CompletedRecord(PutRecordsResultEntry result,  BufferedRecord transit) {
            this.callback = transit.callback;
            this.resultEntry = result;
            this.record = transit.record;
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
