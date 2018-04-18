package com.exonum.binding.qaservice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.storage.indices.MapIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.errorprone.annotations.MustBeClosed;

/**
 * A schema of the QA service.
 *
 * <p>Has two collections:
 * (a) values of the counters (Merklized),
 * (b) names of the counters.
 */
public final class QaSchema implements Schema {

  /** A namespace of QA service collections. */
  private static final String NAMESPACE = QaService.NAME.replace('-', '_');

  private final ViewProxy view;

  public QaSchema(ViewProxy view) {
    this.view = checkNotNull(view);
  }

  /**
   * Returns a proof map of counter values. Must be closed.
   */
  @MustBeClosed
  public ProofMapIndexProxy<HashCode, Long> counters() {
    String name = fullIndexName("counters");
    return new ProofMapIndexProxy<>(name, view, StandardSerializers.hash(),
        StandardSerializers.longs());
  }

  /**
   * Returns a map of counter names. Must be closed.
   */
  @MustBeClosed
  public MapIndex<HashCode, String> counterNames() {
    String name = fullIndexName("counterNames");
    return new MapIndexProxy<>(name, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  private static String fullIndexName(String name) {
    return NAMESPACE + "__" + name;
  }
}
