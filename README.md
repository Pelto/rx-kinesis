# rx-kinesis

This is a small producer library to Kinesis written in Java using RxJava 2.0

## Usage

Start with setting up your producer:

```java
KinesisProducer producer = KinesisProducerBuilder
            .onStream("Name of stream")
            .withRegion("eu-west-1")
            .build();
```

Once your producer has been created you can start sending records:

```java
String message = "Hello";
String partitionKey = "World";

Single<KinesisRecordResult> single = producer.send(new KinesisRecord(message, partitionKey));

single.subscribe(new SingleObserver<KinesisRecordResult>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(KinesisRecordResult value) {
            log.info("Sent record with sequence number {} to shard {}", result.getSequenceNumber(), result.getShardId());
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof KinesisProducerException) {
                KinesisProducerException exception = (KinesisProducerException) e;
                // exception.getErrorCode(), exception.getErrorMessage() will follow the
                // error messages and codes given by Kinesis.
            }
        }
});
```

The `Single<KinesisRecordResult>` returned by the producer can be retried
allowing you retry messages that receive `ProvisionedThroughputExceededException`.

## Metrics

The producer records metrics into two observable that contains information how
large the records are and which shards they went to. All failures are also
emitted.

```java

producer.metrics()
	.sentRecords()
	.window(1, TimeUnit.SECONDS)
	.flatMapSingle(Observable::count)
	.filter(records -> records > 0)
	.forEach(sent -> log.info("Sent {} records to kinesis", sent));

producer.metrics()
	.failedRecords()
	.window(1, TimeUnit.SECONDS)
	.flatMapSingle(Observable::count)
	.filter(failures -> failures > 0)
	.forEach(failures -> log.info("Failed to send {} records", failures));

```
