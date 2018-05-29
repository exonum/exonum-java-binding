package ${groupId};

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code MySchema} provides access to the tables of {@link $.MyService},
 * given a database state: a {@link View}.
 *
 * @see <a href="https://exonum.com/doc/architecture/storage/#table-types">Exonum table types.</a>
 */
public final class MySchema implements Schema {

  private final View view;

  public MySchema(View view) {
    this.view = checkNotNull(view);
  }

  @Override
  public List<HashCode> getStateHashes() {
    // You shall usually return a list of the state hashes
    // of all Merklized collections of this service,
    // see https://exonum.com/doc/architecture/storage/#merkelized-indices
    return Collections.emptyList();
  }
}
