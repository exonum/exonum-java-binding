/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as Exonum from 'exonum-client'
import axios from 'axios'
import bigInt from 'big-integer'
import * as proto from '../proto/proto.js'

const TRANSACTION_URL = '/api/explorer/v1/transactions'
const SERVICE_ID = 42
const TX_TRANSFER_ID = 2
const TX_WALLET_ID = 1
const PER_PAGE = 10
const MAX_VALUE = 2147483647

function getWallet (publicKey) {
  return axios.get(`/api/services/cryptocurrency-demo/wallet/${publicKey}`)
    .then(response => response.data)
    .then(data => {
      return {
        wallet: data,
        transactions: []
      }
    })
}

function CreateTransaction(publicKey) {
  return Exonum.newTransaction({
    author: publicKey,
    service_id: SERVICE_ID,
    message_id: TX_WALLET_ID,
    schema: proto.CreateWalletTx
  })
}

function TransferTransaction(publicKey) {
  return Exonum.newTransaction({
    author: publicKey,
    service_id: SERVICE_ID,
    message_id: TX_TRANSFER_ID,
    schema: proto.TransferTx
  })
}

module.exports = {
  install (Vue) {
    Vue.prototype.$blockchain = {
      generateKeyPair () {
        return Exonum.keyPair()
      },

      generateSeed () {
        return bigInt.randBetween(0, MAX_VALUE).valueOf()
      },

      createWallet (keyPair, balance) {
        // Describe transaction
        const transaction = new CreateTransaction(keyPair.publicKey)

        // Transaction data
        const data = {
          initialBalance: balance
        }

        // Send transaction into blockchain
        return transaction.send(TRANSACTION_URL, data, keyPair.secretKey)
      },

      transfer (keyPair, receiver, amountToTransfer, seed) {
        // Describe transaction
        const transaction = new TransferTransaction(keyPair.publicKey)

        // Transaction data
        const data = {
          seed: seed,
          toWallet: Exonum.hexadecimalToUint8Array(receiver),
          sum: amountToTransfer
        }

        // Send transaction into blockchain
        return transaction.send(TRANSACTION_URL, data, keyPair.secretKey)
      },

      getWallet: getWallet,

      getBlocks (latest) {
        const suffix = !isNaN(latest) ? '&latest=' + latest : ''
        return axios.get(`/api/explorer/v1/blocks?count=${PER_PAGE}${suffix}`).then(response => response.data)
      },

      getBlock (height) {
        return axios.get(`/api/explorer/v1/block?height=${height}`).then(response => response.data)
      },

      getTransaction (hash) {
        return axios.get(`/api/explorer/v1/transactions?hash=${hash}`).then(response => {
          return response.data
        })
      },

      getHistory (publicKey) {
        return axios.get(`/api/services/cryptocurrency-demo/wallet/${publicKey}/history`).then(r => r.data)
      }
    }
  }
}
