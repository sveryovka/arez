package com.example.action;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.Flags;
import arez.component.DisposeNotifier;
import arez.component.DisposeTrackable;
import arez.component.Identifiable;
import arez.component.internal.ComponentKernel;
import java.text.ParseException;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import org.realityforge.braincheck.Guards;

@Generated("arez.processor.ArezProcessor")
public final class Arez_UnsafeSpecificProcedureActionModel extends UnsafeSpecificProcedureActionModel implements Disposable, Identifiable<Integer>, DisposeTrackable {
  private static volatile int $$arezi$$_nextId;

  private final ComponentKernel $$arezi$$_kernel;

  public Arez_UnsafeSpecificProcedureActionModel() {
    super();
    final ArezContext $$arezv$$_context = Arez.context();
    final int $$arezv$$_id = ( Arez.areNamesEnabled() || Arez.areRegistriesEnabled() || Arez.areNativeComponentsEnabled() ) ? ++$$arezi$$_nextId : 0;
    final String $$arezv$$_name = Arez.areNamesEnabled() ? "UnsafeSpecificProcedureActionModel." + $$arezv$$_id : null;
    final Component $$arezv$$_component = Arez.areNativeComponentsEnabled() ? $$arezv$$_context.component( "UnsafeSpecificProcedureActionModel", $$arezv$$_id, $$arezv$$_name, () -> $$arezi$$_preDispose() ) : null;
    this.$$arezi$$_kernel = new ComponentKernel( Arez.areZonesEnabled() ? $$arezv$$_context : null, $$arezv$$_name, $$arezv$$_id, $$arezv$$_component, Arez.areNativeComponentsEnabled() ? null : this::$$arezi$$_dispose, true, false, false );
    this.$$arezi$$_kernel.componentConstructed();
    this.$$arezi$$_kernel.componentReady();
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
  public void doStuff(final long time) throws ParseException {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> null != this.$$arezi$$_kernel && this.$$arezi$$_kernel.isActive(), () -> "Method named 'doStuff' invoked on " + this.$$arezi$$_kernel.describeState() + " component named '" + ( null == this.$$arezi$$_kernel ? '?' : this.$$arezi$$_kernel.getName() ) + "'" );
    }
    try {
      this.$$arezi$$_kernel.getContext().action(Arez.areNamesEnabled() ? this.$$arezi$$_kernel.getName() + ".doStuff" : null, () -> super.doStuff( time ), Flags.READ_WRITE | Flags.ENVIRONMENT_NOT_REQUIRED | Flags.VERIFY_ACTION_REQUIRED, Arez.areSpiesEnabled() ? new Object[] { time } : null );
    } catch( final ParseException | RuntimeException | Error $$arez_exception$$ ) {
      throw $$arez_exception$$;
    } catch( final Throwable $$arez_exception$$ ) {
      throw new IllegalStateException( $$arez_exception$$ );
    }
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
      if ( o instanceof Arez_UnsafeSpecificProcedureActionModel ) {
        final Arez_UnsafeSpecificProcedureActionModel that = (Arez_UnsafeSpecificProcedureActionModel) o;
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
