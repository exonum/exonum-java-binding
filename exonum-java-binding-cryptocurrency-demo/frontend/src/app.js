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

import 'babel-polyfill';
import Vue from 'vue'
import router from './router'
import numeral from './directives/numeral'
import Validate from './plugins/validate'
import Notify from './plugins/notify'
import Blockchain from './plugins/blockchain'
import App from './App.vue'
import store from './store'

Vue.use(numeral)
Vue.use(Validate)
Vue.use(Notify)
Vue.use(Blockchain)

new Vue({
  el: '#app',
  router,
  store,
  render: createElement => createElement(App)
})
