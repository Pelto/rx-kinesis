package rx.kinesis.producer.metrics;

public class RecordSent {

    private final int size;

    private final String shardId;

    public RecordSent(int size, String shardId) {
        this.size = size;
        this.shardId = shardId;
    }

    public int getSize() {
        return size;
    }

    public String getShardId() {
        return shardId;
    }
}
