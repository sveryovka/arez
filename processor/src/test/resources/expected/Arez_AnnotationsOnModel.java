import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.ComputedValue;
import arez.Disposable;
import arez.Observable;
import arez.component.Identifiable;
import java.util.Objects;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.braincheck.Guards;

@Generated("arez.processor.ArezProcessor")
@SuppressWarnings("unchecked")
public final class Arez_AnnotationsOnModel extends AnnotationsOnModel implements Disposable, Identifiable<Long> {
  private static volatile long $$arezi$$_nextId;

  private final long $$arezi$$_id;

  private byte $$arezi$$_state;

  @Nullable
  private final ArezContext $$arezi$$_context;

  private final Component $$arezi$$_component;

  private final Observable<Boolean> $$arezi$$_disposedObservable;

  @Nonnull
  private final Observable<String> $$arez$$_time;

  @Nonnull
  private final ComputedValue<Integer> $$arez$$_someValue;

  public Arez_AnnotationsOnModel() {
    super();
    this.$$arezi$$_context = Arez.areZonesEnabled() ? Arez.context() : null;
    this.$$arezi$$_id = $$arezi$$_nextId++;
    this.$$arezi$$_state = 1;
    this.$$arezi$$_component = Arez.areNativeComponentsEnabled() ? $$arezi$$_context().createComponent( "AnnotationsOnModel", $$arezi$$_id(), $$arezi$$_name(), null, null ) : null;
    this.$$arezi$$_disposedObservable = $$arezi$$_context().createObservable( Arez.areNativeComponentsEnabled() ? this.$$arezi$$_component : null, Arez.areNamesEnabled() ? $$arezi$$_name() + ".isDisposed" : null, Arez.arePropertyIntrospectorsEnabled() ? () -> this.$$arezi$$_state >= 0 : null, null );
    this.$$arez$$_time = $$arezi$$_context().createObservable( Arez.areNativeComponentsEnabled() ? this.$$arezi$$_component : null, Arez.areNamesEnabled() ? $$arezi$$_name() + ".time" : null, Arez.arePropertyIntrospectorsEnabled() ? () -> super.getTime() : null, Arez.arePropertyIntrospectorsEnabled() ? v -> super.setTime( v ) : null );
    this.$$arez$$_someValue = $$arezi$$_context().createComputedValue( Arez.areNativeComponentsEnabled() ? this.$$arezi$$_component : null, Arez.areNamesEnabled() ? $$arezi$$_name() + ".someValue" : null, super::someValue, Objects::equals, null, null, null, null );
    if ( Arez.areNativeComponentsEnabled() ) {
      this.$$arezi$$_component.complete();
    }
    this.$$arezi$$_state = 2;
    this.$$arezi$$_state = 3;
  }

  final ArezContext $$arezi$$_context() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> this.$$arezi$$_state == 0, () -> "Method invoked on uninitialized component named '" + $$arezi$$_name() + "'" );
    }
    return Arez.areZonesEnabled() ? this.$$arezi$$_context : Arez.context();
  }

  final long $$arezi$$_id() {
    return this.$$arezi$$_id;
  }

  @Override
  @Nonnull
  public final Long getArezId() {
    return $$arezi$$_id();
  }

  String $$arezi$$_name() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> this.$$arezi$$_state == 0, () -> "Method invoked on uninitialized component named '" + $$arezi$$_name() + "'" );
    }
    return "AnnotationsOnModel." + $$arezi$$_id();
  }

  @Override
  public boolean isDisposed() {
    if ( $$arezi$$_context().isTransactionActive() && !this.$$arezi$$_disposedObservable.isDisposed() )  {
      this.$$arezi$$_disposedObservable.reportObserved();
    }
    return this.$$arezi$$_state < 0;
  }

  @Override
  public void dispose() {
    if ( !isDisposed() ) {
      this.$$arezi$$_state = -2;
      if ( Arez.areNativeComponentsEnabled() ) {
        this.$$arezi$$_component.dispose();
      } else {
        $$arezi$$_context().safeAction( Arez.areNamesEnabled() ? $$arezi$$_name() + ".dispose" : null, () -> { {
          this.$$arezi$$_disposedObservable.dispose();
          this.$$arez$$_someValue.dispose();
          this.$$arez$$_time.dispose();
        } } );
      }
      this.$$arezi$$_state = -1;
    }
  }

  @Nonnull
  @Override
  public String getTime() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> this.$$arezi$$_state >= 2, () -> "Method invoked on dispos" + (this.$$arezi$$_state == -2 ? "ing" : "ed" ) + " component named '" + $$arezi$$_name() + "'" );
    }
    this.$$arez$$_time.reportObserved();
    return super.getTime();
  }

  @Override
  public void setTime(@Nonnull final String time) {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> this.$$arezi$$_state >= 2, () -> "Method invoked on dispos" + (this.$$arezi$$_state == -2 ? "ing" : "ed" ) + " component named '" + $$arezi$$_name() + "'" );
    }
    if ( !Objects.equals( time, super.getTime() ) ) {
      this.$$arez$$_time.preReportChanged();
      super.setTime(time);
      this.$$arez$$_time.reportChanged();
    }
  }

  @Override
  public void doStuff(@Nonnull final String time) {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> this.$$arezi$$_state >= 2, () -> "Method invoked on dispos" + (this.$$arezi$$_state == -2 ? "ing" : "ed" ) + " component named '" + $$arezi$$_name() + "'" );
    }
    try {
      $$arezi$$_context().safeAction(Arez.areNamesEnabled() ? $$arezi$$_name() + ".doStuff" : null, true, () -> super.doStuff(time), time );
    } catch( final RuntimeException | Error $$arez_exception$$ ) {
      throw $$arez_exception$$;
    } catch( final Throwable $$arez_exception$$ ) {
      throw new IllegalStateException( $$arez_exception$$ );
    }
  }

  @Nonnull
  @Override
  public Integer someValue() {
    if ( Arez.shouldCheckApiInvariants() ) {
      Guards.apiInvariant( () -> this.$$arezi$$_state >= 2, () -> "Method invoked on dispos" + (this.$$arezi$$_state == -2 ? "ing" : "ed" ) + " component named '" + $$arezi$$_name() + "'" );
    }
    return this.$$arez$$_someValue.get();
  }

  @Override
  public final int hashCode() {
    return Long.hashCode( $$arezi$$_id() );
  }

  @Override
  public final boolean equals(final Object o) {
    if ( this == o ) {
      return true;
    } else if ( null == o || !(o instanceof Arez_AnnotationsOnModel) ) {
      return false;
    } else {
      final Arez_AnnotationsOnModel that = (Arez_AnnotationsOnModel) o;;
      return $$arezi$$_id() == that.$$arezi$$_id();
    }
  }

  @Override
  public final String toString() {
    if ( Arez.areNamesEnabled() ) {
      return "ArezComponent[" + $$arezi$$_name() + "]";
    } else {
      return super.toString();
    }
  }
}
