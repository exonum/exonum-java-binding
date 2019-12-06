# Exonum Java Light Client

![Maven Central](https://img.shields.io/maven-central/v/com.exonum.client/exonum-light-client)

Java client for [Exonum blockchain][exonum].

## Overview
Exonum light client is Java library for working with Exonum blockchain
from the client side and can be easily integrated to an existing 
Java application.  
Also, Exonum light client provides access to [common utils][ejb-documentation]
(`com.exonum.binding.common.*` packages) toolkit which contains some helpful
functions for _hashing_, _cryptography_, _serialization_, etc.

## Capabilities
By using the client you are able to perform the following operations:
- Submit transactions to the node
- Receive transaction information 
- Receive blockchain blocks information 
- Receive node system information 
- Receive node status information  
_*Please refer to [the documentation][exonum-client] for details._  
Also, see [examples](#examples). 

## Compatibility
The following table shows versions compatibility:  

| Light Client | Exonum | Exonum Java |
|--------------|--------|-------------|
| 0.4.0        | 0.12.* | 0.8.0       |
| 0.3.0        | 0.11.* | 0.6.0-0.7.0 |
| 0.2.0        | 0.11.* | 0.6.0       |
| 0.1.0        | 0.10.* | 0.4         |

## System Dependencies
- Java 8 or above is required for using this client
- Maven 3.5 or above (only if you need to build it locally)

## QuickStart
If you are using Maven, add this to your _pom.xml_ file
```xml
<dependency>
  <groupId>com.exonum.client</groupId>
  <artifactId>exonum-light-client</artifactId>
  <version>0.4.0</version>
</dependency>
```
If you are using Gradle, add this to your dependencies
```Groovy
compile 'com.exonum.client:exonum-light-client:0.4.0'
```

## Examples
This section contains most frequently used operations with the blockchain.
Please navigate to [Exonum client][exonum-client] API documentation 
to see all supported operations.

### Exonum Client Initialization
The following example shows how to create the instance of exonum client
which will work with Exonum node at `http://localhost:8080` address: 
```java
    ExonumClient exonumClient = ExonumClient.newBuilder()
        .setExonumHost("http://localhost:8080")
        .build();
```

The next example shows how to use a custom configuration of the _http-client_:
```java
    OkHttpClient httpClient = new OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(3))
        .build();

    ExonumClient exonumClient = ExonumClient.newBuilder()
        .setExonumHost("http://localhost:8080")
        .setHttpClient(httpClient)
        .build();

```

### Creating Transaction Message
The following example shows how to create the transaction message.
In addition please read about [transaction message structure][exonum-tx-message-builder].
```java
    TransactionMessage txMessage = TransactionMessage.builder()
        .serviceId(1)
        .transactionId(2)
        .payload(data)
        .sign(keys);
```
* `data` is a bytes array which contains transactional information/parameters
in a service-defined format.
It can be any object which should be serialized to bytes in advance.
We recommend to use [Google Protobuf][protobuf] for serialization,
but it is always an option of your choice.
Also, _common_ package provides [`StandardSerializers`][standard-serializers]
utility class which can be helpful for serialization.  
* `keys` is a key pair of private and public keys which is used for message signature.  
* `ed25519` is the cryptographic function for signing.
 
### Sending Transaction
To send the transaction just call a `submitTransaction`.  
Make notice that it works in a blocking way i.e. your thread will be 
blocked till the response is received.  
```java
HashCode txHash = exonumClient.submitTransaction(tx);
```
*Be aware that this method submits the transaction to the pool of
uncommitted transactions and doesn't wait for the transaction 
acceptance to a new block.*  
<!-- TODO: Replace with a proper example --> 
Also, you can take a look at the [integration test][send-tx-it]
for the full example of how to create a transaction message and
send it to Exonum node.

### Transaction Info
The following method provides a possibility to get information 
on a transaction by its hash - status of the transaction, 
details of the message containing the transaction and, 
if available, transaction location and result:
```java
Optional<TransactionResponse> response = exonumClient.getTransaction(txHash);
```
* `txHash` is a hash of the transaction to search.
  
## How to Build
To build the client locally, clone the repository, and
run next commands from the project's root 
i.e. _exonum-java-binding_ directory:
<!-- TODO: Get rid of the RUSTFLAGS here --> 
```bash
export RUSTFLAGS=none
mvn install -pl exonum-light-client -am
```
It'll build Exonum Light client and the `exonum-java-binding-common` artifact 
which is required for the client.

## License
Apache 2.0 - see [LICENSE](../LICENSE) for more information.

[exonum]: https://github.com/exonum/exonum
[ejb-documentation]: https://exonum.com/doc/api/java-binding/0.8.0/index.html
[exonum-tx-message-builder]: https://exonum.com/doc/api/java-binding/0.8.0/com/exonum/binding/common/message/TransactionMessage.Builder.html
[protobuf]: https://developers.google.com/protocol-buffers/docs/proto3
[standard-serializers]: https://exonum.com/doc/api/java-binding/0.8.0/com/exonum/binding/common/serialization/StandardSerializers.html
[send-tx-it]: ./src/test/java/com/exonum/client/ExonumHttpClientIntegrationTest.java
[exonum-client]: https://exonum.com/doc/api/java-light-client/0.4.0/com/exonum/client/ExonumClient.html
