# Time oracle

Java Binding Time Oracle provides proxy for [Exonum Time Service][exonum-time].
This service allows to determine time, import it from the external world to the blockchain and keep
its current value in the blockchain.

## Usage

Include `exonum-java-binding-time-service` (TODO: change the module name first) as a dependency in your `pom.xml`:

``` xml
    <dependency>
      <groupId>com.exonum.binding</groupId>
      <artifactId>exonum-java-binding-time-service</artifactId>
      <version>0.4.0</version>
    </dependency>
```

To use the time oracle, it needs to be enabled. To do that, include its name `time` in
`ejb_app_services.toml` configuration file in the EJB App's directory with the following content:

```toml
services = ["time"]
```

See more information on built-in services [here][built-in-services].

Below is an example of a service method that uses time oracle to return the consolidated time. Note that at the time when a new blockchain is launched, the consolidated time is unknown. In that case the result will not contain a value.

```java
public Optional<ZonedDateTime> getTime() {
  return node.withSnapshot(s -> {
    TimeSchema timeOracle = TimeSchema.newInstance(s);
    EntryIndexProxy<ZonedDateTime> currentTime = timeOracle.getTime();
    return toOptional(currentTime);
  });
}
```

## License

`exonum-java-binding-time-service` (TODO: change the module name first) is licensed under the
Apache License (Version 2.0).
See [LICENSE](LICENSE) for details.

[exonum-time]: https://exonum.com/doc/version/0.10/advanced/time/
[built-in-services]: https://exonum.com/doc/version/0.10/get-started/java-binding/#built-in-services
