package rx.kinesis.producer.metrics;

public class RecordFailed {

    private final String shardId;

    private final String errorCode;

    public RecordFailed(String shardId, String errorCode) {
        this.shardId = shardId;
        this.errorCode = errorCode;
    }
}
