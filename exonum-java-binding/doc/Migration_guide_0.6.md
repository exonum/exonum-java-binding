# Migrating Java Service to 0.6

## Update the Service

### Service Module Requirements

Modify the service module so that it:
1. Is `public`
1. Extends [`AbstractServiceModule`][abstract-service-module-javadoc]
1. Has `@org.pf4j.Extension` annotation.

See the [dependencies management section][dep-management-docs] for details.

<details>
<summary>
An example of the ServiceModule in the cryptocurrency demo
</summary>

<!-- TODO: Or just link the file? -->

```java
package com.exonum.binding.cryptocurrency;

import com.exonum.binding.cryptocurrency.transactions.CryptocurrencyTransactionConverter;
import com.exonum.binding.service.AbstractServiceModule;
import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.Singleton;
import org.pf4j.Extension;

@Extension
public final class CryptocurrencyServiceModule extends AbstractServiceModule {

  @Override
  protected void configure() {
    bind(Service.class).to(CryptocurrencyServiceImpl.class);
    bind(CryptocurrencyService.class).to(CryptocurrencyServiceImpl.class).in(Singleton.class);
    bind(TransactionConverter.class).to(CryptocurrencyTransactionConverter.class);
  }
}
```

</details>

[abstract-service-module-javadoc]: https://todo.com
<!-- todo: Dependencies Management -->
[dep-management-docs]: https://todo.com

### Service Packaging Requirements
 
Exonum Java 0.6.0 brings a new packaging format of services. Service classes
and resources must be packaged into a single JAR archive together with some metadata.

To update an existing service:

1. Configure a `maven-assembly-plugin` or `maven-shade-plugin` to create
   a single JAR with all service dependencies.
   See [How to Build Service Artifact][how-to-build-docs] section for details.
1. Add the following manifest entries to the JAR:
    - "Plugin-Id" equal to `${project.groupId}:${project.artifactId}:${project.version}`
    - "Plugin-Version" equal to `${project.version}`
1. Add a dependency on PF4J with the "provided" scope.
1. Exclude framework dependencies, such as "exonum-java-binding-core",
   or "guice", from the service artifact by setting their scope
   to "provided". See the [documentation][using-libraries-docs]
   for a full list of libraries provided with the framework.


<details>
<summary>
Highlights of the project build definition from the cryptocurrency demo
</summary>

```xml
<project>
  
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.exonum.binding</groupId>
        <artifactId>exonum-java-binding-bom</artifactId>
        <version>${project.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  
  <dependencies>
    <dependency>
      <groupId>com.exonum.binding</groupId>
      <artifactId>exonum-java-binding-core</artifactId>
      <!-- The scope must be provided; version inherited from BOM -->
      <scope>provided</scope>
    </dependency>

    <dependency>
      <groupId>org.pf4j</groupId>
      <artifactId>pf4j</artifactId>
      <!-- The scope must be provided; version inherited from BOM -->
      <scope>provided</scope>
    </dependency>
     ⋮
  </dependencies>
  
  <build>
    <plugins>
       ⋮
      <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <executions>
          <execution>
            <id>package-service-artifact</id>
            <phase>package</phase>
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
          </descriptorRefs>
          <finalName>${project.artifactId}-${project.version}-artifact</finalName>
          <appendAssemblyId>false</appendAssemblyId>
          <archive>
            <manifestEntries>
              <Plugin-Id>${project.groupId}:${project.artifactId}:${project.version}</Plugin-Id>
              <Plugin-Version>${project.version}</Plugin-Version>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>
       ⋮
    </plugins>
  </build>
    ⋮  
</project>
```

</details>

<!-- TODO: Using Libraries in the docs -->
[using-libraries-docs]: https://todo.com
<!-- TODO: How to Build a Service Artifact in the docs -->
[how-to-build-docs]: https://todo.com

### Use Example

See a complete diff of changes applied to the cryptocurrency-demo service
to support the new service artifact format. Your service implementation
is likely to require similar changes:

```
git clone git@github.com:exonum/exonum-java-binding.git
cd exonum-java-binding/exonum-java-binding/cryptocurrency-demo
git diff ejb/v0.5.0 ejb/v0.6.0 .
```

## Update the Node Configuration

Below are the highlights of the changes in the node configuration:

1. It is no longer required to pass either the system or the service classpath.
   Paths to service artifacts are specified in the "ejb_app_services.toml" 
   configuration file in the *working directory* of each node application.
1. Some configuration options are now set within different configuration phases.

See details in the [network configuration tutorial][config-tutorial].

<!-- todo: link app tutorial or the (non-existent at the moment) section on the website -->
[config-tutorial]: http://todo

## Update the Clients

Update your client applications to use the new services API prefix `/api/services/` 
(it used to be `/api`). For example, `/api/my-timestamping` becomes `/api/services/my-timestamping`.

## Use the Application

Exonum 0.6.0 comes with pre-built application packages, available
on the [release page][release-page]. It is no longer required to build an application, 
you can use a pre-built one. However, it may still make sense to build it 
on target architecture to benefit from an extended instruction set and better optimizations. 

[release-page]: https://github.com/exonum/exonum-java-binding/releases/tag/ejb/v0.6.0