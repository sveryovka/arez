package arez.component.internal;

import arez.ActionFlags;
import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.ComputableValue;
import arez.Disposable;
import arez.ObservableValue;
import arez.Procedure;
import arez.SafeProcedure;
import arez.Task;
import arez.annotations.ArezComponent;
import arez.annotations.CascadeDispose;
import arez.annotations.Observe;
import arez.component.ComponentObservable;
import grim.annotations.OmitClinit;
import grim.annotations.OmitSymbol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * The "kernel" of the components generated by the annotation processor.
 * This class exists so that code common across multiple components is not present in every
 * generated class but is instead in a single location. This results in smaller, faster code.
 */
@OmitClinit
public final class ComponentKernel
  implements Disposable, ComponentObservable
{
  /**
   * The component has been created, but not yet initialized.
   */
  private final static byte COMPONENT_CREATED = 0;
  /**
   * The components constructor has been called, the {@link ArezContext} field initialized (if necessary),
   * and the synthetic id has been generated (if required).
   */
  private final static byte COMPONENT_INITIALIZED = 1;
  /**
   * The reactive elements have been created (i.e. the {@link ObservableValue}, {@link arez.Observer},
   * {@link ComputableValue} etc.). The {@link arez.annotations.PostConstruct} has NOT been invoked nor
   * has the {@link Component} been instantiated. This means the component is ready to be interacted with
   * in a {@link arez.annotations.PostConstruct} method but has not been fully constructed.
   */
  private final static byte COMPONENT_CONSTRUCTED = 2;
  /**
   * The {@link arez.annotations.PostConstruct} method has been invoked and
   * the {@link Component} has been instantiated. Observers have been scheduled but the scheduler
   * has not been triggered.
   */
  private final static byte COMPONENT_COMPLETE = 3;
  /**
   * The scheduler has been triggered and any {@link Observe} methods have been invoked if runtime managed.
   */
  private final static byte COMPONENT_READY = 4;
  /**
   * The component is disposing.
   */
  private final static byte COMPONENT_DISPOSING = -2;
  /**
   * The component has been disposed.
   */
  private final static byte COMPONENT_DISPOSED = -1;
  /**
   * Reference to the context to which this component belongs.
   */
  @OmitSymbol( unless = "arez.enable_zones" )
  @Nullable
  private final ArezContext _context;
  /**
   * A human consumable name for component. It should be non-null if {@link Arez#areNamesEnabled()} returns
   * <code>true</code> and <tt>null</tt> otherwise.
   */
  @Nullable
  @OmitSymbol( unless = "arez.enable_names" )
  private final String _name;
  /**
   * The runtime managed synthetic id for component. This will be 0 if the component has supplied a custom
   * id via a method annotated with {@link arez.annotations.ComponentId} or the annotation processor has
   * determined that no id is required. The id must be supplied with a non-zero value if:
   *
   * <ul>
   * <li>the component type is annotated with the {@link arez.annotations.Repository} annotation.</li>
   * <li>the component declared it requires an id (i.e. {@link ArezComponent#requireId()} is <code>true</code>) but
   * no method annotated with {@link arez.annotations.ComponentId} is present on the components type.</li>
   * <li>The runtime requires an id as part of debugging infrastructure. (i.e. @link Arez#areNamesEnabled(), {@link Arez#areRegistriesEnabled()}
   * or {@link Arez#areNativeComponentsEnabled()} returns <code>true</code>.</li>
   * </ul>
   */
  private final int _id;
  /**
   * The initialization state of the component. Possible values are defined by the constants in the
   * this class however this field is only used for determining whether a component
   * is disposed when invariant checking is disabled so states other than {@link #COMPONENT_DISPOSING} are not set
   * when invariant checking is disabled.
   */
  private byte _state;
  /**
   * The native component associated with the component. This should be non-null if {@link Arez#areNativeComponentsEnabled()}
   * returns <code>true</code> and <tt>null</tt> otherwise.
   */
  @OmitSymbol( unless = "arez.enable_native_components" )
  @Nullable
  private final Component _component;
  /**
   * This callback is invoked before the component is disposed.
   */
  @Nullable
  private final SafeProcedure _preDisposeCallback;
  /**
   * This callback is invoked to dispose the reactive elements of the component.
   */
  @Nullable
  private final SafeProcedure _disposeCallback;
  /**
   * This callback is invoked after the component is disposed.
   */
  @Nullable
  private final SafeProcedure _postDisposeCallback;
  /**
   * The mechanisms to notify downstream elements that the component has been disposed. This should be non-null
   * if the {@link ArezComponent#disposeNotifier()} is enabled, and <code>null</code> otherwise.
   */
  @Nullable
  private final Map<Object, SafeProcedure> _onDisposeListeners;
  /**
   * Mechanism for implementing {@link ComponentObservable} on the component.
   */
  @Nullable
  private final ObservableValue<Boolean> _componentObservable;
  /**
   * Mechanism for implementing {@link ArezComponent#disposeOnDeactivate()} on the component.
   */
  @Nullable
  private final ComputableValue<Boolean> _disposeOnDeactivate;
  /**
   * Guard to ensure we never try to schedule a dispose multiple times, otherwise the underlying task
   * system will detect multiple tasks with the same name and object.
   */
  private boolean _disposeScheduled;

  public ComponentKernel( @Nullable final ArezContext context,
                          @Nullable final String name,
                          final int id,
                          @Nullable final Component component,
                          @Nullable final SafeProcedure preDisposeCallback,
                          @Nullable final SafeProcedure disposeCallback,
                          @Nullable final SafeProcedure postDisposeCallback,
                          final boolean notifyOnDispose,
                          final boolean isComponentObservable,
                          final boolean disposeOnDeactivate )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Arez.areZonesEnabled() || null == context,
                    () -> "Arez-0100: ComponentKernel passed a context but Arez.areZonesEnabled() is false" );
      apiInvariant( () -> Arez.areNamesEnabled() || null == name,
                    () -> "Arez-0156: ComponentKernel passed a name '" + name +
                          "' but Arez.areNamesEnabled() returns false." );
      apiInvariant( () -> !Arez.areNativeComponentsEnabled() ||
                          null == component ||
                          0 == id ||
                          ( (Integer) id ).equals( component.getId() ),
                    () -> "Arez-0222: ComponentKernel named '" + name +
                          "' passed an id " + id + " and a component but the component had a different id (" +
                          Objects.requireNonNull( component ).getId() + ")" );
    }

    if ( Arez.shouldCheckApiInvariants() )
    {
      _state = COMPONENT_INITIALIZED;
    }
    _name = Arez.areNamesEnabled() ? name : null;
    _context = Arez.areZonesEnabled() ? context : null;
    _component = Arez.areNativeComponentsEnabled() ? Objects.requireNonNull( component ) : null;
    _id = id;
    _onDisposeListeners = notifyOnDispose ? new HashMap<>() : null;
    _preDisposeCallback = Arez.areNativeComponentsEnabled() ? null : preDisposeCallback;
    _disposeCallback = Arez.areNativeComponentsEnabled() ? null : disposeCallback;
    _postDisposeCallback = Arez.areNativeComponentsEnabled() ? null : postDisposeCallback;
    _componentObservable = isComponentObservable ? createComponentObservable() : null;
    _disposeOnDeactivate = disposeOnDeactivate ? createDisposeOnDeactivate() : null;
  }

  @Nonnull
  private ComputableValue<Boolean> createDisposeOnDeactivate()
  {
    return getContext().computable( Arez.areNativeComponentsEnabled() ? getComponent() : null,
                                    Arez.areNamesEnabled() ? getName() + ".disposeOnDeactivate" : null,
                                    this::observe0,
                                    null,
                                    this::scheduleDispose,
                                    null,
                                    ComputableValue.Flags.PRIORITY_HIGHEST );
  }

  private void scheduleDispose()
  {
    /*
     * Guard against a scenario where due to interleaving of scheduled tasks a component is disposed due,
     * to deactivation and then is re-observed and deactivated again prior to the dispose task running.
     * This scenario was thought to be practically impossible but several applications did the impossible.
     *
     * There is still a bug or at least an ambiguity where a disposeOnDeactivate component deactivates, schedules
     * dispose and then activates before the dispose task runs. Should the dispose be aborted or should it go ahead?
     * Currently the Arez API does not expose a flag indicating whether computableValues are observed and not possible
     * to implement the first strategy even though it may seem to be the right one.
     */
    if ( !_disposeScheduled )
    {
      _disposeScheduled = true;
      getContext().task( Arez.areNamesEnabled() ? getName() + ".disposeOnDeactivate.task" : null,
                         this::dispose,
                         Task.Flags.PRIORITY_HIGHEST | Task.Flags.DISPOSE_ON_COMPLETE | Task.Flags.NO_WRAP_TASK );
    }
  }

  @Nonnull
  private ObservableValue<Boolean> createComponentObservable()
  {
    return getContext().observable( Arez.areNativeComponentsEnabled() ? getComponent() : null,
                                    Arez.areNamesEnabled() ? getName() + ".isDisposed" : null,
                                    Arez.arePropertyIntrospectorsEnabled() ? () -> _state > 0 : null );
  }

  @Override
  public boolean observe()
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != _disposeOnDeactivate || null != _componentObservable,
                    () -> "Arez-0221: ComponentKernel.observe() invoked on component named '" + getName() +
                          "' but observing is not enabled for component." );
    }
    if ( null != _disposeOnDeactivate )
    {
      return isNotDisposed() ? _disposeOnDeactivate.get() : false;
    }
    else
    {
      return observe0();
    }
  }

  /**
   * Internal observe method that may be directly used or used from computable if disposeOnDeactivate is true.
   */
  private boolean observe0()
  {
    assert null != _componentObservable;
    final boolean isNotDisposed = isNotDisposed();
    if ( isNotDisposed )
    {
      _componentObservable.reportObserved();
    }
    return isNotDisposed;
  }

  @Override
  public void dispose()
  {
    if ( isNotDisposed() )
    {
      // Note that his state transition occurs outside the guard as it is required to compute isDisposed() state
      _state = COMPONENT_DISPOSING;
      if ( Arez.areNativeComponentsEnabled() )
      {
        assert null != _component;
        _component.dispose();
      }
      else
      {
        getContext().safeAction( Arez.areNamesEnabled() ? getName() + ".dispose" : null,
                                 this::performDispose,
                                 ActionFlags.NO_VERIFY_ACTION_REQUIRED );
      }
      if ( Arez.shouldCheckApiInvariants() )
      {
        _state = COMPONENT_DISPOSED;
      }
    }
  }

  private void performDispose()
  {
    invokeCallbackIfNecessary( _preDisposeCallback );
    releaseResources();
    invokeCallbackIfNecessary( _disposeCallback );
    invokeCallbackIfNecessary( _postDisposeCallback );
  }

  private void invokeCallbackIfNecessary( @Nullable final SafeProcedure callback )
  {
    if ( null != callback )
    {
      callback.call();
    }
  }

  @Override
  public boolean isDisposed()
  {
    return _state < 0;
  }

  private void releaseResources()
  {
    if ( null != _onDisposeListeners )
    {
      notifyOnDisposeListeners();
    }
    // If native components are enabled, these elements are registered with native component
    // and will thus be disposed as part
    if ( !Arez.areNativeComponentsEnabled() )
    {
      Disposable.dispose( _componentObservable );
      Disposable.dispose( _disposeOnDeactivate );
    }
  }

  /**
   * Notify an OnDispose listeners that have been added to the component.
   * This method MUST only be called if the component has enabled onDisposeNotify feature.
   */
  public void notifyOnDisposeListeners()
  {
    assert null != _onDisposeListeners;
    for ( final Map.Entry<Object, SafeProcedure> entry : new ArrayList<>( _onDisposeListeners.entrySet() ) )
    {
      final Object key = entry.getKey();
      /*
       * There is scenarios where there is multiple elements being simultaneously disposed and
       * the @CascadeDispose has not triggered so a disposed object is in this list waiting to
       * be called back. If the callback is triggered and the @CascadeDispose is on an observable
       * property then the framework will attempt to null field and generate invariant failures
       * or runtime errors unless we skip the callback and just remove the listener.
       */
      if ( !Disposable.isDisposed( key ) )
      {
        entry.getValue().call();
      }
    }
  }

  /**
   * Return true if the component has been initialized.
   *
   * @return true if the component has been initialized.
   */
  public boolean hasBeenInitialized()
  {
    assert Arez.shouldCheckInvariants() || Arez.shouldCheckApiInvariants();
    return COMPONENT_CREATED != _state;
  }

  /**
   * Return true if the component has been constructed.
   *
   * @return true if the component has been constructed.
   */
  public boolean hasBeenConstructed()
  {
    assert Arez.shouldCheckInvariants() || Arez.shouldCheckApiInvariants();
    return hasBeenInitialized() && COMPONENT_INITIALIZED != _state;
  }

  /**
   * Return true if the component has been completed.
   *
   * @return true if the component has been completed.
   */
  public boolean hasBeenCompleted()
  {
    assert Arez.shouldCheckInvariants() || Arez.shouldCheckApiInvariants();
    return hasBeenConstructed() && COMPONENT_CONSTRUCTED != _state;
  }

  /**
   * Return true if the component is in COMPONENT_CONSTRUCTED state.
   *
   * @return true if the component is in COMPONENT_CONSTRUCTED state.
   */
  public boolean isConstructed()
  {
    return COMPONENT_CONSTRUCTED == _state;
  }

  /**
   * Return true if the component is in COMPONENT_COMPLETE state.
   *
   * @return true if the component is in COMPONENT_COMPLETE state.
   */
  public boolean isComplete()
  {
    return COMPONENT_COMPLETE == _state;
  }

  /**
   * Return true if the component is ready.
   *
   * @return true if the component is ready.
   */
  public boolean isReady()
  {
    return COMPONENT_READY == _state;
  }

  /**
   * Return true if the component is disposing.
   *
   * @return true if the component is disposing.
   */
  public boolean isDisposing()
  {
    return COMPONENT_DISPOSING == _state;
  }

  /**
   * Return true if the component is active and can be interacted with.
   * This means that the component has been constructed and has not started to be disposed.
   *
   * @return true if the component is active.
   */
  public boolean isActive()
  {
    assert Arez.shouldCheckInvariants() || Arez.shouldCheckApiInvariants();
    return COMPONENT_CONSTRUCTED == _state || COMPONENT_COMPLETE == _state || COMPONENT_READY == _state;
  }

  /**
   * Describe component state. This is usually used to provide error messages.
   *
   * @return a string description of the state.
   */
  @Nonnull
  public String describeState()
  {
    return describeState( _state );
  }

  @Nonnull
  private String describeState( final int state )
  {
    assert Arez.shouldCheckInvariants() || Arez.shouldCheckApiInvariants();
    switch ( state )
    {
      case ComponentKernel.COMPONENT_CREATED:
        return "created";
      case ComponentKernel.COMPONENT_INITIALIZED:
        return "initialized";
      case ComponentKernel.COMPONENT_CONSTRUCTED:
        return "constructed";
      case ComponentKernel.COMPONENT_COMPLETE:
        return "complete";
      case ComponentKernel.COMPONENT_READY:
        return "ready";
      case ComponentKernel.COMPONENT_DISPOSING:
        return "disposing";
      default:
        assert ComponentKernel.COMPONENT_DISPOSED == state;
        return "disposed";
    }
  }

  /**
   * Transition component state from {@link ComponentKernel#COMPONENT_INITIALIZED} to {@link ComponentKernel#COMPONENT_CONSTRUCTED}.
   */
  public void componentConstructed()
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> COMPONENT_INITIALIZED == _state,
                    () -> "Arez-0219: Bad state transition from " + describeState( _state ) +
                          " to " + describeState( COMPONENT_CONSTRUCTED ) +
                          " on component named '" + getName() + "'." );
      _state = COMPONENT_CONSTRUCTED;
    }
  }

  /**
   * Transition component state from {@link ComponentKernel#COMPONENT_INITIALIZED} to
   * {@link ComponentKernel#COMPONENT_CONSTRUCTED} and then to {@link ComponentKernel#COMPONENT_READY}.
   * This should only be called if there is active elements that are part of the component that need to be scheduled,
   * otherwise the component can transition directly to ready.
   */
  public void componentComplete()
  {
    completeNativeComponent();
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> COMPONENT_CONSTRUCTED == _state,
                    () -> "Arez-0220: Bad state transition from " + describeState( _state ) +
                          " to " + describeState( COMPONENT_COMPLETE ) +
                          " on component named '" + getName() + "'." );
      _state = COMPONENT_COMPLETE;
    }
    // Trigger scheduler so active parts of components can react
    getContext().triggerScheduler();
    makeComponentReady();
  }

  /**
   * Transition component state from {@link ComponentKernel#COMPONENT_CONSTRUCTED} to {@link ComponentKernel#COMPONENT_READY}.
   * This should be invoked rather than {@link #componentComplete()} if there is no active elements of the component that
   * need to be scheduled.
   */
  public void componentReady()
  {
    completeNativeComponent();
    makeComponentReady();
  }

  /**
   * Mark the native component if present as complete.
   */
  private void completeNativeComponent()
  {
    if ( Arez.areNativeComponentsEnabled() )
    {
      assert null != _component;
      _component.complete();
    }
  }

  private void makeComponentReady()
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> COMPONENT_CONSTRUCTED == _state || COMPONENT_COMPLETE == _state,
                    () -> "Arez-0218: Bad state transition from " + describeState( _state ) +
                          " to " + describeState( COMPONENT_READY ) +
                          " on component named '" + getName() + "'." );
      _state = COMPONENT_READY;
    }
  }

  /**
   * Return the context in which this component was created.
   *
   * @return the associated context.
   */
  @Nonnull
  public ArezContext getContext()
  {
    return Arez.areZonesEnabled() ? Objects.requireNonNull( _context ) : Arez.context();
  }

  /**
   * Invoke the setter in a transaction.
   * If a transaction is active then invoke the setter directly, otherwise wrap the setter in an action.
   *
   * @param name   the name of the action if it is needed.
   * @param setter the setter action to invoke.
   */
  public void safeSetObservable( @Nullable final String name, @Nonnull final SafeProcedure setter )
  {
    if ( getContext().isTransactionActive() )
    {
      setter.call();
    }
    else
    {
      getContext().safeAction( Arez.areNamesEnabled() ? name : null, setter );
    }
  }

  /**
   * Invoke the setter in a transaction.
   * If a transaction is active then invoke the setter directly, otherwise wrap the setter in an action.
   *
   * @param name   the name of the action if it is needed.
   * @param setter the setter action to invoke.
   * @throws Throwable if setter throws an exception.
   */
  public void setObservable( @Nullable final String name, @Nonnull final Procedure setter )
    throws Throwable
  {
    if ( getContext().isTransactionActive() )
    {
      setter.call();
    }
    else
    {
      getContext().action( Arez.areNamesEnabled() ? name : null, setter );
    }
  }

  /**
   * Return the name of the component.
   * This method should NOT be invoked unless {@link Arez#areNamesEnabled()} returns true and will throw an
   * exception if invariant checking is enabled.
   *
   * @return the name of the component.
   */
  @Nonnull
  public String getName()
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( Arez::areNamesEnabled,
                    () -> "Arez-0164: ComponentKernel.getName() invoked when Arez.areNamesEnabled() returns false." );
    }
    assert null != _name;
    return _name;
  }

  /**
   * Return the synthetic id associated with the component.
   * This method MUST NOT be invoked if a synthetic id is not present and will generate an invariant failure
   * when invariants are enabled.
   *
   * @return the synthetic id associated with the component.
   */
  public int getId()
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> 0 != _id,
                    () -> "Arez-0213: Attempted to unexpectedly invoke ComponentKernel.getId() method to access " +
                          "synthetic id on component named '" + getName() + "'." );
    }
    return _id;
  }

  /**
   * Return the native component associated with the component.
   * This method MUST NOT be invoked if native components are disabled.
   *
   * @return the native component associated with the component.
   */
  @OmitSymbol( unless = "arez.enable_native_components" )
  @Nonnull
  public Component getComponent()
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != _component,
                    () -> "Arez-0216: ComponentKernel.getComponent() invoked when Arez.areNativeComponentsEnabled() " +
                          "returns false on component named '" + getName() + "'." );
    }
    assert null != _component;
    return _component;
  }

  /**
   * Add the listener to notify list under key.
   * This method MUST NOT be invoked after {@link #dispose()} has been invoked.
   * This method should not be invoked if another listener has been added with the same key without
   * being removed.
   *
   * <p>If the key implements {@link Disposable} and {@link Disposable#isDisposed()} returns <code>true</code>
   * when invoking the calback then the callback will be skipped. This rare situation only occurs when there is
   * circular dependency in the object model usually involving {@link CascadeDispose}.</p>
   *
   * @param key    the key to uniquely identify listener.
   * @param action the listener callback.
   */
  public void addOnDisposeListener( @Nonnull final Object key, @Nonnull final SafeProcedure action )
  {
    assert null != _onDisposeListeners;
    if ( Arez.shouldCheckApiInvariants() )
    {
      invariant( this::isNotDisposed,
                 () -> "Arez-0170: Attempting to add OnDispose listener but ComponentKernel has been disposed." );
      invariant( () -> !_onDisposeListeners.containsKey( key ),
                 () -> "Arez-0166: Attempting to add OnDispose listener with key '" + key +
                       "' but a listener with that key already exists." );
    }
    _onDisposeListeners.put( key, action );
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
    assert null != _onDisposeListeners;
    // This method can be called when the notifier is disposed to avoid the caller (i.e. per-component
    // generated code) from checking the disposed state of the notifier before invoking this method.
    // This is necessary in a few rare circumstances but requiring the caller to check before invocation
    // increases the generated code size.
    final SafeProcedure removed = _onDisposeListeners.remove( key );
    if ( Arez.shouldCheckApiInvariants() )
    {
      invariant( () -> null != removed,
                 () -> "Arez-0167: Attempting to remove OnDispose listener with key '" + key +
                       "' but no such listener exists." );
    }
  }

  boolean hasOnDisposeListeners()
  {
    return null != _onDisposeListeners;
  }

  @Nonnull
  Map<Object, SafeProcedure> getOnDisposeListeners()
  {
    assert null != _onDisposeListeners;
    return _onDisposeListeners;
  }

  @Nonnull
  @Override
  public String toString()
  {
    if ( Arez.areNamesEnabled() )
    {
      return getName();
    }
    else
    {
      return super.toString();
    }
  }
}
