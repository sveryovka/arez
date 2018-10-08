package arez.component;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.ComputedValue;
import arez.Disposable;
import arez.Flags;
import arez.Procedure;
import arez.SafeFunction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * The class responsible for caching
 */
public final class MemoizeCache<T>
  implements Disposable
{
  /**
   * Functional interface for calculating memoizable value.
   *
   * @param <T> The type of the returned value.
   */
  @FunctionalInterface
  public interface Function<T>
  {
    /**
     * Return calculated memoizable value.
     *
     * @param args the functions arguments.
     * @return the value generated by function.
     */
    T call( @Nonnull final Object... args );
  }

  /**
   * Reference to the system to which this node belongs.
   */
  @Nullable
  private final ArezContext _context;
  /**
   * A human consumable prefix for computed values. It should be non-null if {@link Arez#areNamesEnabled()} returns
   * true and <tt>null</tt> otherwise.
   */
  @Nullable
  private final String _name;
  /**
   * The component that this memoize cache is contained within.
   * This should only be set if {@link Arez#areNativeComponentsEnabled()} is true but can be null even if this is true.
   */
  @Nullable
  private final Component _component;
  /**
   * The function memoized.
   */
  @Nonnull
  private final Function<T> _function;
  /**
   * The cache of all the ComputedValues created for each unique combination of parameters.
   */
  private final Map<Object, Object> _cache = new HashMap<>();
  /**
   * The number of arguments passed to memoized function.
   */
  private final int _argCount;
  /**
   * The flags passed to the created ComputedValues.
   */
  private final int _flags;
  /**
   * The index of the next ComputedValue created.
   * This is only used when creating unique names for ComputedValues.
   */
  private int _nextIndex;
  /**
   * Flag indicating that the cache is currently being disposed.
   */
  private boolean _disposed;

  /**
   * Create the Memoize method cache.
   *
   * @param context   the context in which to create ComputedValue instances.
   * @param component the associated native component if any. This should only be set if {@link Arez#areNativeComponentsEnabled()} returns true.
   * @param name      a human consumable prefix for computed values.
   * @param function  the memoized function.
   * @param argCount  the number of arguments expected to be passed to memoized function.
   */
  public MemoizeCache( @Nullable final ArezContext context,
                       @Nullable final Component component,
                       @Nullable final String name,
                       @Nonnull final Function<T> function,
                       final int argCount )
  {
    this( context, component, name, function, argCount, 0 );
  }

  /**
   * Create the Memoize method cache.
   *
   * @param context   the context in which to create ComputedValue instances.
   * @param component the associated native component if any. This should only be set if {@link Arez#areNativeComponentsEnabled()} returns true.
   * @param name      a human consumable prefix for computed values.
   * @param function  the memoized function.
   * @param argCount  the number of arguments expected to be passed to memoized function.
   * @param flags     the flags that are used when creating ComputedValue instances. The only flags supported are the PRIORITY_* flags and {@link Flags#OBSERVE_LOWER_PRIORITY_DEPENDENCIES}.
   */
  public MemoizeCache( @Nullable final ArezContext context,
                       @Nullable final Component component,
                       @Nullable final String name,
                       @Nonnull final Function<T> function,
                       final int argCount,
                       final int flags )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Arez.areZonesEnabled() || null == context,
                    () -> "Arez-174: MemoizeCache passed a context but Arez.areZonesEnabled() is false" );
      apiInvariant( () -> Arez.areNamesEnabled() || null == name,
                    () -> "Arez-0159: MemoizeCache passed a name '" + name + "' but Arez.areNamesEnabled() is false" );
      apiInvariant( () -> argCount > 0,
                    () -> "Arez-0160: MemoizeCache constructed with invalid argCount: " + argCount +
                          ". Expected positive value." );
      final int mask = Flags.PRIORITY_HIGHEST |
                       Flags.PRIORITY_HIGH |
                       Flags.PRIORITY_NORMAL |
                       Flags.PRIORITY_LOW |
                       Flags.PRIORITY_LOWEST |
                       Flags.ENVIRONMENT_REQUIRED |
                       Flags.ENVIRONMENT_NOT_REQUIRED |
                       Flags.OBSERVE_LOWER_PRIORITY_DEPENDENCIES;
      apiInvariant( () -> ( ~mask & flags ) == 0,
                    () -> "Arez-0211: MemoizeCache passed unsupported flags. Unsupported bits: " + ( ~mask & flags ) );
    }
    _context = Arez.areZonesEnabled() ? Objects.requireNonNull( context ) : null;
    _component = Arez.areNativeComponentsEnabled() ? component : null;
    _name = Arez.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _function = Objects.requireNonNull( function );
    _argCount = argCount;
    _flags = flags;
  }

  /**
   * Return the result of the memoized function, calculating if necessary.
   *
   * @param args the arguments passed to the memoized function.
   * @return the result of the memoized function.
   */
  public T get( @Nonnull final Object... args )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( this::isNotDisposed,
                    () -> "Arez-0161: MemoizeCache named '" + _name + "' had get() invoked when disposed." );
    }
    return getComputedValue( args ).get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisposed()
  {
    return _disposed;
  }

  @Override
  public void dispose()
  {
    if ( !_disposed )
    {
      _disposed = true;
      getContext().safeAction( Arez.areNamesEnabled() ? _name : null, () -> {
        disposeMap( _cache, _argCount );
        _cache.clear();
      }, Flags.NO_VERIFY_ACTION_REQUIRED );
    }
  }

  @Nonnull
  final ArezContext getContext()
  {
    return Arez.areZonesEnabled() ? Objects.requireNonNull( _context ) : Arez.context();
  }

  /**
   * Traverse to leaf map elements and dispose all contained ComputedValue instances.
   */
  @SuppressWarnings( "unchecked" )
  private void disposeMap( @Nonnull final Map<Object, Object> map, final int depth )
  {
    if ( 1 == depth )
    {
      for ( final Map.Entry<Object, Object> entry : map.entrySet() )
      {
        final ComputedValue<?> computedValue = (ComputedValue<?>) entry.getValue();
        computedValue.dispose();
      }
    }
    else
    {
      for ( final Map.Entry<Object, Object> entry : map.entrySet() )
      {
        disposeMap( (Map<Object, Object>) entry.getValue(), depth - 1 );
      }
    }
  }

  /**
   * Retrieve the computed value for specified parameters, creating it if necessary.
   *
   * @param args the arguments passed to the memoized function.
   */
  @SuppressWarnings( "unchecked" )
  ComputedValue<T> getComputedValue( @Nonnull final Object... args )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> args.length == _argCount,
                    () -> "Arez-0162: MemoizeCache.getComputedValue called with " + args.length +
                          " arguments but expected " + _argCount + " arguments." );
    }
    Map<Object, Object> map = _cache;
    final int size = args.length - 1;
    for ( int i = 0; i < size; i++ )
    {
      map = (Map<Object, Object>) map.computeIfAbsent( args[ i ], v -> new HashMap<>() );
    }
    ComputedValue<T> computedValue =
      (ComputedValue<T>) map.computeIfAbsent( args[ size ], v -> createComputedValue( args ) );
    if ( Disposable.isDisposed( computedValue ) )
    {
      computedValue = createComputedValue( args );
      map.put( args[ size ], computedValue );
    }
    return computedValue;
  }

  /**
   * Create computed value for specified parameters.
   *
   * @param args the arguments passed to the memoized function.
   */
  private ComputedValue<T> createComputedValue( @Nonnull final Object... args )
  {
    final Component component = Arez.areNativeComponentsEnabled() ? _component : null;
    final String name = Arez.areNamesEnabled() ? _name + "." + _nextIndex++ : null;
    final Procedure onDeactivate = () -> disposeComputedValue( args );
    final SafeFunction<T> function = () -> _function.call( args );
    return getContext().computed( component, name, function, null, onDeactivate, null, _flags );
  }

  /**
   * Method invoked to dispose memoized value.
   * This is called from deactivate hook so there should always by a cached value present
   * and thus we never check for missing elements in chain.
   *
   * @param args the arguments originally passed to the memoized function.
   */
  @SuppressWarnings( "unchecked" )
  void disposeComputedValue( @Nonnull final Object... args )
  {
    if ( Arez.shouldCheckInvariants() )
    {
      invariant( () -> args.length == _argCount,
                 () -> "Arez-0163: MemoizeCache.disposeComputedValue called with " + args.length +
                       " argument(s) but expected " + _argCount + " argument(s)." );
    }
    if ( _disposed )
    {
      return;
    }
    final Stack<Map> stack = new Stack<>();
    stack.push( _cache );
    final int size = args.length - 1;
    for ( int i = 0; i < size; i++ )
    {
      stack.push( (Map) stack.peek().get( args[ i ] ) );
    }
    final ComputedValue<T> computedValue = (ComputedValue<T>) stack.peek().remove( args[ size ] );
    if ( Arez.shouldCheckInvariants() )
    {
      invariant( () -> null != computedValue,
                 () -> "Arez-0193: MemoizeCache.disposeComputedValue called with args " + Arrays.asList( args ) +
                       " but unable to locate corresponding ComputedValue." );
    }
    assert null != computedValue;
    getContext().scheduleDispose( computedValue );
    while ( stack.size() > 1 )
    {
      final Map map = stack.pop();
      if ( map.isEmpty() )
      {
        stack.peek().remove( args[ stack.size() - 1 ] );
      }
      else
      {
        return;
      }
    }
  }

  Map<Object, Object> getCache()
  {
    return _cache;
  }

  int getNextIndex()
  {
    return _nextIndex;
  }

  int getFlags()
  {
    return _flags;
  }
}
