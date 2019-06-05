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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockchainIntegrationTest {

  private static final CryptoFunction CRYPTO_FUNCTION = CryptoFunctions.ed25519();
  private static final KeyPair KEY_PAIR = CRYPTO_FUNCTION.generateKeyPair();
  private static final short VALIDATOR_COUNT = 1;
  private static final HashCode ZERO_HASH_CODE = HashCode.fromBytes(
      new byte[DEFAULT_HASH_SIZE_BYTES]);

  private TestKit testKit;
  private Block block;
  private TransactionMessage expectedBlockTransaction;

  @BeforeEach
  void setUp() {
    testKit = TestKit.builder(EmulatedNodeType.VALIDATOR)
        .withService(TestServiceModule.class)
        .withValidators(VALIDATOR_COUNT)
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
    testKitTest((blockchain) -> {
      assertThat(blockchain.containsBlock(block)).isTrue();
    });
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
      HashCode expectedHash = block.getBlockHash();
      ListIndex<HashCode> blockHashes = blockchain.getBlockHashes();
      // Contains both genesis and committed blocks
      HashCode actualHash = blockHashes.get(1);
      assertThat(actualHash).isEqualTo(expectedHash);
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
      // Should include one executed and one in-pool (submitted in afterCommit) transaction
      assertThat(txMessagesMap).hasSize(2);
      assertThat(txMessagesMap.get(expectedBlockTransaction.hash()))
          .isEqualTo(expectedBlockTransaction);
    });
  }

  @Test
  void getTxResults() {
    testKitTest((blockchain) -> {
      ProofMapIndexProxy<HashCode, TransactionResult> txResults = blockchain.getTxResults();
      Map<HashCode, TransactionResult> txResultsMap = toMap(txResults);
      Map<HashCode, TransactionResult> expected =
          ImmutableMap.of(expectedBlockTransaction.hash(), TransactionResult.successful());
      assertThat(txResultsMap).isEqualTo(expected);
    });
  }

  @Test
  void getTxResult() {
    testKitTest((blockchain) -> {
      Optional<TransactionResult> txResult =
          blockchain.getTxResult(expectedBlockTransaction.hash());
      assertThat(txResult).hasValue(TransactionResult.successful());
    });
  }

  @Test
  void getTxResultOfUnknownTx() {
    testKitTest((blockchain) -> {
      HashCode unknownHash = HashCode.fromBytes(new byte[DEFAULT_HASH_SIZE_BYTES]);
      Optional<TransactionResult> txResult = blockchain.getTxResult(unknownHash);
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
      long genesisBlockHeight = 0;
      Block genesisBlock = blockchain.getBlock(0);
      assertThat(genesisBlock.getHeight()).isEqualTo(genesisBlockHeight);
      assertThat(genesisBlock.getPreviousBlockHash()).isEqualTo(ZERO_HASH_CODE);
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
  void getActualConfiguration() {
    testKitTest((blockchain) -> {
      StoredConfiguration configuration = blockchain.getActualConfiguration();
      List<ValidatorKey> validatorKeys = configuration.validatorKeys();
      // Check the number of validator keys
      assertThat(validatorKeys).hasSize(VALIDATOR_COUNT);

      // Check the public key of the emulated node is included
      List<PublicKey> serviceKeys = validatorKeys.stream()
          .map(ValidatorKey::serviceKey)
          .collect(toList());
      EmulatedNode emulatedNode = testKit.getEmulatedNode();
      PublicKey emulatedNodeServiceKey = emulatedNode.getServiceKeyPair().getPublicKey();
      List<PublicKey> expectedKeys = ImmutableList.of(emulatedNodeServiceKey);
      assertThat(serviceKeys).isEqualTo(expectedKeys);

      // Check the previous config is empty
      assertThat(configuration.previousCfgHash()).isEqualTo(ZERO_HASH_CODE);
    });
  }

  @Test
  void getTransactionPool() throws Exception {
    TestService service = testKit.getService(TestService.SERVICE_ID, TestService.class);
    TransactionMessage message = constructTestTransactionMessage("Test message", testKit);
    RawTransaction rawTransaction = RawTransaction.fromMessage(message);
    service.getNode().submitTransaction(rawTransaction);

    testKitTest((blockchain) -> {
      KeySetIndexProxy<HashCode> transactionPool = blockchain.getTransactionPool();
      assertThat(transactionPool.contains(message.hash()))
          .describedAs("pool=%s", transactionPool)
          .isTrue();
    });
  }

  private void testKitTest(Consumer<Blockchain> test) {
    testKit.withSnapshot((view) -> {
      Blockchain blockchain = Blockchain.newInstance(view);
      test.accept(blockchain);

      return null;
    });
  }

  private <K, V> Map<K, V> toMap(MapIndex<K, V> mapIndex) {
    return Maps.toMap(mapIndex.keys(), mapIndex::get);
  }

  private static TransactionMessage constructTestTransactionMessage(
      String payload, TestKit testKit) {
    EmulatedNode emulatedNode = testKit.getEmulatedNode();
    KeyPair emulatedNodeKeyPair = emulatedNode.getServiceKeyPair();
    return constructTestTransactionMessage(payload, emulatedNodeKeyPair);
  }

  private static TransactionMessage constructTestTransactionMessage(String payload) {
    return constructTestTransactionMessage(payload, KEY_PAIR);
  }

  private static TransactionMessage constructTestTransactionMessage(
      String payload, KeyPair keyPair) {
    return TransactionMessage.builder()
        .serviceId(TestService.SERVICE_ID)
        .transactionId(TestTransaction.ID)
        .payload(payload.getBytes(BODY_CHARSET))
        .sign(keyPair, CRYPTO_FUNCTION);
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
