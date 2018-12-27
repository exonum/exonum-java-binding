# Migrating Java service to v0.4

## Update the client apps
Update the clients to submit transactions in the new format, introduced in Exonum 0.10.
The most notable change in the format is that the public key of the transaction author 
is always included in the transaction message. This allows the framework to verify 
the message signature with no conversion to an executable transaction.

If the client application is in Java, use `com.exonum.binding.common.message.TransactionMessage`
from "[com.exonum.binding:exonum-java-binding-common:0.4][common-0.4]";
if it is in Java Script, use the latest version of the JS 
[light client](https://github.com/exonum/exonum-client).

The light client shall submit transactions directly to the core endpoint instead of a custom one
in the service controller:
`<public api IP address and port>/api/explorer/v1/transactions`
 <!-- TODO: Link the docs when/if they are available -->

[common-0.4]: https://search.maven.org/artifact/com.exonum.binding/exonum-java-binding-common/0.4/jar

## Update the service
### Use the new message format

EJB 0.4 accepts the transactions via a system endpoint and passes the payload to the `Service`
implementation to convert into an executable transaction. The `TransactionConverter` now accepts
a `RawTransaction` that includes only the transaction identifiers (service id and transaction id)
and its payload — the serialized transaction parameters (equivalent to `Message#getBody` from v0.3).

Also, consider removing the public key of the transaction author from its _payload_ because
it is now included in a standard field of the message, and is accessible in `Transaction#execute`
from the passed context: `TransactionContext#getFooKey`.

### Update `Transaction`s
The following methods are **removed** from `Transaction` interface:
  - `isValid`. In v0.4 the framework verifies the cryptographic signature itself, using the
  public key from the message. If your service performs any additional checks in `isValid`, 
  move them to the constructor of your transaction or to `TransactionConverter`, whichever
  works best.
  - `getMessage`. Transactions are no longer required to store the corresponding 
  `TransactionMessage`, which makes it easier to instantiate them in unit or integration tests.
  If your transaction code needs a whole message of this or _any_ other transaction,
  use `com.exonum.binding.blockchain.Blockchain.getTxMessages`.
  - `hash`. You can access the SHA-256 hash of this transaction message in `Transaction#execute`
  from the passed context: `TransactionContext#getTransactionMessageHash`. As a reminder,
  it uniquely identifies the message in the system.
  
That leaves `Transaction` interface with two methods: `execute` and `info`.
The argument of `execute` changed too — it now accepts a `TransactionContext` that allows
to access a `Fork` to modify the database state, as previously; and some information about 
the corresponding transaction message: its hash and the public key of the author.

### Update your controller
As the v0.4 framework accepts binary transactions itself, remove the endpoint that accepted
transactions from the clients.

### Use Java 11
We have updated all project dependencies to the most recent versions supporting Java 11 and made
our internal system tests run on 11. 8 is still fine too.

## Explore the new features

### Use the Core Schema
v0.4 provides access to some data stored by the framework in the database: transaction messages,
execution results; blocks; node configuration, etc — take a look at `Blockchain` to see if will
simplify your service implementation. 

### Handle blockchain events
v0.4 also brings `Service#afterCommit` handler which is invoked by the blockchain after each 
block commit. For example, a service can create one or more transactions if a specific condition
has occurred. <!-- TODO: Link the docs -->

### Sign and submit transactions
The framework now provides a method to create a transaction message and sign it with the
_service key_. This allows service implementations to create new transactions and securely sign
them, e.g., in their event handlers. But use with care — each node has its own service keypair.
<!-- TODO: Some links would be welcome -->

## Use example
See how the cryptocurrency service has changed since the last release to understand what
changes might be needed in your service: <!-- TODO: diff link -->

## Questions?

In case of any questions, feel free to [ask][gitter] a question in Gitter or [open][new-issue] an issue
on Github.    

[gitter]: https://gitter.im/exonum/exonum-java-binding
[new-issue]: https://github.com/exonum/exonum-java-binding/issues/new

  
