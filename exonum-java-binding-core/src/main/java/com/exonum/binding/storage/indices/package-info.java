/**
 * Contains Exonum indexes — persistent, named collections built on top of Exonum key-value storage.
 *
 * <p>Indexes are also known as collections, tables, and rarely as views for
 * a {@linkplain com.exonum.binding.storage.database.View database view} is inherently
 * associated with an index.
 *
 * <h2><a name="families">Index families</a></h2>
 * <p>An index family is a named group of indexes of the same type. Each index in the group
 * is identified by an <em>identifier</em>, an arbitrary byte string. An index in the group works
 * the same as an individual index. Indexes in a group are isolated from each other.
 * It is not possible to iterate through all elements that are stored inside an index group.
 *
 * <h3><a name="families-use-cases">Uses cases</a></h3>
 * <p>Index families provide a way to separate elements by a certain criterion. Applications include
 * indexing, where you create a separate collection group to index another collection of elements
 * by a certain criterion; referencing another collection Bar from elements of collection Foo,
 * where you keep an identifier into a collection in group Bar in a structure stored
 * in collection Foo.
 *
 * <h3><a name="families-limitations">Limitations</a></h3>
 * <p>Currently Exonum prepends an index identifier within a group to internal,
 * implementation-specific, keys of that index to keep their elements separate from each other.
 * The resulting database key includes the index identifier followed by the internal index key.
 * Such implementation of index separation implies that each index identifier withing a group
 * <strong>must not</strong> be a prefix of another index identifier in this group. The easiest
 * way to achieve that is to use fixed-length identifiers.
 *
 * <p>Until this limitation is fixed, care must be taken when using this feature, because
 * the identifiers are not checked.
 *
 * @see <a href="https://exonum.com/doc/architecture/storage/#table-types">Exonum indexes reference documentation</a>
 */
package com.exonum.binding.storage.indices;
