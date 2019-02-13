package arez.component;

import arez.Arez;
import arez.Disposable;
import arez.SafeProcedure;
import java.util.ArrayList;
import java.util.HashMap;
import javax.annotation.Nonnull;
import static org.realityforge.braincheck.Guards.*;

/**
 * The class responsible for notifying listeners when an element is disposed.
 * Listeners are added to a notify list using a key and should be added at most once.
 * The listeners can also be removed from the notify list. The notifier will then notifier
 * will then notify all listeners when {@link #dispose()} is invoked after which this
 * class should no longer been interacted with.
 */
public final class DisposeNotifier
  implements Disposable
{
  private final HashMap<Object, SafeProcedure> _listeners = new HashMap<>();
  private boolean _disposed;

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose()
  {
    if ( isNotDisposed() )
    {
      for ( final SafeProcedure procedure : new ArrayList<>( _listeners.values() ) )
      {
        procedure.call();
      }
      _disposed = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisposed()
  {
    return _disposed;
  }

  /**
   * Add the listener to notify list under key.
   * This method MUST NOT be invoked after {@link #dispose()} has been invoked.
   * This method should not be invoked if another listener has been added with the same key without
   * being removed.
   *
   * @param key    the key to uniquely identify listener.
   * @param action the listener callback.
   */
  public void addOnDisposeListener( @Nonnull final Object key, @Nonnull final SafeProcedure action )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      invariant( this::isNotDisposed,
                 () -> "Arez-0170: Attempting to add OnDispose listener but DisposeNotifier has been disposed." );
      invariant( () -> !_listeners.containsKey( key ),
                 () -> "Arez-0166: Attempting to add dispose listener with key '" + key +
                       "' but a listener with that key already exists." );
    }
    _listeners.put( key, action );
  }

  /**
   * Remove the listener with specified key from the notify list.
   * This method should only be invoked when a listener has been added for specific key using
   * {@link #addOnDisposeListener(Object, SafeProcedure)} and has not been removed by another
   * call to this method.
   *
   * @param key the key under which the listener was previously added.
   */
  public void removeOnDisposeListener( @Nonnull final Object key )
  {
    // This method can be called when the notifier is disposed to avoid the caller (i.e. per-component
    // generated code) from checking the disposed state of the notifier before invoking this method.
    // This is necessary in a few rare circumstances but requiring the caller to check before invocation
    // increases the generated code size.
    final SafeProcedure removed = _listeners.remove( key );
    if ( Arez.shouldCheckApiInvariants() )
    {
      invariant( () -> null != removed,
                 () -> "Arez-0167: Attempting to remove dispose listener with key '" + key +
                       "' but no such listener exists." );
    }
  }

  @Nonnull
  HashMap<Object, SafeProcedure> getListeners()
  {
    return _listeners;
  }
}
