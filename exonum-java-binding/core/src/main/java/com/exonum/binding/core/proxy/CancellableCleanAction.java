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

package com.exonum.binding.core.proxy;

/**
 * A cancellable clean action can be cancelled. That is useful, for instance, when a Java native
 * proxy transfers ownership over the native peer back to the native code.
 */
public interface CancellableCleanAction<ResourceDescriptionT>
    extends CleanAction<ResourceDescriptionT> {

  /**
   * Cancels this clean action, making {@link #clean()} a no-op. This operation cannot be reversed.
   */
  void cancel();
}
