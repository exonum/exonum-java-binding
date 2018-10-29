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
              <td :class="{'text-success':item.wallet_from === keyPair.publicKey}">
                {{ item.wallet_from | slice }}
              </td>
              <td :class="{'text-success':item.wallet_to === keyPair.publicKey}">
                {{ item.wallet_to | slice }}
              </td>
              <td :class="[item.wallet_from === keyPair.publicKey ? 'text-danger': 'text-success']">
                {{ item.wallet_from === keyPair.publicKey ? '-' : '+' }}{{ item.amount }}$
              </td>
              <td>
                <router-link :to="{ name: 'transaction', params: { hash: item.transaction_hash } }">
                  {{ item.transaction_hash | slice }}
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
  import {mapState} from 'vuex'

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
