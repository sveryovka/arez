package com.example.inverse;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.Locator;
import arez.SafeProcedure;
import arez.component.DisposeNotifier;
import arez.component.Identifiable;
import arez.component.Verifiable;
import arez.component.internal.ComponentKernel;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.braincheck.Guards;

@Generated("arez.processor.ArezProcessor")
final class NonObservableNullableOneReferenceModel_Arez_Element extends NonObservableNullableOneReferenceModel.Element implements Disposable, Identifiable<Integer>, Verifiable, DisposeNotifier {
  private static volatile int $$arezi$$_nextId;

  private final ComponentKernel $$arezi$$_kernel;

  @Nullable
  private NonObservableNullableOneReferenceModel $$arezr$$_nonObservableNullableOneReferenceModel;

  NonObservableNullableOneReferenceModel_Arez_Element() {
    super();
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> Arez.areReferencesEnabled(), () -> "Attempted to create instance of component of type 'Element' that contains references but Arez.areReferencesEnabled() returns false. References need to be enabled to use this component" );
    }
    final ArezContext $$arezv$$_context = Arez.context();
    final int $$arezv$$_id = ( Arez.areNamesEnabled() || Arez.areRegistriesEnabled() || Arez.areNativeComponentsEnabled() ) ? ++$$arezi$$_nextId : 0;
    final String $$arezv$$_name = Arez.areNamesEnabled() ? "Element." + $$arezv$$_id : null;
    final Component $$arezv$$_component = Arez.areNativeComponentsEnabled() ? $$arezv$$_context.component( "Element", $$arezv$$_id, $$arezv$$_name, () -> $$arezi$$_nativeComponentPreDispose() ) : null;
    this.$$arezi$$_kernel = new ComponentKernel( Arez.areZonesEnabled() ? $$arezv$$_context : null, Arez.areNamesEnabled() ? $$arezv$$_name : null, $$arezv$$_id, Arez.areNativeComponentsEnabled() ? $$arezv$$_component : null, Arez.areNativeComponentsEnabled() ? null : this::$$arezi$$_preDispose, null, null, true, false, false );
    this.$$arezi$$_kernel.componentConstructed();
    this.$$arezi$$_link_nonObservableNullableOneReferenceModel();
    this.$$arezi$$_kernel.componentReady();
  }

  @Nonnull
  final Locator $$arezi$$_locator() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.hasBeenInitialized(), () -> "Method named '$$arezi$$_locator' invoked on uninitialized component of type 'Element'" );
    }
    return this.$$arezi$$_kernel.getContext().locator();
  }

  final int $$arezi$$_id() {
    return this.$$arezi$$_kernel.getId();
  }

  @Override
  @Nonnull
  public final Integer getArezId() {
    return $$arezi$$_id();
  }

  private void $$arezi$$_preDispose() {
    this.$$arezi$$_delink_nonObservableNullableOneReferenceModel();
  }

  private void $$arezi$$_nativeComponentPreDispose() {
    this.$$arezi$$_preDispose();
    this.$$arezi$$_kernel.notifyOnDisposeListeners();
  }

  @Override
  public void addOnDisposeListener(@Nonnull final Object key, @Nonnull final SafeProcedure action) {
    this.$$arezi$$_kernel.addOnDisposeListener( key, action );
  }

  @Override
  public void removeOnDisposeListener(@Nonnull final Object key) {
    this.$$arezi$$_kernel.removeOnDisposeListener( key );
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
  public void verify() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named 'verify' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    if ( Arez.shouldCheckApiInvariants() && Arez.isVerifyEnabled() ) {
      Guards.apiInvariant( () -> this == $$arezi$$_locator().findById( NonObservableNullableOneReferenceModel.Element.class, $$arezi$$_id() ), () -> "Attempted to lookup self in Locator with type NonObservableNullableOneReferenceModel.Element and id '" + $$arezi$$_id() + "' but unable to locate self. Actual value: " + $$arezi$$_locator().findById( NonObservableNullableOneReferenceModel.Element.class, $$arezi$$_id() ) );
      final Integer $$arezv$$_nonObservableNullableOneReferenceModelId = this.getNonObservableNullableOneReferenceModelId();
      if ( null != $$arezv$$_nonObservableNullableOneReferenceModelId ) {
        final NonObservableNullableOneReferenceModel $$arezv$$_nonObservableNullableOneReferenceModel = this.$$arezi$$_locator().findById( NonObservableNullableOneReferenceModel.class, $$arezv$$_nonObservableNullableOneReferenceModelId );
        Guards.apiInvariant( () -> null != $$arezv$$_nonObservableNullableOneReferenceModel, () -> "Reference named 'nonObservableNullableOneReferenceModel' on component named '" + this.$$arezi$$_kernel.getName() + "' is unable to resolve entity of type com.example.inverse.NonObservableNullableOneReferenceModel and id = " + getNonObservableNullableOneReferenceModelId() );
      }
    }
  }

  @Nullable
  @Override
  NonObservableNullableOneReferenceModel getNonObservableNullableOneReferenceModel() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named 'getNonObservableNullableOneReferenceModel' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != $$arezr$$_nonObservableNullableOneReferenceModel || null == getNonObservableNullableOneReferenceModelId(), () -> "Nullable reference method named 'getNonObservableNullableOneReferenceModel' invoked on component named '" + this.$$arezi$$_kernel.getName() + "' and reference has not been resolved yet is not lazy. Id = " + getNonObservableNullableOneReferenceModelId() );
    }
    return this.$$arezr$$_nonObservableNullableOneReferenceModel;
  }

  private void $$arezi$$_link_nonObservableNullableOneReferenceModel() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named '$$arezi$$_link_nonObservableNullableOneReferenceModel' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    final Integer id = this.getNonObservableNullableOneReferenceModelId();
    if ( null != id ) {
      this.$$arezr$$_nonObservableNullableOneReferenceModel = this.$$arezi$$_locator().findById( NonObservableNullableOneReferenceModel.class, id );
      if ( Arez.shouldCheckApiInvariants() ) {
        Guards.apiInvariant( () -> null != $$arezr$$_nonObservableNullableOneReferenceModel, () -> "Reference named 'nonObservableNullableOneReferenceModel' on component named '" + this.$$arezi$$_kernel.getName() + "' is unable to resolve entity of type com.example.inverse.NonObservableNullableOneReferenceModel and id = " + getNonObservableNullableOneReferenceModelId() );
      }
      ( (Arez_NonObservableNullableOneReferenceModel) this.$$arezr$$_nonObservableNullableOneReferenceModel ).$$arezir$$_element_set( this );
    } else {
      this.$$arezr$$_nonObservableNullableOneReferenceModel = null;
    }
  }

  void $$arezi$$_delink_nonObservableNullableOneReferenceModel() {
    if ( null != $$arezr$$_nonObservableNullableOneReferenceModel && Disposable.isNotDisposed( $$arezr$$_nonObservableNullableOneReferenceModel ) ) {
      ( (Arez_NonObservableNullableOneReferenceModel) this.$$arezr$$_nonObservableNullableOneReferenceModel ).$$arezir$$_element_unset( this );
    }
    this.$$arezr$$_nonObservableNullableOneReferenceModel = null;
  }

  @Override
  public final int hashCode() {
    if ( Arez.areNativeComponentsEnabled() ) {
      return Integer.hashCode( $$arezi$$_id() );
    } else {
      return super.hashCode();
    }
  }

  @Override
  public final boolean equals(final Object o) {
    if ( Arez.areNativeComponentsEnabled() ) {
      if ( o instanceof NonObservableNullableOneReferenceModel_Arez_Element ) {
        final NonObservableNullableOneReferenceModel_Arez_Element that = (NonObservableNullableOneReferenceModel_Arez_Element) o;
        return $$arezi$$_id() == that.$$arezi$$_id();
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
