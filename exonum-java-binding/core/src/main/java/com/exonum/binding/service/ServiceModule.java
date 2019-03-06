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

package com.exonum.binding.service;

import com.google.inject.Module;
import org.pf4j.ExtensionPoint;

/**
 * A service module configures the service bindings so that the framework can resolve
 * the service implementation and all its dependencies.
 *
 * <p>An implementation must at least configure the following bindings:
 * <ul>
 *   <li>{@link Service} in {@linkplain com.google.inject.Singleton singleton scope}.
 *   TODO: Is this scope *required* now?
 * </ul>
 *
 * <p>A service module implementation must be marked with {@link org.pf4j.Extension} annotation.
 *
 * <p>Implementations shall generally extend {@link AbstractServiceModule}.
 * todo: Actually, we can easily allow several Guice modules per service for complex services.
 *   However, is it needed?
 */
public interface ServiceModule extends Module, ExtensionPoint {
}
