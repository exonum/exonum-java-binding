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

package com.exonum.binding.common.proofs.list;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.hash.Funnel;
import com.exonum.binding.common.hash.PrimitiveSink;
import org.junit.jupiter.api.Test;

class ListProofElementTest {

  private static final byte[] E1 = bytes("element 1");

  private ListProofElement node;

  @Test
  void getElement() {
    node = new ListProofElement(E1);

    assertThat(node.getElement(), equalTo(E1));
  }

  @Test
  void funnel() {
    node = new ListProofElement(E1);

    Funnel<ListProofElement> funnel = ListProofElement.funnel();
    assertNotNull(funnel);

    PrimitiveSink sink = mock(PrimitiveSink.class);
    funnel.funnel(node, sink);

    verify(sink).putBytes(eq(E1));
  }
}
