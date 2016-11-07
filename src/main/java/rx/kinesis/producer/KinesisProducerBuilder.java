package rx.kinesis.producer;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import rx.kinesis.producer.buffering.BufferingPolicy;
import rx.kinesis.producer.buffering.NoBufferingPolicy;
import rx.kinesis.producer.buffering.TimedBufferingPolicy;
import rx.kinesis.producer.metrics.KinesisMetrics;
import rx.kinesis.producer.metrics.MetricsReporter;

import java.util.concurrent.TimeUnit;

public class KinesisProducerBuilder {

    private final String streamName;

    private final String region;

    private final AWSCredentialsProvider credentialsProvider;

    private final BufferingPolicy bufferingPolicy;

    public KinesisProducerBuilder(String streamName, String region, AWSCredentialsProvider credentialsProvider, BufferingPolicy bufferingPolicy) {
        this.streamName = streamName;
        this.region = region;
        this.credentialsProvider = credentialsProvider;
        this.bufferingPolicy = bufferingPolicy;
    }

    public static KinesisProducerBuilder builder() {
        return new KinesisProducerBuilder(
                null,
                "eu-west-1",
                new DefaultAWSCredentialsProviderChain(),
                new TimedBufferingPolicy(500, TimeUnit.MILLISECONDS, 500));
    }

    public static KinesisProducerBuilder onStream(String streamName) {
        return builder().withStream(streamName);
    }

    public KinesisProducerBuilder withStream(String streamName) {
        return new KinesisProducerBuilder(streamName, region, credentialsProvider, bufferingPolicy);
    }

    public KinesisProducerBuilder withRegion(String region) {
        return new KinesisProducerBuilder(streamName, region, credentialsProvider, bufferingPolicy);
    }

    public KinesisProducerBuilder withBuffering(long timespan, TimeUnit timeUnit, int maxSize) {
        return new KinesisProducerBuilder(streamName, region, credentialsProvider, new TimedBufferingPolicy(timespan, timeUnit, maxSize));
    }

    public KinesisProducerBuilder withoutBuffering() {
        return new KinesisProducerBuilder(streamName, region, credentialsProvider, new NoBufferingPolicy());
    }

    public KinesisProducer build() {
        if (streamName == null) {
            throw new NullPointerException("No stream specified");
        }

        if (credentialsProvider == null) {
            throw new NullPointerException("No credentials specified");
        }

        if (region == null) {
            throw new NullPointerException("No region specified");
        }

        AmazonKinesisAsync kinesis = new AmazonKinesisAsyncClient(credentialsProvider);
        kinesis.setRegion(Region.getRegion(Regions.fromName(region)));

        KinesisMetrics metrics = new KinesisMetrics(new MetricsReporter());
        KinesisProducerConfiguration configuration = new KinesisProducerConfiguration(bufferingPolicy, streamName);

        return new KinesisProducer(configuration, kinesis, metrics);
    }
}
