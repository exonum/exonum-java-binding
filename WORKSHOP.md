# Exonum Java Binding Workshop Guide

## How to Build
### System Dependencies
You need to install the following dependencies:
  * Linux or macOS. Windows support is coming soon. <!-- TODO: Link Java roadmap when it is published -->
  * [JDK 1.8+](http://jdk.java.net/10/).
  * [Maven 3.5+](https://maven.apache.org/download.cgi).
  * [Rust 1.27.2](https://www.rust-lang.org/).
    To install a specific Rust version, use `rustup install 1.27.2` command.
  * The [system dependencies](https://exonum.com/doc/get-started/install/) of Exonum. 
  You do _not_ need to manually fetch and compile Exonum.

### Mac OS
Java:
```$sh
$ brew cask install java
```

```$sh
$ brew install maven
```

Rust installation:
```$sh
$ curl https://sh.rustup.rs -sSf | sh
```
```$sh
$ rustup install 1.27.2
```
```$sh
$ rustup default 1.27.2
```
```$sh
$ rustc --version
```
Node.js installation:
```$sh
$ brew install node
```

You need to install system dependencies of Exonum:
```$sh
$ brew install libsodium rocksdb pkg-config
```
For linux users:
```$sh
$ apt-get install build-essential libsodium-dev libsnappy-dev \
      librocksdb-dev pkg-config
```

### Building
Set required environment variables, once in a shell you use to build the project:
```$sh
$ source tests_profile
```
Then run:
```$sh
$ mvn install
```

#### Building Java Binding App
You have to configure Exonum blockchain
```$sh
$ cd exonum-java-binding-core/rust/ejb-app/scripts
```
Also, we need to have a configuration for two blockchain nodes
```$sh
$ ./configure.nodes.sh 2
```
Run first node:
```$sh
$ ./start_crypto_node.sh 1
```
Run second node:
```$sh
$ ./start_crypto_node.sh 2
```

#### Building Cryptocurrency Frontend App
```$sh
$ cd ../../../exonum-java-binding-cryptocurrency-demo/frontend/
```
Install frontend dependencies:
```$sh
$ npm install
```
Build sources:
```$sh
$ npm run build
```
Run the application:

```sh
$ npm start -- --port=6040 --api-root=http://127.0.0.1:6001 --explorer-root=http://127.0.0.1:3001
```

`--port` is a port for Node.JS app.

`--api-root` is a root URL of public API address of one of nodes.

`--explorer-root` is a root URL of public API address of blockchain explorer.

Ready! Find demo at [http://127.0.0.1:6040](http://127.0.0.1:6040).

