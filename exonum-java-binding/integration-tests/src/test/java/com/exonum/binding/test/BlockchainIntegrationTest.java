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
import static com.exonum.binding.test.TestTransaction.BODY_CHARSET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.blockchain.Block;
import com.exonum.binding.blockchain.Blockchain;
import com.exonum.binding.common.blockchain.TransactionLocation;
import com.exonum.binding.common.blockchain.TransactionResult;
import com.exonum.binding.common.configuration.StoredConfiguration;
import com.exonum.binding.common.configuration.ValidatorKey;
import com.exonum.binding.common.crypto.CryptoFunction;
import com.exonum.binding.common.crypto.CryptoFunctions;
import com.exonum.binding.common.crypto.KeyPair;
import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;
import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.storage.indices.KeySetIndexProxy;
import com.exonum.binding.storage.indices.ListIndex;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.testkit.EmulatedNode;
import com.exonum.binding.testkit.EmulatedNodeType;
import com.exonum.binding.testkit.TestKit;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockchainIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final KeyPair KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();

  static {
    LibraryLoader.load();
  }

  private TestKit testKit;
  private Block block;
  private TransactionMessage expectedBlockTransaction;

  @BeforeEach
  void setUp() {
    testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .build();
    String payload = "Test";
    TransactionMessage transactionMessage = constructTestTransactionMessage(payload);
    expectedBlockTransaction = transactionMessage;
    block = testKit.createBlockWithTransactions(transactionMessage);
  }

  @AfterEach
  void destroyTestKit() {
    testKit.close();
  }

  @Test
  void containsBlock() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      assertThat(blockchain.containsBlock(block)).isTrue();
      return null;
    });
  }

  @Test
  void getHeight() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      long expectedHeight = 1;
      assertThat(blockchain.getHeight()).isEqualTo(expectedHeight);
      return null;
    });
  }

  @Test
  void getBlockHashes() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      HashCode expectedHash = block.getBlockHash();
      ListIndex<HashCode> blockHashes = blockchain.getBlockHashes();
      // Contains both genesis and committed blocks
      HashCode actualHash = blockHashes.get(1);
      assertThat(actualHash).isEqualTo(expectedHash);
      return null;
    });
  }

  @Test
  void containsBlockNoSuchBlock() {
    testKit.withSnapshot((view) -> {
      // Check that genesis block was committed
      Blockchain blockchain = Blockchain.newInstance(view);
      Block unknownBlock = aBlock(Long.MAX_VALUE).build();
      assertThat(blockchain.containsBlock(unknownBlock)).isFalse();
      return null;
    });
  }

  @Test
  void containsBlockSameHashDistinctFields() {
    testKit.withSnapshot((view) -> {
      // Check against a block that has the same hash, but different fields
      Blockchain blockchain = Blockchain.newInstance(view);
      Block unknownBlock = aBlock(10L)
          .blockHash(block.getBlockHash())
          .build();
      assertThat(blockchain.containsBlock(unknownBlock)).isFalse();
      return null;
    });
  }

  @Test
  void getBlockTransactionsByHeight() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      long blockHeight = block.getHeight();
      List<HashCode> expectedBlockTransactionHashes =
          ImmutableList.of(expectedBlockTransaction.hash());
      assertThat(blockchain.getBlockTransactions(blockHeight))
          .hasSameElementsAs(expectedBlockTransactionHashes);
      return null;
    });
  }

  @Test
  void getBlockTransactionsByInvalidHeight() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      long invalidBlockHeight = block.getHeight() + 1;
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> blockchain.getBlockTransactions(invalidBlockHeight));
      String expectedMessage =
          String.format(
              "Height should be less or equal compared to blockchain height %s, but was %s",
              block.getHeight(), invalidBlockHeight);
      assertThat(e).hasMessageContaining(expectedMessage);
      return null;
    });
  }

  @Test
  void getBlockTransactionsByHash() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      HashCode blockHash = block.getBlockHash();
      List<HashCode> expectedBlockTransactionHashes =
          ImmutableList.of(expectedBlockTransaction.hash());
      assertThat(blockchain.getBlockTransactions(blockHash))
          .hasSameElementsAs(expectedBlockTransactionHashes);
      return null;
    });
  }

  @Test
  void getBlockTransactionsByInvalidHash() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      HashCode invalidBlockHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      Exception e = assertThrows(IllegalArgumentException.class,
          () -> blockchain.getBlockTransactions(invalidBlockHash));
      String expectedMessage = String.format("No block found for given id %s", invalidBlockHash);
      assertThat(e).hasMessageContaining(expectedMessage);
      return null;
    });
  }

  @Test
  void getBlockTransactionsByBlock() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      List<HashCode> expectedBlockTransactionHashes =
          ImmutableList.of(expectedBlockTransaction.hash());
      assertThat(blockchain.getBlockTransactions(block))
          .hasSameElementsAs(expectedBlockTransactionHashes);
      return null;
    });
  }

  @Test
  void getBlockTransactionsByInvalidBlock() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Block unknownBlock = aBlock(Long.MAX_VALUE).build();

      Exception e = assertThrows(IllegalArgumentException.class,
          () -> blockchain.getBlockTransactions(unknownBlock));

      String expectedMessage = String.format("No such block (%s) in the database", unknownBlock);
      assertThat(e).hasMessageContaining(expectedMessage);
      return null;
    });
  }

  @Test
  void getTxMessages() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionMessage> txMessages = blockchain.getTxMessages();
      Map<HashCode, TransactionMessage> txMessagesMap = toMap(txMessages);
      // Should include one executed and one in-pool (submitted in afterCommit) transaction
      assertThat(txMessagesMap).hasSize(2);
      assertThat(txMessagesMap.get(expectedBlockTransaction.hash()))
          .isEqualTo(expectedBlockTransaction);
      return null;
    });
  }

  @Test
  void getTxResults() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      ProofMapIndexProxy<HashCode, TransactionResult> txResults = blockchain.getTxResults();
      Map<HashCode, TransactionResult> txResultsMap = toMap(txResults);
      Map<HashCode, TransactionResult> expected =
          ImmutableMap.of(expectedBlockTransaction.hash(), TransactionResult.successful());
      assertThat(txResultsMap).isEqualTo(expected);
      return null;
    });
  }

  @Test
  void getTxResult() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Optional<TransactionResult> txResult =
          blockchain.getTxResult(expectedBlockTransaction.hash());
      assertThat(txResult).hasValue(TransactionResult.successful());
      return null;
    });
  }

  @Test
  void getTxResultOfUnknownTx() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      HashCode unknownHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      Optional<TransactionResult> txResult = blockchain.getTxResult(unknownHash);
      assertThat(txResult).isEmpty();
      return null;
    });
  }

  @Test
  void getTxLocations() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, TransactionLocation> txLocations = blockchain.getTxLocations();
      Map<HashCode, TransactionLocation> txLocationsMap = toMap(txLocations);
      TransactionLocation expectedTransactionLocation =
          TransactionLocation.valueOf(block.getHeight(), 0L);
      assertThat(txLocationsMap)
          .isEqualTo(ImmutableMap.of(expectedBlockTransaction.hash(),
              expectedTransactionLocation));
      return null;
    });
  }

  @Test
  void getTxLocation() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Optional<TransactionLocation> txLocation =
          blockchain.getTxLocation(expectedBlockTransaction.hash());
      TransactionLocation expectedTransactionLocation =
          TransactionLocation.valueOf(block.getHeight(), 0L);
      assertThat(txLocation).hasValue(expectedTransactionLocation);
      return null;
    });
  }

  @Test
  void getTxLocationOfUnknownTx() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      HashCode unknownHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      Optional<TransactionLocation> txLocation = blockchain.getTxLocation(unknownHash);
      assertThat(txLocation).isEmpty();
      return null;
    });
  }

  @Test
  void getBlocks() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      MapIndex<HashCode, Block> blocks = blockchain.getBlocks();
      Map<HashCode, Block> blocksMap = toMap(blocks);
      // Contains both genesis and committed blocks
      assertThat(blocksMap).hasSize(2);
      assertThat(blocksMap.get(block.getBlockHash())).isEqualTo(block);
      return null;
    });
  }

  @Test
  void getBlockByHeight() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Block actualBlock = blockchain.getBlock(block.getHeight());
      assertThat(actualBlock).isEqualTo(block);
      return null;
    });
  }

  @Test
  void getBlockByInvalidHeight() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      long invalidBlockHeight = block.getHeight() + 1;
      Exception e = assertThrows(IndexOutOfBoundsException.class,
          () -> blockchain.getBlock(invalidBlockHeight));
      String expectedMessage = String.format("Block height (%s) is out of range [0, %s]",
          invalidBlockHeight, block.getHeight());
      assertThat(e).hasMessageContaining(expectedMessage);
      return null;
    });
  }

  @Test
  void getBlockById() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Optional<Block> actualBlock = blockchain.findBlock(block.getBlockHash());
      assertThat(actualBlock).hasValue(block);
      return null;
    });
  }

  @Test
  void getLastBlock() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      Block lastBlock = blockchain.getLastBlock();
      assertThat(lastBlock).isEqualTo(block);
      return null;
    });
  }

  @Test
  void getActualConfiguration() {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      StoredConfiguration configuration = blockchain.getActualConfiguration();
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      List<ValidatorKey> validatorKeys = configuration.validatorKeys();
      short validatorCount = 1;
      assertThat(validatorKeys).hasSize(validatorCount);

      PublicKey emulatedNodeServiceKey = emulatedNode.getServiceKeyPair().getPublicKey();
      List<PublicKey> serviceKeys = configuration.validatorKeys().stream()
          .map(ValidatorKey::serviceKey)
          .collect(toList());

      assertThat(serviceKeys).hasSize(validatorCount);
      assertThat(serviceKeys).contains(emulatedNodeServiceKey);
      HashCode zeroHashCode = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      assertThat(configuration.previousCfgHash()).isEqualTo(zeroHashCode);
      return null;
    });
  }

  @Test
  void getTransactionPool() throws Exception {
    TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
    RawTransaction rawTransaction = toRawTransaction(message);
    service.getNode().submitTransaction(rawTransaction);

    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);

      KeySetIndexProxy<HashCode> transactionPool = blockchain.getTransactionPool();
      assertThat(transactionPool.contains(message.hash())).isTrue();
      return null;
    });
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }

  private static TransactionMessage constructTestTransactionMessage(String payload, TestKit testKit) {
    EmulatedNode emulatedNode = testKit.getEmulatedNode();
    KeyPair emulatedNodeKeyPair = emulatedNode.getServiceKeyPair();
    return constructTestTransactionMessage(payload, emulatedNodeKeyPair);
  }

  private static TransactionMessage constructTestTransactionMessage(String payload) {
    return constructTestTransactionMessage(payload, KEY_PAIR);
  }

  private static TransactionMessage constructTestTransactionMessage(String payload, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(TestService.SERVICE_ID)
        .transactionId(TestTransaction.ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .sign(keyPair, CRYPTO_FUNCTION);
  }

  private static RawTransaction toRawTransaction(TransactionMessage transactionMessage) {
    return RawTransaction.newBuilder()
        .serviceId(transactionMessage.getServiceId())
        .transactionId(transactionMessage.getTransactionId())
        .payload(transactionMessage.getPayload())
        .build();
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
}
