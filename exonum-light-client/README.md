# Exonum Java Light Client
Java client for [Exonum blockchain][exonum].

## Overview
Exonum light client is Java library for working with Exonum blockchain
from the client side and can be easily integrated to an existing 
Java application.  
Also, Exonum light client provides access to [common utils][ejb-common]
toolkit which contains some helpful functions for _hashing_,
_cryptography_, _serialization_ etc. 

## Compatibility
The following table shows versions compatibility:  

| Light Client | Exonum | Exonum Java |
|--------------|--------|-------------|
| 0.1          | 0.10.+ | 0.4         |

## Prerequisites
- Java 8 or above is required for using this client
- Maven 3.5 or above (only if you need to build it locally)

## QuickStart
If you are using Maven, add this to your _pom.xml_ file
```xml
<dependency>
  <groupId>com.exonum.client</groupId>
  <artifactId>exonum-light-client</artifactId>
  <version>0.1-SNAPSHOT</version>
</dependency>
```
If you are using Gradle, add this to your dependencies
```Groovy
compile 'com.exonum.client:exonum-light-client:0.1-SNAPSHOT'
```

## Examples

### Exonum Client initialization
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
### Creating transaction message
The following example shows how to create the transaction message.
In addition please read about [transaction message structure][exonum-tx-message-builder].
```java
    TransactionMessage txMessage = TransactionMessage.builder()
        .serviceId((short) 1)
        .transactionId((short) 2)
        .payload(data)
        .sign(keys, CryptoFunctions.ed25519());
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
 
### Sending transaction
To send the transaction just call a `submitTransaction`.  
Make notice that it works in a blocking way i.e. your thread will be 
blocked till the response is received.  
```java
HashCode txHash = exonumClient.submitTransaction(tx);
```
*Be aware that this method submits the transaction to the pool of
uncommitted transactions and dosn't wait for the transaction 
acceptance to a new block.* 
 

## How to build
To build the client locally, clone the repository, and
run next commands from the project's root 
i.e. _exonum-java-binding_ directory:
```bash
cd exonum-light-client
mvn install
```
In case you need an unreleased version of the [`common`][common-mvn] module
then run next commands from the project's root:
```bash
export RUSTFLAGS=none
mvn install -pl exonum-light-client -am
```
It'll build Exonum Light client and the `exonum-java-binding-common` artifact 
which is required for the client.

## License
Apache 2.0 - see [LICENSE](../LICENSE) for more information.

[exonum]: https://github.com/exonum/exonum
[ejb-common]: https://exonum.com/doc/api/java-binding-common/0.4/
[exonum-tx-message-builder]: https://exonum.com/doc/api/java-binding-common/0.4/com/exonum/binding/common/message/TransactionMessage.Builder.html
[protobuf]: https://developers.google.com/protocol-buffers/docs/proto3
[standard-serializers]: ../exonum-java-binding/common/src/main/java/com/exonum/binding/common/serialization/StandardSerializers.java
[common-mvn]: https://mvnrepository.com/artifact/com.exonum.binding/exonum-java-binding-common
