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
      <version>0.5-SNAPSHOT</version>
    </dependency>
```

To use the time oracle, we should enable it. To do that, include its name `time` in
`ejb_app_services.toml` configuration file in the EJB App's directory with the following content:

```toml
services = ["time"]
```

See more information on built-in services [here][built-in-services].

Below is an example of a service method that uses time oracle to return the consolidated time. Note
that at the time when a new blockchain is launched, the consolidated time is unknown until the
transactions containing time values from at least two thirds of validator nodes are processed.
Before that, the method will return a result with an empty value. Validator nodes send the
transactions after the commit of each block.

```java
private final Node node;

public Optional<ZonedDateTime> getTime() {
  return node.withSnapshot(s -> {
    TimeSchema timeOracle = TimeSchema.newInstance(s);
    EntryIndexProxy<ZonedDateTime> currentTime = timeOracle.getTime();
    if (currentTime.isPresent()) {
      return Optional.of(currentTime.get());
    } else {
      return Optional.empty();
    }
  });
}
```

## License

`exonum-time-oracle` is licensed under the
Apache License (Version 2.0).
See [LICENSE](../../LICENSE) for details.

[exonum-time]: https://exonum.com/doc/version/0.10/advanced/time/
[built-in-services]: https://exonum.com/doc/version/0.10/get-started/java-binding/#built-in-services
