package arez.integration.observe;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Executor;
import arez.annotations.Observable;
import arez.annotations.Observe;
import arez.annotations.OnDepsChanged;
import arez.integration.AbstractArezIntegrationTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TrackNoNestActionTest
  extends AbstractArezIntegrationTest
{
  @ArezComponent
  public static abstract class TestComponent1
  {
    int _renderCallCount;
    int _depsChangedCallCount;
    int _actionCallCount;

    @Observe( executor = Executor.APPLICATION )
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
  {
    final TestComponent1 component = new TrackNoNestActionTest_Arez_TestComponent1();

    assertEquals( component._renderCallCount, 0 );
    assertEquals( component._actionCallCount, 0 );
    assertEquals( component._depsChangedCallCount, 0 );

    assertInvariant( component::render,
                     "Arez-0187: Attempting to nest action named 'TestComponent1.0.myAction' inside transaction named 'TestComponent1.0.render' created by an observer that does not allow nested actions." );

    assertEquals( component._renderCallCount, 1 );
    assertEquals( component._actionCallCount, 0 );
    assertEquals( component._depsChangedCallCount, 0 );

    // This should not trigger renderDepsUpdated flag as render not observing as action obscures dependency
    safeAction( () -> component.setTime( 33L ) );

    assertEquals( component._depsChangedCallCount, 0 );

    safeAction( () -> component.setTime2( 33L ) );

    assertEquals( component._depsChangedCallCount, 1 );
  }
}