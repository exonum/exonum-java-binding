# Exonum Java Light Client
Java client for [Exonum blockchain][exonum].

## Overview
Exonum light client is Java library for working with Exonum blockchain
from the client side and can be easily integrated to existing 
Java application.  
Also, Exonum light client provides an access to [common utils][ejb-common]
toolkit which contains some helpful functions for _hashing_,
_serialization_ ect. 

## Prerequisites
- Java 8 or above is required for using this client
- Maven 3.5 or above (only if you need to build it locally)

## How to build
To build the client locally, clone the repository, and
run next command from the project's root 
i.e. _exonum-java-binding_ directory:
```bash
mvn install -pl exonum-light-client -am
```
It'll build Exonum Light client within the `exonum-java-binding-common` artifact 
which is required for the client.

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
 

## License
Apache 2.0 - see [LICENSE](../LICENSE) for more information.

[exonum]: https://github.com/exonum/exonum
[ejb-common]: https://exonum.com/doc/api/java-binding-common/0.4/
