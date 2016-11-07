package rx.kinesis.producer.retry;

import io.reactivex.Single;

public abstract class RetryPolicy {

    public abstract <T> Single<T> attach(Single<T> source);
}
