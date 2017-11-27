package org.realityforge.arez.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import org.realityforge.arez.annotations.ArezComponent;
import org.realityforge.arez.annotations.Autorun;
import org.realityforge.arez.annotations.ComponentId;
import org.realityforge.arez.annotations.ComponentName;
import org.realityforge.arez.annotations.ComponentRef;
import org.realityforge.arez.annotations.ComponentTypeName;
import org.realityforge.arez.annotations.Computed;
import org.realityforge.arez.annotations.ComputedValueRef;
import org.realityforge.arez.annotations.ContextRef;
import org.realityforge.arez.annotations.Observable;
import org.realityforge.arez.annotations.ObservableRef;
import org.realityforge.arez.annotations.ObserverRef;
import org.realityforge.arez.annotations.OnActivate;
import org.realityforge.arez.annotations.OnDeactivate;
import org.realityforge.arez.annotations.OnDepsChanged;
import org.realityforge.arez.annotations.OnDispose;
import org.realityforge.arez.annotations.OnStale;
import org.realityforge.arez.annotations.PostDispose;
import org.realityforge.arez.annotations.PreDispose;
import org.realityforge.arez.annotations.Track;

/**
 * The class that represents the parsed state of ArezComponent annotated class.
 */
@SuppressWarnings( "Duplicates" )
final class ComponentDescriptor
{
  private static final String ENTITIES_FIELD_NAME = GeneratorUtil.FIELD_PREFIX + "entities";
  private static final String ENTITYLIST_FIELD_NAME = GeneratorUtil.FIELD_PREFIX + "entityList";
  private static final String GET_OBSERVABLE_METHOD = "getEntitiesObservable";
  private static final Pattern OBSERVABLE_REF_PATTERN = Pattern.compile( "^get([A-Z].*)Observable$" );
  private static final Pattern COMPUTED_VALUE_REF_PATTERN = Pattern.compile( "^get([A-Z].*)ComputedValue$" );
  private static final Pattern OBSERVER_REF_PATTERN = Pattern.compile( "^get([A-Z].*)Observer$" );
  private static final Pattern SETTER_PATTERN = Pattern.compile( "^set([A-Z].*)$" );
  private static final Pattern GETTER_PATTERN = Pattern.compile( "^get([A-Z].*)$" );
  private static final Pattern ISSER_PATTERN = Pattern.compile( "^is([A-Z].*)$" );
  private static final List<String> OBJECT_METHODS =
    Arrays.asList( "hashCode", "equals", "clone", "toString", "finalize" );

  @Nullable
  private String _repositoryName;
  @Nullable
  private List<TypeElement> _repositoryExtensions;
  /**
   * Flag controlling whether dagger module is created for repository.
   */
  private boolean _generateDaggerModule;
  @Nonnull
  private final String _type;
  private final boolean _nameIncludesId;
  private final boolean _allowEmpty;
  private final boolean _generateToString;
  @Nonnull
  private final PackageElement _packageElement;
  @Nonnull
  private final TypeElement _element;
  @Nullable
  private ExecutableElement _postConstruct;
  @Nullable
  private ExecutableElement _componentId;
  @Nullable
  private ExecutableElement _componentRef;
  @Nullable
  private ExecutableElement _contextRef;
  @Nullable
  private ExecutableElement _componentTypeName;
  @Nullable
  private ExecutableElement _componentName;
  @Nullable
  private ExecutableElement _preDispose;
  @Nullable
  private ExecutableElement _postDispose;
  private final Map<String, CandidateMethod> _observerRefs = new HashMap<>();
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

  ComponentDescriptor( @Nonnull final String type,
                       final boolean nameIncludesId,
                       final boolean allowEmpty,
                       final boolean generateToString,
                       @Nonnull final PackageElement packageElement,
                       @Nonnull final TypeElement element )
  {
    _type = Objects.requireNonNull( type );
    _nameIncludesId = nameIncludesId;
    _allowEmpty = allowEmpty;
    _generateToString = generateToString;
    _packageElement = Objects.requireNonNull( packageElement );
    _element = Objects.requireNonNull( element );
  }

  @Nonnull
  private DeclaredType asDeclaredType()
  {
    return (DeclaredType) _element.asType();
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
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Observable specified invalid name " + name,
                                          method );
      }
    }
    checkNameUnique( name, method, Observable.class );

    if ( setter && !annotation.expectSetter() )
    {
      throw new ArezProcessorException( "Method annotated with @Observable is a setter but defines " +
                                        "expectSetter = false for observable named " + name, method );
    }

    final ObservableDescriptor observable = findOrCreateObservable( name );
    if ( !annotation.expectSetter() )
    {
      observable.setExpectSetter( false );
    }
    if ( !observable.expectSetter() )
    {
      if ( observable.hasSetter() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable defines expectSetter = false but a " +
                                          "setter exists named " + observable.getSetter().getSimpleName() +
                                          "for observable named " + name, method );
      }
    }
    if ( setter )
    {
      if ( observable.hasSetter() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable defines duplicate setter for " +
                                          "observable named " + name, method );
      }
      if ( !observable.expectSetter() )
      {
        throw new ArezProcessorException( "Method annotated with @Observable defines expectSetter = false but a " +
                                          "setter exists for observable named " + name, method );
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

  private void addObservableRef( @Nonnull final ObservableRef annotation,
                                 @Nonnull final ExecutableElement method,
                                 @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ObservableRef.class, method );
    MethodChecks.mustNotHaveAnyParameters( ObservableRef.class, method );
    MethodChecks.mustNotThrowAnyExceptions( ObservableRef.class, method );

    final TypeMirror returnType = methodType.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !toRawType( returnType ).toString().equals( "org.realityforge.arez.Observable" ) )
    {
      throw new ArezProcessorException( "Method annotated with @ObservableRef must return an instance of " +
                                        "org.realityforge.arez.Observable", method );
    }

    final String name;
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      name = ProcessorUtil.deriveName( method, OBSERVABLE_REF_PATTERN, annotation.name() );
      if ( null == name )
      {
        throw new ArezProcessorException( "Method annotated with @ObservableRef should specify name or be " +
                                          "named according to the convention get[Name]Observable", method );
      }
    }
    else
    {
      name = annotation.name();
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @ObservableRef specified invalid name " + name,
                                          method );
      }
    }

    final ObservableDescriptor observable = findOrCreateObservable( name );

    if ( observable.hasRefMethod() )
    {
      throw new ArezProcessorException( "Method annotated with @ObservableRef defines duplicate ref accessor for " +
                                        "observable named " + name, method );
    }
    observable.setRefMethod( method, methodType );
  }

  @Nonnull
  private TypeName toRawType( @Nonnull final TypeMirror type )
  {
    final TypeName typeName = TypeName.get( type );
    if ( typeName instanceof ParameterizedTypeName )
    {
      return ( (ParameterizedTypeName) typeName ).rawType;
    }
    else
    {
      return typeName;
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
    final ActionDescriptor action =
      new ActionDescriptor( this, name, annotation.mutation(), annotation.reportParameters(), method, methodType );
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
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
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
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Autorun specified invalid name " + name, method );
      }
      return name;
    }
  }

  private void addOnDepsChanged( @Nonnull final OnDepsChanged annotation, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    final String name =
      deriveHookName( method, TrackedDescriptor.ON_DEPS_CHANGED_PATTERN, "DepsChanged", annotation.name() );
    findOrCreateTracked( name ).setOnDepsChangedMethod( method );
  }

  private void addTracked( @Nonnull final Track annotation,
                           @Nonnull final ExecutableElement method,
                           @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    final String name = deriveTrackedName( method, annotation );
    checkNameUnique( name, method, Track.class );
    final TrackedDescriptor tracked = findOrCreateTracked( name );
    tracked.setTrackedMethod( annotation.mutation(), annotation.reportParameters(), method, methodType );
  }

  @Nonnull
  private String deriveTrackedName( @Nonnull final ExecutableElement method, @Nonnull final Track annotation )
    throws ArezProcessorException
  {
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      return method.getSimpleName().toString();
    }
    else
    {
      final String name = annotation.name();
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @Track specified invalid name " + name, method );
      }
      return name;
    }
  }

  private void addObserverRef( @Nonnull final ObserverRef annotation,
                               @Nonnull final ExecutableElement method,
                               @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ObserverRef.class, method );
    MethodChecks.mustNotHaveAnyParameters( ObserverRef.class, method );
    MethodChecks.mustNotThrowAnyExceptions( ObserverRef.class, method );

    final TypeMirror returnType = method.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !returnType.toString().equals( "org.realityforge.arez.Observer" ) )
    {
      throw new ArezProcessorException( "Method annotated with @ObserverRef must return an instance of " +
                                        "org.realityforge.arez.Observer", method );
    }

    final String name;
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      name = ProcessorUtil.deriveName( method, OBSERVER_REF_PATTERN, annotation.name() );
      if ( null == name )
      {
        throw new ArezProcessorException( "Method annotated with @ObserverRef should specify name or be " +
                                          "named according to the convention get[Name]Observer", method );
      }
    }
    else
    {
      name = annotation.name();
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @ObserverRef specified invalid name " + name,
                                          method );
      }
    }
    if ( _observerRefs.containsKey( name ) )
    {
      throw new ArezProcessorException( "Method annotated with @ObserverRef defines duplicate ref accessor for " +
                                        "observer named " + name, method );
    }
    _observerRefs.put( name, new CandidateMethod( method, methodType ) );
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

  private void addComputedValueRef( @Nonnull final ComputedValueRef annotation,
                                    @Nonnull final ExecutableElement method,
                                    @Nonnull final ExecutableType methodType )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ComputedValueRef.class, method );
    MethodChecks.mustNotHaveAnyParameters( ComputedValueRef.class, method );
    MethodChecks.mustNotThrowAnyExceptions( ComputedValueRef.class, method );

    final TypeMirror returnType = methodType.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !toRawType( returnType ).toString().equals( "org.realityforge.arez.ComputedValue" ) )
    {
      throw new ArezProcessorException( "Method annotated with @ComputedValueRef must return an instance of " +
                                        "org.realityforge.arez.ComputedValue", method );
    }

    final String name;
    if ( ProcessorUtil.isSentinelName( annotation.name() ) )
    {
      name = ProcessorUtil.deriveName( method, COMPUTED_VALUE_REF_PATTERN, annotation.name() );
      if ( null == name )
      {
        throw new ArezProcessorException( "Method annotated with @ComputedValueRef should specify name or be " +
                                          "named according to the convention get[Name]ComputedValue", method );
      }
    }
    else
    {
      name = annotation.name();
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Method annotated with @ComputedValueRef specified invalid name " + name,
                                          method );
      }
    }

    findOrCreateComputed( name ).setRefMethod( method, methodType );
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
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
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
    else if ( !ProcessorUtil.isJavaIdentifier( value ) )
    {
      throw new ArezProcessorException( "Method annotated with @On" + type + " specified invalid name " + value,
                                        method );
    }
    else
    {
      return value;
    }
  }

  private void setContextRef( @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ContextRef.class, method );
    MethodChecks.mustNotHaveAnyParameters( ContextRef.class, method );
    MethodChecks.mustReturnAValue( ContextRef.class, method );
    MethodChecks.mustNotThrowAnyExceptions( ContextRef.class, method );

    final TypeMirror returnType = method.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !returnType.toString().equals( "org.realityforge.arez.ArezContext" ) )
    {
      throw new ArezProcessorException( "Method annotated with @ContextRef must return an instance of " +
                                        "org.realityforge.arez.ArezContext", method );
    }

    if ( null != _contextRef )
    {
      throw new ArezProcessorException( "@ContextRef target duplicates existing method named " +
                                        _contextRef.getSimpleName(), method );
    }
    else
    {
      _contextRef = Objects.requireNonNull( method );
    }
  }

  private void setComponentRef( @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ComponentRef.class, method );
    MethodChecks.mustNotHaveAnyParameters( ComponentRef.class, method );
    MethodChecks.mustReturnAValue( ComponentRef.class, method );
    MethodChecks.mustNotThrowAnyExceptions( ComponentRef.class, method );

    final TypeMirror returnType = method.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !returnType.toString().equals( "org.realityforge.arez.Component" ) )
    {
      throw new ArezProcessorException( "Method annotated with @ComponentRef must return an instance of " +
                                        "org.realityforge.arez.Component", method );
    }

    if ( null != _componentRef )
    {
      throw new ArezProcessorException( "@ComponentRef target duplicates existing method named " +
                                        _componentRef.getSimpleName(), method );
    }
    else
    {
      _componentRef = Objects.requireNonNull( method );
    }
  }

  private void setComponentId( @Nonnull final ExecutableElement componentId )
    throws ArezProcessorException
  {
    MethodChecks.mustBeSubclassCallable( ComponentId.class, componentId );
    MethodChecks.mustBeFinal( ComponentId.class, componentId );
    MethodChecks.mustNotHaveAnyParameters( ComponentId.class, componentId );
    MethodChecks.mustReturnAValue( ComponentId.class, componentId );
    MethodChecks.mustNotThrowAnyExceptions( ComponentId.class, componentId );

    if ( null != _componentId )
    {
      throw new ArezProcessorException( "@ComponentId target duplicates existing method named " +
                                        _componentId.getSimpleName(), componentId );
    }
    else
    {
      _componentId = Objects.requireNonNull( componentId );
    }
  }

  private void setComponentTypeName( @Nonnull final ExecutableElement componentTypeName )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ComponentTypeName.class, componentTypeName );
    MethodChecks.mustNotHaveAnyParameters( ComponentTypeName.class, componentTypeName );
    MethodChecks.mustReturnAValue( ComponentTypeName.class, componentTypeName );
    MethodChecks.mustNotThrowAnyExceptions( ComponentTypeName.class, componentTypeName );

    final TypeMirror returnType = componentTypeName.getReturnType();
    if ( !( TypeKind.DECLARED == returnType.getKind() &&
            returnType.toString().equals( String.class.getName() ) ) )
    {
      throw new ArezProcessorException( "@ComponentTypeName target must return a String", componentTypeName );
    }

    if ( null != _componentTypeName )
    {
      throw new ArezProcessorException( "@ComponentTypeName target duplicates existing method named " +
                                        _componentTypeName.getSimpleName(), componentTypeName );
    }
    else
    {
      _componentTypeName = Objects.requireNonNull( componentTypeName );
    }
  }

  private void setComponentName( @Nonnull final ExecutableElement componentName )
    throws ArezProcessorException
  {
    MethodChecks.mustBeOverridable( ComponentName.class, componentName );
    MethodChecks.mustNotHaveAnyParameters( ComponentName.class, componentName );
    MethodChecks.mustReturnAValue( ComponentName.class, componentName );
    MethodChecks.mustNotThrowAnyExceptions( ComponentName.class, componentName );

    if ( null != _componentName )
    {
      throw new ArezProcessorException( "@ComponentName target duplicates existing method named " +
                                        _componentName.getSimpleName(), componentName );
    }
    else
    {
      _componentName = Objects.requireNonNull( componentName );
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

  @Nonnull
  Collection<ObservableDescriptor> getObservables()
  {
    return _roObservables;
  }

  void validate()
    throws ArezProcessorException
  {
    validateObservables();
    validateComputeds();

    if ( !_allowEmpty &&
         _roObservables.isEmpty() &&
         _roActions.isEmpty() &&
         _roComputeds.isEmpty() &&
         _roTrackeds.isEmpty() &&
         _roAutoruns.isEmpty() )
    {
      throw new ArezProcessorException( "@ArezComponent target has no methods annotated with @Action, " +
                                        "@Computed, @Observable, @Track or @Autorun", _element );
    }
  }

  private void validateObservables()
    throws ArezProcessorException
  {
    for ( final ObservableDescriptor observable : _roObservables )
    {
      observable.validate();
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
    if ( null != computed && computed.hasComputed() )
    {
      throw toException( name, sourceType, sourceMethod, Computed.class, computed.getComputed() );
    }
    final AutorunDescriptor autorun = _autoruns.get( name );
    if ( null != autorun )
    {
      throw toException( name, sourceType, sourceMethod, Autorun.class, autorun.getAutorun() );
    }
    // Track have pairs so let the caller determine whether a duplicate occurs in that scenario
    if ( Track.class != sourceType )
    {
      final TrackedDescriptor tracked = _trackeds.get( name );
      if ( null != tracked )
      {
        throw toException( name, sourceType, sourceMethod, Track.class, tracked.getTrackedMethod() );
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
          ProcessorUtil.deriveName( method, TrackedDescriptor.ON_DEPS_CHANGED_PATTERN, ProcessorUtil.SENTINEL_NAME );
        if ( voidReturn && 0 == parameterCount && null != name )
        {
          onDepsChangeds.put( name, candidateMethod );
          continue;
        }

        final String methodName = method.getSimpleName().toString();
        if ( !OBJECT_METHODS.contains( methodName ) )
        {
          trackeds.put( methodName, candidateMethod );
        }
      }
    }

    linkUnAnnotatedObservables( getters, setters );
    linkUnAnnotatedTracked( trackeds, onDepsChangeds );
    linkObserverRefs();
  }

  private void linkObserverRefs()
  {
    for ( final Map.Entry<String, CandidateMethod> entry : _observerRefs.entrySet() )
    {
      final String key = entry.getKey();
      final CandidateMethod method = entry.getValue();
      final AutorunDescriptor autorunDescriptor = _autoruns.get( key );
      if ( null != autorunDescriptor )
      {
        autorunDescriptor.setRefMethod( method.getMethod(), method.getMethodType() );
      }
      else
      {
        final TrackedDescriptor trackedDescriptor = _trackeds.get( key );
        if ( null != trackedDescriptor )
        {
          trackedDescriptor.setRefMethod( method.getMethod(), method.getMethodType() );
        }
        else
        {
          throw new ArezProcessorException( "@ObserverRef target defined observer named '" + key + "' but no " +
                                            "@Autorun or @Track method with that name exists", method.getMethod() );
        }
      }
    }
  }

  private void linkUnAnnotatedObservables( @Nonnull final Map<String, CandidateMethod> getters,
                                           @Nonnull final Map<String, CandidateMethod> setters )
    throws ArezProcessorException
  {
    for ( final ObservableDescriptor observable : _roObservables )
    {
      if ( !observable.hasSetter() && !observable.hasGetter() )
      {
        throw new ArezProcessorException( "@ObservableRef target unable to associated with an Observable property",
                                          observable.getRefMethod() );
      }
      else if ( !observable.hasSetter() && observable.expectSetter() )
      {
        final CandidateMethod candidate = setters.get( observable.getName() );
        if ( null != candidate )
        {
          observable.setSetter( candidate.getMethod(), candidate.getMethodType() );
        }
        else if ( observable.hasGetter() )
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
          tracked.setTrackedMethod( false, true, candidate.getMethod(), candidate.getMethodType() );
        }
        else
        {
          throw new ArezProcessorException( "@OnDepsChanged target has no corresponding @Track that could " +
                                            "be automatically determined", tracked.getOnDepsChangedMethod() );
        }
      }
      else if ( !tracked.hasOnDepsChangedMethod() )
      {
        final CandidateMethod candidate = onDepsChangeds.get( tracked.getName() );
        if ( null != candidate )
        {
          tracked.setOnDepsChangedMethod( candidate.getMethod() );
        }
        else
        {
          throw new ArezProcessorException( "@Track target has no corresponding @OnDepsChanged that could " +
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
    final ObservableRef observableRef = method.getAnnotation( ObservableRef.class );
    final Computed computed = method.getAnnotation( Computed.class );
    final ComputedValueRef computedValueRef = method.getAnnotation( ComputedValueRef.class );
    final ContextRef contextRef = method.getAnnotation( ContextRef.class );
    final ComponentRef componentRef = method.getAnnotation( ComponentRef.class );
    final ComponentId componentId = method.getAnnotation( ComponentId.class );
    final ComponentTypeName componentTypeName = method.getAnnotation( ComponentTypeName.class );
    final ComponentName componentName = method.getAnnotation( ComponentName.class );
    final PostConstruct postConstruct = method.getAnnotation( PostConstruct.class );
    final PreDispose preDispose = method.getAnnotation( PreDispose.class );
    final PostDispose postDispose = method.getAnnotation( PostDispose.class );
    final OnActivate onActivate = method.getAnnotation( OnActivate.class );
    final OnDeactivate onDeactivate = method.getAnnotation( OnDeactivate.class );
    final OnStale onStale = method.getAnnotation( OnStale.class );
    final OnDispose onDispose = method.getAnnotation( OnDispose.class );
    final Track track = method.getAnnotation( Track.class );
    final OnDepsChanged onDepsChanged = method.getAnnotation( OnDepsChanged.class );
    final ObserverRef observerRef = method.getAnnotation( ObserverRef.class );

    if ( null != observable )
    {
      addObservable( observable, method, methodType );
      return true;
    }
    else if ( null != observableRef )
    {
      addObservableRef( observableRef, method, methodType );
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
    else if ( null != track )
    {
      addTracked( track, method, methodType );
      return true;
    }
    else if ( null != onDepsChanged )
    {
      addOnDepsChanged( onDepsChanged, method );
      return true;
    }
    else if ( null != observerRef )
    {
      addObserverRef( observerRef, method, methodType );
      return true;
    }
    else if ( null != contextRef )
    {
      setContextRef( method );
      return true;
    }
    else if ( null != computed )
    {
      addComputed( computed, method, methodType );
      return true;
    }
    else if ( null != computedValueRef )
    {
      addComputedValueRef( computedValueRef, method, methodType );
      return true;
    }
    else if ( null != componentRef )
    {
      setComponentRef( method );
      return true;
    }
    else if ( null != componentId )
    {
      setComponentId( method );
      return true;
    }
    else if ( null != componentName )
    {
      setComponentName( method );
      return true;
    }
    else if ( null != componentTypeName )
    {
      setComponentTypeName( method );
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
                   Track.class,
                   OnDepsChanged.class,
                   ObserverRef.class,
                   Observable.class,
                   ObservableRef.class,
                   Computed.class,
                   ComputedValueRef.class,
                   ComponentRef.class,
                   ComponentId.class,
                   ComponentName.class,
                   ComponentTypeName.class,
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

  @Nonnull
  private String getNestedClassPrefix()
  {
    final StringBuilder name = new StringBuilder();
    TypeElement t = getElement();
    while ( NestingKind.TOP_LEVEL != t.getNestingKind() )
    {
      t = (TypeElement) t.getEnclosingElement();
      name.insert( 0, t.getSimpleName() + "_" );
    }
    return name.toString();
  }

  /**
   * Build the enhanced class for the component.
   */
  @Nonnull
  TypeSpec buildType( @Nonnull final Types typeUtils )
    throws ArezProcessorException
  {
    final TypeSpec.Builder builder = TypeSpec.classBuilder( getArezClassName() ).
      superclass( TypeName.get( getElement().asType() ) ).
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
    ProcessorUtil.copyAccessModifiers( getElement(), builder );

    builder.addSuperinterface( GeneratorUtil.DISPOSABLE_CLASSNAME );
    if ( hasRepository() )
    {
      final TypeSpec onDispose =
        TypeSpec.interfaceBuilder( "OnDispose" ).
          addModifiers( Modifier.STATIC ).
          addAnnotation( FunctionalInterface.class ).
          addMethod( MethodSpec.methodBuilder( "onDispose" ).
            addModifiers( Modifier.ABSTRACT, Modifier.PUBLIC ).
            addParameter( ParameterSpec.builder( ClassName.bestGuess( getArezClassName() ), "entity" ).build() ).
            build() ).
          build();
      builder.addType( onDispose );
    }

    buildFields( builder );

    buildConstructors( builder, typeUtils );

    if ( null != _contextRef )
    {
      builder.addMethod( buildContextRefMethod() );
    }
    if ( null != _componentRef )
    {
      builder.addMethod( buildComponentRefMethod() );
    }
    if ( null == _componentId )
    {
      builder.addMethod( buildComponentIdMethod() );
    }
    builder.addMethod( buildComponentNameMethod() );
    final MethodSpec method = buildComponentTypeNameMethod();
    if ( null != method )
    {
      builder.addMethod( method );
    }

    builder.addMethod( buildIsDisposed() );
    builder.addMethod( buildDispose() );
    if ( hasRepository() )
    {
      builder.addMethod( buildPreDispose() );
      builder.addMethod( buildSetOnDispose() );
    }

    _roObservables.forEach( e -> e.buildMethods( builder ) );
    _roAutoruns.forEach( e -> e.buildMethods( builder ) );
    _roActions.forEach( e -> e.buildMethods( builder ) );
    _roComputeds.forEach( e -> e.buildMethods( builder ) );
    _roTrackeds.forEach( e -> e.buildMethods( builder ) );

    builder.addMethod( buildHashcodeMethod() );
    builder.addMethod( buildEqualsMethod() );

    if ( _generateToString )
    {
      builder.addMethod( buildToStringMethod() );
    }

    return builder.build();
  }

  @Nonnull
  private MethodSpec buildToStringMethod()
    throws ArezProcessorException
  {
    assert _generateToString;

    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "toString" ).
        addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
        addAnnotation( Override.class ).
        returns( TypeName.get( String.class ) );

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.beginControlFlow( "if ( $T.areNamesEnabled() )", GeneratorUtil.AREZ_CLASSNAME );
    codeBlock.addStatement( "return $S + $N() + $S", "ArezComponent[", getComponentNameMethodName(), "]" );
    codeBlock.nextControlFlow( "else" );
    codeBlock.addStatement( "return super.toString()" );
    codeBlock.endControlFlow();
    method.addCode( codeBlock.build() );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildEqualsMethod()
    throws ArezProcessorException
  {
    final String idMethod =
      null == _componentId ? GeneratorUtil.ID_FIELD_NAME : _componentId.getSimpleName().toString();

    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "equals" ).
        addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
        addAnnotation( Override.class ).
        addParameter( Object.class, "o", Modifier.FINAL ).
        returns( TypeName.BOOLEAN );

    final ClassName generatedClass = ClassName.get( getPackageName(), getArezClassName() );

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.beginControlFlow( "if ( this == o )" );
    codeBlock.addStatement( "return true" );
    codeBlock.nextControlFlow( "else if ( null == o || !(o instanceof $T) )", generatedClass );
    codeBlock.addStatement( "return false" );
    codeBlock.nextControlFlow( "else" );
    codeBlock.addStatement( "final $T that = ($T) o;", generatedClass, generatedClass );
    final TypeKind kind = null != _componentId ? _componentId.getReturnType().getKind() : TypeKind.LONG;
    if ( kind == TypeKind.DECLARED || kind == TypeKind.TYPEVAR )
    {
      codeBlock.addStatement( "return null != $N() && $N().equals( that.$N() )", idMethod, idMethod, idMethod );
    }
    else
    {
      codeBlock.addStatement( "return $N() == that.$N()", idMethod, idMethod );
    }
    codeBlock.endControlFlow();
    method.addCode( codeBlock.build() );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildHashcodeMethod()
    throws ArezProcessorException
  {
    final String idMethod =
      null == _componentId ? GeneratorUtil.ID_FIELD_NAME : _componentId.getSimpleName().toString();

    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "hashCode" ).
        addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
        addAnnotation( Override.class ).
        returns( TypeName.INT );
    final TypeKind kind = null != _componentId ? _componentId.getReturnType().getKind() : TypeKind.LONG;
    if ( kind == TypeKind.DECLARED || kind == TypeKind.TYPEVAR )
    {
      method.addStatement( "return null != $N() ? $N().hashCode() : $T.identityHashCode( this )",
                           idMethod,
                           idMethod,
                           System.class );
    }
    else if ( kind == TypeKind.BYTE )
    {
      method.addStatement( "return $T.hashCode( $N() )", Byte.class, idMethod );
    }
    else if ( kind == TypeKind.CHAR )
    {
      method.addStatement( "return $T.hashCode( $N() )", Character.class, idMethod );
    }
    else if ( kind == TypeKind.SHORT )
    {
      method.addStatement( "return $T.hashCode( $N() )", Short.class, idMethod );
    }
    else if ( kind == TypeKind.INT )
    {
      method.addStatement( "return $T.hashCode( $N() )", Integer.class, idMethod );
    }
    else if ( kind == TypeKind.LONG )
    {
      method.addStatement( "return $T.hashCode( $N() )", Long.class, idMethod );
    }
    else if ( kind == TypeKind.FLOAT )
    {
      method.addStatement( "return $T.hashCode( $N() )", Float.class, idMethod );
    }
    else if ( kind == TypeKind.DOUBLE )
    {
      method.addStatement( "return $T.hashCode( $N() )", Double.class, idMethod );
    }
    else
    {
      // So very unlikely but will cover it for completeness
      assert kind == TypeKind.BOOLEAN;
      method.addStatement( "return $T.hashCode( $N() )", Boolean.class, idMethod );
    }
    return method.build();
  }

  @Nonnull
  String getComponentNameMethodName()
  {
    return null == _componentName ? GeneratorUtil.NAME_METHOD_NAME : _componentName.getSimpleName().toString();
  }

  @Nonnull
  private MethodSpec buildContextRefMethod()
    throws ArezProcessorException
  {
    assert null != _contextRef;

    final MethodSpec.Builder method = MethodSpec.methodBuilder( _contextRef.getSimpleName().toString() ).
      addModifiers( Modifier.FINAL ).
      returns( GeneratorUtil.AREZ_CONTEXT_CLASSNAME ).
      addStatement( "return this.$N", GeneratorUtil.CONTEXT_FIELD_NAME );
    ProcessorUtil.copyDocumentedAnnotations( _contextRef, method );
    ProcessorUtil.copyAccessModifiers( _contextRef, method );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildComponentRefMethod()
    throws ArezProcessorException
  {
    assert null != _componentRef;

    final MethodSpec.Builder method = MethodSpec.methodBuilder( _componentRef.getSimpleName().toString() ).
      addModifiers( Modifier.FINAL ).
      returns( GeneratorUtil.COMPONENT_CLASSNAME ).
      addStatement( "$T.invariant( () -> $T.areNativeComponentsEnabled(), () -> \"Invoked @ComponentRef method '$N' " +
                    "but Arez.areNativeComponentsEnabled() returned false.\" )",
                    GeneratorUtil.GUARDS_CLASSNAME,
                    GeneratorUtil.AREZ_CLASSNAME,
                    _componentRef.getSimpleName().toString() ).
      addStatement( "return this.$N", GeneratorUtil.COMPONENT_FIELD_NAME );
    ProcessorUtil.copyDocumentedAnnotations( _componentRef, method );
    ProcessorUtil.copyAccessModifiers( _componentRef, method );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildComponentIdMethod()
    throws ArezProcessorException
  {
    assert null == _componentId;

    return MethodSpec.methodBuilder( GeneratorUtil.ID_FIELD_NAME ).
      addModifiers( Modifier.FINAL ).
      returns( TypeName.LONG ).
      addStatement( "return this.$N", GeneratorUtil.ID_FIELD_NAME ).build();
  }

  /**
   * Generate the getter for component name.
   */
  @Nonnull
  private MethodSpec buildComponentNameMethod()
    throws ArezProcessorException
  {
    final MethodSpec.Builder builder;
    if ( null == _componentName )
    {
      builder = MethodSpec.methodBuilder( GeneratorUtil.NAME_METHOD_NAME );
    }
    else
    {
      builder = MethodSpec.methodBuilder( _componentName.getSimpleName().toString() );
      ProcessorUtil.copyAccessModifiers( _componentName, builder );
      builder.addModifiers( Modifier.FINAL );
    }

    builder.returns( TypeName.get( String.class ) );
    if ( _nameIncludesId )
    {
      builder.addStatement( "return $S + $N()",
                            _type.isEmpty() ? "" : _type + ".",
                            null == _componentId ? GeneratorUtil.ID_FIELD_NAME : _componentId.getSimpleName() );
    }
    else
    {
      builder.addStatement( "return $S", _type );
    }
    return builder.build();
  }

  @Nullable
  private MethodSpec buildComponentTypeNameMethod()
    throws ArezProcessorException
  {
    if ( null == _componentTypeName )
    {
      return null;
    }

    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( _componentTypeName.getSimpleName().toString() );
    ProcessorUtil.copyAccessModifiers( _componentTypeName, builder );
    builder.addModifiers( Modifier.FINAL );
    builder.addAnnotation( Nonnull.class );

    builder.returns( TypeName.get( String.class ) );
    builder.addStatement( "return $S", _type );
    return builder.build();
  }

  @Nonnull
  private MethodSpec buildPreDispose()
    throws ArezProcessorException
  {
    final MethodSpec.Builder method = MethodSpec.methodBuilder( GeneratorUtil.PRE_DISPOSE_METHOD_NAME );
    final CodeBlock.Builder onDisposeCodeBlock = CodeBlock.builder();
    onDisposeCodeBlock.beginControlFlow( "if ( null != this.$N )", GeneratorUtil.ON_DISPOSE_FIELD_NAME );
    onDisposeCodeBlock.addStatement( "this.$N.onDispose( this )", GeneratorUtil.ON_DISPOSE_FIELD_NAME );
    onDisposeCodeBlock.addStatement( "this.$N = null", GeneratorUtil.ON_DISPOSE_FIELD_NAME );
    onDisposeCodeBlock.endControlFlow();
    method.addCode( onDisposeCodeBlock.build() );
    if ( null != _preDispose )
    {
      method.addStatement( "super.$N()", _preDispose.getSimpleName().toString() );
    }
    return method.build();
  }

  @Nonnull
  private MethodSpec buildSetOnDispose()
    throws ArezProcessorException
  {
    return MethodSpec.methodBuilder( GeneratorUtil.SET_ON_DISPOSE_METHOD_NAME ).
      addParameter( ParameterSpec.builder( ClassName.bestGuess( "OnDispose" ), "onDispose" ).build() ).
      addStatement( "this.$N = onDispose", GeneratorUtil.ON_DISPOSE_FIELD_NAME ).build();
  }

  /**
   * Generate the dispose method.
   */
  @Nonnull
  private MethodSpec buildDispose()
    throws ArezProcessorException
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "dispose" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class );

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.beginControlFlow( "if ( !isDisposed() )" );
    codeBlock.addStatement( "this.$N = true", GeneratorUtil.DISPOSED_FIELD_NAME );
    final CodeBlock.Builder nativeComponentBlock = CodeBlock.builder();
    nativeComponentBlock.beginControlFlow( "if ( $T.areNativeComponentsEnabled() )", GeneratorUtil.AREZ_CLASSNAME );
    nativeComponentBlock.addStatement( "this.$N.dispose()", GeneratorUtil.COMPONENT_FIELD_NAME );
    nativeComponentBlock.nextControlFlow( "else" );

    final CodeBlock.Builder actionBlock = CodeBlock.builder();
    actionBlock.beginControlFlow( "this.$N.safeAction( $T.areNamesEnabled() ? $N() + \".dispose\" : null, () -> {",
                                  GeneratorUtil.CONTEXT_FIELD_NAME,
                                  GeneratorUtil.AREZ_CLASSNAME,
                                  getComponentNameMethodName() );

    if ( hasRepository() )
    {
      actionBlock.addStatement( "$N()", GeneratorUtil.PRE_DISPOSE_METHOD_NAME );
    }
    else if ( null != _preDispose )
    {
      actionBlock.addStatement( "super.$N()", _preDispose.getSimpleName() );
    }
    actionBlock.addStatement( "this.$N.dispose()", GeneratorUtil.DISPOSED_OBSERVABLE_FIELD_NAME );
    _roAutoruns.forEach( autorun -> autorun.buildDisposer( actionBlock ) );
    _roTrackeds.forEach( tracked -> tracked.buildDisposer( actionBlock ) );
    _roComputeds.forEach( computed -> computed.buildDisposer( actionBlock ) );
    _roObservables.forEach( observable -> observable.buildDisposer( actionBlock ) );
    if ( null != _postDispose )
    {
      actionBlock.addStatement( "super.$N()", _postDispose.getSimpleName() );
    }
    actionBlock.endControlFlow( "} )" );
    nativeComponentBlock.add( actionBlock.build() );
    nativeComponentBlock.endControlFlow();
    codeBlock.add( nativeComponentBlock.build() );
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
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "isDisposed" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class ).
        returns( TypeName.BOOLEAN );

    final CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow( "if ( this.$N.isTransactionActive() && !this.$N.isDisposed() ) ",
                            GeneratorUtil.CONTEXT_FIELD_NAME,
                            GeneratorUtil.DISPOSED_OBSERVABLE_FIELD_NAME );
    block.addStatement( "this.$N.reportObserved()", GeneratorUtil.DISPOSED_OBSERVABLE_FIELD_NAME );
    block.addStatement( "return this.$N", GeneratorUtil.DISPOSED_FIELD_NAME );
    block.nextControlFlow( "else" );
    block.addStatement( "return this.$N", GeneratorUtil.DISPOSED_FIELD_NAME );
    block.endControlFlow();
    builder.addCode( block.build() );

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
    if ( null == _componentId )
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

    final FieldSpec.Builder disposableField =
      FieldSpec.builder( TypeName.BOOLEAN, GeneratorUtil.DISPOSED_FIELD_NAME, Modifier.PRIVATE );
    builder.addField( disposableField.build() );
    if ( hasRepository() )
    {
      final FieldSpec.Builder onDisposeField =
        FieldSpec.builder( ClassName.bestGuess( "OnDispose" ),
                           GeneratorUtil.ON_DISPOSE_FIELD_NAME,
                           Modifier.PRIVATE );
      builder.addField( onDisposeField.build() );
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

    //Create the field that contains the component
    {
      final FieldSpec.Builder field =
        FieldSpec.builder( GeneratorUtil.COMPONENT_CLASSNAME,
                           GeneratorUtil.COMPONENT_FIELD_NAME,
                           Modifier.FINAL,
                           Modifier.PRIVATE );
      builder.addField( field.build() );

    }
    {
      final ParameterizedTypeName typeName =
        ParameterizedTypeName.get( GeneratorUtil.OBSERVABLE_CLASSNAME, TypeName.BOOLEAN.box() );
      final FieldSpec.Builder field =
        FieldSpec.builder( typeName,
                           GeneratorUtil.DISPOSED_OBSERVABLE_FIELD_NAME,
                           Modifier.FINAL,
                           Modifier.PRIVATE );
      builder.addField( field.build() );

    }
    _roObservables.forEach( observable -> observable.buildFields( builder ) );
    _roComputeds.forEach( computed -> computed.buildFields( builder ) );
    _roAutoruns.forEach( autorun -> autorun.buildFields( builder ) );
    _roTrackeds.forEach( tracked -> tracked.buildFields( builder ) );
  }

  /**
   * Build all constructors as they appear on the ArezComponent class.
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
    if ( null == _componentId )
    {
      builder.addStatement( "this.$N = $N++", GeneratorUtil.ID_FIELD_NAME, GeneratorUtil.NEXT_ID_FIELD_NAME );
    }

    // Create component representation if required
    {
      final StringBuilder sb = new StringBuilder();
      final ArrayList<Object> params = new ArrayList<>();
      sb.append( "this.$N = $T.areNativeComponentsEnabled() ? this.$N.createComponent( $S, $N(), $N(), " );
      params.add( GeneratorUtil.COMPONENT_FIELD_NAME );
      params.add( GeneratorUtil.AREZ_CLASSNAME );
      params.add( GeneratorUtil.CONTEXT_FIELD_NAME );
      params.add( _type );
      params.add( getIdMethodName() );
      params.add( getComponentNameMethodName() );
      if ( hasRepository() )
      {
        sb.append( "() -> $N(), " );
        params.add( GeneratorUtil.PRE_DISPOSE_METHOD_NAME );
      }
      else if ( null != _preDispose )
      {
        sb.append( "() -> super.$N(), " );
        params.add( _preDispose.getSimpleName().toString() );
      }
      else
      {
        sb.append( "null, " );
      }
      if ( null != _postDispose )
      {
        sb.append( "() -> super.$N() " );
        params.add( _postDispose.getSimpleName().toString() );
      }
      else
      {
        sb.append( "null " );
      }
      sb.append( ") : null" );
      builder.addStatement( sb.toString(), params.toArray() );
    }
    {
      builder.addStatement( "this.$N = this.$N.createObservable( " +
                            "$T.areNativeComponentsEnabled() ? this.$N : null, " +
                            "$T.areNamesEnabled() ? $N() + $S : null, " +
                            "$T.arePropertyIntrospectorsEnabled() ? () -> this.$N : null, null )",
                            GeneratorUtil.DISPOSED_OBSERVABLE_FIELD_NAME,
                            GeneratorUtil.CONTEXT_FIELD_NAME,
                            GeneratorUtil.AREZ_CLASSNAME,
                            GeneratorUtil.COMPONENT_FIELD_NAME,
                            GeneratorUtil.AREZ_CLASSNAME,
                            getComponentNameMethodName(),
                            ".isDisposed",
                            GeneratorUtil.AREZ_CLASSNAME,
                            GeneratorUtil.DISPOSED_FIELD_NAME );
    }

    _roObservables.forEach( observable -> observable.buildInitializer( builder ) );
    _roComputeds.forEach( computed -> computed.buildInitializer( builder ) );
    _roAutoruns.forEach( autorun -> autorun.buildInitializer( builder ) );
    _roTrackeds.forEach( tracked -> tracked.buildInitializer( builder ) );

    final ExecutableElement postConstruct = getPostConstruct();
    if ( null != postConstruct )
    {
      builder.addStatement( "super.$N()", postConstruct.getSimpleName().toString() );
    }

    final CodeBlock.Builder componentEnabledBlock = CodeBlock.builder();
    componentEnabledBlock.beginControlFlow( "if ( $T.areNativeComponentsEnabled() )",
                                            GeneratorUtil.AREZ_CLASSNAME );
    componentEnabledBlock.addStatement( "this.$N.complete()", GeneratorUtil.COMPONENT_FIELD_NAME );
    componentEnabledBlock.endControlFlow();
    builder.addCode( componentEnabledBlock.build() );

    if ( !_roAutoruns.isEmpty() )
    {
      builder.addStatement( "this.$N.triggerScheduler()", GeneratorUtil.CONTEXT_FIELD_NAME );
    }

    return builder.build();
  }

  boolean hasRepository()
  {
    return null != _repositoryName;
  }

  @SuppressWarnings( "ConstantConditions" )
  void configureRepository( @Nonnull final String name,
                            @Nonnull final List<TypeElement> extensions,
                            final boolean generateDaggerModule )
  {
    assert null != name;
    assert null != extensions;
    _generateDaggerModule = generateDaggerModule;
    if ( ProcessorUtil.isSentinelName( name ) )
    {
      _repositoryName = _type + "Repository";
    }
    else
    {
      if ( !ProcessorUtil.isJavaIdentifier( name ) )
      {
        throw new ArezProcessorException( "Class annotated with @Repository specified an invalid name " + name,
                                          _element );
      }
      _repositoryName = name;
    }
    for ( final TypeElement extension : extensions )
    {
      if ( ElementKind.INTERFACE != extension.getKind() )
      {
        throw new ArezProcessorException( "Class annotated with @Repository defined an extension that is " +
                                          "not an interface. Extension: " + extension.getQualifiedName(),
                                          getElement() );
      }

      for ( final Element enclosedElement : extension.getEnclosedElements() )
      {
        if ( ElementKind.METHOD == enclosedElement.getKind() )
        {
          final ExecutableElement method = (ExecutableElement) enclosedElement;
          if ( !method.isDefault() &&
               !( method.getSimpleName().toString().equals( "self" ) && 0 == method.getParameters().size() ) )
          {
            throw new ArezProcessorException( "Class annotated with @Repository defined an extension that has " +
                                              "a non default method. Extension: " + extension.getQualifiedName() +
                                              " Method: " + method, getElement() );
          }
        }
      }
    }
    _repositoryExtensions = extensions;
  }

  @Nonnull
  TypeSpec buildRepositoryExtension()
    throws ArezProcessorException
  {
    final TypeSpec.Builder builder = TypeSpec.interfaceBuilder( getRepositoryExtensionName() ).
      addTypeVariables( ProcessorUtil.getTypeArgumentsAsNames( asDeclaredType() ) );

    builder.addAnnotation( AnnotationSpec.builder( Generated.class ).
      addMember( "value", "$S", ArezProcessor.class.getName() ).
      build() );

    ProcessorUtil.copyAccessModifiers( getElement(), builder );

    builder.addMethod( MethodSpec.methodBuilder( "self" ).
      addAnnotation( Nonnull.class ).
      addModifiers( Modifier.ABSTRACT, Modifier.PUBLIC ).
      returns( ClassName.get( getPackageName(), getRepositoryName() ) ).build() );

    return builder.build();
  }

  boolean shouldGenerateDaggerModule()
  {
    return _generateDaggerModule;
  }

  @Nonnull
  TypeSpec buildDaggerModule()
    throws ArezProcessorException
  {
    assert _generateDaggerModule;
    final TypeSpec.Builder builder = TypeSpec.interfaceBuilder( getDaggerModuleName() ).
      addTypeVariables( ProcessorUtil.getTypeArgumentsAsNames( asDeclaredType() ) );

    builder.addAnnotation( AnnotationSpec.builder( Generated.class ).
      addMember( "value", "$S", ArezProcessor.class.getName() ).
      build() );
    builder.addAnnotation( GeneratorUtil.DAGGER_MODULE_CLASSNAME );

    ProcessorUtil.copyAccessModifiers( getElement(), builder );

    builder.addMethod( MethodSpec.methodBuilder( "repository" ).
      addAnnotation( Nonnull.class ).
      addAnnotation( GeneratorUtil.SINGLETON_CLASSNAME ).
      addAnnotation( GeneratorUtil.DAGGER_PROVIDES_CLASSNAME ).
      addModifiers( Modifier.STATIC, Modifier.PUBLIC ).
      addStatement( "return $T.newRepository()", ClassName.get( getPackageName(), getRepositoryName() ) ).
      returns( ClassName.get( getPackageName(), getRepositoryName() ) ).build() );

    return builder.build();
  }

  /**
   * Build the enhanced class for the component.
   */
  @Nonnull
  TypeSpec buildRepository( @Nonnull final Types typeUtils )
    throws ArezProcessorException
  {
    assert null != _repositoryExtensions;
    final TypeElement element = getElement();

    final ClassName arezType = ClassName.get( getPackageName(), getArezClassName() );

    final TypeSpec.Builder builder = TypeSpec.classBuilder( getRepositoryName() ).
      addTypeVariables( ProcessorUtil.getTypeArgumentsAsNames( asDeclaredType() ) );

    builder.addAnnotation( AnnotationSpec.builder( Generated.class ).
      addMember( "value", "$S", ArezProcessor.class.getName() ).
      build() );
    builder.addAnnotation( AnnotationSpec.builder( ArezComponent.class ).
      addMember( "nameIncludesId", "false" ).
      build() );

    builder.addSuperinterface( ClassName.get( getPackageName(), getRepositoryExtensionName() ) );
    _repositoryExtensions.forEach( e -> builder.addSuperinterface( TypeName.get( e.asType() ) ) );

    ProcessorUtil.copyAccessModifiers( element, builder );

    //Add the default access, no-args constructor
    builder.addMethod( MethodSpec.constructorBuilder().build() );

    buildRepositoryFields( builder );

    // Add the factory method
    builder.addMethod( buildFactoryMethod() );

    for ( final ExecutableElement constructor : ProcessorUtil.getConstructors( getElement() ) )
    {
      final ExecutableType methodType =
        (ExecutableType) typeUtils.asMemberOf( (DeclaredType) _element.asType(), constructor );
      builder.addMethod( buildRepositoryCreate( constructor, methodType, arezType ) );
    }

    builder.addMethod( buildPreDisposeMethod() );

    builder.addMethod( buildContainsMethod( element, arezType ) );
    builder.addMethod( buildDestroyMethod( arezType ) );
    if ( null != _componentId )
    {
      builder.addMethod( buildFindByIdMethod() );
      builder.addMethod( buildGetByIdMethod() );
    }
    builder.addMethod( buildObservableAccessorMethod() );
    builder.addMethod( buildEntitiesMethod() );
    builder.addMethod( buildResultWrapperMethod() );
    builder.addMethod( buildListConverterMethod() );
    builder.addMethod( buildFindAllMethod() );
    builder.addMethod( buildFindAllSortedMethod() );
    builder.addMethod( buildFindAllByQueryMethod() );
    builder.addMethod( buildFindAllByQuerySortedMethod() );
    builder.addMethod( buildFindByQueryMethod() );
    builder.addMethod( buildGetByQueryMethod() );

    builder.addMethod( buildSelfMethod() );

    return builder.build();
  }

  @Nonnull
  private String getArezClassName()
  {
    return getNestedClassPrefix() + "Arez_" + getElement().getSimpleName();
  }

  @Nonnull
  private String getDaggerModuleName()
  {
    return getNestedClassPrefix() + getElement().getSimpleName() + "DaggerModule";
  }

  @Nonnull
  private String getRepositoryExtensionName()
  {
    return getNestedClassPrefix() + getElement().getSimpleName() + "BaseRepositoryExtension";
  }

  @Nonnull
  private String getArezRepositoryName()
  {
    return "Arez_" + getNestedClassPrefix() + getElement().getSimpleName() + "Repository";
  }

  @Nonnull
  private String getRepositoryName()
  {
    return getNestedClassPrefix() + getElement().getSimpleName() + "Repository";
  }

  @Nonnull
  private MethodSpec buildEntitiesMethod()
  {
    return MethodSpec.methodBuilder( "entities" ).
      addModifiers( Modifier.PROTECTED ).
      addAnnotation( AnnotationSpec.builder( Observable.class ).addMember( "expectSetter", "false" ).build() ).
      addJavadoc( "Return the raw collection of entities in the repository.\n" +
                  "This collection should not be exposed to the user but may be used be repository extensions when\n" +
                  "they define custom queries. NOTE: use of this method marks the list as observed.\n" ).
      addAnnotation( Nonnull.class ).
      returns( ParameterizedTypeName.get( ClassName.get( Collection.class ), TypeName.get( getElement().asType() ) ) ).
      addStatement( "return $N", ENTITYLIST_FIELD_NAME ).
      build();
  }

  @Nonnull
  private MethodSpec buildResultWrapperMethod()
  {
    final ParameterizedTypeName listType =
      ParameterizedTypeName.get( ClassName.get( List.class ), TypeName.get( getElement().asType() ) );
    return MethodSpec.methodBuilder( "wrap" ).
      addModifiers( Modifier.PROTECTED, Modifier.FINAL ).
      addJavadoc( "If config option enabled, wrap the specified list in an immutable list and return it.\n" +
                  "This method should be called by repository extensions when returning list results " +
                  "when not using {@link #toList(List)}.\n" ).
      addAnnotation( Nonnull.class ).
      addParameter( ParameterSpec.builder( listType, "list", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( listType ).
      addStatement( "return $T.areRepositoryResultsModifiable() ? $T.unmodifiableList( list ) : list",
                    GeneratorUtil.AREZ_CLASSNAME,
                    Collections.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildListConverterMethod()
  {
    final ParameterizedTypeName streamType =
      ParameterizedTypeName.get( ClassName.get( Stream.class ), TypeName.get( getElement().asType() ) );
    return MethodSpec.methodBuilder( "toList" ).
      addModifiers( Modifier.PROTECTED, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      addJavadoc( "Convert specified stream to a list, wrapping as an immutable list if required.\n" +
                  "This method should be called by repository extensions when returning list results.\n" ).
      addParameter( ParameterSpec.builder( streamType, "stream", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( ParameterizedTypeName.get( ClassName.get( List.class ), TypeName.get( getElement().asType() ) ) ).
      addStatement( "return wrap( stream.collect( $T.toList() ) )", Collectors.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildFindAllMethod()
  {
    return MethodSpec.methodBuilder( "findAll" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      returns( ParameterizedTypeName.get( ClassName.get( List.class ), TypeName.get( getElement().asType() ) ) ).
      addStatement( "return toList( entities().stream() )", Collectors.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildFindAllSortedMethod()
  {
    final ParameterizedTypeName sorterType =
      ParameterizedTypeName.get( ClassName.get( Comparator.class ), TypeName.get( getElement().asType() ) );
    return MethodSpec.methodBuilder( "findAll" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      addParameter( ParameterSpec.builder( sorterType, "sorter", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( ParameterizedTypeName.get( ClassName.get( List.class ), TypeName.get( getElement().asType() ) ) ).
      addStatement( "return toList( entities().stream().sorted( sorter ) )", Collectors.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildFindAllByQueryMethod()
  {
    final ParameterizedTypeName queryType =
      ParameterizedTypeName.get( ClassName.get( Predicate.class ), TypeName.get( getElement().asType() ) );
    return MethodSpec.methodBuilder( "findAllByQuery" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      addParameter( ParameterSpec.builder( queryType, "query", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( ParameterizedTypeName.get( ClassName.get( List.class ), TypeName.get( getElement().asType() ) ) ).
      addStatement( "return toList( entities().stream().filter( query ) )", Collectors.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildFindAllByQuerySortedMethod()
  {
    final ParameterizedTypeName queryType =
      ParameterizedTypeName.get( ClassName.get( Predicate.class ), TypeName.get( getElement().asType() ) );
    final ParameterizedTypeName sorterType =
      ParameterizedTypeName.get( ClassName.get( Comparator.class ), TypeName.get( getElement().asType() ) );
    return MethodSpec.methodBuilder( "findAllByQuery" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      addParameter( ParameterSpec.builder( queryType, "query", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      addParameter( ParameterSpec.builder( sorterType, "sorter", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( ParameterizedTypeName.get( ClassName.get( List.class ), TypeName.get( getElement().asType() ) ) ).
      addStatement( "return toList( entities().stream().filter( query ).sorted( sorter ) )",
                    Collectors.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildFindByQueryMethod()
  {
    final ParameterizedTypeName queryType =
      ParameterizedTypeName.get( ClassName.get( Predicate.class ), TypeName.get( getElement().asType() ) );
    return MethodSpec.methodBuilder( "findByQuery" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nullable.class ).
      addParameter( ParameterSpec.builder( queryType, "query", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( TypeName.get( getElement().asType() ) ).
      addStatement( "return entities().stream().filter( query ).findFirst().orElse( null )", Collectors.class ).
      build();
  }

  @Nonnull
  private MethodSpec buildGetByQueryMethod()
  {
    final TypeName entityType = TypeName.get( getElement().asType() );
    final ParameterizedTypeName queryType =
      ParameterizedTypeName.get( ClassName.get( Predicate.class ), entityType );
    return MethodSpec.methodBuilder( "getByQuery" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      addParameter( ParameterSpec.builder( queryType, "query", Modifier.FINAL ).
        addAnnotation( Nonnull.class ).build() ).
      returns( entityType ).
      addStatement( "final $T entity = findByQuery( query )", entityType ).
      addCode( CodeBlock.builder().
        beginControlFlow( "if ( null == entity )" ).
        addStatement( "throw new $T()", GeneratorUtil.NO_RESULT_EXCEPTION_CLASSNAME ).
        endControlFlow().
        build() ).
      addStatement( "return entity" ).
      build();
  }

  @Nonnull
  private MethodSpec buildSelfMethod()
  {
    return MethodSpec.methodBuilder( "self" ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Override.class ).
      addAnnotation( Nonnull.class ).
      returns( ClassName.get( getPackageName(), getRepositoryName() ) ).
      addStatement( "return this" ).build();
  }

  @Nonnull
  private MethodSpec buildObservableAccessorMethod()
  {
    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( GET_OBSERVABLE_METHOD ).
        addAnnotation( ObservableRef.class ).
        returns( GeneratorUtil.OBSERVABLE_CLASSNAME );
    method.addStatement( "throw new $T()", IllegalStateException.class );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildContainsMethod( final TypeElement element, final ClassName arezType )
  {
    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "contains" ).
        addModifiers( Modifier.PUBLIC ).
        addParameter( ParameterSpec.builder( TypeName.get( element.asType() ), "entity", Modifier.FINAL ).
          addAnnotation( Nonnull.class ).build() ).
        returns( TypeName.BOOLEAN );
    method.addStatement( "$N().reportObserved()", GET_OBSERVABLE_METHOD );
    if ( null != _componentId )
    {
      method.addStatement( "return this.$N.containsKey( entity.$N() )", ENTITIES_FIELD_NAME, getIdMethodName() );
    }
    else
    {
      method.addStatement( "return entity instanceof $T && this.$N.containsKey( (($T) entity).$N() )",
                           arezType,
                           ENTITIES_FIELD_NAME,
                           arezType,
                           getIdMethodName() );
    }
    return method.build();
  }

  @Nonnull
  private MethodSpec buildDestroyMethod( @Nonnull final ClassName arezType )
  {
    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "destroy" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Action.class ).
        addParameter( ParameterSpec.builder( TypeName.get( getElement().asType() ), "entity", Modifier.FINAL ).
          addAnnotation( Nonnull.class ).build() );
    method.addStatement( "assert null != entity" );
    final CodeBlock.Builder builder = CodeBlock.builder();
    if ( null != _componentId )
    {
      builder.beginControlFlow( "if ( null != this.$N.remove( entity.$N() ) )",
                                ENTITIES_FIELD_NAME,
                                getIdMethodName() );
    }
    else
    {
      builder.beginControlFlow( "if ( entity instanceof $T && null != this.$N.remove( (($T) entity).$N() ) )",
                                arezType,
                                ENTITIES_FIELD_NAME,
                                arezType,
                                getIdMethodName() );
    }
    builder.addStatement( "(($N) entity).$N( null )", getArezClassName(), GeneratorUtil.SET_ON_DISPOSE_METHOD_NAME );
    builder.addStatement( "$T.dispose( entity )", GeneratorUtil.DISPOSABLE_CLASSNAME );
    builder.addStatement( "$N().reportChanged()", GET_OBSERVABLE_METHOD );
    builder.nextControlFlow( "else" );
    builder.addStatement( "$T.fail( () -> \"Called destroy() passing an entity that was not in the repository. " +
                          "Entity: \" + entity )", GeneratorUtil.GUARDS_CLASSNAME );
    builder.endControlFlow();
    method.addCode( builder.build() );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildPreDisposeMethod()
  {
    return MethodSpec.methodBuilder( "preDispose" ).
      addModifiers( Modifier.FINAL ).
      addAnnotation( PreDispose.class ).
      addStatement( "this.$N.forEach( e -> $T.dispose( e ) )",
                    ENTITYLIST_FIELD_NAME,
                    GeneratorUtil.DISPOSABLE_CLASSNAME ).
      addStatement( "this.$N.clear()", ENTITIES_FIELD_NAME ).
      addStatement( "$N().reportChanged()", GET_OBSERVABLE_METHOD ).
      build();
  }

  @Nonnull
  private MethodSpec buildFindByIdMethod()
  {
    assert null != _componentId;

    return MethodSpec.methodBuilder( "findBy" + getIdName() ).
      addModifiers( Modifier.PUBLIC ).
      addParameter( ParameterSpec.builder( getIdType(), "id", Modifier.FINAL ).build() ).
      addAnnotation( Nullable.class ).
      returns( TypeName.get( getElement().asType() ) ).
      addStatement( "$N().reportObserved()", GET_OBSERVABLE_METHOD ).
      addStatement( "return this.$N.get( id )", ENTITIES_FIELD_NAME ).build();
  }

  @Nonnull
  private MethodSpec buildGetByIdMethod()
  {
    final TypeName entityType = TypeName.get( getElement().asType() );
    return MethodSpec.methodBuilder( "getBy" + getIdName() ).
      addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
      addAnnotation( Nonnull.class ).
      addParameter( ParameterSpec.builder( getIdType(), "id", Modifier.FINAL ).build() ).
      returns( entityType ).
      addStatement( "final $T entity = $N( id )", entityType, "findBy" + getIdName() ).
      addCode( CodeBlock.builder().
        beginControlFlow( "if ( null == entity )" ).
        addStatement( "throw new $T( $T.class, id )", GeneratorUtil.NO_SUCH_ENTITY_EXCEPTION_CLASSNAME, entityType ).
        endControlFlow().
        build() ).
      addStatement( "return entity" ).
      build();
  }

  @Nonnull
  private MethodSpec buildFactoryMethod()
  {
    return MethodSpec.methodBuilder( "newRepository" ).
      addModifiers( Modifier.PUBLIC, Modifier.STATIC ).
      addAnnotation( Nonnull.class ).
      returns( ClassName.get( getPackageName(), getRepositoryName() ) ).
      addStatement( "return new $T()", ClassName.get( getPackageName(), getArezRepositoryName() ) ).build();
  }

  @Nonnull
  private MethodSpec buildRepositoryCreate( @Nonnull final ExecutableElement constructor,
                                            @Nonnull final ExecutableType methodType,
                                            @Nonnull final ClassName arezType )
  {
    final String suffix = constructor.getParameters().stream().
      map( p -> p.getSimpleName().toString() ).collect( Collectors.joining( "_" ) );
    final String actionName = "create" + ( suffix.isEmpty() ? "" : "_" + suffix );
    final AnnotationSpec annotationSpec =
      AnnotationSpec.builder( Action.class ).addMember( "name", "$S", actionName ).build();
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "create" ).
        addAnnotation( annotationSpec ).
        addAnnotation( Nonnull.class ).
        returns( TypeName.get( asDeclaredType() ) );

    ProcessorUtil.copyAccessModifiers( constructor, builder );
    ProcessorUtil.copyExceptions( methodType, builder );
    ProcessorUtil.copyTypeParameters( methodType, builder );

    final StringBuilder newCall = new StringBuilder();
    newCall.append( "final $T entity = new $T(" );
    final ArrayList<Object> parameters = new ArrayList<>();
    parameters.add( arezType );
    parameters.add( arezType );

    boolean firstParam = true;
    for ( final VariableElement element : constructor.getParameters() )
    {
      final ParameterSpec.Builder param =
        ParameterSpec.builder( TypeName.get( element.asType() ), element.getSimpleName().toString(), Modifier.FINAL );
      ProcessorUtil.copyDocumentedAnnotations( element, param );
      builder.addParameter( param.build() );
      parameters.add( element.getSimpleName().toString() );
      if ( !firstParam )
      {
        newCall.append( "," );
      }
      firstParam = false;
      newCall.append( "$N" );
    }

    newCall.append( ")" );
    builder.addStatement( newCall.toString(), parameters.toArray() );

    builder.addStatement( "entity.$N( e -> destroy( e ) )", GeneratorUtil.SET_ON_DISPOSE_METHOD_NAME );

    builder.addStatement( "this.$N.put( entity.$N(), entity )", ENTITIES_FIELD_NAME, getIdMethodName() );
    builder.addStatement( "$N().reportChanged()", GET_OBSERVABLE_METHOD );
    builder.addStatement( "return entity", ENTITIES_FIELD_NAME );
    return builder.build();
  }

  @Nonnull
  String getPackageName()
  {
    return _packageElement.getQualifiedName().toString();
  }

  private void buildRepositoryFields( @Nonnull final TypeSpec.Builder builder )
  {
    // Create the entities field
    {
      final CodeBlock.Builder initializer = CodeBlock.builder().addStatement( "new $T<>()", HashMap.class );
      final ParameterizedTypeName fieldType =
        ParameterizedTypeName.get( ClassName.get( HashMap.class ),
                                   getIdType().box(),
                                   TypeName.get( _element.asType() ) );
      final FieldSpec.Builder field =
        FieldSpec.builder( fieldType, ENTITIES_FIELD_NAME, Modifier.FINAL, Modifier.PRIVATE ).
          initializer( initializer.build() );
      builder.addField( field.build() );
    }

    // Create the entityList field
    {
      final CodeBlock.Builder initializer =
        CodeBlock.builder().addStatement( "$T.unmodifiableCollection( this.$N.values() )",
                                          Collections.class,
                                          ENTITIES_FIELD_NAME );
      final ParameterizedTypeName fieldType =
        ParameterizedTypeName.get( ClassName.get( Collection.class ), TypeName.get( _element.asType() ) );
      final FieldSpec.Builder field =
        FieldSpec.builder( fieldType, ENTITYLIST_FIELD_NAME, Modifier.FINAL, Modifier.PRIVATE ).
          initializer( initializer.build() );
      builder.addField( field.build() );
    }
  }

  @Nonnull
  private String getIdMethodName()
  {
    return null != _componentId ? _componentId.getSimpleName().toString() : GeneratorUtil.ID_FIELD_NAME;
  }

  @Nonnull
  private String getIdName()
  {
    if ( null != _componentId )
    {
      final String name = ProcessorUtil.deriveName( _componentId, GETTER_PATTERN, ProcessorUtil.SENTINEL_NAME );
      if ( null != name )
      {
        return Character.toUpperCase( name.charAt( 0 ) ) + ( name.length() > 1 ? name.substring( 1 ) : "" );
      }
    }
    return "Id";
  }

  @Nonnull
  private TypeName getIdType()
  {
    return null == _componentId ?
           TypeName.LONG :
           TypeName.get( _componentId.getReturnType() );
  }
}
