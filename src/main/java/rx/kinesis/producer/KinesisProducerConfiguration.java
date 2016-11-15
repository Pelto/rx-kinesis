package rx.kinesis.producer;

import rx.kinesis.producer.buffering.BufferingPolicy;
import rx.kinesis.producer.retry.RetryPolicy;

public class KinesisProducerConfiguration {

    private final String streamName;

    private final BufferingPolicy bufferingPolicy;

    private final RetryPolicy kinesisRetryPolicy;

    private final RetryPolicy recordRetryPolicy;

    public KinesisProducerConfiguration(String streamName,
                                        BufferingPolicy bufferingPolicy,
                                        RetryPolicy kinesisRetryPolicy,
                                        RetryPolicy recordRetryPolicy) {
        this.bufferingPolicy = bufferingPolicy;
        this.streamName = streamName;
        this.kinesisRetryPolicy = kinesisRetryPolicy;
        this.recordRetryPolicy = recordRetryPolicy;
    }

    public String getStreamName() {
        return streamName;
    }

    public BufferingPolicy getBufferingPolicy() {
        return bufferingPolicy;
    }

    public RetryPolicy getKinesisRetryPolicy() {
        return kinesisRetryPolicy;
    }

    public RetryPolicy getRecordRetryPolicy() {
        return recordRetryPolicy;
    }
}
