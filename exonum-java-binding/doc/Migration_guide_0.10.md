# Migrating Java Services to 0.10

This guide provides instructions on updating existing services to be compatible with the new
and updated features.

## Update the Service

### Update the Schema

1. Use `Prefixed` Access instead of View as database access object. If the schema
needs accessing data of some other services, the core or dispatcher — use `BlockchainData`.

2. Remove explicit namespacing, if any, from your schema. The executing service data,
accessible via `BlockchainData#getExecutingServiceData` is isolated in a unique to
each service instance namespace.

3. Instantiate MerkleDB indexes using Access methods instead of index factory methods.
For example, use `Prefixed#getProofMap` instead of `ProofMapIndexProxy.newInstance`.

4. Remove `Schema#getStateHashes` — the aggregation is performed automatically by Exonum MerkleDB.

5. Use `BlockchainData#getBlockchain` to access `Blockchain` instead of `Blockchain#newInstance`.

#### Example of a Schema

```java
final class ExampleSchema implements Schema {

  private final Prefixed access;
  
  // (1) Or BlockchainData instead of Prefixed
  ExampleSchema(Prefixed access) {
    this.access = access;
  }

  /** Creates a test proof map. */
  public ProofMapIndexProxy<String, String> testMap() {
    // (2) Use a simple name — the indexes are isolated between instances 
    // automatically
    IndexAddress address = IndexAddress.valueOf("test-map");
    // (3) Use Access factory methods to create indexes      
    return access.getProofMap(address, string(), string());
  }
}
```

### Update the Transactions

1. Replace Transactions-as classes implementing `Transaction` interface 
with methods of your Service annotated with `@Transaction`. Transaction methods
may accept the arguments either as byte arrays (as TransactionConverter used to)
or as protobuf messages.

2. Remove the `TransactionConverter` — it is no longer needed.

3. If the transactions throw TransactionExecutionException — replace it with 
ExecutionException. Consider using TransactionPreconditions.

```java
public final class ExampleService extends AbstractService {

  public static final int PUT_TX_ID = 0;

  @Inject
  public ExampleService(ServiceInstanceSpec instanceSpec) {
    super(instanceSpec);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No handlers
  }

  /**
   * Puts an entry (a key-value pair) into the test proof map.
   */
  @Transaction(PUT_TX_ID)
  public void putEntry(Transactions.PutTransactionArgs arguments,
      TransactionContext context) {
    ExampleSchema schema = new ExampleSchema(context.getServiceData());
    String key = arguments.getKey();
    String value = arguments.getValue();
    schema.testMap()
        .put(key, value);
  }
}
```

### Update the Service Methods

1. Use `Node#withServiceData` or `Node#withBlockchainData` to access the blockchain data
instead of `Node#withSnapshot`.
    
2. Rename `Service#beforeCommit` into `Service#afterTransactions`. 
Prefer ExecutionException for service-defined errors to other runtime exceptions.

### Update the Project Build Definition

1. Update the Plugin-Id manifest entry to «javaRuntimeId:groupId/artifactId:version», where
  1. javaRuntimeId = 1
  2. groupId, artifactId, version are the standard Maven coordinates.
  3. The «groupId/artifactId» part is considered to be the _Exonum service artifact name_.  
  For example, "1:com.acme/example-service:1.2.1".
2. Then, update the exonum-launcher configuration files with the new service name: «groupId/artifactId»,
and add the version to them.

### Update the Integration Tests

1. Use `TestKit#getServiceData` or `TestKit#getBlockchainData` to access the service data. 
Snapshot remains for advanced usages.

### Use Example

As a ~~picture~~ diff is worth a thousand words, see a complete diff of changes
applied to the cryptocurrency-demo service to support the new Exonum Java version. 
Your service implementation is likely to require similar changes:

```
git clone git@github.com:exonum/exonum-java-binding.git
cd exonum-java-binding/exonum-java-binding/cryptocurrency-demo
git diff ejb/v0.9.0-rc1 ejb/v0.10.0 .
```

## Update Node Configuration

When launching a new network, specify one of the desired supervisor operation modes: 
simple or the new decentralized.

See the updated node and application configuration [documentation][node-config] on the site
for the detailed instructions.

[node-config]: https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#node-configuration

## Update the Clients

Update the light client to a compatible version to interact with the newer
Exonum version.

## Explore the New Features

See the [release page][release-page] for an overview of the new features.

## See Also

- The 0.10.0 [release page][release-page] for the changelog and pre-built binaries.
- [User Guide](https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/)
- [Javadocs](https://exonum.com/doc/api/java-binding/0.9.0-rc1/index.html)

[release-page]: https://github.com/exonum/exonum-java-binding/releases/tag/ejb/v0.10.0
