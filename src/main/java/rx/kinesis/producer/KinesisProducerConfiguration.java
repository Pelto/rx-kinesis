package rx.kinesis.producer;

import rx.kinesis.producer.buffering.BufferingPolicy;

public class KinesisProducerConfiguration {

    private final BufferingPolicy bufferingPolicy;

    private final String streamName;

    public KinesisProducerConfiguration(BufferingPolicy bufferingPolicy, String streamName) {
        this.bufferingPolicy = bufferingPolicy;
        this.streamName = streamName;
    }

    public BufferingPolicy getBufferingPolicy() {
        return bufferingPolicy;
    }

    public String getStreamName() {
        return streamName;
    }
}
