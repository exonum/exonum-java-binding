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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * A builder of service artifacts in PF4J format. Intended to be used in various integration tests.
 * Allows to create malformed artifacts (with missing classes, extensions, incorrect or incomplete
 * metadata).
 *
 * <p>Methods are non-null by default.
 */
public final class ServiceArtifactBuilder {

  @VisibleForTesting
  static final String PLUGIN_ID_ATTRIBUTE_NAME = "Plugin-Id";
  @VisibleForTesting
  static final String PLUGIN_VERSION_ATTRIBUTE_NAME = "Plugin-Version";
  @VisibleForTesting
  static final String EXTENSIONS_INDEX_NAME = "META-INF/extensions.idx";

  private final Manifest manifest;
  private final Set<Class<?>> artifactClasses;
  /** Entries to put into the list of extensions. */
  private final LinkedHashSet<String> extensions;

  /** Creates a new artifact builder with empty manifest, no classes and no extensions. */
  public ServiceArtifactBuilder() {
    manifest = new Manifest();
    artifactClasses = new HashSet<>();
    extensions = new LinkedHashSet<>();

    manifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
  }

  /**
   * Sets a PF4J plugin identifier.
   * @param pluginId a plugin id to set; in Exonum it must have a certain format, but this class
   *     allows any to be set
   * @see <a href="https://pf4j.org/doc/plugins.html#how-plugin-metadata-is-defined">Plugin meta data</a>
   */
  public ServiceArtifactBuilder setPluginId(String pluginId) {
    return setManifestEntry(PLUGIN_ID_ATTRIBUTE_NAME, pluginId);
  }

  /**
   * Sets a PF4J plugin version.
   * @param version a plugin version to set
   * @see <a href="https://pf4j.org/doc/plugins.html#how-plugin-metadata-is-defined">Plugin meta data</a>
   */
  public ServiceArtifactBuilder setPluginVersion(String version) {
    return setManifestEntry(PLUGIN_VERSION_ATTRIBUTE_NAME, version);
  }

  /**
   * Sets a manifest entry in the {@linkplain Manifest main section} of the manifest.
   * @param name an attribute name
   * @param value an attribute value
   * @throws IllegalArgumentException if the attribute name is
   *     {@linkplain java.util.jar.Attributes invalid}
   */
  public ServiceArtifactBuilder setManifestEntry(String name, String value) {
    manifest.getMainAttributes().putValue(name, value);
    return this;
  }

  /**
   * Adds classes to the archive.
   * @param artifactClasses classes to add
   */
  public ServiceArtifactBuilder addClasses(Class<?>... artifactClasses) {
    return addClasses(asList(artifactClasses));
  }

  /**
   * Adds classes to the archive.
   * @param artifactClasses classes to add
   */
  public ServiceArtifactBuilder addClasses(Iterable<? extends Class<?>> artifactClasses) {
    artifactClasses.forEach(this::addClass);
    return this;
  }

  /**
   * Adds a class to the archive.
   * @param artifactClass a class to add
   */
  public ServiceArtifactBuilder addClass(Class<?> artifactClass) {
    checkValidClass(artifactClass);
    artifactClasses.add(artifactClass);
    return this;
  }

  private static void checkValidClass(Class<?> artifactClass) {
    checkArgument(!(artifactClass.isPrimitive() || artifactClass.isArray()),
        "Cannot write %s to the archive", artifactClass);
  }

  /**
   * Adds an entry to put in the extensions index file.
   * If no entries are added, an empty file will be written.
   * @see <a href="https://pf4j.org/doc/extensions.html#about-extensions">Extensions</a>
   */
  public ServiceArtifactBuilder addExtensionEntry(String extensionEntry) {
    extensions.add(checkNotNull(extensionEntry));
    return this;
  }

  /**
   * Writes the JAR artifact to the specified location.
   * @param artifactLocation a filesystem path where to put an artifact archive
   * @throws IOException if the archive file cannot be written
   */
  public void writeTo(Path artifactLocation) throws IOException {
    try (JarOutputStream out = new JarOutputStream(new BufferedOutputStream(
        new FileOutputStream(artifactLocation.toFile())),
        manifest)) {
      // Write directories corresponding to the class packages
      writePackages(out);

      // Write classes contents
      writeClasses(out);

      // Write extensions index
      writeExtensionIndex(out);
    }
  }

  private void writePackages(JarOutputStream out) {
    Stream<String> packageDirs = artifactClasses.stream()
        .map(Class::getPackage)
        .distinct()
        .map(this::getPath);

    packageDirs.forEach(path -> {
      ZipEntry packageEntry = new ZipEntry(path);
      try {
        out.putNextEntry(packageEntry);
        out.closeEntry();
      } catch (IOException e1) {
        throw new RuntimeException(e1);
      }
    });
  }

  @SuppressWarnings("UnstableApiUsage") // That's internal code, so `Beta` APIs are OK.
  private void writeClasses(JarOutputStream out) throws IOException {
    for (Class<?> serviceClass : artifactClasses) {
      String path = getPath(serviceClass);
      ZipEntry classEntry = new ZipEntry(path);
      out.putNextEntry(classEntry);

      InputStream classDataStream = serviceClass.getClassLoader().getResourceAsStream(path);
      checkNotNull(classDataStream, "classDataStream for %s, path=%s", serviceClass, path);
      ByteStreams.copy(classDataStream, out);
      out.closeEntry();
    }
  }

  private void writeExtensionIndex(JarOutputStream out) throws IOException {
    ZipEntry indexEntry = new ZipEntry(EXTENSIONS_INDEX_NAME);
    out.putNextEntry(indexEntry);

    PrintWriter extensionsOut = new PrintWriter(out);
    for (String extension : extensions) {
      extensionsOut.println(extension);
    }
    extensionsOut.flush();
    out.closeEntry();
  }

  private String getPath(Package p) {
    String packageName = p.getName();
    return packageName.replace('.', '/') + '/';
  }

  private String getPath(Class<?> serviceClass) {
    String name = serviceClass.getName();
    return name.replace('.', '/') + ".class";
  }
}
