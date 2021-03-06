/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The Exonum light client.
 * Can be used to submit transactions to the Exonum blockchain.
 * The following example shows how to instantiate the client and submit transaction:
 * <pre>
 *   {@code
 *       ExonumClient exonumClient = ExonumClient.newBuilder()
 *         .setExonumHost("http://<host>:<port>")
 *         .build();
 *       exonumClient.submitTransaction(tx);
 *   }
 * </pre>
 *
 * <p>See more examples in the project <a href="https://github.com/exonum/exonum-java-binding/blob/master/exonum-light-client/README.md#examples">readme</a>.
 */
package com.exonum.client;
