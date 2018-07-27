# Cryptocurrency demo

This project demonstrates how to bootstrap own cryptocurrency
with [Java binding Exonum blockchain](https://github.com/exonum/exonum).

Exonum blockchain keeps balances of users and handles secure
transactions between them.

It implements most basic operations:

- Create a new user
- Transfer funds between users

## Install and run

### Prerequisites

Be sure you installed the necessary packages:
- Linux or macOS. Windows support is coming soon.
- [JDK 1.8+](http://jdk.java.net/10/).
- [Maven 3.5+](https://maven.apache.org/download.cgi).
- [git](https://git-scm.com/downloads)
- [Node.js with npm](https://nodejs.org/en/download/)
- The [system dependencies](https://exonum.com/doc/get-started/install/) of Exonum. You do _not_ need to manually fetch and compile Exonum.
- [Rust compiler](https://rustup.rs/)

### Build the project

Build the project:

```sh
$ mvn install

$ cd exonum-java-binding-core/rust/ejb-app/

$ cargo install --debug
```

### Run the demo

Run the Exonum service from `exonum-java-binding-core/rust/ejb-app/`:
```
$ ./start_cryptocurrency_node.sh
```

<!-- markdownlint-enable MD013 -->

Run the frontend application:

```sh
$ cd ../../exonum-java-binding-cryptocurrency-demo/frontend/

$ npm start -- --port=6040 --api-root=http://127.0.0.1:6000 --explorer-root=http://127.0.0.1:3000
```

`--port` is a port for Node.JS app.

`--api-root` is a root URL of public API address of one of the nodes.

`--explorer-root` is a root URL of public API address of blockchain explorer.

Ready! Find demo at [http://127.0.0.1:6040](http://127.0.0.1:6040).

### Building the frontend manually
If you don’t want to rebuild the whole project whilst working 
on the frontend, use the following commands from the frontend base directory 
`exonum-java-binding-cryptocurrency-demo/frontend/`.

Install frontend dependencies:

```sh
$ npm install
```

Build sources:

```sh
$ npm run build
```
