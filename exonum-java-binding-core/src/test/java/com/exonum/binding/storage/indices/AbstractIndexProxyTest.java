package com.exonum.binding.storage.indices;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewModificationCounter;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    ViewModificationCounter.class,
})
public class AbstractIndexProxyTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ViewModificationCounter modCounter;

  private AbstractIndexProxy proxy;

  @Before
  public void setUp() throws Exception {
    mockStatic(ViewModificationCounter.class);
    when(ViewModificationCounter.getInstance()).thenReturn(modCounter);
  }

  @Test
  public void testConstructor() throws Exception {
    View view = createFork();
    proxy = new IndexProxyImpl(view);

    assertThat(proxy.dbView, equalTo(view));
  }

  @Test
  public void constructorFailsIfNullView() throws Exception {
    View dbView = null;

    expectedException.expect(NullPointerException.class);
    proxy = new IndexProxyImpl(dbView);
  }

  @Test
  public void checkCanModifyThrowsIfSnapshotPassed() throws Exception {
    Snapshot dbView = createSnapshot();
    proxy = new IndexProxyImpl(dbView);

    Pattern pattern = Pattern.compile("Cannot modify the view: .*[Ss]napshot.*"
        + "\\nUse a Fork to modify any collection\\.", Pattern.MULTILINE);
    expectedException.expectMessage(matchesPattern(pattern));
    expectedException.expect(UnsupportedOperationException.class);
    proxy.notifyModified();
  }

  @Test
  public void checkCanModifyAcceptsFork() throws Exception {
    Fork dbView = createFork();
    proxy = new IndexProxyImpl(dbView);

    proxy.notifyModified();
    verify(modCounter).notifyModified(eq(dbView));
  }

  private Fork createFork() {
    Cleaner unnecessaryCleaner = new Cleaner();
    return Fork.newInstance(1L, false, unnecessaryCleaner);
  }

  private Snapshot createSnapshot() {
    Cleaner unnecessaryCleaner = new Cleaner();
    return Snapshot.newInstance(2L, false, unnecessaryCleaner);
  }

  private static class IndexProxyImpl extends AbstractIndexProxy {

    private static final long NATIVE_HANDLE = 0x11L;

    IndexProxyImpl(View view) {
      super(NATIVE_HANDLE, "index_name", view);
    }

    @Override
    protected void disposeInternal() {
      // no-op
    }
  }

}
