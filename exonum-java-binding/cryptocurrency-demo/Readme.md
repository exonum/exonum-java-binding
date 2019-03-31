# Cryptocurrency demo

This project demonstrates how to bootstrap own cryptocurrency
with [Java binding Exonum blockchain](https://github.com/exonum/exonum).

Exonum blockchain keeps balances of users and handles secure
transactions between them.

It implements most basic operations:

- Create a new user
- Transfer funds between users
- Transfer funds between users with multisig wallet in two ops: multisignTransfer and signMultisignTransfer

### API Description
```javascript
const TRANSACTION_URL = '/api/explorer/v1/transactions'
```
#### Create wallet
Create wallet with base balance, pending balance equals 0, if wallet is not multisig, you can place in signer any address
and send ordinary transfer. If transfer is multisig, then (signer != walletTo || signer != walletFrom). Also, 
when initiating wallet, signer should not be empty.
```javascript
function CreateTransaction(publicKey) {
  return Exonum.newTransaction({
    author: publicKey,
    service_id: SERVICE_ID,
    message_id: 1,
    schema: proto.CreateWalletTx
  })
}

createWallet (keyPair, balance, signer) {
    // Describe transaction
    const transaction = new CreateTransaction(keyPair.publicKey)

    // Transaction data
    const data = {
        initialBalance: balance,
        signer: signer
    }

    // Send transaction into blockchain
    return transaction.send(TRANSACTION_URL, data, keyPair.secretKey)
}

```
```proto
message Wallet {
    int64 balance = 1;
    int64 pendingBalance = 2;
    bytes signer = 3;
}
```
#### Multisig transfer transaction
Send multisig transfer. Will decrease pending balance on wallet
```javascript
function MultisignTransferTransaction(publicKey) {
  return Exonum.newTransaction({
    author: publicKey,
    service_id: SERVICE_ID,
    message_id: 3,
    schema: proto.TransferTx
  })
}
multisigTransfer (keyPair, receiver, amountToTransfer, seed) {
  // Describe transaction
  const transaction = new MultisignTransferTransaction(keyPair.publicKey)

  // Transaction data
  const data = {
    seed: seed,
    toWallet: Exonum.hexadecimalToUint8Array(receiver),
    sum: amountToTransfer
  }

  // Send transaction into blockchain
  return transaction.send(TRANSACTION_URL, data, keyPair.secretKey)
}
```
```proto
message TransferTx {
  int64 seed = 1;
  bytes toWallet = 2;
  int64 sum = 3;
}
```

#### Sign multisig transfer transaction
Will sign multisig tranfer and calculate balances based on multisig transfer
```javascript
function SignMultisignTransferTransaction(publicKey) {
  return Exonum.newTransaction({
    author: publicKey,
    service_id: SERVICE_ID,
    message_id: 4,
    schema: proto.SignMultisignTransferTx
  })
}
transfer (keyPair, txHashToSign, seed) {
  // Describe transaction
  const transaction = new SignMultisignTransferTransaction(keyPair.publicKey)

  // Transaction data
  const data = {
    seed: seed,
    txHash: txHashToSign
  }

  // Send transaction into blockchain
  return transaction.send(TRANSACTION_URL, data, keyPair.secretKey)
},
```
```proto
message SignMultisignTransferTx {
  int64 seed = 1;
  bytes txHash = 2; //transaction to sign
}
```
#### Get pending transactions
Get pending transactions, if transaction signed, then it will be deleted from list
```
GET    /api/cryptocurrency-demo-service/transactions/pending
```

## Install and run

### Manually

#### Getting started

Be sure you installed necessary packages:
- Linux or macOS. Windows support is coming soon.
- [JDK 1.8+](http://jdk.java.net/10/).
- [Maven 3.5+](https://maven.apache.org/download.cgi).
- [git](https://git-scm.com/downloads)
- [Node.js with npm](https://nodejs.org/en/download/)
- The [system dependencies](https://exonum.com/doc/version/0.10/get-started/install/) of Exonum. You do _not_ need to manually fetch and compile Exonum.
- [Rust 1.32.0](https://rustup.rs/). To install a specific Rust version, you can use the following command:
  ```bash
  rustup install 1.32.0
  ```

#### Install and run

Build the project:

```sh
$ source exonum-java-binding/tests_profile

$ mvn install

$ cd exonum-java-binding/core/rust/ejb-app/

$ cargo build

$ ./start_cryptocurrency_node.sh
```

<!-- markdownlint-enable MD013 -->

Install frontend dependencies:

```sh
$ cd ../../cryptocurrency-demo/frontend/

$ npm install
```

Build sources:

```sh
$ npm run build
```

Run the application:

```sh
$ npm start -- --port=6040 --api-root=http://127.0.0.1:6000 --explorer-root=http://127.0.0.1:3000
```

`--port` is a port for Node.JS app.

`--api-root` is a root URL of public API address of one of nodes.

`--explorer-root` is a root URL of public API address of blockchain explorer.

Ready! Find demo at [http://127.0.0.1:6040](http://127.0.0.1:6040).

## See Also
- [Reference Documentation](https://exonum.com/doc/version/0.10/get-started/java-binding).
- [Instructions][app-tutorial] explaining how to configure and run any Java service.

[app-tutorial]: https://github.com/exonum/exonum-java-binding/blob/master/exonum-java-binding/core/rust/ejb-app/TUTORIAL.md
