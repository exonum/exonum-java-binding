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

import static com.exonum.binding.common.hash.Hashing.DEFAULT_HASH_SIZE_BYTES;
import static com.exonum.binding.fakeservice.FakeService.PUT_TX_ID;
import static com.exonum.binding.fakeservice.FakeService.RAISE_ERROR_TX_ID;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_DIR;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_FILENAME;
import static com.exonum.binding.test.TestArtifactInfo.ARTIFACT_ID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.exonum.binding.common.blockchain.CallInBlocks;
import com.exonum.binding.common.blockchain.ExecutionStatuses;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.message.SignedMessage;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.blockchain.Block;
import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.blockchain.proofs.BlockProof;
import com.exonum.binding.core.blockchain.proofs.IndexProof;
import com.exonum.binding.core.storage.database.Snapshot;
import com.exonum.binding.core.storage.indices.KeySetIndexProxy;
import com.exonum.binding.core.storage.indices.MapIndex;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.fakeservice.FakeSchema;
import com.exonum.binding.fakeservice.Transactions.PutTransactionArgs;
import com.exonum.binding.fakeservice.Transactions.RaiseErrorArgs;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.TestKit;
import com.exonum.core.messages.Blockchain.CallInBlock;
import com.exonum.core.messages.Blockchain.Config;
import com.exonum.core.messages.Blockchain.ValidatorKeys;
import com.exonum.core.messages.Consensus;
import com.exonum.core.messages.Consensus.ExonumMessage;
import com.exonum.core.messages.Consensus.ExonumMessage.KindCase;
import com.exonum.core.messages.Consensus.Precommit;
import com.exonum.core.messages.MapProofOuterClass.MapProof;
import com.exonum.core.messages.MapProofOuterClass.OptionalEntry;
import com.exonum.core.messages.Proofs;
import com.exonum.core.messages.Runtime.ErrorKind;
import com.exonum.core.messages.Runtime.ExecutionError;
import com.exonum.core.messages.Runtime.ExecutionStatus;
import com.exonum.core.messages.Types;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlockchainIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final KeyPair KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();
  private static final short VALIDATOR_COUNT = 1;
  private static final HashCode ZERO_HASH_CODE = HashCode.fromBytes(
      new byte[DEFAULT_HASH_SIZE_BYTES]);
  private static final long GENESIS_BLOCK_HEIGHT = 0;
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
    void createBlockProof() {
      testKitTest(blockchain -> {
        BlockProof blockProof = blockchain.createBlockProof(GENESIS_BLOCK_HEIGHT);

        // Check the block proof message
        Proofs.BlockProof proof = blockProof.getAsMessage();
        com.exonum.core.messages.Blockchain.Block genesisBlock = proof.getBlock();
        assertThat(genesisBlock.getHeight()).isEqualTo(GENESIS_BLOCK_HEIGHT);
        // A genesis block proof is a special case: it does not have precommit messages,
        // for it is created based on the network configuration only, with no messages.
        assertThat(proof.getPrecommitsList()).isEmpty();
      });
    }

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
    void commitBlockWithSingleTx() {
      TransactionMessage transactionMessage = createPutTransactionMessage();
      expectedBlockTransaction = transactionMessage;
      block = testKit.createBlockWithTransactions(transactionMessage);
    }

    @Test
    void createBlockProof() {
      testKitTest(blockchain -> {
        long height = 1L;
        BlockProof blockProof = blockchain.createBlockProof(height);

        // Check the block proof message
        Proofs.BlockProof proof = blockProof.getAsMessage();
        // 1 Verify the block
        Block blockInProof = Block.fromMessage(proof.getBlock());
        assertThat(blockInProof).isEqualTo(block);
        // 2 Verify the proof: the precommit messages
        assertThat(proof.getPrecommitsList()).hasSize(VALIDATOR_COUNT);
        // Check the precommit message from the single validator
        Consensus.SignedMessage rawPrecommitMessage = proof.getPrecommits(0).getRaw();
        SignedMessage rawPrecommit = SignedMessage.fromProto(rawPrecommitMessage);
        ExonumMessage payload = rawPrecommit.getPayload();
        assertThat(payload.getKindCase()).isEqualTo(KindCase.PRECOMMIT);
        Precommit precommit = payload.getPrecommit();
        HashCode blockHash = hashFromProto(precommit.getBlockHash());
        // Check the block hash in precommit matches the actual block hash
        assertThat(blockHash).isEqualTo(block.getBlockHash());
      });
    }

    @Test
    void createIndexProof() {
      testKitTest(blockchain -> {
        String testMapName = SERVICE_NAME + ".test-map";
        IndexProof indexProof = blockchain.createIndexProof(testMapName);

        // Check the index proof message
        Proofs.IndexProof proof = indexProof.getAsMessage();
        // 1 Verify the block proof
        Proofs.BlockProof blockProof = proof.getBlockProof();
        Block blockInProof = Block.fromMessage(blockProof.getBlock());
        assertThat(blockInProof).isEqualTo(block);
        // Verify the precommits
        assertThat(blockProof.getPrecommitsList()).hasSize(VALIDATOR_COUNT);

        // 2 Verify the aggregating index proof
        MapProof aggregatingIndexProof = proof.getIndexProof();
        // It must have a single entry: (testMapName, indexHash(testMap))
        Snapshot snapshot = testKit.getSnapshot();
        FakeSchema serviceSchema = new FakeSchema(SERVICE_NAME, snapshot);
        HashCode testMapHash = serviceSchema.testMap().getIndexHash();
        OptionalEntry expectedEntry = OptionalEntry.newBuilder()
            .setKey(ByteString.copyFromUtf8(testMapName))
            .setValue(ByteString.copyFrom(testMapHash.asBytes()))
            .build();
        assertThat(aggregatingIndexProof.getEntriesList()).containsExactly(expectedEntry);
      });
    }

    @Test
    void createIndexProofForUnknownIndex() {
      testKitTest(blockchain -> {
        String testIndexName = "unknown-index";

        Exception e = assertThrows(IllegalArgumentException.class,
            () -> blockchain.createIndexProof(testIndexName));

        assertThat(e.getMessage()).contains(testIndexName);
      });
    }

    @Test
    void containsBlock() {
      testKitTest((blockchain) -> assertThat(blockchain.containsBlock(block)).isTrue());
    }

    @Test
    void getHeight() {
      testKitTest((blockchain) -> {
        long expectedHeight = 1L;
        assertThat(blockchain.getHeight()).isEqualTo(expectedHeight);
      });
    }

    @Test
    void getBlockHashes() {
      testKitTest((blockchain) -> {
        Block genesisBlock = blockchain.getBlock(GENESIS_BLOCK_HEIGHT);
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
                "Height should be less than or equal to the blockchain height %s, but was %s",
                block.getHeight(), invalidBlockHeight);
        assertThat(e).hasMessageContaining(expectedMessage);
      });
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -2, Long.MIN_VALUE})
    void getBlockTransactionsByNegativeHeight(long height) {
      testKitTest((blockchain) -> {
        Exception e = assertThrows(IllegalArgumentException.class,
            () -> blockchain.getBlockTransactions(height));
        assertThat(e.getMessage()).containsIgnoringCase("negative")
            .containsIgnoringCase("height")
            .contains(Long.toString(height));
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
    void getCallErrorsNoErrors() {
      testKitTest((blockchain) -> {
        long height = block.getHeight();
        ProofMapIndexProxy<CallInBlock, ExecutionError> callErrors =
            blockchain.getCallErrors(height);
        Map<CallInBlock, ExecutionError> callErrorsMap = toMap(callErrors);
        assertThat(callErrorsMap).isEmpty();
      });
    }

    @Test
    void getCallErrorsInvalidHeight() {
      testKitTest((blockchain) -> {
        long invalidHeight = block.getHeight() + 1;
        assertThrows(IllegalArgumentException.class, () -> blockchain.getCallErrors(invalidHeight));
      });
    }

    @Test
    void getTxResultSuccess() {
      testKitTest((blockchain) -> {
        Optional<ExecutionStatus> txResult =
            blockchain.getTxResult(expectedBlockTransaction.hash());
        assertThat(txResult).hasValue(ExecutionStatuses.SUCCESS);
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
            TransactionLocation.valueOf(block.getHeight(), 0);
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
            TransactionLocation.valueOf(block.getHeight(), 0);
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
    void getConsensusConfiguration() {
      testKitTest((blockchain) -> {
        Config configuration = blockchain.getConsensusConfiguration();
        int numKeysInConfig = configuration.getValidatorKeysCount();
        // Check the number of validator keys
        assertThat(numKeysInConfig).isEqualTo(VALIDATOR_COUNT);

        // Check the public service key of the emulated node is included
        List<PublicKey> serviceKeys = configuration.getValidatorKeysList().stream()
            .map(ValidatorKeys::getServiceKey)
            .map(key -> pkFromProto(key))
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

  @Nested
  class WithErrorTx {

    final int errorCode = 17;
    TransactionMessage transactionMessage;
    Block block;

    @BeforeEach
    void commitBlockWithErrorTx() {
      RaiseErrorArgs args = RaiseErrorArgs.newBuilder()
          .setCode(errorCode)
          .build();
      transactionMessage = createTestTransactionMessage(RAISE_ERROR_TX_ID, args);
      block = testKit.createBlockWithTransactions(transactionMessage);
    }

    @Test
    void getCallErrorsWithError() {
      testKitTest(blockchain -> {
        ProofMapIndexProxy<CallInBlock, ExecutionError> callErrors = blockchain
            .getCallErrors(block.getHeight());
        Map<CallInBlock, ExecutionError> callErrorsAsMap = toMap(callErrors);

        int txPosition = 0; // A single tx in block must be at 0 position
        CallInBlock callId = CallInBlocks.transaction(txPosition);
        assertThat(callErrorsAsMap).containsOnlyKeys(callId);
        ExecutionError executionError = callErrorsAsMap.get(callId);
        checkExecutionError(executionError);
      });
    }

    @Test
    void getTxResultWithError() {
      testKitTest(blockchain -> {
        HashCode txHash = transactionMessage.hash();
        Optional<ExecutionStatus> txResult = blockchain.getTxResult(txHash);

        assertThat(txResult).isPresent();
        ExecutionStatus executionStatus = txResult.get();
        assertTrue(executionStatus.hasError());
        checkExecutionError(executionStatus.getError());
      });
    }

    private void checkExecutionError(ExecutionError executionError) {
      // It's a Blockchain test, therefore it's enough to check that the error
      // has some of its attributes correct — the rest may be checked in QaService tests
      // or other ITs.
      assertThat(executionError.getKind()).isEqualTo(ErrorKind.SERVICE);
      assertThat(executionError.getCode()).isEqualTo(errorCode);
    }
  }

  private void testKitTest(ThrowingConsumer<Blockchain> test) {
    Snapshot view = testKit.getSnapshot();
    Blockchain blockchain = Blockchain.newInstance(view);
    try {
      test.accept(blockchain);
    } catch (Throwable t) {
      fail(t);
    }
  }

  private static void assertGenesisBlock(Block actualBlock) {
    assertThat(actualBlock.getHeight()).isEqualTo(GENESIS_BLOCK_HEIGHT);
    assertThat(actualBlock.getPreviousBlockHash()).isEqualTo(ZERO_HASH_CODE);
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }

  private static TransactionMessage createPutTransactionMessage() {
    return createPutTransactionMessage("k1", "v1");
  }

  private static TransactionMessage createPutTransactionMessage(String key, String value) {
    PutTransactionArgs args = PutTransactionArgs.newBuilder()
        .setKey(key)
        .setValue(value)
        .build();
    return createTestTransactionMessage(PUT_TX_ID, args);
  }

  private static TransactionMessage createTestTransactionMessage(int txId, MessageLite payload) {
    return TransactionMessage.builder()
        .serviceId(SERVICE_ID)
        .transactionId(txId)
        .payload(payload)
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
        .stateHash(hashFunction.hashString("state hash at " + blockHeight, UTF_8))
        .errorHash(HashCode.fromString("ab"))
        .additionalHeaders(ImmutableMap.of());
  }

  private static PublicKey pkFromProto(Types.PublicKey key) {
    // todo: [ECR-3734] highly error-prone and verbose key#getData.toByteArray susceptible
    //  to incorrect key#toByteArray.
    return PublicKey.fromBytes(key.getData().toByteArray());
  }

  private static HashCode hashFromProto(Types.Hash hash) {
    // todo: [ECR-3734] highly error-prone and verbose hash#getData.toByteArray susceptible
    //  to incorrect hash#toByteArray.
    return HashCode.fromBytes(hash.getData().toByteArray());
  }
}
