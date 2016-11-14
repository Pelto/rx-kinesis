package rx.kinesis.producer.retry;

import io.reactivex.Single;

public class SimpleRetryPolicy implements RetryPolicy {

    private final int retries;

    public SimpleRetryPolicy(int retries) {
        this.retries = retries;
    }

    @Override
    public <T> Single<T> attach(Single<T> source) {
        return source.retry(retries);
    }
}
