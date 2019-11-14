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

import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.core.storage.database.View;
import com.exonum.binding.test.Bytes;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;

@Disabled("Disabled until native support is implemented - ECR-3765")
class ProofMapIndexProxyIntegrationTest
    extends BaseProofMapIndexProxyIntegrationTestable {

  private static final List<HashCode> TEST_KEYS = Stream.of(
      Bytes.bytes(0x00),
      Bytes.bytes(0x01),
      Bytes.bytes(0x02),
      Bytes.bytes(0x08),
      Bytes.bytes(0x0f),
      Bytes.bytes(0x10),
      Bytes.bytes(0x20),
      Bytes.bytes(0x80),
      Bytes.bytes(0xf0),
      Bytes.bytes(0xff),
      Bytes.bytes(0x01, 0x01),
      Bytes.bytes(0x01, 0x10),
      Bytes.bytes(0x10, 0x01),
      Bytes.bytes(0x10, 0x10)
  )
      .map(HashCode::fromBytes)
      .collect(toList());

  @Override
  List<HashCode> getTestKeys() {
    return TEST_KEYS;
  }

  @Override
  ProofMapIndexProxy<HashCode, String> create(String name, View view) {
    return ProofMapIndexProxy.newInstance(name, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  @Override
  ProofMapIndexProxy<HashCode, String> createInGroup(String groupName, byte[] idInGroup,
      View view) {
    return ProofMapIndexProxy.newInGroupUnsafe(groupName, idInGroup, view,
        StandardSerializers.hash(), StandardSerializers.string());
  }
}
