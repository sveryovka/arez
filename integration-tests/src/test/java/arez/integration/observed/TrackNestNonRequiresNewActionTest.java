package arez.integration.observed;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Executor;
import arez.annotations.Observable;
import arez.annotations.Observed;
import arez.annotations.OnDepsChanged;
import arez.integration.AbstractArezIntegrationTest;
import arez.integration.util.SpyEventRecorder;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TrackNestNonRequiresNewActionTest
  extends AbstractArezIntegrationTest
{
  @ArezComponent
  public static abstract class TestComponent1
  {
    int _renderCallCount;
    int _depsChangedCallCount;
    int _actionCallCount;

    @Observed( executor = Executor.APPLICATION, nestedActionsAllowed = true )
    public void render()
    {
      getTime2();
      _renderCallCount++;
      myAction();
    }

    @OnDepsChanged
    final void onRenderDepsChanged()
    {
      _depsChangedCallCount++;
    }

    @Action( mutation = false )
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

    final TestComponent1 component = new TrackNestNonRequiresNewActionTest_Arez_TestComponent1();

    assertEquals( component._renderCallCount, 0 );
    assertEquals( component._actionCallCount, 0 );
    assertEquals( component._depsChangedCallCount, 0 );

    component.render();

    assertEquals( component._renderCallCount, 1 );
    assertEquals( component._actionCallCount, 1 );
    assertEquals( component._depsChangedCallCount, 0 );

    // This will cause rescheduling as the action does not start a new transaction
    safeAction( () -> component.setTime( 33L ) );

    assertEquals( component._depsChangedCallCount, 1 );

    assertMatchesFixture( recorder );
  }
}