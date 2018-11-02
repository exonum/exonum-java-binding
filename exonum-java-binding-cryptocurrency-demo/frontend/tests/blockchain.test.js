import Vue from 'vue/dist/vue'
import axios from 'axios'
import fs from 'fs'
import path from 'path'
import MockAdapter from 'axios-mock-adapter'
import * as Exonum from 'exonum-client'
import * as Blockchain from '../src/plugins/blockchain.js'

const mock = new MockAdapter(axios)
const MAX_VALUE = 2147483647
const hexRegex = /[0-9A-Fa-f]+/i

Vue.use(Blockchain)

const bufferToHex = buffer => Exonum.uint8ArrayToHexadecimal(new Uint8Array(buffer))

const createWalletTX = bufferToHex(fs.readFileSync(path.resolve(__dirname, './mocks/createWallet.tx')))
const transferFundsTX = bufferToHex(fs.readFileSync(path.resolve(__dirname, './mocks/transferFunds.tx')))

mock.onPost('/api/cryptocurrency-demo-service/submit-transaction')
  .replyOnce(config => {
    const dataHex = bufferToHex(config.data)
    if (dataHex === createWalletTX) return [200, 'ad248cd86e1fc27bde07f25a6d4632c3a8c596851a78cd0035f1ababcee2986d']
  })

mock.onGet('/api/explorer/v1/transactions/ad248cd86e1fc27bde07f25a6d4632c3a8c596851a78cd0035f1ababcee2986d').replyOnce(200, {
  'type': 'in-pool'
})

mock.onGet('/api/explorer/v1/transactions/ad248cd86e1fc27bde07f25a6d4632c3a8c596851a78cd0035f1ababcee2986d').replyOnce(200, {
  'type': 'committed'
})

mock.onPost('/api/cryptocurrency-demo-service/submit-transaction').replyOnce(config => {
  const dataHex = bufferToHex(config.data)
  if (dataHex === transferFundsTX) return [200, '1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127']
})

mock.onGet('/api/explorer/v1/transactions?hash=1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {
  'type': 'in-pool'
})

mock.onGet('/api/explorer/v1/transactions?hash=1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {
  'type': 'committed'
})

mock.onGet('/api/cryptocurrency-demo-service/wallet/9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13').replyOnce(200, {
  'balance': '75'
})

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
    const keyPair = {
      publicKey: '47ac527ee7b903b0da76d1e0e6b649e9b2e7456aa56343204de9c5cc5a8efb0c',
      secretKey: '82f74d982b2a750466294b1d162fd8b12609414f8abf00561865700fe367c7f247ac527ee7b903b0da76d1e0e6b649e9b2e7456aa56343204de9c5cc5a8efb0c'
    }
    const data = await Vue.prototype.$blockchain.createWallet(keyPair, 432)
    expect(data.data).toEqual('ad248cd86e1fc27bde07f25a6d4632c3a8c596851a78cd0035f1ababcee2986d')
  })

  it('should transfer funds', async () => {
    const keyPair = {
      publicKey: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
      secretKey: 'e6361a3221d68148bcb5875af57b2ef8f3cbf821dadaab8ad27bc50d29abf22a9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13'
    }
    const receiver = '7c3cd965f8084b5730f0f95da1f3b5baf7554c044078d986e249d78c4ee00a98'
    const amountToTransfer = '25'
    const seed = '21093588264774074'
    const data = await Vue.prototype.$blockchain.transfer(keyPair, receiver, amountToTransfer, seed)

    expect(data).toEqual({
      'type': 'committed'
    })
  })
})