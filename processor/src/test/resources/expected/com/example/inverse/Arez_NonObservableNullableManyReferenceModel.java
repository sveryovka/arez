package com.example.inverse;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.Locator;
import arez.ObservableValue;
import arez.component.CollectionsUtil;
import arez.component.ComponentKernel;
import arez.component.DisposeNotifier;
import arez.component.DisposeTrackable;
import arez.component.Identifiable;
import arez.component.Verifiable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import org.realityforge.braincheck.Guards;

@Generated("arez.processor.ArezProcessor")
final class Arez_NonObservableNullableManyReferenceModel extends NonObservableNullableManyReferenceModel implements Disposable, Identifiable<Integer>, Verifiable, DisposeTrackable {
  private static volatile int $$arezi$$_nextId;

  private final ComponentKernel $$arezi$$_kernel;

  @Nonnull
  private final ObservableValue<List<NonObservableNullableManyReferenceModel.Element>> $$arez$$_elements;

  private List<NonObservableNullableManyReferenceModel.Element> $$arezd$$_elements;

  private List<NonObservableNullableManyReferenceModel.Element> $$arezd$$_$$cache$$_elements;

  Arez_NonObservableNullableManyReferenceModel() {
    super();
    final ArezContext $$arezv$$_context = Arez.context();
    final int $$arezv$$_id = ( Arez.areNamesEnabled() || Arez.areRegistriesEnabled() || Arez.areNativeComponentsEnabled() ) ? ++$$arezi$$_nextId : 0;
    final String $$arezv$$_name = Arez.areNamesEnabled() ? "NonObservableNullableManyReferenceModel." + $$arezv$$_id : null;
    final Component $$arezv$$_component = Arez.areNativeComponentsEnabled() ? $$arezv$$_context.component( "NonObservableNullableManyReferenceModel", $$arezv$$_id, $$arezv$$_name, () -> $$arezi$$_preDispose() ) : null;
    this.$$arezi$$_kernel = new ComponentKernel( Arez.areZonesEnabled() ? $$arezv$$_context : null, $$arezv$$_name, $$arezv$$_id, $$arezv$$_component, new DisposeNotifier(), Arez.areNativeComponentsEnabled() ? null : this::$$arezi$$_dispose );
    this.$$arez$$_elements = $$arezv$$_context.observable( Arez.areNativeComponentsEnabled() ? $$arezv$$_component : null, Arez.areNamesEnabled() ? $$arezv$$_name + ".elements" : null, Arez.arePropertyIntrospectorsEnabled() ? () -> this.$$arezd$$_elements : null, null );
    this.$$arezd$$_elements = new ArrayList<>();
    this.$$arezd$$_$$cache$$_elements = null;
    this.$$arezi$$_kernel.componentConstructed();
    this.$$arezi$$_kernel.componentReady();
  }

  final Locator $$arezi$$_locator() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.hasBeenInitialized(), () -> "Method named '$$arezi$$_locator' invoked on uninitialized component of type 'NonObservableNullableManyReferenceModel'" );
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
    for ( final NonObservableNullableManyReferenceModel.Element other : new ArrayList<>( $$arezd$$_elements ) ) {
      ( (NonObservableNullableManyReferenceModel_Arez_Element) other ).$$arezi$$_delink_nonObservableNullableManyReferenceModel();
    }
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
    this.$$arez$$_elements.dispose();
  }

  @Override
  public void verify() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named 'verify' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    if ( Arez.shouldCheckApiInvariants() && Arez.isVerifyEnabled() ) {
      Guards.apiInvariant( () -> this == $$arezi$$_locator().findById( NonObservableNullableManyReferenceModel.class, $$arezi$$_id() ), () -> "Attempted to lookup self in Locator with type NonObservableNullableManyReferenceModel and id '" + $$arezi$$_id() + "' but unable to locate self. Actual value: " + $$arezi$$_locator().findById( NonObservableNullableManyReferenceModel.class, $$arezi$$_id() ) );
      for( final NonObservableNullableManyReferenceModel.Element element : this.$$arezd$$_elements ) {
        if ( Arez.shouldCheckApiInvariants() ) {
          Guards.apiInvariant( () -> Disposable.isNotDisposed( element ), () -> "Inverse relationship named 'elements' on component named '" + this.$$arezi$$_kernel.getName() + "' contains disposed element '" + element + "'" );
        }
      }
    }
  }

  @Nonnull
  @Override
  List<NonObservableNullableManyReferenceModel.Element> getElements() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named 'getElements' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    this.$$arez$$_elements.reportObserved();
    if ( Arez.areCollectionsPropertiesUnmodifiable() ) {
      if ( null == this.$$arezd$$_$$cache$$_elements ) {
        this.$$arezd$$_$$cache$$_elements = CollectionsUtil.wrap( this.$$arezd$$_elements );
      }
      return $$arezd$$_$$cache$$_elements;
    } else {
      return this.$$arezd$$_elements;
    }
  }

  void $$arezir$$_elements_add(
      @Nonnull final NonObservableNullableManyReferenceModel.Element element) {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named '$$arezir$$_elements_add' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    this.$$arez$$_elements.preReportChanged();
    if ( Arez.shouldCheckInvariants() ) {
      Guards.invariant( () -> !this.$$arezd$$_elements.contains( element ), () -> "Attempted to add reference 'element' to inverse 'elements' but inverse already contained element. Inverse = " + $$arez$$_elements );
    }
    this.$$arezd$$_elements.add( element );
    if ( Arez.areCollectionsPropertiesUnmodifiable() ) {
      this.$$arezd$$_$$cache$$_elements = null;
    }
    this.$$arez$$_elements.reportChanged();
  }

  void $$arezir$$_elements_remove(
      @Nonnull final NonObservableNullableManyReferenceModel.Element element) {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named '$$arezir$$_elements_remove' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    this.$$arez$$_elements.preReportChanged();
    if ( Arez.shouldCheckInvariants() ) {
      Guards.invariant( () -> this.$$arezd$$_elements.contains( element ), () -> "Attempted to remove reference 'element' from inverse 'elements' but inverse does not contain element. Inverse = " + $$arez$$_elements );
    }
    this.$$arezd$$_elements.remove( element );
    if ( Arez.areCollectionsPropertiesUnmodifiable() ) {
      this.$$arezd$$_$$cache$$_elements = null;
    }
    this.$$arez$$_elements.reportChanged();
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
      if ( o instanceof Arez_NonObservableNullableManyReferenceModel ) {
        final Arez_NonObservableNullableManyReferenceModel that = (Arez_NonObservableNullableManyReferenceModel) o;
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
