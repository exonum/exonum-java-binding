/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.example.guide;

import com.exonum.binding.core.service.AbstractServiceModule;
import com.exonum.binding.core.service.Service;
import org.pf4j.Extension;

@SuppressWarnings("unused") // Example code
// ci-block ci_service_module {
@Extension
public final class ServiceModule extends AbstractServiceModule {

  @Override
  protected void configure() {
    // Define the Service implementation
    bind(Service.class).to(FooService.class);
  }
}
// }
