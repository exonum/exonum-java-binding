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

package com.exonum.binding.core.blockchain.serialization;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

/**
 * Class used to automatically create Gson type adapters for AutoValue classes.
 * Note that you need to provide static factory method in your AutoValue class.
 *
 * <pre><code>
 *   public static TypeAdapter&lt;Block&gt; typeAdapter(Gson gson) {
 *     return new AutoValue_Block.GsonTypeAdapter(gson);
 *   }
 * </code></pre>
 */
@GsonTypeAdapterFactory
public abstract class BlockAdapterFactory implements TypeAdapterFactory {

  public static TypeAdapterFactory create() {
    return new AutoValueGson_BlockAdapterFactory();
  }
}
