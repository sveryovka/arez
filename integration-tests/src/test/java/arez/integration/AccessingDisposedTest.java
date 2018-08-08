package arez.integration;

import arez.Arez;
import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AccessingDisposedTest
  extends AbstractArezIntegrationTest
{
  @ArezComponent
  static abstract class TestComponent
  {
    int invokeCount;

    @Action
    void myAction()
    {
      Arez.context().observable().reportObserved();
      invokeCount++;
    }
  }

  @ArezComponent( nameIncludesId = false )
  static abstract class TestSingletonComponent
  {
    int invokeCount;

    @Action
    void myAction()
    {
      Arez.context().observable().reportObserved();
      invokeCount++;
    }
  }

  @Test
  public void accessingDisposedComponentResultsInError()
  {
    final TestComponent component = new AccessingDisposedTest_Arez_TestComponent();

    assertEquals( component.invokeCount, 0 );

    component.myAction();

    assertEquals( component.invokeCount, 1 );

    Disposable.dispose( component );

    assertTrue( Disposable.isDisposed( component ) );
    assertInvariant( component::myAction,
                     "Method named 'myAction' invoked on disposed component named 'TestComponent.0'" );
  }

  @Test
  public void accessingDisposedSingletonComponentResultsInError()
  {
    final TestSingletonComponent component = new AccessingDisposedTest_Arez_TestSingletonComponent();

    assertEquals( component.invokeCount, 0 );

    component.myAction();

    assertEquals( component.invokeCount, 1 );

    Disposable.dispose( component );

    assertTrue( Disposable.isDisposed( component ) );
    assertInvariant( component::myAction,
                     "Method named 'myAction' invoked on disposed component named 'TestSingletonComponent'" );
  }
}
