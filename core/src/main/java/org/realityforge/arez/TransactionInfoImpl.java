package org.realityforge.arez;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.arez.spy.TransactionInfo;

/**
 * Adapter of Transaction to TransactionInfo for spy capabilities.
 */
final class TransactionInfoImpl
  implements TransactionInfo
{
  @Nonnull
  private final Transaction _transaction;
  @Nullable
  private TransactionInfo _parent;

  TransactionInfoImpl( @Nonnull final Transaction transaction )
  {
    _transaction = Objects.requireNonNull( transaction );
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public String getName()
  {
    return getTransaction().getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadOnly()
  {
    return TransactionMode.READ_WRITE != getTransaction().getMode();
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public TransactionInfo getParent()
  {
    if ( null != _parent )
    {
      return _parent;
    }
    else
    {
      final Transaction previous = getTransaction().getPrevious();
      if ( null != previous )
      {
        _parent = new TransactionInfoImpl( previous );
        return _parent;
      }
      else
      {
        return null;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTracking()
  {
    return null != getTransaction().getTracker();
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public Observer getTracker()
  {
    final Observer tracker = getTransaction().getTracker();
    Guards.invariant( () -> null != tracker,
                      () -> "Invoked getTracker on TransactionInfo named '" + getName() + "' but no tracker exists." );
    assert null != tracker;
    return tracker;
  }

  @Nonnull
  Transaction getTransaction()
  {
    return _transaction;
  }
}
