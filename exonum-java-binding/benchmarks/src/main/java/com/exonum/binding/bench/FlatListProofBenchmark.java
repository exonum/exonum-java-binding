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

package com.exonum.binding.bench;

import static com.exonum.binding.common.hash.Hashing.sha256;
import static com.exonum.binding.common.proofs.list.ListProofEntry.checkHeight;
import static java.util.Collections.singletonList;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.list.CheckedListProof;
import com.exonum.binding.common.proofs.list.FlatListProof;
import com.exonum.binding.common.proofs.list.ListProofElementEntry;
import com.exonum.binding.common.proofs.list.ListProofHashedEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FlatListProofBenchmark {

  @Param({"10"})
  private int height;

  private FlatListProof proof;

  /**
   * Creates a proof for a single element from a tree of the given height.
   */
  @Setup(Level.Trial)
  public void createProof() {
    /*
               o
              / \
                 h
            /
           o
          / \
         o   h
        / \
       e   h
     */
    checkHeight(height);
    long size = 1L << height;
    // Create the element(s)
    // The value size can also be made configurable, though it is not expected to have
    // a large effect on anything but the first hashing step.
    byte[] value = {1};
    List<ListProofElementEntry> elements = singletonList(
        ListProofElementEntry.newInstance(0L, value));
    // Create the hash nodes
    List<ListProofHashedEntry> hashed = new ArrayList<>(height);
    for (int h = 0; h < height; h++) {
      HashCode hash = sha256().hashInt(h);
      hashed.add(ListProofHashedEntry.newInstance(1L, h, hash));
    }
    proof = new FlatListProof(elements, hashed, size);
  }

  @Benchmark
  public CheckedListProof<?> verify() {
    return proof.verify();
  }
}
