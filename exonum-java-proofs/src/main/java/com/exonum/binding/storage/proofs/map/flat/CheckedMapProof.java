package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import java.util.List;

public interface CheckedMapProof {
  List<MapProofEntry> getProofList();

  boolean containsKey(byte[] key);

  boolean isValid(HashCode rootHash);

  byte[] get(byte[] key);

  ProofStatus getStatus();
}
