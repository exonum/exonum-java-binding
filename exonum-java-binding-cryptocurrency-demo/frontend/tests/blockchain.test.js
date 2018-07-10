import Vue from 'vue/dist/vue'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import * as Exonum from 'exonum-client'
import * as Blockchain from '../src/plugins/blockchain.js'
const mock = new MockAdapter(axios)
const bigIntRegex = /[0-9]+/i;
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
}).replyOnce(200, '362c7ad9827776b07e160b4dc857f46a5c918112b501aecfca5040c02c60cde3')

mock.onGet('/api/explorer/v1/transactions/362c7ad9827776b07e160b4dc857f46a5c918112b501aecfca5040c02c60cde3').replyOnce(200, {
  'type': 'in-pool'
})

mock.onGet('/api/explorer/v1/transactions/362c7ad9827776b07e160b4dc857f46a5c918112b501aecfca5040c02c60cde3').replyOnce(200, {
  'type': 'committed'
})

mock.onPost('/api/cryptocurrency-demo-service/submit-transaction', {
  protocol_version: 0,
  service_id: 42,
  message_id: 2,
  signature: '088ae36319fceb2bf9a6378c1a540261af6b1c634274579637ba140c008e6308d3a427854d19b4c2036daef8a066175c926662ca00248b1526214da3e9bd860f',
  body: {
    seed: '21093588264774074',
    senderId: '9f3ed8007937950d889981a1f0beff041f54d704840e01c207b07b0d5581db13',
    recipientId: '7c3cd965f8084b5730f0f95da1f3b5baf7554c044078d986e249d78c4ee00a98',
    amount: '25'
  }
}).replyOnce(200, '1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127')

mock.onGet('/api/explorer/v1/transactions/1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {
  'type': 'in-pool'
})

mock.onGet('/api/explorer/v1/transactions/1ef4ad31435588a8290a460d1bd0f57edce7ec2e34258693b25216818ed2b127').replyOnce(200, {
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

    expect(seed).toMatch(bigIntRegex)
  })

  it('should create new wallet', async () => {
    const keyPair = {
      publicKey: 'f9cb8a984c8270d67c0eb8d6e6940ea96f859cf9697e2e5a8806ec26008a5722',
      secretKey: 'a188a678048f810253b0c30484ef7a862fe917617bedfac7da57a48658c9c2a9f9cb8a984c8270d67c0eb8d6e6940ea96f859cf9697e2e5a8806ec26008a5722'
    }
    const data = await Vue.prototype.$blockchain.createWallet(keyPair, 100)
    expect(data.data).toEqual('362c7ad9827776b07e160b4dc857f46a5c918112b501aecfca5040c02c60cde3')
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
      "type": "committed"
    })
  })
})