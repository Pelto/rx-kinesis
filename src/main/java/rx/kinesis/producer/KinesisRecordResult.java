package rx.kinesis.producer;

public class KinesisRecordResult {

    private final String shardId;

    private final String sequenceNumber;

    public KinesisRecordResult(String shardId, String sequenceNumber) {
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
    }

    public String getShardId() {
        return shardId;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }
}
