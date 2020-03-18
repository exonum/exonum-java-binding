# Cryptocurrency Demo

This project demonstrates how to bootstrap your own cryptocurrency
with [Java binding Exonum blockchain](https://github.com/exonum/exonum).

Exonum blockchain keeps balances of users and handles secure
transactions between them.

It implements most basic operations:

- Create a new user
- Transfer funds between users

## Install and Run

### Manually

#### Getting Started

Be sure you installed necessary packages:
- Linux or macOS. Windows support is coming soon.
- [JDK 1.8+](http://jdk.java.net/12/).
- [Maven 3.5+](https://maven.apache.org/download.cgi).
- [git](https://git-scm.com/downloads)
- [Node.js with npm](https://nodejs.org/en/download/)
- [Exonum Java][ejb-installation] application.
- [Exonum Launcher Plugins](../exonum_launcher_java_plugins/README.md).

[ejb-installation]: https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#installation

#### Build and Run

Build the service project:

```sh
$ cd exonum-java-binding/cryptocurrency-demo

$ mvn -P with-installed-app install 
```

Start the node:
```sh
$ ./start-node.sh
```

Start the service:
```sh
$ python3 -m exonum_launcher -i cryptocurrency-demo.yml
```

---

### 🛑

*Frontend has not been updated to DS yet as of 0.10.0.
Skip the subsequent instructions.*

---

Install frontend dependencies:

```sh
$ cd ../../cryptocurrency-demo/frontend/

$ npm install
```

Build sources:

```sh
$ npm run build
```

Run the frontend application:

```sh
$ npm start -- --port=6040 --api-root=http://127.0.0.1:7000 --explorer-root=http://127.0.0.1:3000
```

`--port` is a port for Node.JS app.

`--api-root` is a root URL of public API address of one of nodes.

`--explorer-root` is a root URL of public API address of blockchain explorer.

Ready! Find demo at [http://127.0.0.1:6040](http://127.0.0.1:6040).

## See Also

- [Reference Documentation](https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding)
- [Java Service Development Tutorial](https://exonum.com/doc/version/1.0/get-started/first-java-service/)