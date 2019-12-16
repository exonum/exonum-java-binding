# Migrating Java Services to 0.9

The flagship feature of Exonum Java 0.9 release is support for dynamic services&nbsp;—
services that can be added to the blockchain network after it has been started;
and multiple instances of the same service.

This guide provides instructions on updating existing services to be compatible with the new
and updated features.

## Dynamic Services Overview

Dynamic Java services are packaged in an _artifact_: a JAR archive with some metadata. An Exonum
artifact has a runtime-specific _name_. Java services must use the name in the format 
'groupId:artifactId:version', where each component of the name corresponds
to a Maven project coordinate, for example: 'com.exonum.example:timestamping:1.0.2' is a valid
artifact name. Rust service names are usually in the format '<crate-name>:<version>'.
The format of the metadata has not changed since 0.6.0.

Artifacts are _deployed_ to the Exonum blockchain network and then used to instantiate
_service instances_. A single artifact can be used to create many instances. Each instance
has a _name_ (ex `Service#getName`) and a numeric _identifier_ (ex `Service#getId`).
An instance name is specified by the network administrators during the service instantiation;
an ID is assigned by the framework automatically and can be [queried][service-id-lc-operation]
using the light client.

[service-id-lc-operation]: https://github.com/exonum/exonum-java-binding/blob/master/exonum-light-client/README.md#creating-transaction-message

## Update the Service

The [`Service`][service] implementation must be updated to support multiple instances;
new initialization and reconfiguration procedures.

1. Remove the hardcoded service ID and name. Add a constructor parameter 
`ServiceInstanceSpec instanceSpec` which provides the instance name
and ID.
    - If your service extends
    [`com.exonum.binding.core.service.AbstractService`][abstract-service] —
    pass the `instanceSpec` to the superclass constructor and use the provided `AbstractService#getId`,
    `getName`, and `getInstanceSpec` methods
    - If it implements `Service` directly — save the needed information to the fields.

```java
public final class FooService extends AbstractService {
  @Inject
  public FooService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }
}
```

2. If the service requires initialization — migrate to the new 
[initialization mechanism][service-initialize]. 

3. If the service needs reconfiguration support: implement the [`Configurable`][configurable] interface.
Reconfiguration shall be performed through the new [supervisor][supervisor] service which
replaces the configuration service.

[service]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/service/Service.html
[service-initialize]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/service/Service.html#initialize-com.exonum.binding.core.storage.database.Fork-com.exonum.binding.core.service.Configuration-
[configurable]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/service/Configurable.html
<!-- todo: Check the anchor when the docs land -->
[supervisor]: https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#deploy-and-start-service

### Update the Schema

#### Isolate Collections via Namespaces

Use a namespace for service collections that is unique for each service instance
(e.g., a service name, or an ID).

```java
class FooSchema implements Schema {

  private final String namespace;
  private final View view;

  FooSchema(String serviceName, View view) {
    this.namespace = serviceName;
    this.view = view;
  }

  MapIndexProxy<String, String> testMap() {
    String fullName = fullIndexName("test-map");
    return MapIndexProxy.newInstance(fullName, view, string(), string());
  }

  // ...

  private String fullIndexName(String name) {
    return namespace + "." + name;
  }
}
```

#### Update ProofMaps

The default [`ProofMap`][proof-map] implementation has been changed to hash user keys to produce an internal key.
That allows _any_ type to be used as a key. But the new implementation adds an extra hashing operation for every insert
operation and changes how proofs need to be verified.

In some rare cases, it might be needed to **omit** hashing the keys
and use them as _internal_ keys. Such `ProofMap` variant is still supported. See if your
service implementation fits the [*requirements*][proof-map-non-hashing] to keep using it.
If it does not — please migrate your schema to use the new default one.

[proof-map]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/storage/indices/ProofMapIndexProxy.html
[proof-map-non-hashing]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/storage/indices/ProofMapIndexProxy.html#key-hashing

#### Disable Proofs

Proofs are temporarily disabled. Creating proofs is _not_ supported in this release.
They will be re-enabled in one of the following releases.

### Update the Transactions

1. Update [`TransactionConverter#toTransaction`][to-transaction]
to its new signature.

2. Use `int` IDs in transactions.

3. To access the schema in `Transaction#execute`,
use [`TransactionContext.getServiceName`][tx-context-get-name]
or `TransactionContext.getServiceId`:

```java
  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    MySchema schema = new MySchema(context.getFork(), context.getServiceName());
    // ...
  }
```

4. Remove `Transaction#info` implementation: this method is no longer provided.
The core returns complete transaction message in response to a transaction request,
which can be decoded by the client using appropriate Protobuf declarations.

[to-transaction]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/service/TransactionConverter.html
[tx-context-get-name]: https://exonum.com/doc/api/java-binding/0.9.0-rc1/com/exonum/binding/core/transaction/TransactionContext.html#getServiceName--

### Update the Integration Tests

The integration tests that use Exonum Testkit to create a test network with a service-under-test need
to be updated. In the new tests you need to pass the parameters required for artifact deployment and service instantiation.

Use the following Testkit builder methods:
  - `withArtifactsDirectory(artifactsDirectory: Path)` to specify the directory in which the service
  artifacts are located (usually, the `${project.build.directory}` = `target` directory)
  - `withDeployedArtifact(artifactId: ServiceArtifactId, artifactFilename: String)` 
  to specify an artifact to deploy in the test network
  - `withService(artifactId: ServiceArtifactId, serviceName: String, serviceId: int)` 
  to add a service instance of the previously deployed artifact with the given name and ID.
  If needed, supply initialization parameters using an overloaded method.

The parameters: artifactsDirectory, artifactId, artifactFilename may be passed to the test code
from the build configuration properties (where they are already specified) using system properties.

Finally, if the integration tests using the Testkit are located in the same module as the service,
bound them to the `verify` Maven phase. Then the service artifact built during the `package` 
phase is available.

See the [updated section on testing](https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#testing)
for extra examples.

### Use Example

See a complete diff of changes applied to the cryptocurrency-demo service
to support the new Exonum Java version. Your service implementation
is likely to require similar changes:

```
git clone git@github.com:exonum/exonum-java-binding.git
cd exonum-java-binding/exonum-java-binding/cryptocurrency-demo
git diff ejb/v0.8.0 ejb/v0.9.0-rc1 .
```

## Update Node Configuration

1. Remove "services.toml" configuration file. Deploy and instantiate both Java services and the built-in services
using exonum-launcher or an alternative tool — see 
the reference below.
2. Specify the artifacts directory when starting a node using the `artifacts-path` argument.
3. Copy the artifacts to be deployed into that directory on each node in the network.

See the updated node and application configuration [documentation][node-config] on the site
for the detailed instructions.

[node-config]: https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#node-configuration

## Update the Clients

Update the light client to a compatible version to interact with the newer Exonum version.
In Exonum 0.13/Exonum Java 0.9 the transaction message format has been changed. 
`TransactionMessage.Builder` supports this newest format.

Also, configure the clients with the instance name and ID to be able
to submit transactions to a correct instance. 

## Explore the New Features
- Consider if a new ProofMap supporting any keys will simplify the schema of your service.

## See Also

- The 0.9.0 [release page][release-page] for the changelog and pre-built binaries.
- [User Guide](https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/)
- [Javadocs](https://exonum.com/doc/api/java-binding/0.9.0-rc1/index.html)

[release-page]: https://github.com/exonum/exonum-java-binding/releases/tag/ejb/v0.9.0-rc1
