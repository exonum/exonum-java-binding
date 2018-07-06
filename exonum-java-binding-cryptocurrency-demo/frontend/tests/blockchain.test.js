import Vue from 'vue/dist/vue'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import * as Exonum from 'exonum-client'
import * as Blockchain from '../src/plugins/blockchain.js'
import actual from './data/actual.json'
const mock = new MockAdapter(axios)
const bigIntRegex = /[0-9]+/i;
const hexRegex = /[0-9A-Fa-f]+/i;

Vue.use(Blockchain)

mock.onGet('/api/services/configuration/v1/configs/actual').reply(200, actual)


mock.onPost('/api/cryptocurrency-demo-service/submit-transaction', {
  protocol_version: 0,
  service_id: 42,
  message_id: 1,
  signature: 'bce82d9cba7723b272314770069e34e2618a1e18d3222cf591b60222392d439ac7d45d6526bec47317b46bef28d346448ee8685bb0fee134ad5eec86d1835f02',
  body: {
    ownerPublicKey: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
    initialBalance: 100
  }
}).replyOnce(200, '8055cd33cf11106f16321feb37777c3a92cbeaa23b9f7984a5b819ae51fee596')

mock.onPost('/api/cryptocurrency-demo-service/submit-transaction', { protocol_version: 0,
  service_id: 42,
  message_id: 2,
  signature: '5ece7da83b9e3e1150657dff02288a22acecf909b9aec1c1486379ddf5abebe9d8e5342477c6114244b1557f1bfca4114ac284ee70e1eb9991cad08ac3924500',
  body:
   { seed: '21093588264774074',
     senderId: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
     recipientId: '7c3cd965f8084b5730f0f95da1f3b5baf7554c044078d986e249d78c4ee00a98',
     amount: '25' } }).replyOnce(200, '1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127')

mock.onGet('/api/explorer/v1/transactions/1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {
  'type': 'in-pool'
})

mock.onGet('/api/explorer/v1/transactions/1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {
  'type': 'committed'
})

mock.onGet('/api/cryptocurrency-demo-service/wallet/9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13').replyOnce(200, {
  'balance': '75'
})

mock.onGet('/api/explorer/v1/transactions/8055cd33cf11106f16321feb37777c3a92cbeaa23b9f7984a5b819ae51fee596').replyOnce(200, {
  'type': 'in-pool'
})

mock.onGet('/api/explorer/v1/transactions/8055cd33cf11106f16321feb37777c3a92cbeaa23b9f7984a5b819ae51fee596').replyOnce(200, {
  'type': 'committed'
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

    expect(seed).toMatch(bigIntRegex)
  })


  it('should create new wallet', async () => {
    const keyPair = {
      publicKey: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
      secretKey: 'e6361a3221d68148bcb5875af57b2ef8f3cbf821dadaab8ad27bc50d29abf22a9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13'
    }
    const data = await Vue.prototype.$blockchain.createWallet(keyPair, 100)

    expect(data.data).toEqual('8055cd33cf11106f16321feb37777c3a92cbeaa23b9f7984a5b819ae51fee596')
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

    expect(data).toEqual({"type": "committed"})
  })
})