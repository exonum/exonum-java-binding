# Migrating Java Service to 0.6

## Update the Service

### Service Module Requirements

Modify the service module so that it:
1. Is `public`
1. Extends [`AbstractServiceModule`][abstract-service-module-javadoc]
1. Has `@org.pf4j.Extension` annotation.

See the [dependencies management section][dep-management-docs] for details.

[abstract-service-module-javadoc]: https://todo.com
<!-- todo: Dependencies Management -->
[dep-management-docs]: https://todo.com

### Service Packaging Requirements
 
Exonum Java 0.6.0 brings a new packaging format of services. Service
classes and resources must be packaged in a single JAR archive
with some metadata set.

To update an existing service:

1. Configure a maven-assembly-plugin or maven-shade-plugin to create
   a single JAR with all service dependencies.
   See [How to Build Service Artifact][how-to-build-docs] section for details.
1. Add the following manifest entries to the JAR:
    - "Plugin-Id" equal to "${project.groupId}:${project.artifactId}:${project.version}"
    - "Plugin-Version" equal to "${project.version}"
1. Add a "provided"-scoped dependency on PF4J.
1. Exclude framework dependencies, such as "exonum-java-binding-core",
   or "guice", from the service artifact by specifying their scope
   as "provided". See the [documentation][using-libraries-docs]
   for a full list of libraries provided with the framework.

<!-- TODO: Using Libraries in the docs -->
[using-libraries-docs]: https://todo.com
<!-- TODO: How to Build a Service Artifact in the docs -->
[how-to-build-docs]: https://todo.com

### Use Example
Below is a diff of changes applied to the cryptocurrency-demo service
to support the new service artifact format. Your service implementation
is likely to require similar changes:

<details>
<summary>
Changes to cryptocurrency service since 0.5.0
</summary>

<!-- This diff is not the easiest thing to read, maybe, just give
a git diff command (the output will, however, include some unrelated
changes? --> 
```
Index: exonum-java-binding/cryptocurrency-demo/pom.xml
IDEA additional info:
Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
<+>UTF-8
===================================================================
--- exonum-java-binding/cryptocurrency-demo/pom.xml	(date 1552387521000)
+++ exonum-java-binding/cryptocurrency-demo/pom.xml	(date 1552405034000)
@@ -60,11 +60,22 @@
     <dependency>
       <groupId>com.google.code.gson</groupId>
       <artifactId>gson</artifactId>
+      <scope>provided</scope>
     </dependency>
 
     <dependency>
       <groupId>com.google.protobuf</groupId>
       <artifactId>protobuf-java</artifactId>
+      <scope>provided</scope>
+    </dependency>
+
+    <dependency>
+      <groupId>org.pf4j</groupId>
+      <artifactId>pf4j</artifactId>
+      <scope>provided</scope>
     </dependency>
 
     <dependency>
@@ -152,23 +163,31 @@
         </executions>
       </plugin>
 
-      <!-- Generates a runtime classpath file of ejb-cryptocurrency.
-           Bound to the default phase (generate-sources).
-           FIXME: a service may be packaged in a fat jar probably? -->
       <plugin>
-        <artifactId>maven-dependency-plugin</artifactId>
-        <configuration>
-          <outputFile>${project.build.directory}/cryptocurrency-classpath.txt</outputFile>
-          <includeScope>runtime</includeScope>
-        </configuration>
+        <artifactId>maven-assembly-plugin</artifactId>
         <executions>
           <execution>
-            <id>generate-classpath-file</id>
+            <id>package-service-artifact</id>
+            <phase>package</phase>
             <goals>
-              <goal>build-classpath</goal>
+              <goal>single</goal>
             </goals>
           </execution>
         </executions>
+        <configuration>
+          <descriptorRefs>
+            <descriptorRef>jar-with-dependencies</descriptorRef>
+          </descriptorRefs>
+          <finalName>${project.artifactId}-${project.version}-artifact</finalName>
+          <appendAssemblyId>false</appendAssemblyId>
+          <archive>
+            <manifestEntries>
+              <Plugin-Id>${project.groupId}:${project.artifactId}:${project.version}</Plugin-Id>
+              <Plugin-Version>${project.version}</Plugin-Version>
+              <Plugin-Provider>${project.groupId}</Plugin-Provider>
+            </manifestEntries>
+          </archive>
+        </configuration>
       </plugin>
 
       <!-- Skip the deployment of internal module as it is inherited from parent pom -->
Index: exonum-java-binding/cryptocurrency-demo/src/main/java/com/exonum/binding/cryptocurrency/ServiceModule.java
===================================================================
--- exonum-java-binding/cryptocurrency-demo/src/main/java/com/exonum/binding/cryptocurrency/ServiceModule.java	(date 1552387521000)
+++ exonum-java-binding/cryptocurrency-demo/src/main/java/com/exonum/binding/cryptocurrency/CryptocurrencyServiceModule.java	(date 1552405034000)
@@ -17,13 +17,14 @@
 package com.exonum.binding.cryptocurrency;
 
 import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionConverter;
+import com.exonum.binding.service.AbstractServiceModule;
 import com.exonum.binding.service.Service;
 import com.exonum.binding.service.TransactionConverter;
-import com.google.inject.AbstractModule;
 import com.google.inject.Singleton;
+import org.pf4j.Extension;
 
-@SuppressWarnings("unused") // Instantiated through reflection by Guice.
-public final class ServiceModule extends AbstractModule {
+@Extension
+public final class CryptocurrencyServiceModule extends AbstractServiceModule {
 
   @Override
   protected void configure() 

```

</details>

## Update the node configuration

Below are the highlights of the changes in node configuration:

1. It is no longer needed to pass the system nor service classpath.
   Paths to service artifacts are specified in "ejb_app_services.toml" 
   configuration file in the *working directory* of each node application.
1. Some configuration options were moved between configuration phases.

See details in the [network configuration tutorial][config-tutorial].

<!-- todo: link app tutorial or the (non-existent at the moment) section on the website -->
[config-tutorial]: http://todo
