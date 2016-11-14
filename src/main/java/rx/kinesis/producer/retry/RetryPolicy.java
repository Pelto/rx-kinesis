package rx.kinesis.producer.retry;

import io.reactivex.Single;

public interface RetryPolicy {

    <T> Single<T> attach(Single<T> source);
}
