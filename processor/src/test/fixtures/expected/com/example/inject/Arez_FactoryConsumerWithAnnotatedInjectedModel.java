package com.example.inject;

import arez.ActionFlags;
import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.component.Identifiable;
import arez.component.internal.ComponentKernel;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.realityforge.braincheck.Guards;

@Generated("arez.processor.ArezProcessor")
public final class Arez_FactoryConsumerWithAnnotatedInjectedModel extends FactoryConsumerWithAnnotatedInjectedModel implements Disposable, Identifiable<Integer> {
  private static volatile int $$arezi$$_nextId;

  private final ComponentKernel $$arezi$$_kernel;

  private Arez_FactoryConsumerWithAnnotatedInjectedModel(final int count,
      @Nullable final Runnable nonPerInstanceValue) {
    super(count,nonPerInstanceValue);
    final ArezContext $$arezv$$_context = Arez.context();
    final int $$arezv$$_id = ( Arez.areNamesEnabled() || Arez.areRegistriesEnabled() || Arez.areNativeComponentsEnabled() ) ? ++$$arezi$$_nextId : 0;
    final String $$arezv$$_name = Arez.areNamesEnabled() ? "FactoryConsumerWithAnnotatedInjectedModel." + $$arezv$$_id : null;
    final Component $$arezv$$_component = Arez.areNativeComponentsEnabled() ? $$arezv$$_context.component( "FactoryConsumerWithAnnotatedInjectedModel", $$arezv$$_id, $$arezv$$_name ) : null;
    this.$$arezi$$_kernel = new ComponentKernel( Arez.areZonesEnabled() ? $$arezv$$_context : null, Arez.areNamesEnabled() ? $$arezv$$_name : null, $$arezv$$_id, Arez.areNativeComponentsEnabled() ? $$arezv$$_component : null, null, null, null, false, false, false );
    this.$$arezi$$_kernel.componentConstructed();
    super.postConstruct();
    this.$$arezi$$_kernel.componentReady();
  }

  private int $$arezi$$_id() {
    return this.$$arezi$$_kernel.getId();
  }

  @Override
  @Nonnull
  public Integer getArezId() {
    return $$arezi$$_id();
  }

  @Override
  public boolean isDisposed() {
    return this.$$arezi$$_kernel.isDisposed();
  }

  @Override
  public void dispose() {
    this.$$arezi$$_kernel.dispose();
  }

  @Override
  public void myActionStuff() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named 'myActionStuff' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? "?" : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    this.$$arezi$$_kernel.getContext().safeAction(Arez.areNamesEnabled() ? this.$$arezi$$_kernel.getName() + ".myActionStuff" : null, () -> super.myActionStuff(), ActionFlags.READ_WRITE | ActionFlags.VERIFY_ACTION_REQUIRED, null );
  }

  @Override
  public int hashCode() {
    if ( Arez.areNativeComponentsEnabled() ) {
      return Integer.hashCode( $$arezi$$_id() );
    } else {
      return super.hashCode();
    }
  }

  @Override
  public boolean equals(final Object o) {
    if ( Arez.areNativeComponentsEnabled() ) {
      if ( o instanceof Arez_FactoryConsumerWithAnnotatedInjectedModel ) {
        final Arez_FactoryConsumerWithAnnotatedInjectedModel that = (Arez_FactoryConsumerWithAnnotatedInjectedModel) o;
        return $$arezi$$_id() == that.$$arezi$$_id();
      } else {
        return false;
      }
    } else {
      return super.equals( o );
    }
  }

  @Override
  public String toString() {
    if ( Arez.areNamesEnabled() ) {
      return "ArezComponent[" + this.$$arezi$$_kernel.getName() + "]";
    } else {
      return super.toString();
    }
  }

  public static final class Factory {
    @Nullable
    private final Runnable nonPerInstanceValue;

    @Inject
    Factory(@Nullable final Runnable nonPerInstanceValue) {
      this.nonPerInstanceValue = nonPerInstanceValue;
    }

    @Nonnull
    public Arez_FactoryConsumerWithAnnotatedInjectedModel create(final int count) {
      return new Arez_FactoryConsumerWithAnnotatedInjectedModel( count, nonPerInstanceValue );
    }
  }
}
