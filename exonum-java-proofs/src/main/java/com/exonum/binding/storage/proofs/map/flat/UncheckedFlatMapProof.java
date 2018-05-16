package com.exonum.binding.storage.proofs.map.flat;

import static com.exonum.binding.hash.Funnels.hashCodeFunnel;
import static com.exonum.binding.storage.proofs.DbKeyFunnel.dbKeyFunnel;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.HashFunction;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.storage.proofs.map.DbKey;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class UncheckedFlatMapProof implements UncheckedMapProof {

  private static final HashFunction HASH_FUNCTION = Hashing.defaultHashFunction();

  private final List<MapProofEntry> proofList;

  UncheckedFlatMapProof(List<MapProofEntry> proofList) {
    this.proofList = proofList;
  }

  /**
   * Checks that a proof has either correct or incorrect structure and returns a CheckedMapProof.
   */
  @Override
  public CheckedMapProof check() {
    ProofStatus orderCheckResult = orderCheck();
    if (orderCheckResult != ProofStatus.CORRECT) {
      return new CheckedFlatMapProof(orderCheckResult);
    }
    if (proofList.isEmpty()) {
      return new CheckedFlatMapProof(ProofStatus.CORRECT, getEmptyProofListHash(), proofList);
    } else if (proofList.size() == 1) {
      MapProofEntry singleEntry = proofList.get(0);
      if (singleEntry instanceof MapProofEntryLeaf) {
        return new CheckedFlatMapProof(
            ProofStatus.CORRECT,
            getSingletonProofListHash((MapProofEntryLeaf) singleEntry),
            proofList);
      } else {
        return new CheckedFlatMapProof(ProofStatus.NON_TERMINAL_NODE);
      }
    } else {
      Stack<MapProofEntry> contour = new Stack<>();
      MapProofEntry first = proofList.get(0);
      MapProofEntry second = proofList.get(1);
      DbKey lastPrefix = first.getDbKey().commonPrefix(second.getDbKey());
      contour.push(first);
      contour.push(second);
      for (int i = 2; i < proofList.size(); i++) {
        MapProofEntry currentEntry = proofList.get(i);
        DbKey newPrefix = contour.peek().getDbKey().commonPrefix(currentEntry.getDbKey());
        while (contour.size() > 1
            && newPrefix.keyBits().getKeyBits().length()
                < lastPrefix.keyBits().getKeyBits().length()) {
          lastPrefix = fold(contour, lastPrefix).orElse(lastPrefix);
        }
        contour.push(currentEntry);
        lastPrefix = newPrefix;
      }
      while (contour.size() > 1) {
        lastPrefix = fold(contour, lastPrefix).orElse(lastPrefix);
      }
      return new CheckedFlatMapProof(ProofStatus.CORRECT, contour.peek().getHash(), proofList);
    }
  }

  /**
   * Folds two last entries in a contour and replaces them with the folded entry.
   * Returns an updated common prefix between two last entries in the contour.
   */
  private Optional<DbKey> fold(Stack<MapProofEntry> contour, DbKey lastPrefix) {
    MapProofEntry lastEntry = contour.pop();
    MapProofEntry penultEntry = contour.pop();
    MapProofEntry newEntry =
        new MapProofEntryBranch(lastPrefix, computeBranchHash(penultEntry, lastEntry));
    contour.push(newEntry);
    if (contour.size() > 1) {
      penultEntry = contour.get(contour.size() - 2);
      return Optional.of(penultEntry.getDbKey().commonPrefix(lastPrefix));
    }
    return Optional.empty();
  }

  private static HashCode computeBranchHash(MapProofEntry leftChild, MapProofEntry rightChild) {
    return HASH_FUNCTION
        .newHasher()
        .putObject(leftChild.getHash(), hashCodeFunnel())
        .putObject(rightChild.getHash(), hashCodeFunnel())
        .putObject(leftChild.getDbKey(), dbKeyFunnel())
        .putObject(rightChild.getDbKey(), dbKeyFunnel())
        .hash();
  }

  private static HashCode getEmptyProofListHash() {
    return HashCode.fromBytes(new byte[Hashing.DEFAULT_HASH_SIZE_BYTES]);
  }

  private static HashCode getSingletonProofListHash(MapProofEntryLeaf entry) {
    return HASH_FUNCTION.newHasher()
        .putObject(entry.getDbKey(), dbKeyFunnel())
        .putObject(entry.getHash(), hashCodeFunnel())
        .hash();
  }

  /**
   * Check that all entries are in the right order.
   */
  private ProofStatus orderCheck() {
    for (int i = 1; i < proofList.size(); i++) {
      DbKey key = proofList.get(i - 1).getDbKey();
      DbKey nextKey = proofList.get(i).getDbKey();
      DbKey commonPrefix = nextKey.commonPrefix(key);
      if (Arrays.equals(key.getKeySlice(), nextKey.getKeySlice())) {
        return ProofStatus.DUPLICATE_PATH;
      }
      if (commonPrefix.keyBits().getKeyBits().length() != key.keyBits().getKeyBits().length()) {
        return ProofStatus.INVALID_ORDER;
      }
    }
    return ProofStatus.CORRECT;
  }
}
