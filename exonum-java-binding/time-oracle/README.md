# Time Oracle

Java Binding Time Oracle provides proxy for [Exonum Time Service][exonum-time].
This service allows user services to access the calendar time supplied by validator nodes to the
blockchain.

## Usage

Include `exonum-time-oracle` as a dependency in your `pom.xml`:

```xml
    <dependency>
      <groupId>com.exonum.binding</groupId>
      <artifactId>exonum-time-oracle</artifactId>
      <version>0.10.0-SNAPSHOT</version>
      <scope>provided</scope>
    </dependency>
```

[`TimeSchema`][time-schema-javadoc] allows to access the state of time oracle.
Below is an example of a service method that uses time oracle to return the consolidated time. Note
that at the time when a new blockchain is launched, the consolidated time is unknown until the
transactions containing time values from at least two thirds of validator nodes are processed.
Before that, the method will return a result with an empty value. Validator nodes send the
transactions after the commit of each block.

```java
private final Node node;

public Optional<ZonedDateTime> getTime() {
  return node.withBlockchainData(bd -> {
    String timeServiceName = "time";
    TimeSchema timeOracle = TimeSchema.newInstance(bd, timeServiceName);
    ProofEntryIndex<ZonedDateTime> currentTime = timeOracle.getTime();
    return currentTime.toOptional();
  });
}
```

### Service Instantiation

To use the time oracle in your service, you should start an instance of it.
See [_Deploy and Start Services_][deploy-start-java-services] for details.

## License

`exonum-time-oracle` is licensed under the
Apache License (Version 2.0).
See [LICENSE](../../LICENSE) for details.

[exonum-time]: https://exonum.com/doc/version/1.0/advanced/time/
[deploy-start-java-services]: https://exonum.com/doc/version/1.0/get-started/java-binding/#deploy-and-start-the-service
[time-schema-javadoc]: https://exonum.com/doc/api/java-binding/0.10.0/com/exonum/binding/time/TimeSchema.html
