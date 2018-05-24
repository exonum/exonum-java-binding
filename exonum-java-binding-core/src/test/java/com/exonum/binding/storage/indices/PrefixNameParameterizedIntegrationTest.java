package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.TestParameters.parameters;
import static java.util.Arrays.asList;
import static org.junit.runners.Parameterized.Parameter;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.IndexConstructors.PartiallyAppliedIndexConstructor;
import com.exonum.binding.util.LibraryLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// todo: Move to BaseIndexProxyTestable when ECR-642 is resolved
// (currently not possible due to ExpectedException incompatibility)
@RunWith(Parameterized.class)
public class PrefixNameParameterizedIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @Parameter(0)
  public String name;

  @Parameter(1)
  public PartiallyAppliedIndexConstructor indexFactory;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testIndexCtor_ThrowsIfInvalidName() throws Exception {
    try (Cleaner cleaner = new Cleaner();
         Database database = MemoryDb.newInstance()) {
      Snapshot view = database.createSnapshot(cleaner);

      expectedException.expect(Exception.class);
      indexFactory.create(name, view);
    }
  }

  @Parameters(name = "{index}: '{0}'")
  public static Collection<Object[]> data() {
    return merge(
        asList(
            new Object[]{ null },
            parameters(""),
            parameters(" name"),
            parameters("name "),
            parameters("name 1"),
            parameters(" name "),
            parameters("?name"),
            parameters("name?"),
            parameters("na?me"),
            parameters("name#1"),
            parameters("name-1")),
        asList(
            parameters(IndexConstructors.from(ListIndexProxy::newInstance)),
            parameters(IndexConstructors.from(ProofListIndexProxy::newInstance)),
            parameters(IndexConstructors.from(MapIndexProxy::newInstance)),
            parameters(IndexConstructors.from(ProofMapIndexProxy::newInstance)),
            parameters(IndexConstructors.from(ValueSetIndexProxy::newInstance)),
            parameters(IndexConstructors.from(KeySetIndexProxy::newInstance)),
            parameters(IndexConstructors.from(EntryIndexProxy::newInstance))
        ));
  }

  @SuppressWarnings("unchecked")
  private static <T> Collection<T[]> merge(Collection<T[]> a, Collection<T[]> b) {
    int size = a.size() * b.size();
    List<T[]> merged = new ArrayList<>(size);
    for (T[] first : a) {
      for (T[] second : b) {
        T[] r = (T[]) new Object[first.length + second.length];
        System.arraycopy(first, 0, r, 0, first.length);
        System.arraycopy(second, 0, r, first.length, second.length);
        merged.add(r);
      }
    }
    return merged;
  }
}
