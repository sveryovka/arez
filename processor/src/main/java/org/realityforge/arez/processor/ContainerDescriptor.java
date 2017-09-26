package org.realityforge.arez.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.realityforge.arez.annotations.Action;
import org.realityforge.arez.annotations.Autorun;
import org.realityforge.arez.annotations.Computed;
import org.realityforge.arez.annotations.ContainerId;
import org.realityforge.arez.annotations.ContainerName;
import org.realityforge.arez.annotations.ContainerNamePrefix;
import org.realityforge.arez.annotations.Observable;
import org.realityforge.arez.annotations.OnActivate;
import org.realityforge.arez.annotations.OnDeactivate;
import org.realityforge.arez.annotations.OnDepsUpdated;
import org.realityforge.arez.annotations.OnDispose;
import org.realityforge.arez.annotations.OnStale;
import org.realityforge.arez.annotations.PostDispose;
import org.realityforge.arez.annotations.PreDispose;
import org.realityforge.arez.annotations.Tracked;

/**
 * The class that represents the parsed state of Container annotated class.
 */
final class ContainerDescriptor
{
  private static final Pattern SETTER_PATTERN = Pattern.compile( "^set([A-Z].*)$" );
  private static final Pattern GETTER_PATTERN = Pattern.compile( "^get([A-Z].*)$" );
  private static final Pattern ISSER_PATTERN = Pattern.compile( "^is([A-Z].*)$" );
  private static final List<String> OBJECT_METHODS =
    Arrays.asList( "hashCode", "equals", "clone", "toString", "finalize" );

  @Nonnull
  private final String _name;
  private final boolean _singleton;
  private final boolean _disposable;
  @Nonnull
  private final PackageElement _packageElement;
  @Nonnull
  private final TypeElement _element;
  @Nullable
  private ExecutableElement _postConstruct;
  @Nullable
  private ExecutableElement _containerId;
  @Nullable
  private ExecutableElement _containerNamePrefix;
  @Nullable
  private ExecutableElement _containerName;
  @Nullable
  private ExecutableElement _preDispose;
  @Nullable
  private ExecutableElement _postDispose;
  private final Map<String, ObservableDescriptor> _observables = new HashMap<>();
  private final Collection<ObservableDescriptor> _roObservables =
    Collections.unmodifiableCollection( _observables.values() );
  private final Map<String, ActionDescriptor> _actions = new HashMap<>();
  private final Collection<ActionDescriptor> _roActions =
    Collections.unmodifiableCollection( _actions.values() );
  private final Map<String, ComputedDescriptor> _computeds = new HashMap<>();
  private final Collection<ComputedDescriptor> _roComputeds =
    Collections.unmodifiableCollection( _computeds.values() );
  private final Map<String, AutorunDescriptor> _autoruns = new HashMap<>();
  private final Collection<AutorunDescriptor> _roAutoruns =
    Collections.unmodifiableCollection( _autoruns.values() );
  private final Map<String, TrackedDescriptor> _trackeds = new HashMap<>();
  private final Collection<TrackedDescriptor> _roTrackeds =
    Collections.unmodifiableCollection( _trackeds.values() );

  ContainerDescriptor( @Nonnull final String name,
                       final boolean singleton,
                       final boolean disposable,
                       @Nonnull final PackageElement packageElement,
                       @Nonnull final TypeElement element )
  {
    _name = Objects.requireNonNull( name );
    _singleton = singleton;
    _disposable = disposable;
    _packageElement = Objects.requireNonNull( packageElement );
    _element = Objects.requireNonNull( element );
  }

  @Nonnull
  String getName()
  {
    return _name;
  }

  /**
   * Get the prefix specified by container if any.
   */
  @Nonnull
  String getNamePrefix()
  {
    return getName().isEmpty() ? "" : getName() + ".";
  }

  boolean isSingleton()
  {
    return _singleton;
  }

  boolean isDisposable()
  {
    return _disposable;
  }

  @Nonnull
  private DeclaredType asDeclaredType()
  {
    return (DeclaredType) _element.asType();
  }

  @Nonnull
  PackageElement getPackageElement()
  {
    return _packageElement;
  }

  @Nonnull
  TypeElement getElement()
  {
    return _element;
  }

  @Nonnull
  private ObservableDescriptor findOrCreateObservable( @Nonnull final String name )
  {
    return _observables.computeIfAbsent( name, n -> new ObservableDescriptor( this, n ) );
  }

  @Nonnull
  private TrackedDescriptor findOrCreateTracked( @Nonnull final String name )
  {
    return _trackeds.computeIfAbsent( name, n -> new TrackedDescriptor( this, n ) );
  }

  private void addObservable( @Nonnull final Observable annotation,
                              @Nonnull final ExecutableElement method,
                              @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( Observable.class, method );

    final TypeMirror returnType = method.getReturnType();
    final String methodName = method.getSimpleName().toString();
    String name;
    final boolean setter;
    if ( TypeKind.VOID == returnType.getKind() )
    {
      setter = true;
      //Should be a setter
      if ( 1 != method.getParameters().size() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable should be a setter or getter", method );
      }

      name = ProcessorUtil.deriveName( method, SETTER_PATTERN, annotation.name() );
      if ( null == name )
      {
        name = methodName;
      }
    }
    else
    {
      setter = false;
      //Must be a getter
      if ( 0 != method.getParameters().size() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable should be a setter or getter", method );
      }
      name = getPropertyAccessorName( method, annotation.name() );
    }
    // Override name if supplied by user
    if ( !ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      name = annotation.name();
      if ( !name.isEmpty() )
      {
        if ( !ProcessorUtil.isJavaIdentifier( name ) )
        {
          throw new ArezProcessorException( "Method annotated with @Observable specified invalid name " + name,
                                            method );
        }
      }
    }
    checkNameUnique( name, method, Observable.class );
    final ObservableDescriptor observable = findOrCreateObservable( name );
    if ( setter )
    {
      if ( observable.hasSetter() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable defines duplicate setter for " +
                                          "observable named " + name, method );
      }
      observable.setSetter( method, methodType );
    }
    else
    {
      if ( observable.hasGetter() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable defines duplicate getter for " +
                                          "observable named " + name, method );
      }
      observable.setGetter( method, methodType );
    }
  }

  private void addAction( @Nonnull final Action annotation,
                          @Nonnull final ExecutableElement method,
                          @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( Action.class, method );

    final String name = deriveActionName( method, annotation );
    checkNameUnique( name, method, Action.class );
    final ActionDescriptor action = new ActionDescriptor( this, name, annotation.mutation(), method, methodType );
    _actions.put( action.getName(), action );
  }

  @Nonnull
  private String deriveActionName( @Nonnull final ExecutableElement method, @Nonnull final Action annotation )
    throws ArezProcessorException
  {
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      return method.getSimpleName().toString();
    }
    else
    {
      final String name = annotation.name();
      if ( name.isEmpty() || !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Action specified invalid name " + name, method );
      }
      return name;
    }
  }

  private void addAutorun( @Nonnull final Autorun annotation,
                           @Nonnull final ExecutableElement method,
                           @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( Autorun.class, method );
    MethodChecks.mustNotHaveAnyParameters( Autorun.class, method );
    MethodChecks.mustNotThrowAnyExceptions( Autorun.class, method );
    MethodChecks.mustNotReturnAnyValue( Autorun.class, method );

    final String name = deriveAutorunName( method, annotation );
    checkNameUnique( name, method, Autorun.class );
    final AutorunDescriptor autorun = new AutorunDescriptor( this, name, annotation.mutation(), method, methodType );
    _autoruns.put( autorun.getName(), autorun );
  }

  @Nonnull
  private String deriveAutorunName( @Nonnull final ExecutableElement method, @Nonnull final Autorun annotation )
    throws ArezProcessorException
  {
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      return method.getSimpleName().toString();
    }
    else
    {
      final String name = annotation.name();
      if ( name.isEmpty() || !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Autorun specified invalid name " + name, method );
      }
      return name;
    }
  }

  private void addOnDepsUpdated( @Nonnull final OnDepsUpdated annotation, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    final String name =
      deriveHookName( method, TrackedDescriptor.ON_DEPS_UPDATED_PATTERN, "DepsUpdated", annotation.name() );
    findOrCreateTracked( name ).setOnDepsUpdatedMethod( method );
  }

  private void addTracked( @Nonnull final Tracked annotation,
                           @Nonnull final ExecutableElement method,
                           @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    final String name = deriveTrackedName( method, annotation );
    checkNameUnique( name, method, Tracked.class );
    final TrackedDescriptor tracked = findOrCreateTracked( name );
    tracked.setTrackedMethod( annotation.mutation(), method, methodType );
  }

  @Nonnull
  private String deriveTrackedName( @Nonnull final ExecutableElement method, @Nonnull final Tracked annotation )
    throws ArezProcessorException
  {
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      return method.getSimpleName().toString();
    }
    else
    {
      final String name = annotation.name();
      if ( name.isEmpty() || !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Tracked specified invalid name " + name, method );
      }
      return name;
    }
  }

  @Nonnull
  private ComputedDescriptor findOrCreateComputed( @Nonnull final String name )
  {
    return _computeds.computeIfAbsent( name, n -> new ComputedDescriptor( this, n ) );
  }

  private void addComputed( @Nonnull final Computed annotation,
                            @Nonnull final ExecutableElement method,
                            @Nonnull final ExecutableType computedType )
    throws ArezProcessorException
  {
    final String name = deriveComputedName( method, annotation );
    checkNameUnique( name, method, Computed.class );
    findOrCreateComputed( name ).setComputed( method, computedType );
  }

  @Nonnull
  private String deriveComputedName( @Nonnull final ExecutableElement method, @Nonnull final Computed annotation )
    throws ArezProcessorException
  {
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      return getPropertyAccessorName( method, annotation.name() );
    }
    else
    {
      final String name = annotation.name();
      if ( name.isEmpty() || !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Computed specified invalid name " + name, method );
      }
      return name;
    }
  }

  private void addOnActivate( @Nonnull final OnActivate annotation, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    final String name = deriveHookName( method, ComputedDescriptor.ON_ACTIVATE_PATTERN, "Activate", annotation.name() );
    findOrCreateComputed( name ).setOnActivate( method );
  }

  private void addOnDeactivate( @Nonnull final OnDeactivate annotation, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    final String name =
      deriveHookName( method, ComputedDescriptor.ON_DEACTIVATE_PATTERN, "Deactivate", annotation.name() );
    findOrCreateComputed( name ).setOnDeactivate( method );
  }

  private void addOnStale( @Nonnull final OnStale annotation, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    final String name = deriveHookName( method, ComputedDescriptor.ON_STALE_PATTERN, "Stale", annotation.name() );
    findOrCreateComputed( name ).setOnStale( method );
  }

  private void addOnDispose( @Nonnull final OnDispose annotation, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    final String name = deriveHookName( method, ComputedDescriptor.ON_DISPOSE_PATTERN, "Dispose", annotation.name() );
    findOrCreateComputed( name ).setOnDispose( method );
  }

  @Nonnull
  private String deriveHookName( @Nonnull final ExecutableElement method,
                                 @Nonnull final Pattern pattern,
                                 @Nonnull final String type,
                                 @Nonnull final String name )
    throws ArezProcessorException
  {
    final String value = ProcessorUtil.deriveName( method, pattern, name );
    if ( null == value )
    {
      throw new ArezProcessorException( "Unable to derive name for @On" + type + " as does not match " +
                                        "on[Name]" + type + " pattern. Please specify name.", method );
    }
    else if ( value.isEmpty() || !ProcessorUtil.isJavaIdentifier( value ) )
    {
      throw new ArezProcessorException( "Method annotated with @On" + type + " specified invalid name " + value,
                                        method );
    }
    else
    {
      return value;
    }
  }

  private void setContainerId( @Nonnull final ExecutableElement containerId )
    throws ArezProcessorException
  {
    if ( isSingleton() )
    {
      throw new ArezProcessorException( "@ContainerId must not exist if @Container is a singleton", containerId );
    }

    MethodChecks.mustBeSubclassCallable( ContainerId.class, containerId );
    MethodChecks.mustBeFinal( ContainerId.class, containerId );
    MethodChecks.mustNotHaveAnyParameters( ContainerId.class, containerId );
    MethodChecks.mustReturnAValue( ContainerId.class, containerId );
    MethodChecks.mustNotThrowAnyExceptions( ContainerId.class, containerId );

    if ( null != _containerId )
    {
      throw new ArezProcessorException( "@ContainerId target duplicates existing method named " +
                                        _containerId.getSimpleName(), containerId );
    }
    else
    {
      _containerId = Objects.requireNonNull( containerId );
    }
  }

  private void setContainerNamePrefix( @Nonnull final ExecutableElement containerNamePrefix )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ContainerNamePrefix.class, containerNamePrefix );
    MethodChecks.mustNotHaveAnyParameters( ContainerNamePrefix.class, containerNamePrefix );
    MethodChecks.mustReturnAValue( ContainerNamePrefix.class, containerNamePrefix );
    MethodChecks.mustNotThrowAnyExceptions( ContainerNamePrefix.class, containerNamePrefix );

    final TypeMirror returnType = containerNamePrefix.getReturnType();
    if ( !( TypeKind.DECLARED == returnType.getKind() &&
            returnType.toString().equals( String.class.getName() ) ) )
    {
      throw new ArezProcessorException( "@ContainerNamePrefix target must return a String", containerNamePrefix );
    }

    if ( null != _containerNamePrefix )
    {
      throw new ArezProcessorException( "@ContainerNamePrefix target duplicates existing method named " +
                                        _containerNamePrefix.getSimpleName(), containerNamePrefix );
    }
    else
    {
      _containerNamePrefix = Objects.requireNonNull( containerNamePrefix );
    }
  }

  private void setContainerName( @Nonnull final ExecutableElement containerName )
    throws ArezProcessorException
  {
    if ( isSingleton() )
    {
      throw new ArezProcessorException( "@ContainerName must not exist if @Container is a singleton", containerName );
    }

    MethodChecks.mustBeOverridable( ContainerName.class, containerName );
    MethodChecks.mustNotHaveAnyParameters( ContainerName.class, containerName );
    MethodChecks.mustReturnAValue( ContainerName.class, containerName );
    MethodChecks.mustNotThrowAnyExceptions( ContainerName.class, containerName );

    if ( null != _containerName )
    {
      throw new ArezProcessorException( "@ContainerName target duplicates existing method named " +
                                        _containerName.getSimpleName(), containerName );
    }
    else
    {
      _containerName = Objects.requireNonNull( containerName );
    }
  }

  @Nullable
  ExecutableElement getPostConstruct()
  {
    return _postConstruct;
  }

  void setPostConstruct( @Nonnull final ExecutableElement postConstruct )
    throws ArezProcessorException
  {
    MethodChecks.mustBeLifecycleHook( PostConstruct.class, postConstruct );

    if ( null != _postConstruct )
    {
      throw new ArezProcessorException( "@PostConstruct target duplicates existing method named " +
                                        _postConstruct.getSimpleName(), postConstruct );
    }
    else
    {
      _postConstruct = postConstruct;
    }
  }

  private void setPreDispose( @Nonnull final ExecutableElement preDispose )
    throws ArezProcessorException
  {
    if ( !isDisposable() )
    {
      throw new ArezProcessorException( "@PreDispose must not exist if @Container set disposable to false",
                                        preDispose );
    }
    MethodChecks.mustBeLifecycleHook( PreDispose.class, preDispose );

    if ( null != _preDispose )
    {
      throw new ArezProcessorException( "@PreDispose target duplicates existing method named " +
                                        _preDispose.getSimpleName(), preDispose );
    }
    else
    {
      _preDispose = preDispose;
    }
  }

  private void setPostDispose( @Nonnull final ExecutableElement postDispose )
    throws ArezProcessorException
  {
    if ( !isDisposable() )
    {
      throw new ArezProcessorException( "@PostDispose must not exist if @Container set disposable to false",
                                        postDispose );
    }

    MethodChecks.mustBeLifecycleHook( PostDispose.class, postDispose );

    if ( null != _postDispose )
    {
      throw new ArezProcessorException( "@PostDispose target duplicates existing method named " +
                                        _postDispose.getSimpleName(), postDispose );
    }
    else
    {
      _postDispose = postDispose;
    }
  }

  void validate()
    throws ArezProcessorException
  {
    validateComputeds();

    if ( _roObservables.isEmpty() &&
         _roActions.isEmpty() &&
         _roComputeds.isEmpty() &&
         _roTrackeds.isEmpty() &&
         _roAutoruns.isEmpty() )
    {
      throw new ArezProcessorException( "@Container target has no methods annotated with @Action, " +
                                        "@Computed, @Observable, @Tracked or @Autorun", _element );
    }
  }

  private void validateComputeds()
    throws ArezProcessorException
  {
    for ( final ComputedDescriptor computed : _roComputeds )
    {
      computed.validate();
    }
  }

  private void checkNameUnique( @Nonnull final String name,
                                @Nonnull final ExecutableElement sourceMethod,
                                @Nonnull final Class<? extends Annotation> sourceType )
    throws ArezProcessorException
  {
    final ActionDescriptor action = _actions.get( name );
    if ( null != action )
    {
      throw toException( name, sourceType, sourceMethod, Action.class, action.getAction() );
    }
    final ComputedDescriptor computed = _computeds.get( name );
    if ( null != computed )
    {
      throw toException( name, sourceType, sourceMethod, Computed.class, computed.getComputed() );
    }
    final AutorunDescriptor autorun = _autoruns.get( name );
    if ( null != autorun )
    {
      throw toException( name, sourceType, sourceMethod, Autorun.class, autorun.getAutorun() );
    }
    // Tracked have pairs so let the caller determine whether a duplicate occurs in that scenario
    if ( Tracked.class != sourceType )
    {
      final TrackedDescriptor tracked = _trackeds.get( name );
      if ( null != tracked )
      {
        throw toException( name, sourceType, sourceMethod, Tracked.class, tracked.getTrackedMethod() );
      }
    }
    // Observables have pairs so let the caller determine whether a duplicate occurs in that scenario
    if ( Observable.class != sourceType )
    {
      final ObservableDescriptor observable = _observables.get( name );
      if ( null != observable )
      {
        throw toException( name, sourceType, sourceMethod, Observable.class, observable.getDefiner() );
      }
    }
  }

  @Nonnull
  private ArezProcessorException toException( @Nonnull final String name,
                                              @Nonnull final Class<? extends Annotation> source,
                                              @Nonnull final ExecutableElement sourceMethod,
                                              @Nonnull final Class<? extends Annotation> target,
                                              @Nonnull final ExecutableElement targetElement )
  {
    return new ArezProcessorException( "Method annotated with @" + source.getSimpleName() + " specified name " +
                                       name + " that duplicates @" + target.getSimpleName() + " defined by " +
                                       "method " + targetElement.getSimpleName(), sourceMethod );
  }

  void analyzeCandidateMethods( @Nonnull final List<ExecutableElement> methods,
                                @Nonnull final Types typeUtils )
    throws ArezProcessorException
  {
    final Map<String, CandidateMethod> getters = new HashMap<>();
    final Map<String, CandidateMethod> setters = new HashMap<>();
    final Map<String, CandidateMethod> trackeds = new HashMap<>();
    final Map<String, CandidateMethod> onDepsChangeds = new HashMap<>();
    for ( final ExecutableElement method : methods )
    {
      final ExecutableType methodType =
        (ExecutableType) typeUtils.asMemberOf( (DeclaredType) _element.asType(), method );
      if ( !analyzeMethod( method, methodType ) )
      {
        /*
         * If we get here the method was not annotated so we can try to detect if it is a
         * candidate @Observable in case some @Observables are not fully specified.
         */
        if ( method.getModifiers().contains( Modifier.FINAL ) )
        {
          continue;
        }
        else if ( method.getModifiers().contains( Modifier.STATIC ) )
        {
          continue;
        }

        final CandidateMethod candidateMethod = new CandidateMethod( method, methodType );
        final boolean voidReturn = method.getReturnType().getKind() == TypeKind.VOID;
        final int parameterCount = method.getParameters().size();
        String name;

        name = ProcessorUtil.deriveName( method, SETTER_PATTERN, ProcessorUtil.SENTINEL_NAME );
        if ( voidReturn && 1 == parameterCount && null != name )
        {
          setters.put( name, candidateMethod );
          continue;
        }
        name = ProcessorUtil.deriveName( method, ISSER_PATTERN, ProcessorUtil.SENTINEL_NAME );
        if ( !voidReturn && 0 == parameterCount && null != name )
        {
          getters.put( name, candidateMethod );
          continue;
        }
        name = ProcessorUtil.deriveName( method, GETTER_PATTERN, ProcessorUtil.SENTINEL_NAME );
        if ( !voidReturn && 0 == parameterCount && null != name )
        {
          getters.put( name, candidateMethod );
          continue;
        }
        name =
          ProcessorUtil.deriveName( method, TrackedDescriptor.ON_DEPS_UPDATED_PATTERN, ProcessorUtil.SENTINEL_NAME );
        if ( voidReturn && 0 == parameterCount && null != name )
        {
          onDepsChangeds.put( name, candidateMethod );
          continue;
        }

        final String methodName = method.getSimpleName().toString();
        if ( !OBJECT_METHODS.contains( methodName) )
        {
          trackeds.put( methodName, candidateMethod );
        }
      }
    }

    linkUnAnnotatedObservables( getters, setters );
    linkUnAnnotatedTracked( trackeds, onDepsChangeds );
  }

  private void linkUnAnnotatedObservables( @Nonnull final Map<String, CandidateMethod> getters,
                                           @Nonnull final Map<String, CandidateMethod> setters )
    throws ArezProcessorException
  {
    for ( final ObservableDescriptor observable : _roObservables )
    {
      if ( !observable.hasSetter() )
      {
        final CandidateMethod candidate = setters.get( observable.getName() );
        if ( null != candidate )
        {
          observable.setSetter( candidate.getMethod(), candidate.getMethodType() );
        }
        else
        {
          throw new ArezProcessorException( "@Observable target defined getter but no setter was defined and no " +
                                            "setter could be automatically determined", observable.getGetter() );
        }
      }
      else if ( !observable.hasGetter() )
      {
        final CandidateMethod candidate = getters.get( observable.getName() );
        if ( null != candidate )
        {
          observable.setGetter( candidate.getMethod(), candidate.getMethodType() );
        }
        else
        {
          throw new ArezProcessorException( "@Observable target defined setter but no getter was defined and no " +
                                            "getter could be automatically determined", observable.getSetter() );
        }
      }
    }
  }

  private void linkUnAnnotatedTracked( @Nonnull final Map<String, CandidateMethod> trackeds,
                                       @Nonnull final Map<String, CandidateMethod> onDepsChangeds )
    throws ArezProcessorException
  {
    for ( final TrackedDescriptor tracked : _roTrackeds )
    {
      if ( !tracked.hasTrackedMethod() )
      {
        final CandidateMethod candidate = trackeds.get( tracked.getName() );
        if ( null != candidate )
        {
          tracked.setTrackedMethod( false, candidate.getMethod(), candidate.getMethodType() );
        }
        else
        {
          throw new ArezProcessorException( "@OnDepsUpdated target has no corresponding @Tracked that could " +
                                            "be automatically determined", tracked.getOnDepsUpdatedMethod() );
        }
      }
      else if ( !tracked.hasOnDepsUpdatedMethod() )
      {
        final CandidateMethod candidate = onDepsChangeds.get( tracked.getName() );
        if ( null != candidate )
        {
          tracked.setOnDepsUpdatedMethod( candidate.getMethod() );
        }
        else
        {
          throw new ArezProcessorException( "@Tracked target has no corresponding @OnDepsUpdated that could " +
                                            "be automatically determined", tracked.getTrackedMethod() );
        }
      }
    }
  }

  private boolean analyzeMethod( @Nonnull final ExecutableElement method,
                                 @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    verifyNoDuplicateAnnotations( method );

    final Action action = method.getAnnotation( Action.class );
    final Autorun autorun = method.getAnnotation( Autorun.class );
    final Observable observable = method.getAnnotation( Observable.class );
    final Computed computed = method.getAnnotation( Computed.class );
    final ContainerId containerId = method.getAnnotation( ContainerId.class );
    final ContainerNamePrefix containerNamePrefix = method.getAnnotation( ContainerNamePrefix.class );
    final ContainerName containerName = method.getAnnotation( ContainerName.class );
    final PostConstruct postConstruct = method.getAnnotation( PostConstruct.class );
    final PreDispose preDispose = method.getAnnotation( PreDispose.class );
    final PostDispose postDispose = method.getAnnotation( PostDispose.class );
    final OnActivate onActivate = method.getAnnotation( OnActivate.class );
    final OnDeactivate onDeactivate = method.getAnnotation( OnDeactivate.class );
    final OnStale onStale = method.getAnnotation( OnStale.class );
    final OnDispose onDispose = method.getAnnotation( OnDispose.class );
    final Tracked tracked = method.getAnnotation( Tracked.class );
    final OnDepsUpdated onDepsUpdated = method.getAnnotation( OnDepsUpdated.class );

    if ( null != observable )
    {
      addObservable( observable, method, methodType );
      return true;
    }
    else if ( null != action )
    {
      addAction( action, method, methodType );
      return true;
    }
    else if ( null != autorun )
    {
      addAutorun( autorun, method, methodType );
      return true;
    }
    else if ( null != tracked )
    {
      addTracked( tracked, method, methodType );
      return true;
    }
    else if ( null != onDepsUpdated )
    {
      addOnDepsUpdated( onDepsUpdated, method );
      return true;
    }
    else if ( null != computed )
    {
      addComputed( computed, method, methodType );
      return true;
    }
    else if ( null != containerId )
    {
      setContainerId( method );
      return true;
    }
    else if ( null != containerName )
    {
      setContainerName( method );
      return true;
    }
    else if ( null != containerNamePrefix )
    {
      setContainerNamePrefix( method );
      return true;
    }
    else if ( null != postConstruct )
    {
      setPostConstruct( method );
      return true;
    }
    else if ( null != preDispose )
    {
      setPreDispose( method );
      return true;
    }
    else if ( null != postDispose )
    {
      setPostDispose( method );
      return true;
    }
    else if ( null != onActivate )
    {
      addOnActivate( onActivate, method );
      return true;
    }
    else if ( null != onDeactivate )
    {
      addOnDeactivate( onDeactivate, method );
      return true;
    }
    else if ( null != onStale )
    {
      addOnStale( onStale, method );
      return true;
    }
    else if ( null != onDispose )
    {
      addOnDispose( onDispose, method );
      return true;
    }
    else
    {
      return false;
    }
  }

  private void verifyNoDuplicateAnnotations( @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    @SuppressWarnings( "unchecked" )
    final Class<? extends Annotation>[] annotationTypes =
      new Class[]{ Action.class,
                   Autorun.class,
                   Tracked.class,
                   OnDepsUpdated.class,
                   Observable.class,
                   Computed.class,
                   ContainerId.class,
                   ContainerName.class,
                   ContainerNamePrefix.class,
                   PostConstruct.class,
                   PreDispose.class,
                   PostDispose.class,
                   OnActivate.class,
                   OnDeactivate.class,
                   OnStale.class };
    for ( int i = 0; i < annotationTypes.length; i++ )
    {
      final Class<? extends Annotation> type1 = annotationTypes[ i ];
      final Object annotation1 = method.getAnnotation( type1 );
      if ( null != annotation1 )
      {
        for ( int j = i + 1; j < annotationTypes.length; j++ )
        {
          final Class<? extends Annotation> type2 = annotationTypes[ j ];
          final Object annotation2 = method.getAnnotation( type2 );
          if ( null != annotation2 )
          {
            final String message =
              "Method can not be annotated with both @" + type1.getSimpleName() + " and @" + type2.getSimpleName();
            throw new ArezProcessorException( message, method );
          }
        }
      }
    }
  }

  @Nonnull
  private String getPropertyAccessorName( @Nonnull final ExecutableElement method, @Nonnull final String specifiedName )
    throws ArezProcessorException
  {
    String name = ProcessorUtil.deriveName( method, GETTER_PATTERN, specifiedName );
    if ( null != name )
    {
      return name;
    }
    if ( method.getReturnType().getKind() == TypeKind.BOOLEAN )
    {
      name = ProcessorUtil.deriveName( method, ISSER_PATTERN, specifiedName );
      if ( null != name )
      {
        return name;
      }
    }
    return method.getSimpleName().toString();
  }

  /**
   * Build the enhanced class for specified container.
   */
  @Nonnull
  TypeSpec buildType( @Nonnull final Types typeUtils )
    throws ArezProcessorException
  {
    final TypeElement element = getElement();

    final StringBuilder name = new StringBuilder( "Arez_" + element.getSimpleName() );

    TypeElement t = element;
    while ( NestingKind.TOP_LEVEL != t.getNestingKind() )
    {
      t = (TypeElement) t.getEnclosingElement();
      name.insert( 0, t.getSimpleName() + "$" );
    }

    final TypeSpec.Builder builder = TypeSpec.classBuilder( name.toString() ).
      superclass( TypeName.get( element.asType() ) ).
      addTypeVariables( ProcessorUtil.getTypeArgumentsAsNames( asDeclaredType() ) ).
      addModifiers( Modifier.FINAL );

    builder.addAnnotation( AnnotationSpec.builder( Generated.class ).
      addMember( "value", "$S", ArezProcessor.class.getName() ).
      build() );
    if ( !_roComputeds.isEmpty() )
    {
      builder.addAnnotation( AnnotationSpec.builder( SuppressWarnings.class ).
        addMember( "value", "$S", "unchecked" ).
        build() );
    }
    ProcessorUtil.copyAccessModifiers( element, builder );

    if ( isDisposable() )
    {
      builder.addSuperinterface( GeneratorUtil.DISPOSABLE_CLASSNAME );
    }

    buildFields( builder );

    buildConstructors( builder, typeUtils );

    if ( !isSingleton() )
    {
      builder.addMethod( buildContainerNameMethod() );
    }
    final MethodSpec method = buildContainerNamePrefixMethod();
    if ( null != method )
    {
      builder.addMethod( method );
    }

    if ( isDisposable() )
    {
      builder.addMethod( buildIsDisposed() );
      builder.addMethod( buildDispose() );
    }

    _roObservables.forEach( e -> e.buildMethods( builder ) );
    _roAutoruns.forEach( e -> e.buildMethods( builder ) );
    _roActions.forEach( e -> e.buildMethods( builder ) );
    _roComputeds.forEach( e -> e.buildMethods( builder ) );
    _roTrackeds.forEach( e -> e.buildMethods( builder ) );

    return builder.build();
  }

  @Nonnull
  String getContainerNameMethodName()
  {
    return null == _containerName ? GeneratorUtil.ID_FIELD_NAME : _containerName.getSimpleName().toString();
  }

  /**
   * Generate the getter for container name.
   */
  @Nonnull
  private MethodSpec buildContainerNameMethod()
    throws ArezProcessorException
  {
    assert !isSingleton();

    final MethodSpec.Builder builder;
    if ( null == _containerName )
    {
      builder = MethodSpec.methodBuilder( GeneratorUtil.ID_FIELD_NAME ).addModifiers( Modifier.PRIVATE );
    }
    else
    {
      builder = MethodSpec.methodBuilder( _containerName.getSimpleName().toString() );
      ProcessorUtil.copyAccessModifiers( _containerName, builder );
      builder.addModifiers( Modifier.FINAL );
    }

    builder.returns( TypeName.get( String.class ) );
    if ( null == _containerId )
    {
      builder.addStatement( "return $S + $N", getNamePrefix(), GeneratorUtil.ID_FIELD_NAME );
    }
    else
    {
      builder.addStatement( "return $S + $N()", getNamePrefix(), _containerId.getSimpleName() );
    }
    return builder.build();
  }

  /**
   * Generate the container prefix string.
   */
  @Nullable
  private MethodSpec buildContainerNamePrefixMethod()
    throws ArezProcessorException
  {
    if ( null == _containerNamePrefix )
    {
      return null;
    }

    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( _containerNamePrefix.getSimpleName().toString() );
    ProcessorUtil.copyAccessModifiers( _containerNamePrefix, builder );
    builder.addModifiers( Modifier.FINAL );
    builder.addAnnotation( Nonnull.class );

    builder.returns( TypeName.get( String.class ) );
    builder.addStatement( "return $S", getName() );
    return builder.build();
  }

  /**
   * Generate the dispose method.
   */
  @Nonnull
  private MethodSpec buildDispose()
    throws ArezProcessorException
  {
    assert isDisposable();

    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "dispose" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class );

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.beginControlFlow( "if ( !isDisposed() )" );
    codeBlock.addStatement( "$N = true", GeneratorUtil.DISPOSED_FIELD_NAME );
    final ExecutableElement preDispose = _preDispose;
    if ( null != preDispose )
    {
      codeBlock.addStatement( "super.$N()", preDispose.getSimpleName() );
    }
    _roAutoruns.forEach( autorun -> autorun.buildDisposer( codeBlock ) );
    _roTrackeds.forEach( tracked -> tracked.buildDisposer( codeBlock ) );
    _roComputeds.forEach( computed -> computed.buildDisposer( codeBlock ) );
    _roObservables.forEach( observable -> observable.buildDisposer( codeBlock ) );
    final ExecutableElement postDispose = _postDispose;
    if ( null != postDispose )
    {
      codeBlock.addStatement( "super.$N()", postDispose.getSimpleName() );
    }
    codeBlock.endControlFlow();

    builder.addCode( codeBlock.build() );

    return builder.build();
  }

  /**
   * Generate the isDisposed method.
   */
  @Nonnull
  private MethodSpec buildIsDisposed()
    throws ArezProcessorException
  {
    assert isDisposable();

    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "isDisposed" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class ).
        returns( TypeName.BOOLEAN );

    builder.addStatement( "return $N", GeneratorUtil.DISPOSED_FIELD_NAME );

    return builder.build();
  }

  /**
   * Build the fields required to make class Observable. This involves;
   * <ul>
   * <li>the context field if there is any @Action methods.</li>
   * <li>the observable object for every @Observable.</li>
   * <li>the ComputedValue object for every @Computed method.</li>
   * </ul>
   */
  private void buildFields( @Nonnull final TypeSpec.Builder builder )
  {
    // If we don't have a method for object id but we need one then synthesize it
    if ( !isSingleton() && null == _containerId )
    {
      final FieldSpec.Builder nextIdField =
        FieldSpec.builder( TypeName.LONG,
                           GeneratorUtil.NEXT_ID_FIELD_NAME,
                           Modifier.VOLATILE,
                           Modifier.STATIC,
                           Modifier.PRIVATE );
      builder.addField( nextIdField.build() );

      final FieldSpec.Builder idField =
        FieldSpec.builder( TypeName.LONG, GeneratorUtil.ID_FIELD_NAME, Modifier.FINAL, Modifier.PRIVATE );
      builder.addField( idField.build() );
    }

    if ( isDisposable() )
    {
      final FieldSpec.Builder disposableField =
        FieldSpec.builder( TypeName.BOOLEAN, GeneratorUtil.DISPOSED_FIELD_NAME, Modifier.PRIVATE );
      builder.addField( disposableField.build() );
    }

    // Create the field that contains the context
    {
      final FieldSpec.Builder field =
        FieldSpec.builder( GeneratorUtil.AREZ_CONTEXT_CLASSNAME,
                           GeneratorUtil.CONTEXT_FIELD_NAME,
                           Modifier.FINAL,
                           Modifier.PRIVATE ).
          addAnnotation( Nonnull.class );
      builder.addField( field.build() );
    }

    _roObservables.forEach( observable -> observable.buildFields( builder ) );
    _roComputeds.forEach( computed -> computed.buildFields( builder ) );
    _roAutoruns.forEach( autorun -> autorun.buildFields( builder ) );
    _roTrackeds.forEach( tracked -> tracked.buildFields( builder ) );
  }

  /**
   * Build all constructors as they appear on the Container class.
   * Arez Observable fields are populated as required and parameters are passed up to superclass.
   */
  private void buildConstructors( @Nonnull final TypeSpec.Builder builder,
                                  @Nonnull final Types typeUtils )
  {
    for ( final ExecutableElement constructor : ProcessorUtil.getConstructors( getElement() ) )
    {
      final ExecutableType methodType =
        (ExecutableType) typeUtils.asMemberOf( (DeclaredType) _element.asType(), constructor );
      builder.addMethod( buildConstructor( constructor, methodType ) );
    }
  }

  /**
   * Build a constructor based on the supplied constructor
   */
  @Nonnull
  private MethodSpec buildConstructor( @Nonnull final ExecutableElement constructor,
                                       @Nonnull final ExecutableType constructorType )
  {
    final MethodSpec.Builder builder = MethodSpec.constructorBuilder();
    ProcessorUtil.copyAccessModifiers( constructor, builder );
    ProcessorUtil.copyExceptions( constructorType, builder );
    ProcessorUtil.copyTypeParameters( constructorType, builder );

    final StringBuilder superCall = new StringBuilder();
    superCall.append( "super(" );
    final ArrayList<String> parameterNames = new ArrayList<>();

    boolean firstParam = true;
    for ( final VariableElement element : constructor.getParameters() )
    {
      final ParameterSpec.Builder param =
        ParameterSpec.builder( TypeName.get( element.asType() ), element.getSimpleName().toString(), Modifier.FINAL );
      ProcessorUtil.copyDocumentedAnnotations( element, param );
      builder.addParameter( param.build() );
      parameterNames.add( element.getSimpleName().toString() );
      if ( !firstParam )
      {
        superCall.append( "," );
      }
      firstParam = false;
      superCall.append( "$N" );
    }

    superCall.append( ")" );
    builder.addStatement( superCall.toString(), parameterNames.toArray() );

    builder.addStatement( "this.$N = $T.context()", GeneratorUtil.CONTEXT_FIELD_NAME, GeneratorUtil.AREZ_CLASSNAME );

    // Synthesize Id if required
    if ( !isSingleton() && null == _containerId )
    {
      builder.addStatement( "this.$N = $N++", GeneratorUtil.ID_FIELD_NAME, GeneratorUtil.NEXT_ID_FIELD_NAME );
    }

    _roObservables.forEach( observable -> observable.buildInitializer( builder ) );
    _roComputeds.forEach( computed -> computed.buildInitializer( builder ) );
    _roAutoruns.forEach( autorun -> autorun.buildInitializer( builder ) );
    _roTrackeds.forEach( tracked -> tracked.buildInitializer( builder ) );

    if ( !_roAutoruns.isEmpty() )
    {
      builder.addStatement( "this.$N.triggerScheduler()", GeneratorUtil.CONTEXT_FIELD_NAME );
    }

    final ExecutableElement postConstruct = getPostConstruct();
    if ( null != postConstruct )
    {
      builder.addStatement( "super.$N()", postConstruct.getSimpleName().toString() );
    }

    return builder.build();
  }
}
