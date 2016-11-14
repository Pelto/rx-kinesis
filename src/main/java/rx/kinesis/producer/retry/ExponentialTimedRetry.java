package rx.kinesis.producer.retry;

import io.reactivex.Flowable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

public class ExponentialTimedRetry implements RetryPolicy {

    private final int maxRetries;

    private final long initialTimeDelay;

    private final TimeUnit timeUnit;

    public ExponentialTimedRetry(int maxRetries, long initialTimeDelay, TimeUnit timeUnit) {

        if (maxRetries < 1) {
            throw new IllegalArgumentException("No retries specified");
        }

        if (initialTimeDelay < 0) {
            throw new IllegalArgumentException("initialTimeDelay must be positive");
        }

        if (timeUnit == null) {
            throw new NullPointerException("timeUnit can't be null");
        }

        this.maxRetries = maxRetries;
        this.initialTimeDelay = initialTimeDelay;
        this.timeUnit = timeUnit;
    }

    @Override
    public <T> Single<T> attach(Single<T> source) {
        return source
                .retryWhen(errors -> errors
                        .zipWith(Flowable.range(1, maxRetries), (n, i) -> i)
                        .flatMap(retryCount -> Flowable.timer(
                                (long) Math.pow(initialTimeDelay, retryCount),
                                timeUnit)));
    }
}
