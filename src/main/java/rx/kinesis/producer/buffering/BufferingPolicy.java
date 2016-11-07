package rx.kinesis.producer.buffering;

import io.reactivex.Flowable;

public abstract class BufferingPolicy {

    public abstract <T> Flowable<Flowable<T>> attach(Flowable<T> source);
}
