/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.common.serialization.json;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

/**
 * Class used to automatically create Gson type adapters for all AutoValue classes located in this
 * module.
 *
 * <p>Note that you need to provide an accessible static factory method in your AutoValue class.
 *
 * <pre><code>
 *   public static TypeAdapter&lt;TransactionResult&gt; typeAdapter(Gson gson) {
 *     return new AutoValue_TransactionResult.GsonTypeAdapter(gson);
 *   }
 * </code></pre>
 *
 * @see <a href="https://github.com/rharter/auto-value-gson/#factory">Using TypeAdapterFactory</a>
 */
@GsonTypeAdapterFactory
public abstract class CommonTypeAdapterFactory implements TypeAdapterFactory {

  public static TypeAdapterFactory create() {
    return new AutoValueGson_CommonTypeAdapterFactory();
  }
}
