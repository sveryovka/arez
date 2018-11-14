package com.example.component_id_ref;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.component.ComponentKernel;
import arez.component.DisposeNotifier;
import arez.component.DisposeTrackable;
import arez.component.Identifiable;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("arez.processor.ArezProcessor")
public final class Arez_CombinedWithComponentIdModel extends CombinedWithComponentIdModel implements Disposable, Identifiable<Integer>, DisposeTrackable {
  private final ComponentKernel $$arezi$$_kernel;

  public Arez_CombinedWithComponentIdModel() {
    super();
    final ArezContext $$arezv$$_context = Arez.context();
    final Object $$arezv$$_id = id();
    final String $$arezv$$_name = Arez.areNamesEnabled() ? "CombinedWithComponentIdModel." + $$arezv$$_id : null;
    final Component $$arezv$$_component = Arez.areNativeComponentsEnabled() ? $$arezv$$_context.component( "CombinedWithComponentIdModel", $$arezv$$_id, $$arezv$$_name, () -> $$arezi$$_preDispose() ) : null;
    this.$$arezi$$_kernel = new ComponentKernel( Arez.areZonesEnabled() ? $$arezv$$_context : null, $$arezv$$_name, 0, $$arezv$$_component, new DisposeNotifier(), Arez.areNativeComponentsEnabled() ? null : this::$$arezi$$_dispose );
    this.$$arezi$$_kernel.componentConstructed();
    this.$$arezi$$_kernel.componentReady();
  }

  @Override
  final int getId() {
    return this.id();
  }

  @Override
  @Nonnull
  public final Integer getArezId() {
    return id();
  }

  private void $$arezi$$_preDispose() {
    this.$$arezi$$_kernel.getDisposeNotifier().dispose();
  }

  @Override
  @Nonnull
  public DisposeNotifier getNotifier() {
    return this.$$arezi$$_kernel.getDisposeNotifier();
  }

  @Override
  public boolean isDisposed() {
    return this.$$arezi$$_kernel.isDisposed();
  }

  @Override
  public void dispose() {
    this.$$arezi$$_kernel.dispose();
  }

  private void $$arezi$$_dispose() {
    this.$$arezi$$_preDispose();
  }

  @Override
  public final int hashCode() {
    if ( Arez.areNativeComponentsEnabled() ) {
      return Integer.hashCode( id() );
    } else {
      return super.hashCode();
    }
  }

  @Override
  public final boolean equals(final Object o) {
    if ( Arez.areNativeComponentsEnabled() ) {
      if ( o instanceof Arez_CombinedWithComponentIdModel ) {
        final Arez_CombinedWithComponentIdModel that = (Arez_CombinedWithComponentIdModel) o;
        return isDisposed() == that.isDisposed() && id() == that.id();
      } else {
        return false;
      }
    } else {
      return super.equals( o );
    }
  }

  @Override
  public final String toString() {
    if ( Arez.areNamesEnabled() ) {
      return "ArezComponent[" + this.$$arezi$$_kernel.getName() + "]";
    } else {
      return super.toString();
    }
  }
}
