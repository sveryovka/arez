package org.realityforge.arez.api2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ObservableTest
  extends AbstractArezTest
{
  static class TestObservable
    extends Observable
  {
    TestObservable( @Nonnull final ArezContext context,
                    @Nullable final String name,
                    @Nullable final Observer observer )
    {
      super( context, name, observer );
    }
  }

  @Test
  public void initialState()
    throws Exception
  {
    final ArezContext context = new ArezContext();
    final String name = ValueUtil.randomString();
    final TestObservable observable = new TestObservable( context, name, null );
    assertEquals( observable.getName(), name );
    assertEquals( observable.getContext(), context );
    assertEquals( observable.toString(), name );
    assertEquals( observable.isPendingPassivation(), false );
    assertEquals( observable.getObservers().size(), 0 );
    assertEquals( observable.hasObservers(), false );

    observable.invariantLeastStaleObserverState();
  }

  @Test
  public void addObserver()
    throws Exception
  {
    final ArezContext context = new ArezContext();
    final TestObservable observable = new TestObservable( context, ValueUtil.randomString(), null );

    assertEquals( observable.getObservers().size(), 0 );
    assertEquals( observable.hasObservers(), false );
    assertEquals( observable.getLeastStaleObserverState(), ObserverState.NOT_TRACKING );

    final Observer observer = new Observer( context, ValueUtil.randomString() );

    // Handle addition of observer in correct state
    observable.addObserver( observer );

    assertEquals( observable.getObservers().size(), 1 );
    assertEquals( observable.hasObservers(), true );
    assertEquals( observable.getObservers().get( 0 ), observer );
    assertEquals( observable.getLeastStaleObserverState(), ObserverState.NOT_TRACKING );

    observable.invariantLeastStaleObserverState();
  }

  @Test
  public void addObserver_updatesLestStaleObserverState()
    throws Exception
  {
    final ArezContext context = new ArezContext();
    final TestObservable observable = new TestObservable( context, ValueUtil.randomString(), null );
    observable.setLeastStaleObserverState( ObserverState.STALE );

    final Observer observer = new Observer( context, ValueUtil.randomString() );
    observer.setState( ObserverState.POSSIBLY_STALE );

    observable.addObserver( observer );

    assertEquals( observable.getLeastStaleObserverState(), ObserverState.POSSIBLY_STALE );

    observable.invariantLeastStaleObserverState();
  }

  @Test
  public void addObserver_duplicate()
    throws Exception
  {
    final ArezContext context = new ArezContext();
    final TestObservable observable = new TestObservable( context, ValueUtil.randomString(), null );

    assertEquals( observable.getObservers().size(), 0 );
    assertEquals( observable.hasObservers(), false );

    final Observer observer = new Observer( context, ValueUtil.randomString() );

    // Handle addition of observer in correct state
    observable.addObserver( observer );

    assertEquals( observable.getObservers().size(), 1 );
    assertEquals( observable.hasObservers(), true );
    assertEquals( observable.getObservers().get( 0 ), observer );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> observable.addObserver( observer ) );

    assertEquals( exception.getMessage(),
                  "Attempting to add observer named '" + observer.getName() + "' to observable named '" +
                  observable.getName() + "' when observer is already observing observable." );

    assertEquals( observable.getObservers().size(), 1 );
    assertEquals( observable.hasObservers(), true );
    assertEquals( observable.getObservers().get( 0 ), observer );

    observable.invariantLeastStaleObserverState();
  }

  @Test
  public void propagateChanged()
    throws Exception
  {
    final ArezContext context = new ArezContext();
    final TestObservable observable = new TestObservable( context, ValueUtil.randomString(), null );

    final Observer observer = new Observer( context, ValueUtil.randomString() );

    observer.setState( ObserverState.UP_TO_DATE );
    observable.setLeastStaleObserverState( ObserverState.UP_TO_DATE );

    observable.addObserver( observer );

    assertEquals( observable.getLeastStaleObserverState(), ObserverState.UP_TO_DATE );
    assertEquals( observer.getState(), ObserverState.UP_TO_DATE );

    observable.propagateChanged();

    assertEquals( observable.getLeastStaleObserverState(), ObserverState.STALE );
    assertEquals( observer.getState(), ObserverState.STALE );

    observable.invariantLeastStaleObserverState();
  }
}
