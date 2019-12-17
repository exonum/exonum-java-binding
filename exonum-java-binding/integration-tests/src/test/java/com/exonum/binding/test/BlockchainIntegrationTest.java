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

package com.exonum.binding.test;

import static com.exonum.binding.common.blockchain.ExecutionStatuses.success;
import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_DIR;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_FILENAME;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_ID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.ListIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.fakeservice.Transactions.PutTransactionArgs;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.TestKit;
import com.exonum.core.messages.Blockchain.Config;
import com.exonum.core.messages.Blockchain.ValidatorKeys;
import com.exonum.core.messages.Consensus.ExonumMessage;
import com.exonum.core.messages.Consensus.ExonumMessage.KindCase;
import com.exonum.core.messages.Consensus.Precommit;
import com.exonum.core.messages.Consensus.SignedMessage;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.exonum.core.messages.Types;
import com.exonum.core.messages.Types.Hash;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BlockchainIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final KeyPair KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();
  private static final short VALIDATOR_COUNT = 1;
  private static final HashCode ZERO_HASH_CODE = HashCode.fromBytes(
      new byte[DEFAULT_HASH_SIZE_BYTES]);
  private static final int GENESIS_BLOCK_HEIGHT = 0;
  private static final String SERVICE_NAME = "service";
  private static final int SERVICE_ID = 100;

  private TestKit testKit;

  @BeforeEach
  void setUp() {
    testKit = TestKit.builder()
        .withValidators(VALIDATOR_COUNT)
        .withArtifactsDirectory(ARTIFACT_DIR)
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
        .build();
  }

  @AfterEach
  void destroyTestKit() {
    testKit.close();
  }

  /** Tests specific to genesis-block only blockchain. */
  @Nested
  class WithGenesisBlock {
    @Test
    void getHeight() {
      testKitTest((blockchain) -> {
        assertThat(blockchain.getHeight()).isEqualTo(GENESIS_BLOCK_HEIGHT);
      });
    }

    @Test
    void getBlocks() {
      testKitTest(blockchain -> {
        Map<HashCode, Block> blocks = toMap(blockchain.getBlocks());
        // Check there is only a single genesis block
        assertThat(blocks).hasSize(1);
        // Check the entry itself
        Entry<HashCode, Block> genesisBlockEntry = blocks.entrySet()
            .iterator()
            .next();
        HashCode blockHash = genesisBlockEntry.getKey();
        Block block = genesisBlockEntry.getValue();
        // Check the hash (which we do calculate on Java side)
        assertThat(blockHash).isEqualTo(block.getBlockHash());
        // Check the block
        assertGenesisBlock(block);
      });
    }

    @Test
    void getLastBlock() {
      testKitTest((blockchain) -> {
        Block genesisBlock = blockchain.getLastBlock();
        assertGenesisBlock(genesisBlock);
      });
    }
  }

  /** Tests with a blockchain with two blocks: genesis + next. */
  @Nested
  class WithSingleBlock {
    private Block block;
    private TransactionMessage expectedBlockTransaction;

    @BeforeEach
    void commitBlock() {
      TransactionMessage transactionMessage = createTestTransactionMessage();
      expectedBlockTransaction = transactionMessage;
      block = testKit.createBlockWithTransactions(transactionMessage);
    }

    @Test
    void containsBlock() {
      testKitTest((blockchain) -> assertThat(blockchain.containsBlock(block)).isTrue());
    }

    @Test
    void getHeight() {
      testKitTest((blockchain) -> {
        long expectedHeight = 1;
        assertThat(blockchain.getHeight()).isEqualTo(expectedHeight);
      });
    }

    @Test
    void getBlockHashes() {
      testKitTest((blockchain) -> {
        Block genesisBlock = blockchain.getBlock(0);
        List<HashCode> expectedHashes = ImmutableList.of(
            genesisBlock.getBlockHash(),
            block.getBlockHash()
        );
        List<HashCode> blockHashes = ImmutableList.copyOf(blockchain.getBlockHashes());
        assertThat(blockHashes).isEqualTo(expectedHashes);
      });
    }

    @Test
    void containsBlockNoSuchBlock() {
      testKitTest((blockchain) -> {
        Block unknownBlock = aBlock(Long.MAX_VALUE).build();
        assertThat(blockchain.containsBlock(unknownBlock)).isFalse();
      });
    }

    @Test
    void containsBlockSameHashDistinctFields() {
      testKitTest((blockchain) -> {
        // Check against a block that has the same hash, but different fields
        Block unknownBlock = aBlock(10L)
            .blockHash(block.getBlockHash())
            .build();
        assertThat(blockchain.containsBlock(unknownBlock)).isFalse();
      });
    }

    @Test
    void getBlockTransactionsByHeight() {
      testKitTest((blockchain) -> {
        long blockHeight = block.getHeight();
        List<HashCode> expectedBlockTransactionHashes =
            ImmutableList.of(expectedBlockTransaction.hash());
        assertThat(blockchain.getBlockTransactions(blockHeight))
            .hasSameElementsAs(expectedBlockTransactionHashes);
      });
    }

    @Test
    void getBlockTransactionsByInvalidHeight() {
      testKitTest((blockchain) -> {
        long invalidBlockHeight = block.getHeight() + 1;
        Exception e = assertThrows(IllegalArgumentException.class,
            () -> blockchain.getBlockTransactions(invalidBlockHeight));
        String expectedMessage =
            String.format(
                "Height should be less or equal compared to blockchain height %s, but was %s",
                block.getHeight(), invalidBlockHeight);
        assertThat(e).hasMessageContaining(expectedMessage);
      });
    }

    @Test
    void getBlockTransactionsByHash() {
      testKitTest((blockchain) -> {
        HashCode blockHash = block.getBlockHash();
        List<HashCode> expectedBlockTransactionHashes =
            ImmutableList.of(expectedBlockTransaction.hash());
        assertThat(blockchain.getBlockTransactions(blockHash))
            .hasSameElementsAs(expectedBlockTransactionHashes);
      });
    }

    @Test
    void getBlockTransactionsByInvalidHash() {
      testKitTest((blockchain) -> {
        HashCode invalidBlockHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
        Exception e = assertThrows(IllegalArgumentException.class,
            () -> blockchain.getBlockTransactions(invalidBlockHash));
        String expectedMessage = String.format("No block found for given id %s", invalidBlockHash);
        assertThat(e).hasMessageContaining(expectedMessage);
      });
    }

    @Test
    void getBlockTransactionsByBlock() {
      testKitTest((blockchain) -> {
        List<HashCode> expectedBlockTransactionHashes =
            ImmutableList.of(expectedBlockTransaction.hash());
        assertThat(blockchain.getBlockTransactions(block))
            .hasSameElementsAs(expectedBlockTransactionHashes);
      });
    }

    @Test
    void getBlockTransactionsByInvalidBlock() {
      testKitTest((blockchain) -> {
        Block unknownBlock = aBlock(Long.MAX_VALUE).build();

        Exception e = assertThrows(IllegalArgumentException.class,
            () -> blockchain.getBlockTransactions(unknownBlock));

        String expectedMessage = String.format("No such block (%s) in the database", unknownBlock);
        assertThat(e).hasMessageContaining(expectedMessage);
      });
    }

    @Test
    void getTxMessages() {
      testKitTest((blockchain) -> {
        MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();
        Map<HashCode, TransactionMessage> txMessagesMap = toMap(txMessages);
        assertThat(txMessagesMap).hasSize(1);
        assertThat(txMessagesMap.get(expectedBlockTransaction.hash()))
            .isEqualTo(expectedBlockTransaction);
      });
    }

    @Test
    void getTxResults() {
      testKitTest((blockchain) -> {
        ProofMapIndexProxy<HashCode, ExecutionStatus> txResults = blockchain.getTxResults();
        Map<HashCode, ExecutionStatus> txResultsMap = toMap(txResults);
        ImmutableMap<HashCode, ExecutionStatus> expected =
            ImmutableMap.of(expectedBlockTransaction.hash(), success());
        assertThat(txResultsMap).isEqualTo(expected);
      });
    }

    @Test
    void getTxResult() {
      testKitTest((blockchain) -> {
        Optional<ExecutionStatus> txResult =
            blockchain.getTxResult(expectedBlockTransaction.hash());
        assertThat(txResult).hasValue(success());
      });
    }

    @Test
    void getTxResultOfUnknownTx() {
      testKitTest((blockchain) -> {
        HashCode unknownHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
        Optional<ExecutionStatus> txResult = blockchain.getTxResult(unknownHash);
        assertThat(txResult).isEmpty();
      });
    }

    @Test
    void getTxLocations() {
      testKitTest((blockchain) -> {
        MapIndex<HashCode, TransactionLocation> txLocations = blockchain.getTxLocations();
        Map<HashCode, TransactionLocation> txLocationsMap = toMap(txLocations);
        TransactionLocation expectedTransactionLocation =
            TransactionLocation.valueOf(block.getHeight(), 0L);
        assertThat(txLocationsMap)
            .isEqualTo(ImmutableMap.of(expectedBlockTransaction.hash(),
                expectedTransactionLocation));
      });
    }

    @Test
    void getTxLocation() {
      testKitTest((blockchain) -> {
        Optional<TransactionLocation> txLocation =
            blockchain.getTxLocation(expectedBlockTransaction.hash());
        TransactionLocation expectedTransactionLocation =
            TransactionLocation.valueOf(block.getHeight(), 0L);
        assertThat(txLocation).hasValue(expectedTransactionLocation);
      });
    }

    @Test
    void getTxLocationOfUnknownTx() {
      testKitTest((blockchain) -> {
        HashCode unknownHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
        Optional<TransactionLocation> txLocation = blockchain.getTxLocation(unknownHash);
        assertThat(txLocation).isEmpty();
      });
    }

    @Test
    void getBlocks() {
      testKitTest((blockchain) -> {
        MapIndex<HashCode, Block> blocks = blockchain.getBlocks();
        Map<HashCode, Block> blocksMap = toMap(blocks);
        // Contains both genesis and committed blocks
        assertThat(blocksMap).hasSize(2);
        assertThat(blocksMap.get(block.getBlockHash())).isEqualTo(block);
      });
    }

    @Test
    void getBlockByHeight() {
      testKitTest((blockchain) -> {
        Block actualBlock = blockchain.getBlock(block.getHeight());
        assertThat(actualBlock).isEqualTo(block);
      });
    }

    @Test
    void getBlockByHeightGenesis() {
      testKitTest((blockchain) -> {
        Block genesisBlock = blockchain.getBlock(GENESIS_BLOCK_HEIGHT);
        assertGenesisBlock(genesisBlock);
      });
    }

    @Test
    void getBlockByInvalidHeight() {
      testKitTest((blockchain) -> {
        long invalidBlockHeight = block.getHeight() + 1;
        Exception e = assertThrows(IndexOutOfBoundsException.class,
            () -> blockchain.getBlock(invalidBlockHeight));
        String expectedMessage = String.format("Block height (%s) is out of range [0, %s]",
            invalidBlockHeight, block.getHeight());
        assertThat(e).hasMessageContaining(expectedMessage);
      });
    }

    @Test
    void getBlockById() {
      testKitTest((blockchain) -> {
        Optional<Block> actualBlock = blockchain.findBlock(block.getBlockHash());
        assertThat(actualBlock).hasValue(block);
      });
    }

    @Test
    void getUnknownBlockById() {
      testKitTest((blockchain) -> {
        HashCode blockHash = HashCode.fromString("ab");
        Optional<Block> block = blockchain.findBlock(blockHash);
        assertThat(block).isEmpty();
      });
    }

    @Test
    void getLastBlock() {
      testKitTest((blockchain) -> {
        Block lastBlock = blockchain.getLastBlock();
        assertThat(lastBlock).isEqualTo(block);
      });
    }

    @Test
    void getPrecommits() {
      testKitTest((blockchain -> {
        HashCode blockHash = block.getBlockHash();

        ListIndexProxy<SignedMessage> precommits = blockchain.getPrecommits(blockHash);

        // Check the number of precommits
        assertThat(precommits.size()).isEqualTo(VALIDATOR_COUNT);
        // Verify the first message
        SignedMessage signedPrecommit = precommits.get(0);
        // Check the message is signed by one of the validators
        List<Types.PublicKey> consensusKeys = blockchain.getConsensusConfiguration()
            .getValidatorKeysList()
            .stream()
            .map(ValidatorKeys::getConsensusKey)
            .collect(toList());
        assertThat(consensusKeys)
            .describedAs("Signed message must be signed by a validator key")
            .contains(signedPrecommit.getAuthor());

        // Check the message is indeed a precommit
        ExonumMessage exonumMessage;
        try {
          exonumMessage = ExonumMessage.parseFrom(signedPrecommit.getPayload());
        } catch (InvalidProtocolBufferException e) {
          throw new AssertionError("payload must be an Exonum message", e);
        }
        KindCase messageKind = exonumMessage.getKindCase();
        assertThat(messageKind).isEqualTo(KindCase.PRECOMMIT);
        Precommit precommit = exonumMessage.getPrecommit();
        assertThat(precommit.getHeight()).isEqualTo(block.getHeight());
        HashCode blockHashInPrecommit = toHashCode(precommit.getBlockHash());
        assertThat(blockHashInPrecommit).isEqualTo(blockHash);
      }));
    }

    @Test
    void getConsensusConfiguration() {
      testKitTest((blockchain) -> {
        Config configuration = blockchain.getConsensusConfiguration();
        int numKeysInConfig = configuration.getValidatorKeysCount();
        // Check the number of validator keys
        assertThat(numKeysInConfig).isEqualTo(VALIDATOR_COUNT);

        // Check the public service key of the emulated node is included
        List<PublicKey> serviceKeys = configuration.getValidatorKeysList().stream()
            .map(ValidatorKeys::getServiceKey)
            // fixme: [ECR-3734] highly error-prone and verbose key#getData.toByteArray susceptible
            //  to incorrect key#toByteArray.
            .map(key -> PublicKey.fromBytes(key.getData().toByteArray()))
            .collect(toList());
        EmulatedNode emulatedNode = testKit.getEmulatedNode();
        PublicKey emulatedNodeServiceKey = emulatedNode.getServiceKeyPair().getPublicKey();
        List<PublicKey> expectedKeys = ImmutableList.of(emulatedNodeServiceKey);
        assertThat(serviceKeys).isEqualTo(expectedKeys);
      });
    }

    @Test
    // todo: Consider how to test pool operations — through afterCommit?
    void getTransactionPool() {
      testKitTest((blockchain) -> {
        KeySetIndexProxy<HashCode> transactionPool = blockchain.getTransactionPool();
        assertThat(transactionPool).isEmpty();
      });
    }
  }

  private void testKitTest(Consumer<Blockchain> test) {
    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    test.accept(blockchain);
  }

  private static void assertGenesisBlock(Block actualBlock) {
    assertThat(actualBlock.getHeight()).isEqualTo(GENESIS_BLOCK_HEIGHT);
    assertThat(actualBlock.getPreviousBlockHash()).isEqualTo(ZERO_HASH_CODE);
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }

  private static TransactionMessage createTestTransactionMessage() {
    return createTestTransactionMessage("k1", "v1");
  }

  private static TransactionMessage createTestTransactionMessage(String key, String value) {
    return TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(0)
        .payload(PutTransactionArgs.newBuilder()
            .setKey(key)
            .setValue(value)
            .build())
        .sign(KEY_PAIR);
  }

  /**
   * Creates a builder of a block, fully initialized with defaults, inferred from the height.
   * An invocation with the same height will produce exactly the same builder. The block hash
   * is <strong>not</strong> equal to the hash of the block with such parameters.
   *
   * @param blockHeight a block height
   * @return a new block builder
   */
  private static Block.Builder aBlock(long blockHeight) {
    HashFunction hashFunction = Hashing.sha256();
    return Block.builder()
        .proposerId(0)
        .height(blockHeight)
        .numTransactions(0)
        .blockHash(hashFunction.hashLong(blockHeight))
        .previousBlockHash(hashFunction.hashLong(blockHeight - 1))
        .txRootHash(hashFunction.hashString("transactions at" + blockHeight, UTF_8))
        .stateHash(hashFunction.hashString("state hash at " + blockHeight, UTF_8));
  }

  private static HashCode toHashCode(Hash hash) {
    // todo: Migrate to a single utility method in [ECR-3734]
    ByteString bytes = hash.getData();
    return HashCode.fromBytes(bytes.toByteArray());
  }
}
