package rx.kinesis.producer.retry;

import io.reactivex.Single;

public class NoRetryPolicy implements RetryPolicy {

    @Override
    public <T> Single<T> attach(Single<T> source) {
        return source;
    }
}
