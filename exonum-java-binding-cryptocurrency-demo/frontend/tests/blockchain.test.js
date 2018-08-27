import Vue from 'vue/dist/vue'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import * as Blockchain from '../src/plugins/blockchain.js'

const mock = new MockAdapter(axios)
const MAX_VALUE = 2147483647;
const hexRegex = /[0-9A-Fa-f]+/i;

Vue.use(Blockchain)

mock.onPost('/api/cryptocurrency-demo-service/submit-transaction', {
  protocol_version: 0,
  service_id: 42,
  message_id: 1,
  signature: '244ab284e98b4f21f0828d4549b532553a00b95f71683c684a57290ad4dc48202da45bb130902cfe1b20ff6c23b7851d10cff4583ae6e438f7a49692422ae706',
  body: {
    ownerPublicKey: 'f9cb8a984c8270d67c0eb8d6e6940ea96f859cf9697e2e5a8806ec26008a5722',
    initialBalance: 100
  }
}).replyOnce(200)

mock.onGet('/api/explorer/v1/transactions/362c7ad9827776b07e160b4dc857f46a5c918112b501aecfca5040c02c60cde3').replyOnce(200, {'type': 'in-pool'})

mock.onGet('/api/explorer/v1/transactions/362c7ad9827776b07e160b4dc857f46a5c918112b501aecfca5040c02c60cde3').replyOnce(200, {'type': 'committed'})

mock.onPost('/api/cryptocurrency-demo-service/submit-transaction', {
  protocol_version: 0,
  service_id: 42,
  message_id: 2,
  signature: '74b1d84de332385a975cfb19c03dd156ede8ba28f94386942a9ea2ea55430b3b77ecd53f50d8797c841348dd24025aa4c9db025449efc57a838c762f709d780a',
  body: {
    seed: '21093588264774074',
    senderId: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
    recipientId: '7c3cd965f8084b5730f0f95da1f3b5baf7554c044078d986e249d78c4ee00a98',
    amount: '25'
  }
}).replyOnce(200)

mock.onGet('/api/explorer/v1/transactions/1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {'type': 'in-pool'})

mock.onGet('/api/explorer/v1/transactions/1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {'type': 'committed'})

mock.onGet('/api/cryptocurrency-demo-service/wallet/9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13').replyOnce(200, {'balance': '75'})

describe('Interaction with blockchain', () => {
  it('should generate new signing key pair', () => {
    let keyPair = Vue.prototype.$blockchain.generateKeyPair()

    expect(keyPair.publicKey).toMatch(hexRegex)
    expect(keyPair.publicKey).toHaveLength(64)
    expect(keyPair.secretKey).toMatch(hexRegex)
    expect(keyPair.secretKey).toHaveLength(128)
  })

  it('should generate new random seed', () => {
    let seed = Vue.prototype.$blockchain.generateSeed()

    expect(seed).toBeLessThan(MAX_VALUE)
  })

  it('should create new wallet', async () => {
    let keyPair = {
      publicKey: 'f9cb8a984c8270d67c0eb8d6e6940ea96f859cf9697e2e5a8806ec26008a5722',
      secretKey: 'a188a678048f810253b0c30484ef7a862fe917617bedfac7da57a48658c9c2a9f9cb8a984c8270d67c0eb8d6e6940ea96f859cf9697e2e5a8806ec26008a5722'
    }
    let balance = 100

    await expect(Vue.prototype.$blockchain.createWallet(keyPair, balance)).resolves
  })

  it('should transfer funds', async () => {
    let keyPair = {
      publicKey: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
      secretKey: 'e6361a3221d68148bcb5875af57b2ef8f3cbf821dadaab8ad27bc50d29abf22a9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13'
    }
    let receiver = '7c3cd965f8084b5730f0f95da1f3b5baf7554c044078d986e249d78c4ee00a98'
    let amountToTransfer = '25'
    let seed = '21093588264774074'

    await expect(Vue.prototype.$blockchain.createWallet(keyPair, receiver, amountToTransfer, seed)).resolves
  })
})
