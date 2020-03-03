
# car-registry

An Exonum service.

## Project Structure

The project consist of the following modules:

- `car-registry-messages`. Defines the service transaction arguments and 
  persistent data as Protocol Buffers messages.
  May also be used from client applications.
- `car-registry-service`. Contains the service implementation.
  This module produces the _service artifact_.

## How to Build

### Prerequisites

- [Exonum Java][ejb-installation]
- Apache Maven
- JDK 11+

[ejb-installation]: https://exonum.com/doc/version/latest/get-started/java-binding/#installation

### Building

To build the service artifact, invoke:

```shell
mvn package
```

To also run the integration tests, invoke:

```shell
mvn verify
```
