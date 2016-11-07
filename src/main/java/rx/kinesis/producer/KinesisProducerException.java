package rx.kinesis.producer;

public class KinesisProducerException extends Exception {

    private final String errorMessage;

    private final String errorCode;

    private final String streamName;

    public KinesisProducerException(String errorMessage, String errorCode, String streamName) {
        super("Unable to send to " + streamName + " with message " + errorMessage + " (" + errorCode + ")");
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.streamName = streamName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getStreamName() {
        return streamName;
    }
}
