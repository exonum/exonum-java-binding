/*
 * Copyright (C) 2011 The Guava Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * Copyright 2018 The Exonum Team
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

// TODO(user): when things stabilize, flesh this out
/**
 * Hash functions and related structures.
 *
 * <p>See the Guava User Guide article on
 * <a href="https://github.com/google/guava/wiki/HashingExplained">hashing</a>.
 *
 * <p>This package is a repackaged copy of com.google.common.hash from Guava, as of 23.4-jre,
 * since the hashing APIs we re-export are in {@link com.google.common.annotations.Beta}
 * and may change at any time.
 *
 * <p>It includes only the classes, needed for SHA-256 hash function.
 * The other supported hash functions and bloom-filters are removed.
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package com.exonum.binding.hash;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
