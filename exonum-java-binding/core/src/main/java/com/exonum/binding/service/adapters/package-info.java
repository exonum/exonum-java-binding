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

/**
 * An internal package with adapters of Java Service interfaces
 * to the interface, convenient to the native code. That brings such benefits:
 * <ul>
 *   <li>Separates user-facing interface and the framework implementation,
 *     enabling us to change them independently.
 *   <li>Provides the native code with a convenient interface (simpler, faster, more reliable).
 * </ul>
 *
 * <p>Also contains an utility factory to produce proxies of native views.
 */
package com.exonum.binding.service.adapters;
