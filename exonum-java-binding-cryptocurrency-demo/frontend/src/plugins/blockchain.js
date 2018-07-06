import * as Exonum from 'exonum-client'
import axios from 'axios'
import * as Protobuf from 'protobufjs/light'
import bigInt from 'big-integer'
import nacl from 'tweetnacl'

var Root  = Protobuf.Root,
    Type  = Protobuf.Type,
    Field = Protobuf.Field;


let CreateTransactionProtobuf = new Type("CreateTransaction").add(new Field("ownerPublicKey", 1, "bytes"));
CreateTransactionProtobuf.add(new Field("initialBalance", 2, "int64"))

let TransferTransactionProtobuf = new Type("TransferTransaction").add(new Field("seed", 1, "u-int64"));
TransferTransactionProtobuf.add(new Field("senderId", 2, "bytes"))
TransferTransactionProtobuf.add(new Field("recipientId", 3, "bytes"))
TransferTransactionProtobuf.add(new Field("amount", 4, "int64"))

var root = new Root();
root.define("CreateTransactionProtobuf").add(CreateTransactionProtobuf);
root.define("TransferTransactionProtobuf").add(TransferTransactionProtobuf);

const TX_URL = '/api/cryptocurrency-demo-service/submit-transaction'// '/api/services/cryptocurrency/v1/wallets/transaction'
const PER_PAGE = 10
const ATTEMPTS = 10
const ATTEMPT_TIMEOUT = 500
const PROTOCOL_VERSION = 0
const SERVICE_ID = 42
const TX_TRANSFER_ID = 2
const TX_WALLET_ID = 1

const MessageHead = Exonum.newType({
  fields: [
    { name: 'network_id', type: Exonum.Uint8 },
    { name: 'protocol_version', type: Exonum.Uint8 },
    { name: 'message_id', type: Exonum.Uint16 },
    { name: 'service_id', type: Exonum.Uint16 },
    { name: 'payload', type: Exonum.Uint32 }
  ]
})

function getWallet(publicKey) {
  return axios.get('/api/services/configuration/v1/configs/actual').then(response => {
    // actual list of public keys of validators
    const validators = response.data.config.validator_keys.map(validator => {
      return validator.consensus_key
    })
    
    return axios.get(`/api/cryptocurrency-demo-service/wallet/${publicKey}`)
      .then(response => response.data)
      .then(data => {
        const wallet = data

        return {
          //block: data.block_proof.block,
          wallet: wallet,
          transactions: []
        } 
      })
  })
}

function waitForAcceptance(publicKey, hash) {
  let attempt = ATTEMPTS

  return (function makeAttempt() {
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

module.exports = {
  install(Vue) {
    Vue.prototype.$blockchain = {
      generateKeyPair() {
        return Exonum.keyPair()
      },

      generateSeed() {
        const buffer = nacl.randomBytes(7)
        return bigInt.fromArray(Array.from(buffer), 256).toString()
      },
      

      createWallet(keyPair, balance) {

        let buffer = MessageHead.serialize({
          network_id: 0,
          protocol_version: PROTOCOL_VERSION,
          message_id: TX_WALLET_ID,
          service_id: SERVICE_ID,
          payload: 0 // placeholder, real value will be inserted later
        })

        let data = {
          ownerPublicKey: Exonum.hexadecimalToUint8Array(keyPair.publicKey),
          initialBalance: balance
        }

        const body = CreateTransactionProtobuf.encode(data).finish();

        body.forEach(element => {
          buffer.push(element)
        });

        const signature = Exonum.sign(keyPair.secretKey, buffer)

        data.ownerPublicKey = Exonum.uint8ArrayToHexadecimal(data.ownerPublicKey)

        return axios.post(TX_URL, {
         protocol_version: PROTOCOL_VERSION,
         service_id: SERVICE_ID,
         message_id: TX_WALLET_ID,
         signature: signature,
         body: data
        })
      },

      transfer(keyPair, receiver, amountToTransfer, seed) {
        
        let buffer = MessageHead.serialize({
          network_id: 0,
          protocol_version: PROTOCOL_VERSION,
          message_id: TX_TRANSFER_ID,
          service_id: SERVICE_ID,
          payload: 0 // placeholder, real value will be inserted later
        })

        const data = {
          seed: seed,
          senderId: Exonum.hexadecimalToUint8Array(keyPair.publicKey),
          recipientId:  Exonum.hexadecimalToUint8Array(receiver),
          amount: amountToTransfer
        }

        const body = CreateTransactionProtobuf.encode(data).finish();

        body.forEach(element => {
          buffer.push(element)
        });

        const signature = Exonum.sign(keyPair.secretKey, buffer)

        data.senderId = Exonum.uint8ArrayToHexadecimal(data.senderId)
        data.recipientId = Exonum.uint8ArrayToHexadecimal(data.recipientId)

        return axios.post(TX_URL, {
          protocol_version: PROTOCOL_VERSION,
          service_id: SERVICE_ID,
          message_id: TX_TRANSFER_ID,
          signature: signature,
          body: data
        }).then(response => waitForAcceptance(keyPair.publicKey, response.data))
      },

      getWallet: getWallet,

      getBlocks(latest) {
        const suffix = !isNaN(latest) ? '&latest=' + latest : ''
        return axios.get(`/api/explorer/v1/blocks?count=${PER_PAGE}${suffix}`).then(response => response.data)
      },

      getBlock(height) {
        return axios.get(`/api/explorer/v1/blocks/${height}`).then(response => response.data)
      },

      getTransaction(hash) {
        return axios.get(`/api/explorer/v1/transactions/${hash}`).then(response => response.data)
      }
    }
  }
}
