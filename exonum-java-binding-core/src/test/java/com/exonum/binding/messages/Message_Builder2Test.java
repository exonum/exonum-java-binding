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

package com.exonum.binding.messages;

import java.nio.ByteBuffer;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

/**
 * A test of our patches to the auto-generated message builder.
 */
public class Message_Builder2Test {

  @Test
  public void valueEquals() {
    equalsVerifier(Message_Builder2.Value.class)
        .verify();
  }

  @Test
  public void partialEquals() {
    equalsVerifier(Message_Builder2.Partial.class)
        .verify();
  }

  private static <T> EqualsVerifier<T> equalsVerifier(Class<T> classUnderTest) {
    // It is required to provide prefab values for ByteBuffer until
    // support for this class is added to the library:
    // https://github.com/jqno/equalsverifier/issues/198
    return EqualsVerifier.forClass(classUnderTest)
        .withPrefabValues(ByteBuffer.class,
            ByteBuffer.wrap(new byte[] {0}),
            ByteBuffer.wrap(new byte[] {1}));
  }
}
