# Migrating Java Services to 0.7

## Update the Service

### Update the References to Moved Classes

As some packages has been moved, the client code need to update the references.
The following regular expression will do the job:

```
Find: (com.exonum.binding)(.)(annotations|blockchain|proxy|runtime|service|storage|transaction|transport|util)

Replace: $1$2core$2$3
```

<!-- TODO: ## Explore the New Features --> 

[release-page]: https://github.com/exonum/exonum-java-binding/releases/tag/ejb/v0.7.0
