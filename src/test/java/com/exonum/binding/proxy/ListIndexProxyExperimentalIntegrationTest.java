package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.ListIndexProxyExperimentalIntegrationTest.LIST_PREFIX;
import static com.exonum.binding.test.TestStorageItems.bytes;
import static java.util.Arrays.asList;

import com.exonum.binding.test.TestStorageItems;
import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.testing.AbstractTester;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.testers.CollectionAddAllTester;
import com.google.common.collect.testing.testers.CollectionContainsAllTester;
import com.google.common.collect.testing.testers.CollectionContainsTester;
import com.google.common.collect.testing.testers.CollectionRemoveAllTester;
import com.google.common.collect.testing.testers.CollectionRemoveIfTester;
import com.google.common.collect.testing.testers.CollectionRemoveTester;
import com.google.common.collect.testing.testers.CollectionRetainAllTester;
import com.google.common.collect.testing.testers.CollectionToArrayTester;
import com.google.common.collect.testing.testers.ListAddAllAtIndexTester;
import com.google.common.collect.testing.testers.ListAddAllTester;
import com.google.common.collect.testing.testers.ListIndexOfTester;
import com.google.common.collect.testing.testers.ListLastIndexOfTester;
import com.google.common.collect.testing.testers.ListRemoveAllTester;
import com.google.common.collect.testing.testers.ListRemoveAtIndexTester;
import com.google.common.collect.testing.testers.ListRemoveTester;
import com.google.common.collect.testing.testers.ListRetainAllTester;
import com.google.common.collect.testing.testers.ListSubListTester;
import com.google.common.collect.testing.testers.ListToArrayTester;
import com.google.common.primitives.Ints;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ListIndexProxyExperimentalIntegrationTest extends TestCase {

  static {
    LibraryLoader.load();
  }

  static final byte[] LIST_PREFIX = bytes("test list");

  public static Test suite() {
    LibraryLoader.load();
    TestSuite tests = new TestSuite();
    ListTestSuiteBuilder<byte[]> builder = ListTestSuiteBuilder
        .using(new ByteListGenerator())
        .named("ListIndexProxy")
        // todo: consider
//        .withSetUp()
//        .withTearDown()
        .withFeatures(
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.NON_STANDARD_TOSTRING,
            CollectionFeature.SUPPORTS_ADD,
            CollectionSize.ANY,
            ListFeature.SUPPORTS_SET)
        .suppressing(unsupportedListMethods());
    tests.addTest(builder.createTestSuite());
    return tests;
  }

  private static Collection<Method> unsupportedListMethods() {
    // Print unsupported:
//    List<Method> unsupportedMethods = Stream.of(ListIndexAdapter.class.getDeclaredMethods())
//        .filter((m) -> m.getAnnotation(Unsupported.class) != null)
//        .collect(Collectors.toList());

    List<Class<? extends AbstractTester>> excludedTesters = asList(
        // todo: add missing
        CollectionContainsTester.class,
        ListAddAllAtIndexTester.class,
        ListToArrayTester.class,
        CollectionToArrayTester.class,
        CollectionRemoveTester.class,
        CollectionContainsAllTester.class,
        CollectionAddAllTester.class,
        ListAddAllTester.class,
        ListAddAllAtIndexTester.class,
        ListRemoveAllTester.class,
        CollectionRemoveAllTester.class,
        CollectionRemoveIfTester.class,
        ListRetainAllTester.class,
        CollectionRetainAllTester.class,
        ListRemoveTester.class,
        ListRemoveAtIndexTester.class,
        ListIndexOfTester.class,
        ListLastIndexOfTester.class,
        ListSubListTester.class
    );
    return excludedTesters.stream()
        .flatMap((c) -> Stream.of(c.getDeclaredMethods()))
        .collect(Collectors.toSet());
  }
}

class ByteListGenerator implements TestListGenerator<byte[]> {
  private final Set<ListIndex> createdIndices = new HashSet<>();
  private final Set<Fork> createdForks = new HashSet<>();
  private final Database database;

  ByteListGenerator() {
    database = new MemoryDb();
  }

  @Override
  public SampleElements<byte[]> samples() {
    return new SampleElements<>(
        TestStorageItems.V1,
        TestStorageItems.V2,
        TestStorageItems.V3,
        TestStorageItems.V4,
        TestStorageItems.V5
    );
  }

  @Override
  public List<byte[]> create(Object... elements) {
    Fork fork = createFork();
    ListIndex proxy = createList(fork);
    return new ListIndexAdapter(proxy);
  }

  private Fork createFork() {
    Fork f =  database.createFork();
    createdForks.add(f);
    return f;
  }

  private ListIndex createList(Fork fork) {
    ListIndex list = new ListIndexProxy(LIST_PREFIX, fork);
    createdIndices.add(list);
    return list;
  }

  @Override
  public byte[][] createArray(int length) {
    return new byte[length][];
  }

  @Override
  public Iterable<byte[]> order(List<byte[]> insertionOrder) {
    return insertionOrder;
  }

  @Override
  protected void finalize() throws Throwable {
    createdIndices.forEach(NativeProxy::close);
    createdForks.forEach(NativeProxy::close);
    database.close();
    super.finalize();
  }
}

class ListIndexAdapter implements List<byte[]> {

  private final ListIndex proxy;

  ListIndexAdapter(ListIndex proxy) {
    this.proxy = proxy;
  }

  @Override
  public int size() {
    return Ints.checkedCast(proxy.size());
  }

  @Override
  public boolean isEmpty() {
    return proxy.isEmpty();
  }

  @Override
  @Unsupported
  public boolean contains(Object o) {
    // inter-op
    return false;
  }

  @Override
  public Iterator<byte[]> iterator() {
    // fixme: leaks native iterator
    return proxy.iterator();
  }

  @Override
  @Unsupported
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(byte[] bytes) {
    proxy.add(bytes);
    return true;
  }

  @Override
  @Unsupported
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public boolean containsAll(Collection<?> c) {
    // inter-op
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public boolean addAll(Collection<? extends byte[]> c) {
    // inter-op, easy pick
    c.forEach(proxy::add);
    return true;
  }

  @Override
  @Unsupported
  public boolean addAll(int index, Collection<? extends byte[]> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    proxy.clear();
  }

  @Override
  public byte[] get(int index) {
    return proxy.get(index);
  }

  @Override
  public byte[] set(int index, byte[] element) {
    byte[] previous = proxy.get(index);
    proxy.set(index, element);
    return previous;
  }

  @Override
  @Unsupported
  public void add(int index, byte[] element) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public byte[] remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public int indexOf(Object o) {
    // inter-op
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public int lastIndexOf(Object o) {
    // inter-op
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public ListIterator<byte[]> listIterator() {
    // inter-op
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public ListIterator<byte[]> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Unsupported
  public List<byte[]> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }
}

@Target({
    ElementType.METHOD,
})
@Retention(RetentionPolicy.RUNTIME)
@interface Unsupported {}