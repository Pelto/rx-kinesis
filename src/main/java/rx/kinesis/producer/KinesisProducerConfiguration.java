package rx.kinesis.producer;

import rx.kinesis.producer.buffering.BufferingPolicy;
import rx.kinesis.producer.retry.RetryPolicy;

public class KinesisProducerConfiguration {

    private final String streamName;

    private final BufferingPolicy bufferingPolicy;

    private final RetryPolicy retryPolicy;


    public KinesisProducerConfiguration(String streamName, BufferingPolicy bufferingPolicy, RetryPolicy retryPolicy) {
        this.bufferingPolicy = bufferingPolicy;
        this.streamName = streamName;
        this.retryPolicy = retryPolicy;
    }

    public String getStreamName() {
        return streamName;
    }

    public BufferingPolicy getBufferingPolicy() {
        return bufferingPolicy;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
}
