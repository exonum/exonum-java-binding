package com.exonum.binding.storage.proofs.map.flat;

import com.exonum.binding.hash.HashCode;
import java.util.List;
import java.util.Optional;

public interface MapProof {
  List<MapProofEntry> getProofList();

  boolean containsKey(byte[] key);

  boolean isValid(HashCode rootHash);

  Optional<byte[]> get(byte[] key);
}
