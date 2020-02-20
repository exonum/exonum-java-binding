/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ${package};

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.runtime.ServiceArtifactId;
import com.exonum.binding.testkit.TestKit;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class MyServiceIntegrationTest {

  public static final String ARTIFACT_FILENAME = getRequiredProperty("it.artifactFilename");
  public static final ServiceArtifactId ARTIFACT_ID =
      ServiceArtifactId.parseFrom(getRequiredProperty("it.exonumArtifactId"));
  public static Path artifactsDirectory = Paths.get(getRequiredProperty("it.artifactsDirectory"));
  public static final String SERVICE_NAME = "my-service";
  public static final int SERVICE_ID = 42;

  /**
   * This is an example service integration test with Exonum Testkit. It simply verifies
   * that a Service can be instantiated by Testkit, and that the libraries required for Testkit
   * operation are accessible.
   *
   * <p>If you get an UnsatisfiedLinkError in this test — please check that the EXONUM_HOME
   * environment variable is set properly:
   * https://exonum.com/doc/version/0.13-rc.2/get-started/java-binding/#after-install
   */
  @Test
  void testGenesisBlockCommit() {
    try (TestKit testKit = TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID)
        .withArtifactsDirectory(artifactsDirectory)
        .build()) {
      // Check that genesis block was committed
      testKit.withSnapshot((snapshot) -> {
        Blockchain blockchain = Blockchain.newInstance(snapshot);
        assertThat(blockchain.getBlockHashes().size(), equalTo(1L));
      });
    }
  }

  private static String getRequiredProperty(String key) {
    String property = System.getProperty(key);
    checkState(!Strings.isNullOrEmpty(property),
        "Absent property: %s=%s", key, property);
    return property;
  }
}
