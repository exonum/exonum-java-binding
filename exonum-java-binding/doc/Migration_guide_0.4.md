# Migrating Java service to v0.4

## Update the client apps
Update the clients to submit transactions in the new format, introduced in Exonum 0.10.
The most notable change in the format is that the public key of the transaction author 
is always included in the transaction message. This allows the framework to verify 
the message signature with no conversion to an executable transaction.

If the client application is in Java or Java Script, use the latest versions of light client 
libraries:
- Java Script [light client](https://github.com/exonum/exonum-client)
- Java [light client](https://github.com/exonum/exonum-java-binding/tree/master/exonum-light-client)

The light clients for Exonum Java 0.4 submit transactions directly to the core endpoint 
instead of a custom one in the service controller.

[common-0.4]: https://search.maven.org/artifact/com.exonum.binding/exonum-java-binding-common/0.4/jar
[tx-message-jd]: https://exonum.com/doc/api/java-binding-common/0.4/com/exonum/binding/common/message/TransactionMessage.html#builder()

## Update the service
### Use the new message format

EJB 0.4 accepts the transactions via a system endpoint and passes the payload to the `Service`
implementation to convert into an executable transaction, meaning there is [no need](#update-your-controller) 
to create endpoints for accepting transactions anymore.
The [`TransactionConverter`][tx-converter-jd] now accepts
a `RawTransaction` that includes only the transaction identifiers (service id and transaction id)
and its payload — the serialized transaction parameters (equivalent to `Message#getBody` from v0.3).

Also, consider removing the public key of the transaction author from its _payload_ because
it is now included in a standard field of the message, and is accessible in `Transaction#execute`
from the passed context: [`TransactionContext#getAuthorPk`][tx-context-author-jd].

[tx-converter-jd]: https://exonum.com/doc/api/java-binding-core/0.4/com/exonum/binding/service/TransactionConverter.html
[tx-context-author-jd]: https://exonum.com/doc/api/java-binding-core/0.4/com/exonum/binding/transaction/TransactionContext.html#getAuthorPk() 

### Update `Transaction`s
The following methods are **removed** from `Transaction` interface:
  - `isValid`. In v0.4 the framework verifies the cryptographic signature itself, using the
  public key from the message. If your service performs any additional checks in `isValid`, 
  move them to the constructor of your transaction or to `TransactionConverter`, whichever
  works best.
  - `getMessage`. Transactions are no longer required to store the corresponding 
  `TransactionMessage`, which makes it easier to instantiate them in unit or integration tests.
  If your transaction code needs a whole message of this or _any_ other transaction,
  use [`Blockchain.getTxMessages`][blockchain-get-tx-messages-jd].
  - `hash`. You can access the SHA-256 hash of this transaction message in `Transaction#execute`
  from the passed context: [`TransactionContext#getTransactionMessageHash`][tx-context-hash-jd]. 
  As a reminder, it uniquely identifies the message in the system.
  
That leaves `Transaction` interface with two methods: `execute` and `info`.
The argument of `execute` changed too — it now accepts a `TransactionContext` that allows
to access a `Fork` to modify the database state, as previously; and some information about 
the corresponding transaction message: its hash and the public key of the author.

[blockchain-get-tx-messages-jd]: https://exonum.com/doc/api/java-binding-core/0.4/com/exonum/binding/blockchain/Blockchain.html#getTxMessages()
[tx-context-hash-jd]: https://exonum.com/doc/api/java-binding-core/0.4/com/exonum/binding/transaction/TransactionContext.html#getTransactionMessageHash()

### Update your controller
As the v0.4 framework accepts binary transactions itself, remove the endpoints that accepted
transactions from the clients.

### Use Java 11
We have updated all project dependencies to the most recent versions supporting Java 11 and made
our internal system tests run on 11. 8 is still fine too.

## Explore the new features
Here are the highlights of the added features, with links to the documentation sections.
The complete list of changes is available in the [release changelog](https://github.com/exonum/exonum-java-binding/releases/tag/v0.4).

### [Use the Core Schema][core-schema-docs]
v0.4 provides access to some data stored by the framework in the database: transaction messages,
execution results; blocks; node configuration, etc — take a look at [`Blockchain`][blockchain-jd] 
to see if it will simplify your service implementation.

[core-schema-docs]: https://exonum.com/doc/version/0.10/get-started/java-binding#core-schema-api
[blockchain-jd]: https://exonum.com/doc/api/java-binding-core/0.4/com/exonum/binding/blockchain/Blockchain.html

### [Handle blockchain events][core-events-docs]
v0.4 also brings `Service#afterCommit` handler which is invoked by the blockchain after each 
block commit. For example, a service can create one or more transactions if a specific condition
has occurred.

[core-events-docs]: https://exonum.com/doc/version/0.10/get-started/java-binding#blockchain-events

### [Sign and submit transactions][node-submit-docs]
The framework now provides a method to create a transaction message and sign it with the
_service key_. This allows service implementations to create new transactions and securely sign
them, e.g., in their event handlers. But use with care — each node has its own service keypair.

[node-submit-docs]: https://exonum.com/doc/version/0.10/get-started/java-binding#messages

## Use example
See how the cryptocurrency service has changed since the last release to understand what
changes might be needed in your service: [cryptocurrency-demo](../cryptocurrency-demo)

## Questions?

In case of any questions, feel free to [ask][gitter] a question in Gitter or [open][new-issue] an issue
on Github.    

[gitter]: https://gitter.im/exonum/exonum-java-binding
[new-issue]: https://github.com/exonum/exonum-java-binding/issues/new
