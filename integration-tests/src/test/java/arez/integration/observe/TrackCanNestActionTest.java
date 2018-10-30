package arez.integration.observe;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Executor;
import arez.annotations.Observable;
import arez.annotations.Observe;
import arez.annotations.OnDepsChange;
import arez.integration.AbstractArezIntegrationTest;
import arez.integration.util.SpyEventRecorder;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TrackCanNestActionTest
  extends AbstractArezIntegrationTest
{
  @ArezComponent
  public static abstract class TestComponent1
  {
    int _renderCallCount;
    int _depsChangedCallCount;
    int _actionCallCount;

    @Observe( executor = Executor.APPLICATION, nestedActionsAllowed = true )
    public void render()
    {
      getTime2();
      _renderCallCount++;
      myAction();
    }

    @OnDepsChange
    final void onRenderDepsChange()
    {
      _depsChangedCallCount++;
    }

    @Action( mutation = false, requireNewTransaction = true )
    void myAction()
    {
      getTime();
      _actionCallCount++;
    }

    @Observable
    abstract long getTime();

    abstract void setTime( long value );

    @Observable
    abstract long getTime2();

    abstract void setTime2( long value );
  }

  @Test
  public void scenario()
    throws Exception
  {
    final SpyEventRecorder recorder = SpyEventRecorder.beginRecording();

    final TestComponent1 component = new TrackCanNestActionTest_Arez_TestComponent1();

    assertEquals( component._renderCallCount, 0 );
    assertEquals( component._actionCallCount, 0 );
    assertEquals( component._depsChangedCallCount, 0 );

    component.render();

    assertEquals( component._renderCallCount, 1 );
    assertEquals( component._actionCallCount, 1 );
    assertEquals( component._depsChangedCallCount, 0 );

    // This should not trigger renderDepsUpdated flag as render not observing as action obscures dependency
    safeAction( () -> component.setTime( 33L ) );

    assertEquals( component._depsChangedCallCount, 0 );

    safeAction( () -> component.setTime2( 33L ) );

    assertEquals( component._depsChangedCallCount, 1 );

    assertMatchesFixture( recorder );
  }
}
