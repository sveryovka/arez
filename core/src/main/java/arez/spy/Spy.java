package arez.spy;

import arez.Arez;
import arez.Component;
import arez.ComputedValue;
import arez.ObservableValue;
import arez.Observer;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for interacting with spy system.
 */
public interface Spy
{
  /**
   * Add a spy handler to the list of handlers.
   * The handler should not already be in the list.
   *
   * @param handler the spy handler.
   */
  void addSpyEventHandler( @Nonnull SpyEventHandler handler );

  /**
   * Remove spy handler from list of existing handlers.
   * The handler should already be in the list.
   *
   * @param handler the spy handler.
   */
  void removeSpyEventHandler( @Nonnull SpyEventHandler handler );

  /**
   * Return true if spy events will be propagated.
   * This means spies are enabled and there is at least one spy event handler present.
   *
   * @return true if spy events will be propagated, false otherwise.
   */
  boolean willPropagateSpyEvents();

  /**
   * Report an event in the Arez system.
   *
   * @param event the event that occurred.
   */
  void reportSpyEvent( @Nonnull Object event );

  /**
   * Return true if there is a transaction active.
   *
   * @return true if there is a transaction active.
   */
  boolean isTransactionActive();

  /**
   * Return the current transaction.
   * This method should not be invoked unless {@link #isTransactionActive()} returns true.
   *
   * @return the current transaction.
   */
  @Nonnull
  TransactionInfo getTransaction();

  /**
   * Find the component identified by the specified type and id.
   *
   * @param type the component type.
   * @param id   the component id. Should be null if the component is a singleton.
   * @return the component descriptor matching the specified type and id.
   */
  @Nullable
  ComponentInfo findComponent( @Nonnull String type, @Nonnull Object id );

  /**
   * Find all the components identified by the specified type.
   * This collection returned is unmodifiable.
   *
   * @param type the component type.
   * @return the collection of component descriptors of specified type.
   */
  @Nonnull
  Collection<ComponentInfo> findAllComponentsByType( @Nonnull String type );

  /**
   * Find all the component types in the system.
   * This is essentially all the types that have at least 1 instance.
   * This collection returned is unmodifiable.
   *
   * @return the collection of component types.
   */
  @Nonnull
  Collection<String> findAllComponentTypes();

  /**
   * Find all the collection of observables not contained by a native component.
   * This method should not be invoked unless {@link Arez#areRegistriesEnabled()} returns true.
   * This collection returned is unmodifiable.
   *
   * @return the collection of observables not contained by a native component.
   */
  @Nonnull
  Collection<ObservableValueInfo> findAllTopLevelObservableValues();

  /**
   * Find all the collection of observers not contained by a native component.
   * This method should not be invoked unless {@link Arez#areRegistriesEnabled()} returns true.
   * This collection returned is unmodifiable.
   *
   * @return the collection of observers not contained by a native component.
   */
  @Nonnull
  Collection<ObserverInfo> findAllTopLevelObservers();

  /**
   * Find all the collection of computed values not contained by a native component.
   * This method should not be invoked unless {@link Arez#areRegistriesEnabled()} returns true.
   * This collection returned is unmodifiable.
   *
   * @return the collection of computed values not contained by a native component.
   */
  @Nonnull
  Collection<ComputedValueInfo> findAllTopLevelComputedValues();

  /**
   * Convert the specified component into an ComponentInfo.
   *
   * @param component the Component.
   * @return the ComponentInfo.
   */
  @Nonnull
  ComponentInfo asComponentInfo( @Nonnull Component component );

  /**
   * Convert the specified observer into an ObserverInfo.
   *
   * @param observer the Observer.
   * @return the ObserverInfo.
   */
  @Nonnull
  ObserverInfo asObserverInfo( @Nonnull Observer observer );

  /**
   * Convert the specified observableValue into an ObservableValueInfo.
   *
   * @param <T>             The type of the value that is observableValue.
   * @param observableValue the ObservableValue.
   * @return the ObservableValueInfo wrapping observableValue.
   */
  @Nonnull
  <T> ObservableValueInfo asObservableValueInfo( @Nonnull ObservableValue<T> observableValue );

  /**
   * Convert the specified computedValue into an ComputedValueInfo.
   *
   * @param <T>           The type of the value that is computed.
   * @param computedValue the ComputedValue.
   * @return the ComputedValueInfo wrapping the computedValue.
   */
  @Nonnull
  <T> ComputedValueInfo asComputedValueInfo( @Nonnull ComputedValue<T> computedValue );
}