import Vue from 'vue/dist/vue'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import * as Blockchain from '../src/plugins/blockchain.js'

const mock = new MockAdapter(axios)
const MAX_VALUE = 2147483647
const hexRegex = /[0-9A-Fa-f]+/i
const TRANSACTION_URL = '/api/explorer/v1/transactions'
const TRANSACTION_EXPLORER_URL = '/api/explorer/v1/transactions?hash='
const keyPair = {
  publicKey: '78cf8b5e5c020696319eb32a1408e6c65e7d97733d34528fbdce08438a0243e8',
  secretKey: 'b5b3ccf6ca4475b7ff3d910d5ab31e4723098490a3e341dd9d2896b42ebc9f8978cf8b5e5c020696319eb32a1408e6c65e7d97733d34528fbdce08438a0243e8'
}

Vue.use(Blockchain)

// Mock `createWallet` transaction
const createWalletTxHash = '55209b3c6bd8593b9c90eacd3a57cfc448ebc0d47316235a4ca3a1751548a384'
mock.onPost(TRANSACTION_URL, {
  'tx_body': '78cf8b5e5c020696319eb32a1408e6c65e7d97733d34528fbdce08438a0243e80000800002000a084a6f686e20446f65a71558ef2f2d592acfffbac71ea13327a78be83d6977240d3ca8cf4a92ba3d87cd30b9df1ca9b83147be274a85369ce5a2ce3e3a490be6acbaca48c764b40907'
}).replyOnce(200)

mock.onGet(`${TRANSACTION_EXPLORER_URL}${createWalletTxHash}`).replyOnce(200, { 'type': 'in-pool' })

mock.onGet(`${TRANSACTION_EXPLORER_URL}${createWalletTxHash}`).replyOnce(200, { 'type': 'committed' })

// Mock `transfer` transaction
const transferTxHash = '85e2c97aab7d2b6518850b3c9f647b1bb2fa7f8370f33c6f9b6c11cfa6371969'
mock.onPost(TRANSACTION_URL, {
  'tx_body': '78cf8b5e5c020696319eb32a1408e6c65e7d97733d34528fbdce08438a0243e80000800000000a220a20278663010ebe1136011618ad5be1b9d6f51edc5b6c6b51b5450ffc72f54a57df10191880a0db99c6b080bc6ba0bfeb12fc750df184136bd8d9a4f33676b8ee6e1e40754d7d19f0cb4f62db67e36e83253e737dce0ec3a6566857ef71de440d329fd470e77fed232d2411590c'
}).replyOnce(200)

mock.onGet(`${TRANSACTION_EXPLORER_URL}${transferTxHash}`).replyOnce(200, { 'type': 'committed' })

describe('Interaction with blockchain', () => {
  it('should generate new signing key pair', () => {
    const keyPair = Vue.prototype.$blockchain.generateKeyPair()

    expect(keyPair.publicKey).toMatch(hexRegex)
    expect(keyPair.publicKey).toHaveLength(64)
    expect(keyPair.secretKey).toMatch(hexRegex)
    expect(keyPair.secretKey).toHaveLength(128)
  })

  it('should generate new random seed', () => {
    const seed = Vue.prototype.$blockchain.generateSeed()

    expect(seed).toBeLessThan(MAX_VALUE)
  })

  it('should create new wallet', async () => {
    const balance = 93

    await expect(Vue.prototype.$blockchain.createWallet(keyPair, balance)).resolves
  })

  it('should transfer funds', async () => {
    const receiver = '278663010ebe1136011618ad5be1b9d6f51edc5b6c6b51b5450ffc72f54a57df'
    const amountToTransfer = '25'
    const seed = '7743941227375415562'

    await expect(Vue.prototype.$blockchain.transfer(keyPair, receiver, amountToTransfer, seed)).resolves
  })

})
