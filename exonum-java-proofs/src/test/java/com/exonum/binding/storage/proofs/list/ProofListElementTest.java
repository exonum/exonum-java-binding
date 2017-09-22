package com.exonum.binding.storage.proofs.list;

import static com.exonum.binding.test.Bytes.bytes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.junit.Test;

public class ProofListElementTest {

  private static byte[] E1 = bytes("element 1");

  private ProofListElement node;

  @Test
  public void getElement() throws Exception {
    node = new ProofListElement(E1);

    assertThat(node.getElement(), equalTo(E1));
  }

  @Test
  public void funnel() throws Exception {
    node = new ProofListElement(E1);

    Funnel<ProofListElement> funnel = ProofListElement.funnel();
    assertNotNull(funnel);

    PrimitiveSink sink = mock(PrimitiveSink.class);
    funnel.funnel(node, sink);

    verify(sink).putBytes(eq(E1));
  }
}
