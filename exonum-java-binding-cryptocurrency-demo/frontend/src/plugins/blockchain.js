import axios from 'axios'
import bigInt from 'big-integer'
import * as Exonum from 'exonum-client'
import * as Protobuf from 'protobufjs/light'

const Root = Protobuf.Root
const Type = Protobuf.Type
const Field = Protobuf.Field

//  schema for creating thansaction with protobuf
let CreateTransactionProtobuf = new Type("CreateTransaction").add(new Field("ownerPublicKey", 1, "bytes"))
CreateTransactionProtobuf.add(new Field("initialBalance", 2, "int64"))

// creating schema for transfer thansaction with protobuf
let TransferTransactionProtobuf = new Type("TransferTransaction").add(new Field("seed", 1, "int64"))
TransferTransactionProtobuf.add(new Field("senderId", 2, "bytes"))
TransferTransactionProtobuf.add(new Field("recipientId", 3, "bytes"))
TransferTransactionProtobuf.add(new Field("amount", 4, "int64"))

// add types to protobuf root
var root = new Root();
root.define("CreateTransactionProtobuf").add(CreateTransactionProtobuf);
root.define("TransferTransactionProtobuf").add(TransferTransactionProtobuf);

const TX_URL = '/api/cryptocurrency-demo-service/submit-transaction'
const ATTEMPTS = 10
const ATTEMPT_TIMEOUT = 500
const PROTOCOL_VERSION = 0
const SERVICE_ID = 42
const TX_TRANSFER_ID = 2
const TX_WALLET_ID = 1
const SIGNATURE_LENGTH = 64
const PAYLOD_SIZE_OFFSET = 6
const PER_PAGE = 10
const MAX_VALUE = 2147483647

const MessageHead = Exonum.newType({
  fields: [
    { name: 'network_id', type: Exonum.Uint8 },
    { name: 'protocol_version', type: Exonum.Uint8 },
    { name: 'message_id', type: Exonum.Uint16 },
    { name: 'service_id', type: Exonum.Uint16 },
    { name: 'payload', type: Exonum.Uint32 }
  ]
})

function waitForAcceptance(publicKey, hash) {
  let attempt = ATTEMPTS

  return (function makeAttempt() {
    // find transaction in a explorer
    return axios.get(`/api/explorer/v1/transactions/${hash}`).then(response =>  {
      if (response.data.type !== 'committed') {
        if (--attempt > 0) {
          return new Promise(resolve => {
            setTimeout(resolve, ATTEMPT_TIMEOUT)
          }).then(makeAttempt)
        } else {
          throw new Error('Transaction has not been found')
        }
      } else {
        return response.data
      }
    })
  })()
}

function createHeader(messageId) {
  return MessageHead.serialize({
    network_id: 0,
    protocol_version: PROTOCOL_VERSION,
    message_id: messageId,
    service_id: SERVICE_ID,
    payload: 0 // placeholder, real value will be inserted later
  })
}

module.exports = {
  install(Vue) {
    Vue.prototype.$blockchain = {
      generateKeyPair() {
        return Exonum.keyPair()
      },

      generateSeed() {
        return bigInt.randBetween(0, MAX_VALUE).valueOf()
      },  

      createWallet(keyPair, balance) {

        // serialize and append message header
        let buffer = createHeader(TX_WALLET_ID)

        let data = {
          ownerPublicKey: Exonum.hexadecimalToUint8Array(keyPair.publicKey),
          initialBalance: balance
        }

        // serialize and append message body
        const body = CreateTransactionProtobuf.encode(data).finish();

        body.forEach(element => {
          buffer.push(element)
        });

        // calculate payload and insert it into buffer
        Exonum.Uint32.serialize(buffer.length + SIGNATURE_LENGTH, buffer, PAYLOD_SIZE_OFFSET)

        // append signature
        const signature = Exonum.sign(keyPair.secretKey, buffer)

        data.ownerPublicKey = keyPair.publicKey

        return axios.post(TX_URL, {
         protocol_version: PROTOCOL_VERSION,
         service_id: SERVICE_ID,
         message_id: TX_WALLET_ID,
         signature: signature,
         body: data
        })
      },

      transfer(keyPair, receiver, amountToTransfer, seed) {
        
        // serialize and append message header
        let buffer = createHeader(TX_TRANSFER_ID)

        const data = {
          seed: seed,
          senderId: Exonum.hexadecimalToUint8Array(keyPair.publicKey),
          recipientId:  Exonum.hexadecimalToUint8Array(receiver),
          amount: amountToTransfer
        }

        const body = TransferTransactionProtobuf.encode(data).finish();

        body.forEach(element => {
          buffer.push(element)
        });

        // calculate payload and insert it into buffer
        Exonum.Uint32.serialize(buffer.length + SIGNATURE_LENGTH, buffer, PAYLOD_SIZE_OFFSET)

        // append signature
        const signature = Exonum.sign(keyPair.secretKey, buffer)

        data.senderId = keyPair.publicKey
        data.recipientId = receiver
        return axios.post(TX_URL, {
          protocol_version: PROTOCOL_VERSION,
          service_id: SERVICE_ID,
          message_id: TX_TRANSFER_ID,
          signature: signature,
          body: data
        }).then(response => waitForAcceptance(keyPair.publicKey, response.data))
      },

      getWallet (publicKey) {
        return axios.get(`/api/cryptocurrency-demo-service/wallet/${publicKey}`).then(response => { wallet: response.data })
      },

      getBlocks(latest) {
        const suffix = !isNaN(latest) ? '&latest=' + latest : ''
        return axios.get(`/api/explorer/v1/blocks?count=${PER_PAGE}${suffix}`).then(response => response.data)
      },

      getBlock(height) {
        return axios.get(`/api/explorer/v1/blocks/${height}`).then(response => response.data)
      },

      getTransaction(hash) {
        return axios.get(`/api/explorer/v1/transactions/${hash}`).then(response => {
          return response.data })
      }
    }
  }
}
