package com.example.action;

import java.text.ParseException;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import org.realityforge.arez.Arez;
import org.realityforge.arez.ArezContext;
import org.realityforge.arez.Component;
import org.realityforge.arez.Disposable;
import org.realityforge.arez.Observable;
import org.realityforge.braincheck.Guards;

@Generated("org.realityforge.arez.processor.ArezProcessor")
public final class Arez_UnsafeSpecificFunctionActionModel extends UnsafeSpecificFunctionActionModel implements Disposable {
  private static volatile long $$arez$$_nextId;

  private final long $$arez$$_id;

  private boolean $$arez$$_disposed;

  @Nullable
  private final ArezContext $$arez$$_context;

  private final Component $$arez$$_component;

  private final Observable<Boolean> $$arez$$_disposedObservable;

  public Arez_UnsafeSpecificFunctionActionModel() {
    super();
    this.$$arez$$_context = Arez.areZonesEnabled() ? Arez.context() : null;
    this.$$arez$$_id = $$arez$$_nextId++;
    this.$$arez$$_component = Arez.areNativeComponentsEnabled() ? $$arez$$_context().createComponent( "UnsafeSpecificFunctionActionModel", $$arez$$_id(), $$arez$$_name(), null, null ) : null;
    this.$$arez$$_disposedObservable = $$arez$$_context().createObservable( Arez.areNativeComponentsEnabled() ? this.$$arez$$_component : null, Arez.areNamesEnabled() ? $$arez$$_name() + ".isDisposed" : null, Arez.arePropertyIntrospectorsEnabled() ? () -> this.$$arez$$_disposed : null, null );
    if ( Arez.areNativeComponentsEnabled() ) {
      this.$$arez$$_component.complete();
    }
  }

  final ArezContext $$arez$$_context() {
    return Arez.areZonesEnabled() ? this.$$arez$$_context : Arez.context();
  }

  final long $$arez$$_id() {
    return this.$$arez$$_id;
  }

  String $$arez$$_name() {
    return "UnsafeSpecificFunctionActionModel." + $$arez$$_id();
  }

  @Override
  public boolean isDisposed() {
    if ( $$arez$$_context().isTransactionActive() && !this.$$arez$$_disposedObservable.isDisposed() )  {
      this.$$arez$$_disposedObservable.reportObserved();
      return this.$$arez$$_disposed;
    } else {
      return this.$$arez$$_disposed;
    }
  }

  @Override
  public void dispose() {
    if ( !isDisposed() ) {
      this.$$arez$$_disposed = true;
      if ( Arez.areNativeComponentsEnabled() ) {
        this.$$arez$$_component.dispose();
      } else {
        $$arez$$_context().safeAction( Arez.areNamesEnabled() ? $$arez$$_name() + ".dispose" : null, () -> { {
          this.$$arez$$_disposedObservable.dispose();
        } } );
      }
    }
  }

  @Override
  public int doStuff(final long time) throws ParseException {
    Guards.invariant( () -> !this.$$arez$$_disposed, () -> "Method invoked on invalid component '" + $$arez$$_name() + "'" );
    try {
      return $$arez$$_context().action(Arez.areNamesEnabled() ? $$arez$$_name() + ".doStuff" : null, true, () -> super.doStuff(time), time );
    } catch( final ParseException $$arez$$_e ) {
      throw $$arez$$_e;
    } catch( final RuntimeException $$arez$$_e ) {
      throw $$arez$$_e;
    } catch( final Exception $$arez$$_e ) {
      throw new IllegalStateException( $$arez$$_e );
    } catch( final Error $$arez$$_e ) {
      throw $$arez$$_e;
    } catch( final Throwable $$arez$$_e ) {
      throw new IllegalStateException( $$arez$$_e );
    }
  }

  @Override
  public final int hashCode() {
    return Long.hashCode( $$arez$$_id() );
  }

  @Override
  public final boolean equals(final Object o) {
    if ( this == o ) {
      return true;
    } else if ( null == o || !(o instanceof Arez_UnsafeSpecificFunctionActionModel) ) {
      return false;
    } else {
      final Arez_UnsafeSpecificFunctionActionModel that = (Arez_UnsafeSpecificFunctionActionModel) o;;
      return $$arez$$_id() == that.$$arez$$_id();
    }
  }

  @Override
  public final String toString() {
    if ( Arez.areNamesEnabled() ) {
      return "ArezComponent[" + $$arez$$_name() + "]";
    } else {
      return super.toString();
    }
  }
}
