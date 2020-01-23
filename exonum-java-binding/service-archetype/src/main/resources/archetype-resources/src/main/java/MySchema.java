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

package ${package};

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.service.Schema;
import com.exonum.binding.core.storage.database.Access;
import java.util.Collections;
import java.util.List;

/**
 * {@code MySchema} provides access to the tables of {@link MyService},
 * given a database state {@linkplain Access access object}.
 *
 * @see <a href="https://exonum.com/doc/version/0.13-rc.2/architecture/storage/#table-types">Exonum table types.</a>
 */
public final class MySchema implements Schema {

  private final Access access;
  private final String namespace;

  public MySchema(Access access, String serviceName) {
    this.access = checkNotNull(access);
    this.namespace = serviceName + ".";
  }

  // TODO: Add index factories here.
}
