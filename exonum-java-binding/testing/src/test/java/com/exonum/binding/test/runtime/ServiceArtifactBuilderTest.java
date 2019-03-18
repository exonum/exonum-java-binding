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

package com.exonum.binding.test.runtime;

import static com.exonum.binding.test.runtime.ServiceArtifactBuilder.EXTENSIONS_INDEX_NAME;
import static com.exonum.binding.test.runtime.ServiceArtifactBuilder.PLUGIN_ID_ATTRIBUTE_NAME;
import static com.exonum.binding.test.runtime.ServiceArtifactBuilder.PLUGIN_VERSION_ATTRIBUTE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.test.Bytes;
import com.exonum.binding.test.runtime.TestService.Inner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;

class ServiceArtifactBuilderTest {

  private Path jarPath;

  @BeforeEach
  void setUp(@TempDir Path tempDir, TestReporter reporter) {
    jarPath = tempDir.resolve("test.jar");
    reporter.publishEntry("Test JAR path", jarPath.toString());
  }

  @Test
  void createArtifactSingleClass() throws IOException {
    Class<TestService> testClass = TestService.class;
    new ServiceArtifactBuilder()
        .addClass(testClass)
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries).containsEntry(getPath(testClass), readClass(testClass));
  }

  @Test
  void createArtifactSingleInnerClass() throws IOException {
    Class<Inner> testClass = Inner.class;
    new ServiceArtifactBuilder()
        .addClass(testClass)
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries)
        .containsEntry(getPath(testClass), readClass(testClass));
  }

  @Test
  void createArtifactSeveralClasses() throws IOException {
    new ServiceArtifactBuilder()
        .addClass(ImmutableList.class)
        .addClasses(TestService.class, Bytes.class)
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries).containsAllEntriesOf(ImmutableMap.of(
            getPath(TestService.class), readClass(TestService.class),
            getPath(Bytes.class), readClass(Bytes.class),
            getPath(ImmutableList.class), readClass(ImmutableList.class)));
  }

  @Test
  void createArtifactIllegalClass() {
    ServiceArtifactBuilder builder = new ServiceArtifactBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.addClass(int.class));
    assertThrows(IllegalArgumentException.class, () -> builder.addClasses(TestService.class,
        int.class));
    assertThrows(IllegalArgumentException.class, () -> builder.addClasses(TestService[].class));
  }

  @Test
  void createArtifactWithExtensionClass() throws IOException {
    new ServiceArtifactBuilder()
        .addExtensionClass(TestService.class)
        .addExtensionClasses(ImmutableList.class)
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries).containsAllEntriesOf(ImmutableMap.of(
        getPath(TestService.class), readClass(TestService.class),
        getPath(ImmutableList.class), readClass(ImmutableList.class)));

    assertThat(allJarEntries).containsKey(EXTENSIONS_INDEX_NAME);
    String allExtensions = getExtensionsIndex(allJarEntries);

    String expectedExtensions = TestService.class.getName() + "\n"
        + ImmutableList.class.getName() + "\n";
    assertThat(allExtensions).isEqualTo(expectedExtensions);
  }

  @Test
  void createArtifactEmptyExtensions() throws IOException {
    new ServiceArtifactBuilder()
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries).containsAllEntriesOf(ImmutableMap.of(
        EXTENSIONS_INDEX_NAME, new byte[0]));
  }

  @Test
  void createArtifactSeveralExtensions() throws IOException {
    new ServiceArtifactBuilder()
        .addExtensionEntry("e1")
        .addExtensionEntry("e2")
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries).containsKey(EXTENSIONS_INDEX_NAME);
    String allExtensions = getExtensionsIndex(allJarEntries);

    assertThat(allExtensions).isEqualTo("e1\ne2\n");
  }

  @Test
  void setExtensionClassesOverwritesPreviouslySet() throws IOException {
    new ServiceArtifactBuilder()
        .addExtensionEntry("already added extension")
        .addExtensionClass(TestService.class)
        .setExtensionClasses(ImmutableList.of(ImmutableList.class))
        .writeTo(jarPath);

    Map<String, byte[]> allJarEntries = readJarEntries(jarPath);

    assertThat(allJarEntries).containsKey(EXTENSIONS_INDEX_NAME);
    String allExtensions = getExtensionsIndex(allJarEntries);

    assertThat(allExtensions).isEqualTo(ImmutableList.class.getName() + '\n');
  }

  @Test
  void createArtifactWithManifestFields() throws IOException {
    new ServiceArtifactBuilder()
        .setPluginId("foo-service")
        .setPluginVersion("1.0.2")
        .setManifestEntry("Exonum-Version", "3.1.0")
        .writeTo(jarPath);

    Manifest manifest = readJarManifest(jarPath);

    Attributes mainAttributes = manifest.getMainAttributes();
    assertThat(mainAttributes.getValue(PLUGIN_ID_ATTRIBUTE_NAME)).isEqualTo("foo-service");
    assertThat(mainAttributes.getValue(PLUGIN_VERSION_ATTRIBUTE_NAME)).isEqualTo("1.0.2");
    assertThat(mainAttributes.getValue("Exonum-Version")).isEqualTo("3.1.0");
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Map<String, byte[]> readJarEntries(Path jarPath) throws IOException {
    Map<String, byte[]> allJarEntries = new TreeMap<>();
    try (JarInputStream in = new JarInputStream(new FileInputStream(jarPath.toFile()))) {
      // Add manifest
      byte[] manifest = manifestAsBytes(in);
      allJarEntries.put(JarFile.MANIFEST_NAME, manifest);

      // Add other entries
      JarEntry nextEntry;
      while ((nextEntry = in.getNextJarEntry()) != null) {
        String name = nextEntry.getName();
        byte[] bytes = ByteStreams.toByteArray(in);
        allJarEntries.put(name, bytes);
        in.closeEntry();
      }
    }
    return allJarEntries;
  }

  private static byte[] manifestAsBytes(JarInputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    in.getManifest().write(out);
    return out.toByteArray();
  }

  @SuppressWarnings({"UnstableApiUsage", "ConstantConditions"})
  private static byte[] readClass(Class<?> c) throws IOException {
    // Instead of duplicating the code reading a class we could alternatively add some
    // `ClassWriter` or `ClassReader` interface, and inject a mock of it in ServiceArtifactBuilder
    // in tests, but the former seems easier.
    String classPath = getPath(c);
    InputStream classDataStream = c.getClassLoader().getResourceAsStream(classPath);
    return ByteStreams.toByteArray(classDataStream);
  }

  private static String getPath(Class<?> c) {
    return c.getName().replace('.', '/') + ".class";
  }

  private static Manifest readJarManifest(Path jarPath) throws IOException {
    try (JarInputStream in = new JarInputStream(new BufferedInputStream(
        new FileInputStream(jarPath.toFile())))) {
      return in.getManifest();
    }
  }

  private String getExtensionsIndex(Map<String, byte[]> allJarEntries) {
    return new String(allJarEntries.get(EXTENSIONS_INDEX_NAME),
        StandardCharsets.UTF_8);
  }
}
