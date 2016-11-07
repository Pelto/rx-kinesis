package rx.kinesis.producer.retry;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

public class TimedRetryPolicy extends RetryPolicy {

    private final long retryDelay;

    private final TimeUnit retryDelayTimeUnit;

    private final int maxRetries;

    public TimedRetryPolicy(long retryDelay, TimeUnit retryDelayTimeUnit, int maxRetries) {
        this.retryDelay = retryDelay;
        this.retryDelayTimeUnit = retryDelayTimeUnit;
        this.maxRetries = maxRetries;
    }

    @Override
    public <T> Single<T> attach(Single<T> source) {
        return source.retryWhen(throwables -> throwables
                .flatMap(t -> Observable.timer(retryDelay, retryDelayTimeUnit)
                        .toFlowable(BackpressureStrategy.BUFFER)
                        .cast(Object.class))
                .take(maxRetries));

    }
}
