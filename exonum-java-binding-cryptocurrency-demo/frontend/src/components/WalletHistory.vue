<template>
  <div class="card mt-5">
    <div class="card-header">Funds history</div>
    <div class="card-body">
      <div class="table-responsive">
        <table class="table table-bordered">
          <thead>
            <tr>
              <th>From</th>
              <th>To</th>
              <th>Amount</th>
              <th>Transaction</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in history" :key="item.seed">
              <td :class="{'text-success':item.walletFrom === keyPair.publicKey}">
                {{ item.walletFrom | slice }}
              </td>
              <td :class="{'text-success':item.walletTo === keyPair.publicKey}">
                {{ item.walletTo | slice }}
              </td>
              <td :class="[item.walletFrom === keyPair.publicKey ? 'text-danger': 'text-success']">
                {{ item.walletFrom === keyPair.publicKey ? '-' : '+' }}{{ item.amount }}$
              </td>
              <td>
                <router-link :to="{ name: 'transaction', params: { hash: item.transactionHash } }">
                  {{ item.transactionHash | slice }}
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
  import { mapState } from 'vuex'

  module.exports = {
    name: 'walletHistory',
    data () {
      return {
        loading: false,
        loaded: false,
        history: []
      }
    },
    filters: {
      slice: function (value) {
        return `${value.slice(0, 30)}...`
      }
    },
    computed: mapState({
      keyPair: state => state.keyPair
    }),
    methods: {
      async loadHistory () {
        this.history = await this.$blockchain.getHistory(this.keyPair.publicKey)
      }
    },
    mounted () {
      this.loadHistory()
    }
  }
</script>
