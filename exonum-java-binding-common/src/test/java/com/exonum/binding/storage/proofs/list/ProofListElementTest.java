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

package com.exonum.binding.storage.proofs.list;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.common.hash.Funnel;
import com.exonum.binding.common.hash.PrimitiveSink;
import org.junit.Test;

public class ProofListElementTest {

  private static final byte[] E1 = bytes("element 1");

  private ProofListElement node;

  @Test
  public void getElement() {
    node = new ProofListElement(E1);

    assertThat(node.getElement(), equalTo(E1));
  }

  @Test
  public void funnel() {
    node = new ProofListElement(E1);

    Funnel<ProofListElement> funnel = ProofListElement.funnel();
    assertNotNull(funnel);

    PrimitiveSink sink = mock(PrimitiveSink.class);
    funnel.funnel(node, sink);

    verify(sink).putBytes(eq(E1));
  }
}
