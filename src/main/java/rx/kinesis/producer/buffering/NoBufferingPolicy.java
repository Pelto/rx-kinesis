package rx.kinesis.producer.buffering;

import io.reactivex.Flowable;

public class NoBufferingPolicy extends BufferingPolicy {

    @Override
    public <T> Flowable<Flowable<T>> attach(Flowable<T> source) {
        return source.map(Flowable::just);
    }
}
