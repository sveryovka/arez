package arez;

import arez.spy.ComponentInfo;
import arez.spy.ComputedValueInfo;
import arez.spy.ObservableValueInfo;
import arez.spy.ObserverInfo;
import arez.spy.PropertyAccessor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A implementation of {@link ObservableValueInfo} that proxies to a {@link ObservableValue}.
 */
final class ObservableValueInfoImpl
  implements ObservableValueInfo
{
  private final Spy _spy;
  private final ObservableValue<?> _observableValue;

  ObservableValueInfoImpl( @Nonnull final Spy spy, @Nonnull final ObservableValue<?> observableValue )
  {
    _spy = Objects.requireNonNull( spy );
    _observableValue = Objects.requireNonNull( observableValue );
  }

  @Nonnull
  private static List<ObservableValueInfo> asInfos( @Nonnull final Collection<ObservableValue<?>> observableValues )
  {
    return observableValues
      .stream()
      .map( ObservableValue::asInfo )
      .collect( Collectors.toList() );
  }

  @Nonnull
  static List<ObservableValueInfo> asUnmodifiableInfos( @Nonnull final Collection<ObservableValue<?>> observableValues )
  {
    return Collections.unmodifiableList( asInfos( observableValues ) );
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public String getName()
  {
    return _observableValue.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isComputedValue()
  {
    return _observableValue.hasOwner();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ComputedValueInfo asComputedValue()
  {
    return _observableValue.getOwner().getComputedValue().asInfo();
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public List<ObserverInfo> getObservers()
  {
    return ObserverInfoImpl.asUnmodifiableInfos( _observableValue.getObservers() );
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public ComponentInfo getComponent()
  {
    if ( Arez.shouldCheckInvariants() )
    {
      invariant( Arez::areNativeComponentsEnabled,
                 () -> "Arez-0107: Spy.getComponent invoked when Arez.areNativeComponentsEnabled() returns false." );
    }
    final Component component = _observableValue.getComponent();
    return null == component ? null : component.asInfo();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAccessor()
  {
    if ( Arez.shouldCheckInvariants() )
    {
      invariant( Arez::arePropertyIntrospectorsEnabled,
                 () -> "Arez-0110: Spy.hasAccessor invoked when Arez.arePropertyIntrospectorsEnabled() returns false." );
    }
    return null != _observableValue.getAccessor();
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public Object getValue()
    throws Throwable
  {
    if ( Arez.shouldCheckInvariants() )
    {
      invariant( Arez::arePropertyIntrospectorsEnabled,
                 () -> "Arez-0111: Spy.getValue invoked when Arez.arePropertyIntrospectorsEnabled() returns false." );
    }
    final PropertyAccessor<?> accessor = _observableValue.getAccessor();
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != accessor,
                    () -> "Arez-0112: Spy.getValue invoked on ObservableValue named '" + _observableValue.getName() +
                          "' but ObservableValue has no property accessor." );
    }
    assert null != accessor;
    return accessor.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasMutator()
  {
    if ( Arez.shouldCheckInvariants() )
    {
      invariant( Arez::arePropertyIntrospectorsEnabled,
                 () -> "Arez-0113: Spy.hasMutator invoked when Arez.arePropertyIntrospectorsEnabled() returns false." );
    }
    return null != _observableValue.getMutator();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings( "unchecked" )
  @Override
  public void setValue( @Nullable final Object value )
    throws Throwable
  {
    _spy.setValue( (ObservableValue<Object>) _observableValue, value );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisposed()
  {
    return _observableValue.isDisposed();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return _observableValue.toString();
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    else if ( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    else
    {
      final ObservableValueInfoImpl that = (ObservableValueInfoImpl) o;
      return _observableValue.equals( that._observableValue );
    }
  }

  @Override
  public int hashCode()
  {
    return _observableValue.hashCode();
  }
}
