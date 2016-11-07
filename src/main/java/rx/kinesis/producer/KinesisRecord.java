package rx.kinesis.producer;

import java.nio.ByteBuffer;

public class KinesisRecord {

    private final String partitionKey;

    private final ByteBuffer buffer;

    public KinesisRecord(ByteBuffer buffer, String partitionKey) {
        this.partitionKey = partitionKey;
        this.buffer = buffer;
    }

    String getPartitionKey() {
        return partitionKey;
    }

    ByteBuffer getBuffer() {
        return buffer;
    }
}
