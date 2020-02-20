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

package com.exonum.binding.common.message;

import static com.exonum.binding.common.message.ByteStringFunnel.byteStringFunnel;
import static com.exonum.binding.test.Bytes.bytes;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.hash.PrimitiveSink;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ByteStringFunnelTest {

  @Test
  void funnel() {
    byte[] source = bytes(1, 2, 3);
    ByteString sourceAsString = ByteString.copyFrom(source);

    PrimitiveSink sink = mock(PrimitiveSink.class);

    byteStringFunnel().funnel(sourceAsString, sink);

    ByteBuffer expectedOutput = ByteBuffer.wrap(source);
    verify(sink).putBytes(expectedOutput);
  }
}
