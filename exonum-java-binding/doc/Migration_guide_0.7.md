# Migrating Java Services to 0.7

## Update the Service

### Update the References to Moved Classes

As some packages has been moved, the client code need to update the references.
The following regular expression will do the job:

```
Find: (com.exonum.binding)(.)(annotations|blockchain|proxy|runtime|service|storage|transaction|transport|util)

Replace: $1$2core$2$3
```

## Explore the New Features

0.7.0 brings Exonum Testkit, which allows to test service operations in an emulated blockchain
network. See the [documentation][testkit-documentation] for more information and examples.

## See Also

The 0.7.0 [release page][release-page] for the changelog and pre-built binaries.

[release-page]: https://github.com/exonum/exonum-java-binding/releases/tag/ejb/v0.7.0
[testkit-documentation]: https://exonum.com/doc/version/0.11/get-started/java-binding/#testing   
