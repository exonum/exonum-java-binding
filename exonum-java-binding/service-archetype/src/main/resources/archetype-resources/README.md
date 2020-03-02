## Double pound sign starts a comment in the Velocity template
## engine used under the hood.
## Below we use _directives_ to be able to produce a Markdown document:
## https://velocity.apache.org/engine/1.7/user-guide.html#literals
#set($h1 = '#')
#set($h2 = '##')
#set($h3 = '###')
#set($h4 = '####')

$h1 ${rootArtifactId}

An Exonum service.

$h2 Project Structure

The project consist of the following modules:

- `${rootArtifactId}-messages`. Defines the service transaction arguments and 
  persistent data as Protocol Buffers messages.
  May also be used from client applications.
- `${rootArtifactId}-service`. Contains the service implementation.
  This module produces the _service artifact_.

$h2 How to Build

$h3 Prerequisites

- [Exonum Java][ejb-installation]
- Apache Maven
- JDK 11+

[ejb-installation]: https://exonum.com/doc/version/latest/get-started/java-binding/#installation

$h3 Building

To build the service artifact, invoke:

```shell
mvn package
```

To also run the integration tests, invoke:

```shell
mvn verify
```
