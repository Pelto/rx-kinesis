package rx.kinesis.producer.buffering;

import io.reactivex.Flowable;
import io.reactivex.internal.operators.flowable.FlowableWindowTimed;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class TimedBufferingPolicy extends BufferingPolicy {

    private final long timespan;

    private final TimeUnit timeUnit;

    private final int maxSize;

    public TimedBufferingPolicy(long timespan, TimeUnit timeUnit, int maxSize) {

        if (maxSize < 1) {
            throw new IllegalArgumentException("Max size must be positive");
        }

        if (maxSize > 500) {
            throw new IllegalArgumentException("Amazon Kinesis PutRecords have a maximum limit of 500 records");
        }

        this.timespan = timespan;
        this.timeUnit = timeUnit;
        this.maxSize = maxSize;
    }

    @Override
    public <T> Flowable<Flowable<T>> attach(Flowable<T> source) {
        return new FlowableWindowTimed<>(source, timespan, timespan, timeUnit, Schedulers.computation(), maxSize, maxSize, false);
    }
}
