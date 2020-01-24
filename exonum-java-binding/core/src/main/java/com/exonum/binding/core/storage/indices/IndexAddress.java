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

package com.exonum.binding.core.storage.indices;

import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkIdInGroup;
import static com.exonum.binding.core.storage.indices.StoragePreconditions.checkIndexName;

import com.exonum.binding.core.storage.database.Access;
import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An Exonum index address: a pair of the name and an optional id in a group, which identifies
 * an Exonum index.
 *
 * <p>Index addresses are resolved relatively to database {@link Access} objects.
 * An index address cannot be translated into a resolved address without the corresponding
 * {@link Access} object.
 * <!-- todo: Extend given accesses — shall we keep just IndexAddress, or split into Relative
 *       and Resolved IndexAddress? -->
 */
public final class IndexAddress {

  private final String name;
  @Nullable private final byte[] idInGroup;

  /**
   * Creates an address of an individual index.
   *
   * @param name the name of the index: a alphanumeric non-empty identifier of the index
   *     in the MerkleDB: [a-zA-Z0-9_.]
   */
  public static IndexAddress valueOf(String name) {
    return new IndexAddress(checkIndexName(name), null);
  }

  /**
   * Creates an address of an index belonging to
   * an <a href="package-summary.html#families">index group</a>.
   *
   * @param groupName the name of the index group: a alphanumeric non-empty identifier of the index
   *     group in the MerkleDB: [a-zA-Z0-9_.]
   * @param idInGroup the id of the index in group. See a
   *     <a href="package-summary.html#families-limitations">caveat</a> on index identifiers.
   */
  public static IndexAddress valueOf(String groupName, byte[] idInGroup) {
    return new IndexAddress(checkIndexName(groupName), checkIdInGroup(idInGroup));
  }

  private IndexAddress(String name, @Nullable byte[] idInGroup) {
    this.name = name;
    this.idInGroup = idInGroup;
  }

  /**
   * Returns the name of the index or index group.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the index id in a group if it belongs to one, otherwise returns an empty optional.
   */
  public Optional<byte[]> getIdInGroup() {
    return Optional.ofNullable(idInGroup);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IndexAddress)) {
      return false;
    }
    IndexAddress that = (IndexAddress) o;
    return name.equals(that.name)
        && Arrays.equals(idInGroup, that.idInGroup);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name);
    result = 31 * result + Arrays.hashCode(idInGroup);
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("idInGroup", idInGroup)
        .omitNullValues()
        .toString();
  }
}
