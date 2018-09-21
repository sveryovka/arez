package arez.integration.observed;

import arez.annotations.ArezComponent;
import arez.annotations.Executor;
import arez.annotations.Observed;
import arez.annotations.OnDepsChanged;
import arez.integration.AbstractArezIntegrationTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TrackCanNotBeNestedInActionTest
  extends AbstractArezIntegrationTest
{
  @ArezComponent
  static abstract class TestComponent1
  {
    int _renderCallCount;

    @Observed( executor = Executor.APPLICATION )
    void render()
    {
      observeADependency();
      _renderCallCount++;
    }

    @OnDepsChanged
    final void onRenderDepsChanged()
    {
    }
  }

  @Test
  public void scenario()
    throws Exception
  {
    final TestComponent1 component = new TrackCanNotBeNestedInActionTest_Arez_TestComponent1();

    assertEquals( component._renderCallCount, 0 );

    // Call outside action
    component.render();

    assertEquals( component._renderCallCount, 1 );

    // Call observe inside action. This should generate an exception
    assertInvariantFailure( () -> safeAction( component::render ),
                            "Arez-0171: Attempting to create a tracking transaction named 'TestComponent1.0.render' for the observer named 'TestComponent1.0.render' but the transaction is not a top-level transaction when this is required. This may be a result of nesting a observe() call inside an action or another observer function." );

    assertEquals( component._renderCallCount, 1 );
  }
}