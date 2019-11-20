package arez.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * The class that represents the parsed state of ArezComponent annotated class.
 */
final class ComponentDescriptor
{
  enum InjectMode
  {
    NONE,
    CONSUME,
    PROVIDE
  }

  private static final Pattern OBSERVABLE_REF_PATTERN = Pattern.compile( "^get([A-Z].*)ObservableValue$" );
  private static final Pattern COMPUTABLE_VALUE_REF_PATTERN = Pattern.compile( "^get([A-Z].*)ComputableValue$" );
  private static final Pattern OBSERVER_REF_PATTERN = Pattern.compile( "^get([A-Z].*)Observer$" );
  private static final Pattern PRIORITY_OVERRIDE_PATTERN = Pattern.compile( "^(.*)Priority$" );
  private static final Pattern SETTER_PATTERN = Pattern.compile( "^set([A-Z].*)$" );
  private static final Pattern GETTER_PATTERN = Pattern.compile( "^get([A-Z].*)$" );
  private static final Pattern ID_GETTER_PATTERN = Pattern.compile( "^get([A-Z].*)Id$" );
  private static final Pattern RAW_ID_GETTER_PATTERN = Pattern.compile( "^(.*)Id$" );
  private static final Pattern ISSER_PATTERN = Pattern.compile( "^is([A-Z].*)$" );
  private static final List<String> OBJECT_METHODS =
    Arrays.asList( "hashCode", "equals", "clone", "toString", "finalize", "getClass", "wait", "notifyAll", "notify" );
  private static final List<String> AREZ_SPECIAL_METHODS =
    Arrays.asList( "observe", "dispose", "isDisposed", "getArezId" );
  @Nullable
  private List<TypeElement> _repositoryExtensions;
  /**
   * Flag controlling whether dagger module is created for repository.
   */
  private String _repositoryDaggerConfig = "AUTODETECT";
  /**
   * Flag controlling whether Inject annotation is added to repository constructor.
   */
  private String _repositoryInjectMode = "AUTODETECT";
  @Nonnull
  private final ProcessingEnvironment _processingEnv;
  @Nonnull
  private final String _type;
  private final boolean _nameIncludesId;
  private final boolean _allowEmpty;
  @Nullable
  private final Priority _defaultPriority;
  /**
   * Flag indicating that there is a @Generated annotation on arez.
   * In this scenario we are a little more forgiving with our errors as otherwise the generating tool would need to
   * have a deep understanding of the component model to generate the code which may be too demanding for downstream
   * generators.
   */
  private final boolean _generated;
  private final boolean _observable;
  private final boolean _disposeNotifier;
  private final boolean _disposeOnDeactivate;
  @Nonnull
  private final InjectMode _injectMode;
  private final boolean _dagger;
  private final boolean _injectFactory;
  /**
   * Annotation that indicates whether equals/hashCode should be implemented. See arez.annotations.ArezComponent.requireEquals()
   */
  private final boolean _requireEquals;
  /**
   * Flag indicating whether generated component should implement arez.component.Verifiable.
   */
  private final boolean _verify;
  /**
   * Scope annotation that is declared on component and should be transferred to injection providers.
   */
  private final AnnotationMirror _scopeAnnotation;
  private final boolean _deferSchedule;
  private final boolean _generateToString;
  private boolean _idRequired;
  @Nonnull
  private final TypeElement _element;
  @Nullable
  private ExecutableElement _postConstruct;
  @Nullable
  private ExecutableElement _componentId;
  @Nullable
  private ExecutableElement _componentIdRef;
  @Nullable
  private ExecutableType _componentIdMethodType;
  @Nullable
  private ExecutableElement _componentRef;
  @Nullable
  private ExecutableElement _contextRef;
  @Nullable
  private ExecutableElement _componentTypeNameRef;
  @Nullable
  private ExecutableElement _componentNameRef;
  @Nullable
  private ExecutableElement _preDispose;
  @Nullable
  private ExecutableElement _postDispose;
  private final List<ComponentStateRefDescriptor> _componentStateRefs = new ArrayList<>();
  private final Map<String, CandidateMethod> _observerRefs = new LinkedHashMap<>();
  private final Map<String, ObservableDescriptor> _observables = new LinkedHashMap<>();
  private final Collection<ObservableDescriptor> _roObservables =
    Collections.unmodifiableCollection( _observables.values() );
  private final Map<String, ActionDescriptor> _actions = new LinkedHashMap<>();
  private final Collection<ActionDescriptor> _roActions =
    Collections.unmodifiableCollection( _actions.values() );
  private final Map<String, MemoizeDescriptor> _memoizes = new LinkedHashMap<>();
  private final Collection<MemoizeDescriptor> _roMemoizes =
    Collections.unmodifiableCollection( _memoizes.values() );
  private final Map<String, ObserveDescriptor> _observes = new LinkedHashMap<>();
  private final Collection<ObserveDescriptor> _roObserves =
    Collections.unmodifiableCollection( _observes.values() );
  private final Map<Element, DependencyDescriptor> _dependencies = new LinkedHashMap<>();
  private final Collection<DependencyDescriptor> _roDependencies =
    Collections.unmodifiableCollection( _dependencies.values() );
  private final Map<Element, CascadeDisposableDescriptor> _cascadeDisposes = new LinkedHashMap<>();
  private final Collection<CascadeDisposableDescriptor> _roCascadeDisposes =
    Collections.unmodifiableCollection( _cascadeDisposes.values() );
  private final Map<String, ReferenceDescriptor> _references = new LinkedHashMap<>();
  private final Collection<ReferenceDescriptor> _roReferences =
    Collections.unmodifiableCollection( _references.values() );
  private final Map<String, InverseDescriptor> _inverses = new LinkedHashMap<>();
  private final Collection<InverseDescriptor> _roInverses =
    Collections.unmodifiableCollection( _inverses.values() );
  private final Map<String, CandidateMethod> _priorityOverrides = new LinkedHashMap<>();

  ComponentDescriptor( @Nonnull final ProcessingEnvironment processingEnv,
                       @Nonnull final String type,
                       final boolean nameIncludesId,
                       final boolean allowEmpty,
                       @Nullable final Priority defaultPriority,
                       final boolean generated,
                       final boolean observable,
                       final boolean disposeNotifier,
                       final boolean disposeOnDeactivate,
                       @Nonnull final String injectMode,
                       final boolean dagger,
                       final boolean injectFactory,
                       final boolean requireEquals,
                       final boolean verify,
                       @Nullable final AnnotationMirror scopeAnnotation,
                       final boolean deferSchedule,
                       final boolean generateToString,
                       @Nonnull final TypeElement element )
  {
    _processingEnv = Objects.requireNonNull( processingEnv );
    _type = Objects.requireNonNull( type );
    _nameIncludesId = nameIncludesId;
    _allowEmpty = allowEmpty;
    _defaultPriority = defaultPriority;
    _generated = generated;
    _observable = observable;
    _disposeNotifier = disposeNotifier;
    _disposeOnDeactivate = disposeOnDeactivate;
    _injectMode = InjectMode.valueOf( injectMode );
    _dagger = dagger;
    _injectFactory = injectFactory;
    _requireEquals = requireEquals;
    _verify = verify;
    _scopeAnnotation = scopeAnnotation;
    _deferSchedule = deferSchedule;
    _generateToString = generateToString;
    _element = Objects.requireNonNull( element );
  }

  @Nonnull
  ClassName getClassName()
  {
    return ClassName.get( getElement() );
  }

  @Nonnull
  Types getTypeUtils()
  {
    return _processingEnv.getTypeUtils();
  }

  private boolean hasDeprecatedElements()
  {
    return isDeprecated( _postConstruct ) ||
           isDeprecated( _componentId ) ||
           isDeprecated( _componentRef ) ||
           isDeprecated( _contextRef ) ||
           isDeprecated( _componentTypeNameRef ) ||
           isDeprecated( _componentNameRef ) ||
           isDeprecated( _preDispose ) ||
           isDeprecated( _postDispose ) ||
           _roObservables.stream().anyMatch( e -> ( e.hasSetter() && isDeprecated( e.getSetter() ) ) ||
                                                  ( e.hasGetter() && isDeprecated( e.getGetter() ) ) ) ||
           _roMemoizes.stream().anyMatch( e -> ( e.hasMemoize() && isDeprecated( e.getMethod() ) ) ||
                                               isDeprecated( e.getOnActivate() ) ||
                                               isDeprecated( e.getOnDeactivate() ) ||
                                               isDeprecated( e.getOnStale() ) ) ||
           _observerRefs.values().stream().anyMatch( e -> isDeprecated( e.getMethod() ) ) ||
           _roDependencies.stream().anyMatch( e -> ( e.isMethodDependency() && isDeprecated( e.getMethod() ) ) ||
                                                   ( !e.isMethodDependency() && isDeprecated( e.getField() ) ) ) ||
           _roActions.stream().anyMatch( e -> isDeprecated( e.getAction() ) ) ||
           _roObserves.stream().anyMatch( e -> ( e.hasObserve() && isDeprecated( e.getObserve() ) ) ||
                                               ( e.hasOnDepsChange() && isDeprecated( e.getOnDepsChange() ) ) );

  }

  void setIdRequired( final boolean idRequired )
  {
    _idRequired = idRequired;
  }

  boolean shouldVerify()
  {
    return _verify;
  }

  boolean isDisposeNotifier()
  {
    return _disposeNotifier;
  }

  private boolean isDeprecated( @Nullable final Element element )
  {
    return null != element && null != element.getAnnotation( Deprecated.class );
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
  String getType()
  {
    return _type;
  }

  @Nonnull
  private ReferenceDescriptor findOrCreateReference( @Nonnull final String name )
  {
    return _references.computeIfAbsent( name, n -> new ReferenceDescriptor( this, name ) );
  }

  @Nonnull
  private ObservableDescriptor findOrCreateObservable( @Nonnull final String name )
  {
    return _observables.computeIfAbsent( name, n -> new ObservableDescriptor( this, n ) );
  }

  @Nonnull
  private ObserveDescriptor findOrCreateObserve( @Nonnull final String name )
  {
    return _observes.computeIfAbsent( name, n -> new ObserveDescriptor( this, n ) );
  }

  @Nonnull
  private ObservableDescriptor addObservable( @Nonnull final AnnotationMirror annotation,
                                              @Nonnull final ExecutableElement method,
                                              @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    MemberChecks.mustBeOverridable( getElement(),
                                    Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                    Constants.OBSERVABLE_ANNOTATION_CLASSNAME,
                                    method );

    final String declaredName = getAnnotationParameter( annotation, "name" );
    final boolean expectSetter = getAnnotationParameter( annotation, "expectSetter" );
    final boolean readOutsideTransaction = getAnnotationParameter( annotation, "readOutsideTransaction" );
    final boolean writeOutsideTransaction = getAnnotationParameter( annotation, "writeOutsideTransaction" );
    final boolean setterAlwaysMutates = getAnnotationParameter( annotation, "setterAlwaysMutates" );
    final Boolean requireInitializer = isInitializerRequired( method );

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
        throw new ProcessorException( "@Observable target should be a setter or getter", method );
      }

      name = ProcessorUtil.deriveName( method, SETTER_PATTERN, declaredName );
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
        throw new ProcessorException( "@Observable target should be a setter or getter", method );
      }
      name = getPropertyAccessorName( method, declaredName );
    }
    // Override name if supplied by user
    if ( !Constants.SENTINEL.equals( declaredName ) )
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Observable target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Observable target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }
    checkNameUnique( name, method, Constants.OBSERVABLE_ANNOTATION_CLASSNAME );

    if ( setter && !expectSetter )
    {
      throw new ProcessorException( "Method annotated with @Observable is a setter but defines " +
                                    "expectSetter = false for observable named " + name, method );
    }

    final ObservableDescriptor observable = findOrCreateObservable( name );
    if ( readOutsideTransaction )
    {
      observable.setReadOutsideTransaction( true );
    }
    if ( writeOutsideTransaction )
    {
      observable.setWriteOutsideTransaction( true );
    }
    if ( !setterAlwaysMutates )
    {
      observable.setSetterAlwaysMutates( false );
    }
    if ( !expectSetter )
    {
      observable.setExpectSetter( false );
    }
    if ( !observable.expectSetter() )
    {
      if ( observable.hasSetter() )
      {
        throw new ProcessorException( "Method annotated with @Observable defines expectSetter = false but a " +
                                      "setter exists named " + observable.getSetter().getSimpleName() +
                                      "for observable named " + name, method );
      }
    }
    if ( setter )
    {
      if ( observable.hasSetter() )
      {
        throw new ProcessorException( "Method annotated with @Observable defines duplicate setter for " +
                                      "observable named " + name, method );
      }
      if ( !observable.expectSetter() )
      {
        throw new ProcessorException( "Method annotated with @Observable defines expectSetter = false but a " +
                                      "setter exists for observable named " + name, method );
      }
      observable.setSetter( method, methodType );
    }
    else
    {
      if ( observable.hasGetter() )
      {
        throw new ProcessorException( "Method annotated with @Observable defines duplicate getter for " +
                                      "observable named " + name, method );
      }
      observable.setGetter( method, methodType );
    }
    if ( null != requireInitializer )
    {
      if ( !method.getModifiers().contains( Modifier.ABSTRACT ) )
      {
        throw new ProcessorException( "@Observable target set initializer parameter to ENABLED but " +
                                      "method is not abstract.", method );
      }
      final Boolean existing = observable.getInitializer();
      if ( null == existing )
      {
        observable.setInitializer( requireInitializer );
      }
      else if ( existing != requireInitializer )
      {
        throw new ProcessorException( "@Observable target set initializer parameter to value that differs from " +
                                      "the paired observable method.", method );
      }
    }
    return observable;
  }

  private void addObservableValueRef( @Nonnull final AnnotationMirror annotation,
                                      @Nonnull final ExecutableElement method,
                                      @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv,
                                       this,
                                       method,
                                       Constants.OBSERVABLE_VALUE_REF_ANNOTATION_CLASSNAME );

    final TypeMirror returnType = methodType.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !toRawType( returnType ).toString().equals( "arez.ObservableValue" ) )
    {
      throw new ProcessorException( "Method annotated with @ObservableValueRef must return an instance of " +
                                    "arez.ObservableValue", method );
    }

    final String declaredName = getAnnotationParameter( annotation, "name" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      name = ProcessorUtil.deriveName( method, OBSERVABLE_REF_PATTERN, declaredName );
      if ( null == name )
      {
        throw new ProcessorException( "Method annotated with @ObservableValueRef should specify name or be " +
                                      "named according to the convention get[Name]ObservableValue", method );
      }
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@ObservableValueRef target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@ObservableValueRef target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }

    final ObservableDescriptor observable = findOrCreateObservable( name );

    if ( observable.hasRefMethod() )
    {
      throw new ProcessorException( "Method annotated with @ObservableValueRef defines duplicate ref " +
                                    "accessor for observable named " + name, method );
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

  private void addAction( @Nonnull final AnnotationMirror annotation,
                          @Nonnull final ExecutableElement method,
                          @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    MemberChecks.mustBeWrappable( getElement(),
                                  Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                  Constants.ACTION_ANNOTATION_CLASSNAME,
                                  method );

    final String name = deriveActionName( method, annotation );
    checkNameUnique( name, method, Constants.ACTION_ANNOTATION_CLASSNAME );
    final boolean mutation = getAnnotationParameter( annotation, "mutation" );
    final boolean requireNewTransaction = getAnnotationParameter( annotation, "requireNewTransaction" );
    final boolean reportParameters = getAnnotationParameter( annotation, "reportParameters" );
    final boolean reportResult = getAnnotationParameter( annotation, "reportResult" );
    final boolean verifyRequired = getAnnotationParameter( annotation, "verifyRequired" );
    final ActionDescriptor action =
      new ActionDescriptor( this,
                            name,
                            requireNewTransaction,
                            mutation,
                            verifyRequired,
                            reportParameters,
                            reportResult,
                            method,
                            methodType );
    _actions.put( action.getName(), action );
  }

  @Nonnull
  private String deriveActionName( @Nonnull final ExecutableElement method, @Nonnull final AnnotationMirror annotation )
    throws ProcessorException
  {
    final String name = getAnnotationParameter( annotation, "name" );
    if ( Constants.SENTINEL.equals( name ) )
    {
      return method.getSimpleName().toString();
    }
    else
    {
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Action target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Action target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
      return name;
    }
  }

  private void addObserve( @Nonnull final AnnotationMirror annotation,
                           @Nonnull final ExecutableElement method,
                           @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    final String name = deriveObserveName( method, annotation );
    checkNameUnique( name, method, Constants.OBSERVE_ANNOTATION_CLASSNAME );
    final boolean mutation = getAnnotationParameter( annotation, "mutation" );
    final boolean observeLowerPriorityDependencies =
      getAnnotationParameter( annotation, "observeLowerPriorityDependencies" );
    final boolean nestedActionsAllowed = getAnnotationParameter( annotation, "nestedActionsAllowed" );
    final VariableElement priority = getAnnotationParameter( annotation, "priority" );
    final boolean reportParameters = getAnnotationParameter( annotation, "reportParameters" );
    final boolean reportResult = getAnnotationParameter( annotation, "reportResult" );
    final VariableElement executor = getAnnotationParameter( annotation, "executor" );
    final VariableElement depType = getAnnotationParameter( annotation, "depType" );

    findOrCreateObserve( name ).setObserveMethod( mutation,
                                                  toPriority( priority ),
                                                  executor.getSimpleName().toString().equals( "INTERNAL" ),
                                                  reportParameters,
                                                  reportResult,
                                                  depType.getSimpleName().toString(),
                                                  observeLowerPriorityDependencies,
                                                  nestedActionsAllowed,
                                                  method,
                                                  methodType );
  }

  @Nonnull
  private String deriveObserveName( @Nonnull final ExecutableElement method,
                                    @Nonnull final AnnotationMirror annotation )
    throws ProcessorException
  {
    final String name = getAnnotationParameter( annotation, "name" );
    if ( Constants.SENTINEL.equals( name ) )
    {
      return method.getSimpleName().toString();
    }
    else
    {
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Observe target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Observe target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
      return name;
    }
  }

  private void addOnDepsChange( @Nonnull final AnnotationMirror annotation, @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    final String name =
      deriveHookName( method,
                      ObserveDescriptor.ON_DEPS_CHANGE_PATTERN,
                      "DepsChange",
                      getAnnotationParameter( annotation, "name" ) );
    findOrCreateObserve( name ).setOnDepsChange( method );
  }

  private void addObserverRef( @Nonnull final AnnotationMirror annotation,
                               @Nonnull final ExecutableElement method,
                               @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv, this, method, Constants.OBSERVER_REF_ANNOTATION_CLASSNAME );
    MemberChecks.mustReturnAnInstanceOf( _processingEnv,
                                         method,
                                         Constants.OBSERVER_REF_ANNOTATION_CLASSNAME,
                                         Constants.OBSERVER_CLASSNAME );

    final String declaredName = getAnnotationParameter( annotation, "name" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      name = ProcessorUtil.deriveName( method, OBSERVER_REF_PATTERN, declaredName );
      if ( null == name )
      {
        throw new ProcessorException( "Method annotated with @ObserverRef should specify name or be " +
                                      "named according to the convention get[Name]Observer", method );
      }
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@ObserverRef target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@ObserverRef target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }
    if ( _observerRefs.containsKey( name ) )
    {
      throw new ProcessorException( "Method annotated with @ObserverRef defines duplicate ref accessor for " +
                                    "observer named " + name, method );
    }
    _observerRefs.put( name, new CandidateMethod( method, methodType ) );
  }

  @Nonnull
  private MemoizeDescriptor findOrCreateMemoize( @Nonnull final String name )
  {
    return _memoizes.computeIfAbsent( name, n -> new MemoizeDescriptor( this, n ) );
  }

  private void addMemoize( @Nonnull final AnnotationMirror annotation,
                           @Nonnull final ExecutableElement method,
                           @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    final String name = deriveMemoizeName( method, annotation );
    checkNameUnique( name, method, Constants.MEMOIZE_ANNOTATION_CLASSNAME );
    final boolean keepAlive = getAnnotationParameter( annotation, "keepAlive" );
    final boolean reportResult = getAnnotationParameter( annotation, "reportResult" );
    final boolean observeLowerPriorityDependencies =
      getAnnotationParameter( annotation, "observeLowerPriorityDependencies" );
    final boolean readOutsideTransaction = getAnnotationParameter( annotation, "readOutsideTransaction" );
    final VariableElement priority = getAnnotationParameter( annotation, "priority" );
    final VariableElement depType = getAnnotationParameter( annotation, "depType" );
    final String depTypeAsString = depType.getSimpleName().toString();
    findOrCreateMemoize( name ).setMemoize( method,
                                            methodType,
                                            keepAlive,
                                            toPriority( priority ),
                                            reportResult,
                                            observeLowerPriorityDependencies,
                                            readOutsideTransaction,
                                            depTypeAsString );
  }

  @Nonnull
  private Priority toPriority( @Nonnull final VariableElement priorityElement )
  {
    final String priorityName = priorityElement.getSimpleName().toString();
    return "DEFAULT".equals( priorityName ) ?
           null != _defaultPriority ? _defaultPriority : Priority.NORMAL :
           Priority.valueOf( priorityName );
  }

  private void addComputableValueRef( @Nonnull final AnnotationMirror annotation,
                                      @Nonnull final ExecutableElement method,
                                      @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    ArezUtils.mustBeRefMethod( _processingEnv, this, method, Constants.COMPUTABLE_VALUE_REF_ANNOTATION_CLASSNAME );

    final TypeMirror returnType = methodType.getReturnType();
    if ( TypeKind.DECLARED != returnType.getKind() ||
         !toRawType( returnType ).toString().equals( "arez.ComputableValue" ) )
    {
      throw new ProcessorException( "Method annotated with @ComputableValueRef must return an instance of " +
                                    "arez.ComputableValue", method );
    }

    final String declaredName = getAnnotationParameter( annotation, "name" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      name = ProcessorUtil.deriveName( method, COMPUTABLE_VALUE_REF_PATTERN, declaredName );
      if ( null == name )
      {
        throw new ProcessorException( "Method annotated with @ComputableValueRef should specify name or be " +
                                      "named according to the convention get[Name]ComputableValue", method );
      }
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@ComputableValueRef target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@ComputableValueRef target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }

    findOrCreateMemoize( name ).setRefMethod( method, methodType );
  }

  @Nonnull
  private String deriveMemoizeName( @Nonnull final ExecutableElement method,
                                    @Nonnull final AnnotationMirror annotation )
    throws ProcessorException
  {
    final String name = getAnnotationParameter( annotation, "name" );
    if ( Constants.SENTINEL.equals( name ) )
    {
      return getPropertyAccessorName( method, name );
    }
    else
    {
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Memoize target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Memoize target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
      return name;
    }
  }

  private void addOnActivate( @Nonnull final AnnotationMirror annotation, @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    final String name = deriveHookName( method,
                                        MemoizeDescriptor.ON_ACTIVATE_PATTERN,
                                        "Activate",
                                        getAnnotationParameter( annotation, "name" ) );
    findOrCreateMemoize( name ).setOnActivate( method );
  }

  private void addOnDeactivate( @Nonnull final AnnotationMirror annotation, @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    final String name =
      deriveHookName( method,
                      MemoizeDescriptor.ON_DEACTIVATE_PATTERN,
                      "Deactivate",
                      getAnnotationParameter( annotation, "name" ) );
    findOrCreateMemoize( name ).setOnDeactivate( method );
  }

  private void addOnStale( @Nonnull final AnnotationMirror annotation, @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    final String name =
      deriveHookName( method,
                      MemoizeDescriptor.ON_STALE_PATTERN,
                      "Stale",
                      getAnnotationParameter( annotation, "name" ) );
    findOrCreateMemoize( name ).setOnStale( method );
  }

  @Nonnull
  private String deriveHookName( @Nonnull final ExecutableElement method,
                                 @Nonnull final Pattern pattern,
                                 @Nonnull final String type,
                                 @Nonnull final String name )
    throws ProcessorException
  {
    final String value = ProcessorUtil.deriveName( method, pattern, name );
    if ( null == value )
    {
      throw new ProcessorException( "Unable to derive name for @On" + type + " as does not match " +
                                    "on[Name]" + type + " pattern. Please specify name.", method );
    }
    else if ( !SourceVersion.isIdentifier( value ) )
    {
      throw new ProcessorException( "@On" + type + " target specified an invalid name '" + value + "'. The " +
                                    "name must be a valid java identifier.", _element );
    }
    else if ( SourceVersion.isKeyword( value ) )
    {
      throw new ProcessorException( "@On" + type + " target specified an invalid name '" + value + "'. The " +
                                    "name must not be a java keyword.", _element );
    }
    else
    {
      return value;
    }
  }

  private void addComponentStateRef( @Nonnull final AnnotationMirror annotation,
                                     @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv,
                                       this,
                                       method,
                                       Constants.COMPONENT_STATE_REF_ANNOTATION_CLASSNAME );

    final TypeMirror returnType = method.getReturnType();
    if ( TypeKind.BOOLEAN != returnType.getKind() )
    {
      throw new ProcessorException( "Method annotated with @ComponentStateRef must return a boolean", method );
    }
    final VariableElement variableElement = AnnotationsUtil.getAnnotationValue( annotation, "value" );
    final ComponentStateRefDescriptor.State state =
      ComponentStateRefDescriptor.State.valueOf( variableElement.getSimpleName().toString() );

    _componentStateRefs.add( new ComponentStateRefDescriptor( method, state ) );
  }

  private void setContextRef( @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv, this, method, Constants.CONTEXT_REF_ANNOTATION_CLASSNAME );
    MemberChecks.mustReturnAnInstanceOf( _processingEnv,
                                         method,
                                         Constants.OBSERVER_REF_ANNOTATION_CLASSNAME,
                                         "arez.ArezContext" );

    if ( null != _contextRef )
    {
      throw new ProcessorException( "@ContextRef target duplicates existing method named " +
                                    _contextRef.getSimpleName(), method );
    }
    else
    {
      _contextRef = Objects.requireNonNull( method );
    }
  }

  boolean hasComponentIdRefMethod()
  {
    return null != _componentIdRef;
  }

  private void setComponentIdRef( @Nonnull final ExecutableElement method )
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv, this, method, Constants.COMPONENT_ID_REF_ANNOTATION_CLASSNAME );

    if ( null != _componentIdRef )
    {
      throw new ProcessorException( "@ComponentIdRef target duplicates existing method named " +
                                    _componentIdRef.getSimpleName(), method );
    }
    else
    {
      _componentIdRef = Objects.requireNonNull( method );
    }
  }

  private void setComponentRef( @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv, this, method, Constants.COMPONENT_REF_ANNOTATION_CLASSNAME );
    MemberChecks.mustReturnAnInstanceOf( _processingEnv,
                                         method,
                                         Constants.COMPONENT_REF_ANNOTATION_CLASSNAME,
                                         "arez.Component" );
    if ( null != _componentRef )
    {
      throw new ProcessorException( "@ComponentRef target duplicates existing method named " +
                                    _componentRef.getSimpleName(), method );
    }
    else
    {
      _componentRef = Objects.requireNonNull( method );
    }
  }

  boolean hasComponentIdMethod()
  {
    return null != _componentId;
  }

  private void setComponentId( @Nonnull final ExecutableElement componentId,
                               @Nonnull final ExecutableType componentIdMethodType )
    throws ProcessorException
  {
    MemberChecks.mustNotBeAbstract( Constants.COMPONENT_ID_ANNOTATION_CLASSNAME, componentId );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.COMPONENT_ID_ANNOTATION_CLASSNAME,
                                         componentId );
    MemberChecks.mustBeFinal( Constants.COMPONENT_ID_ANNOTATION_CLASSNAME, componentId );
    MemberChecks.mustNotHaveAnyParameters( Constants.COMPONENT_ID_ANNOTATION_CLASSNAME, componentId );
    MemberChecks.mustReturnAValue( Constants.COMPONENT_ID_ANNOTATION_CLASSNAME, componentId );
    MemberChecks.mustNotThrowAnyExceptions( Constants.COMPONENT_ID_ANNOTATION_CLASSNAME, componentId );

    if ( null != _componentId )
    {
      throw new ProcessorException( "@ComponentId target duplicates existing method named " +
                                    _componentId.getSimpleName(), componentId );
    }
    else
    {
      _componentId = Objects.requireNonNull( componentId );
      _componentIdMethodType = componentIdMethodType;
    }
  }

  private void setComponentTypeNameRef( @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv,
                                       this,
                                       method,
                                       Constants.COMPONENT_TYPE_NAME_REF_ANNOTATION_CLASSNAME );
    MemberChecks.mustReturnAnInstanceOf( _processingEnv,
                                         method,
                                         Constants.COMPONENT_TYPE_NAME_REF_ANNOTATION_CLASSNAME,
                                         String.class.getName() );

    if ( null != _componentTypeNameRef )
    {
      throw new ProcessorException( "@ComponentTypeNameRef target duplicates existing method named " +
                                    _componentTypeNameRef.getSimpleName(), method );
    }
    else
    {
      _componentTypeNameRef = Objects.requireNonNull( method );
    }
  }

  private void setComponentNameRef( @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    ArezUtils.mustBeStandardRefMethod( _processingEnv,
                                       this,
                                       method,
                                       Constants.COMPONENT_NAME_REF_ANNOTATION_CLASSNAME );
    MemberChecks.mustReturnAnInstanceOf( _processingEnv,
                                         method,
                                         Constants.COMPONENT_NAME_REF_ANNOTATION_CLASSNAME,
                                         String.class.getName() );

    if ( null != _componentNameRef )
    {
      throw new ProcessorException( "@ComponentNameRef target duplicates existing method named " +
                                    _componentNameRef.getSimpleName(), method );
    }
    else
    {
      _componentNameRef = Objects.requireNonNull( method );
    }
  }

  private void setPostConstruct( @Nonnull final ExecutableElement postConstruct )
    throws ProcessorException
  {
    MemberChecks.mustBeLifecycleHook( getElement(),
                                      Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                      Constants.POST_CONSTRUCT_ANNOTATION_CLASSNAME,
                                      postConstruct );

    if ( null != _postConstruct )
    {
      throw new ProcessorException( "@PostConstruct target duplicates existing method named " +
                                    _postConstruct.getSimpleName(), postConstruct );
    }
    else
    {
      _postConstruct = postConstruct;
    }
  }

  private void setPreDispose( @Nonnull final ExecutableElement preDispose )
    throws ProcessorException
  {
    MemberChecks.mustBeLifecycleHook( getElement(),
                                      Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                      Constants.PRE_DISPOSE_ANNOTATION_CLASSNAME,
                                      preDispose );

    if ( null != _preDispose )
    {
      throw new ProcessorException( "@PreDispose target duplicates existing method named " +
                                    _preDispose.getSimpleName(), preDispose );
    }
    else
    {
      _preDispose = preDispose;
    }
  }

  private void setPostDispose( @Nonnull final ExecutableElement postDispose )
    throws ProcessorException
  {
    MemberChecks.mustBeLifecycleHook( getElement(),
                                      Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                      Constants.POST_DISPOSE_ANNOTATION_CLASSNAME,
                                      postDispose );

    if ( null != _postDispose )
    {
      throw new ProcessorException( "@PostDispose target duplicates existing method named " +
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
    throws ProcessorException
  {
    _cascadeDisposes.values().forEach( CascadeDisposableDescriptor::validate );
    _roObservables.forEach( ObservableDescriptor::validate );
    _roMemoizes.forEach( MemoizeDescriptor::validate );
    _roObserves.forEach( ObserveDescriptor::validate );
    _roDependencies.forEach( DependencyDescriptor::validate );
    _roReferences.forEach( ReferenceDescriptor::validate );
    _roInverses.forEach( InverseDescriptor::validate );

    final boolean hasReactiveElements =
      _roObservables.isEmpty() &&
      _roActions.isEmpty() &&
      _roMemoizes.isEmpty() &&
      _roDependencies.isEmpty() &&
      _roCascadeDisposes.isEmpty() &&
      _roReferences.isEmpty() &&
      _roInverses.isEmpty() &&
      _roObserves.isEmpty();

    if ( null != _defaultPriority &&
         _roMemoizes.isEmpty() &&
         _roObserves.isEmpty() &&
         !isWarningSuppressed( _element, Constants.WARNING_UNNECESSARY_DEFAULT_PRIORITY ) )
    {
      final String message =
        MemberChecks.toSimpleName( Constants.COMPONENT_ANNOTATION_CLASSNAME ) + " target should not specify " +
        "the defaultPriority parameter unless it contains methods annotated with either the " +
        MemberChecks.toSimpleName( Constants.MEMOIZE_ANNOTATION_CLASSNAME ) + " annotation or the " +
        MemberChecks.toSimpleName( Constants.OBSERVE_ANNOTATION_CLASSNAME ) + " annotation. " +
        suppressedBy( Constants.WARNING_UNNECESSARY_DEFAULT_PRIORITY );
      _processingEnv.getMessager().printMessage( Diagnostic.Kind.WARNING, message );
    }
    if ( !_allowEmpty && hasReactiveElements )
    {
      throw new ProcessorException( "@ArezComponent target has no methods annotated with @Action, " +
                                    "@CascadeDispose, @Memoize, @Observable, @Inverse, " +
                                    "@Reference, @ComponentDependency or @Observe", _element );
    }
    else if ( _allowEmpty && !hasReactiveElements && !_generated )
    {
      throw new ProcessorException( "@ArezComponent target has specified allowEmpty = true but has methods " +
                                    "annotated with @Action, @CascadeDispose, @Memoize, @Observable, @Inverse, " +
                                    "@Reference, @ComponentDependency or @Observe", _element );
    }

    if ( _deferSchedule && !requiresSchedule() )
    {
      throw new ProcessorException( "@ArezComponent target has specified the deferSchedule = true " +
                                    "annotation parameter but has no methods annotated with @Observe, " +
                                    "@ComponentDependency or @Memoize(keepAlive=true)", _element );
    }
    if ( null != _componentIdRef &&
         null != _componentId &&
         !_processingEnv.getTypeUtils().isSameType( _componentId.getReturnType(), _componentIdRef.getReturnType() ) )
    {
      throw new ProcessorException( "@ComponentIdRef target has a return type " + _componentIdRef.getReturnType() +
                                    " and a @ComponentId annotated method with a return type " +
                                    _componentIdRef.getReturnType() + ". The types must match.", _element );
    }
    else if ( null != _componentIdRef &&
              null == _componentId &&
              !_processingEnv.getTypeUtils()
                .isSameType( _processingEnv.getTypeUtils().getPrimitiveType( TypeKind.INT ),
                             _componentIdRef.getReturnType() ) )
    {
      throw new ProcessorException( "@ComponentIdRef target has a return type " + _componentIdRef.getReturnType() +
                                    " but no @ComponentId annotated method. The type is expected to be of " +
                                    "type int.", _element );
    }
    else if ( InjectMode.NONE != _injectMode )
    {
      for ( final ExecutableElement constructor : ProcessorUtil.getConstructors( getElement() ) )
      {
        // The annotation processor engine can not distinguish between a "default constructor"
        // synthesized by the compiler and one written by a user that has the same signature.
        // So our check just skips scenarios where the constructor could be synthetic.
        if ( constructor.getModifiers().contains( Modifier.PUBLIC ) &&
             !( constructor.getParameters().isEmpty() && constructor.getThrownTypes().isEmpty() ) )
        {
          throw new ProcessorException( "@ArezComponent target has a public constructor but the inject parameter " +
                                        "does not resolve to NONE. Public constructors are not necessary when " +
                                        "the instantiation of the component is managed by the injection framework.",
                                        constructor );
        }
      }
      if ( InjectMode.PROVIDE == _injectMode &&
           _dagger &&
           !getElement().getModifiers().contains( Modifier.PUBLIC ) )
      {
        throw new ProcessorException( "@ArezComponent target is not public but is configured as inject = PROVIDE " +
                                      "using the dagger injection framework. Due to constraints within the " +
                                      "dagger framework the type needs to made public.",
                                      getElement() );
      }
    }
  }

  private boolean requiresSchedule()
  {
    return _roObserves.stream().anyMatch( ObserveDescriptor::isInternalExecutor ) ||
           !_roDependencies.isEmpty() ||
           _memoizes.values().stream().anyMatch( MemoizeDescriptor::isKeepAlive );
  }

  private void checkNameUnique( @Nonnull final String name,
                                @Nonnull final ExecutableElement sourceMethod,
                                @Nonnull final String sourceAnnotationName )
    throws ProcessorException
  {
    final ActionDescriptor action = _actions.get( name );
    if ( null != action )
    {
      throw toException( name,
                         sourceAnnotationName,
                         sourceMethod,
                         Constants.ACTION_ANNOTATION_CLASSNAME,
                         action.getAction() );
    }
    final MemoizeDescriptor memoize = _memoizes.get( name );
    if ( null != memoize && memoize.hasMemoize() )
    {
      throw toException( name,
                         sourceAnnotationName,
                         sourceMethod,
                         Constants.MEMOIZE_ANNOTATION_CLASSNAME,
                         memoize.getMethod() );
    }
    // Observe have pairs so let the caller determine whether a duplicate occurs in that scenario
    if ( !sourceAnnotationName.equals( Constants.OBSERVE_ANNOTATION_CLASSNAME ) )
    {
      final ObserveDescriptor observed = _observes.get( name );
      if ( null != observed )
      {
        throw toException( name,
                           sourceAnnotationName,
                           sourceMethod,
                           Constants.OBSERVE_ANNOTATION_CLASSNAME,
                           observed.getObserve() );
      }
    }
    // Observables have pairs so let the caller determine whether a duplicate occurs in that scenario
    if ( !sourceAnnotationName.equals( Constants.OBSERVABLE_ANNOTATION_CLASSNAME ) )
    {
      final ObservableDescriptor observable = _observables.get( name );
      if ( null != observable )
      {
        throw toException( name,
                           sourceAnnotationName,
                           sourceMethod,
                           Constants.OBSERVABLE_ANNOTATION_CLASSNAME,
                           observable.getDefiner() );
      }
    }
  }

  @Nonnull
  private ProcessorException toException( @Nonnull final String name,
                                          @Nonnull final String sourceAnnotationName,
                                          @Nonnull final ExecutableElement sourceMethod,
                                          @Nonnull final String targetAnnotationName,
                                          @Nonnull final ExecutableElement targetElement )
  {
    return new ProcessorException( "Method annotated with " + MemberChecks.toSimpleName( sourceAnnotationName ) +
                                   " specified name " + name + " that duplicates " +
                                   MemberChecks.toSimpleName( targetAnnotationName ) + " defined by method " +
                                   targetElement.getSimpleName(), sourceMethod );
  }

  void analyzeCandidateMethods( @Nonnull final List<ExecutableElement> methods,
                                @Nonnull final Types typeUtils )
    throws ProcessorException
  {
    for ( final ExecutableElement method : methods )
    {
      final String methodName = method.getSimpleName().toString();
      if ( AREZ_SPECIAL_METHODS.contains( methodName ) && method.getParameters().isEmpty() )
      {
        throw new ProcessorException( "Method defined on a class annotated by @ArezComponent uses a name " +
                                      "reserved by Arez", method );
      }
      else if ( methodName.startsWith( Generator.FIELD_PREFIX ) ||
                methodName.startsWith( Generator.OBSERVABLE_DATA_FIELD_PREFIX ) ||
                methodName.startsWith( Generator.REFERENCE_FIELD_PREFIX ) ||
                methodName.startsWith( Generator.FRAMEWORK_PREFIX ) )
      {
        throw new ProcessorException( "Method defined on a class annotated by @ArezComponent uses a name " +
                                      "with a prefix reserved by Arez", method );
      }
    }
    final Map<String, CandidateMethod> getters = new HashMap<>();
    final Map<String, CandidateMethod> setters = new HashMap<>();
    final Map<String, CandidateMethod> observes = new HashMap<>();
    final Map<String, CandidateMethod> onDepsChanges = new HashMap<>();
    for ( final ExecutableElement method : methods )
    {
      final ExecutableType methodType =
        (ExecutableType) typeUtils.asMemberOf( (DeclaredType) _element.asType(), method );
      if ( !analyzeMethod( method, methodType ) )
      {
        /*
         * If we get here the method was not annotated so we can try to detect if it is a
         * candidate arez method in case some arez annotations are implied via naming conventions.
         */
        if ( method.getModifiers().contains( Modifier.STATIC ) )
        {
          continue;
        }

        final CandidateMethod candidateMethod = new CandidateMethod( method, methodType );
        final boolean voidReturn = method.getReturnType().getKind() == TypeKind.VOID;
        final int parameterCount = method.getParameters().size();
        String name;

        if ( !method.getModifiers().contains( Modifier.FINAL ) )
        {
          name = ProcessorUtil.deriveName( method, SETTER_PATTERN, Constants.SENTINEL );
          if ( voidReturn && 1 == parameterCount && null != name )
          {
            setters.put( name, candidateMethod );
            continue;
          }
          name = ProcessorUtil.deriveName( method, ISSER_PATTERN, Constants.SENTINEL );
          if ( !voidReturn && 0 == parameterCount && null != name )
          {
            getters.put( name, candidateMethod );
            continue;
          }
          name = ProcessorUtil.deriveName( method, GETTER_PATTERN, Constants.SENTINEL );
          if ( !voidReturn && 0 == parameterCount && null != name )
          {
            getters.put( name, candidateMethod );
            continue;
          }
        }
        name =
          ProcessorUtil.deriveName( method, ObserveDescriptor.ON_DEPS_CHANGE_PATTERN, Constants.SENTINEL );
        if ( voidReturn && null != name )
        {
          if (
            0 == parameterCount ||
            (
              1 == parameterCount &&
              Constants.OBSERVER_CLASSNAME.equals( method.getParameters().get( 0 ).asType().toString() )
            )
          )
          {
            onDepsChanges.put( name, candidateMethod );
            continue;
          }
        }

        final String methodName = method.getSimpleName().toString();
        if ( !OBJECT_METHODS.contains( methodName ) )
        {
          observes.put( methodName, candidateMethod );
        }
      }
    }

    linkUnAnnotatedObservables( getters, setters );
    linkUnAnnotatedObserves( observes, onDepsChanges );
    linkObserverRefs();
    linkCascadeDisposeObservables();
    linkCascadeDisposeReferences();

    // CascadeDispose returned false but it was actually processed so lets remove them from getters set
    _cascadeDisposes.keySet().forEach( method -> {
      for ( final Map.Entry<String, CandidateMethod> entry : new HashMap<>( getters ).entrySet() )
      {
        if ( method.equals( entry.getValue().getMethod() ) )
        {
          getters.remove( entry.getKey() );
        }
      }
    } );

    linkDependencies( getters.values() );

    autodetectObservableInitializers();

    linkPriorityOverrideMethods();

    /*
     * All of the maps will have called remove() for all matching candidates.
     * Thus any left are the non-arez methods.
     */

    ensureNoAbstractMethods( getters.values() );
    ensureNoAbstractMethods( setters.values() );
    ensureNoAbstractMethods( observes.values() );
    ensureNoAbstractMethods( onDepsChanges.values() );

    processCascadeDisposeFields();
    processComponentDependencyFields();
  }

  private void processComponentDependencyFields()
  {
    ProcessorUtil.getFieldElements( _element )
      .stream()
      .filter( f -> AnnotationsUtil.hasAnnotationOfType( f, Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME ) )
      .forEach( this::processComponentDependencyField );
  }

  private void processComponentDependencyField( @Nonnull final VariableElement field )
  {
    verifyNoDuplicateAnnotations( field );
    MemberChecks.mustBeSubclassCallable( _element,
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                         field );
    addDependency( field );
  }

  private void processCascadeDisposeFields()
  {
    ProcessorUtil.getFieldElements( _element )
      .stream()
      .filter( f -> AnnotationsUtil.hasAnnotationOfType( f, Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME ) )
      .forEach( this::processCascadeDisposeField );
  }

  private void processCascadeDisposeField( @Nonnull final VariableElement field )
  {
    verifyNoDuplicateAnnotations( field );
    MemberChecks.mustBeSubclassCallable( _element,
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME,
                                         field );
    mustBeCascadeDisposeTypeCompatible( field );
    _cascadeDisposes.put( field, new CascadeDisposableDescriptor( field ) );
  }

  @SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
  boolean isCascadeDisposeDefined( @Nonnull final Element element )
  {
    return _cascadeDisposes.containsKey( element );
  }

  private void mustBeCascadeDisposeTypeCompatible( @Nonnull final VariableElement field )
  {
    final TypeElement disposable = _processingEnv.getElementUtils().getTypeElement( Constants.DISPOSABLE_CLASSNAME );
    assert null != disposable;
    final TypeMirror typeMirror = field.asType();
    if ( !_processingEnv.getTypeUtils().isAssignable( typeMirror, disposable.asType() ) )
    {
      final TypeElement typeElement = (TypeElement) _processingEnv.getTypeUtils().asElement( typeMirror );
      final AnnotationMirror value =
        null != typeElement ?
        AnnotationsUtil.findAnnotationByType( typeElement, Constants.COMPONENT_ANNOTATION_CLASSNAME ) :
        null;
      if ( null == value || !ProcessorUtil.isDisposableTrackableRequired( typeElement ) )
      {
        //The type of the field must implement {@link arez.Disposable} or must be annotated by {@link ArezComponent}
        throw new ProcessorException( "@CascadeDispose target must be assignable to " +
                                      Constants.DISPOSABLE_CLASSNAME + " or a type annotated with @ArezComponent",
                                      field );
      }
    }
  }

  private void addCascadeDisposeMethod( @Nonnull final ExecutableElement method,
                                        @Nullable final ObservableDescriptor observable )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( _element,
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME,
                                         method );
    mustBeCascadeDisposeTypeCompatible( method );
    _cascadeDisposes.put( method, new CascadeDisposableDescriptor( method, observable ) );
  }

  private void mustBeCascadeDisposeTypeCompatible( @Nonnull final ExecutableElement method )
  {
    final TypeElement disposable = _processingEnv.getElementUtils().getTypeElement( Constants.DISPOSABLE_CLASSNAME );
    assert null != disposable;
    final TypeMirror typeMirror = method.getReturnType();
    if ( !_processingEnv.getTypeUtils().isAssignable( typeMirror, disposable.asType() ) )
    {
      final TypeElement typeElement = (TypeElement) _processingEnv.getTypeUtils().asElement( typeMirror );
      final AnnotationMirror value =
        null != typeElement ?
        AnnotationsUtil.findAnnotationByType( typeElement, Constants.COMPONENT_ANNOTATION_CLASSNAME ) :
        null;
      if ( null == value || !ProcessorUtil.isDisposableTrackableRequired( typeElement ) )
      {
        //The type of the field must implement {@link arez.Disposable} or must be annotated by {@link ArezComponent}
        throw new ProcessorException( "@CascadeDispose target must return a type assignable to " +
                                      Constants.DISPOSABLE_CLASSNAME + " or a type annotated with @ArezComponent",
                                      method );
      }
    }
  }

  private void autodetectObservableInitializers()
  {
    for ( final ObservableDescriptor observable : getObservables() )
    {
      if ( null == observable.getInitializer() && observable.hasGetter() )
      {
        if ( observable.hasSetter() )
        {
          final boolean initializer =
            autodetectInitializer( observable.getGetter() ) && autodetectInitializer( observable.getSetter() );
          observable.setInitializer( initializer );
        }
        else
        {
          final boolean initializer = autodetectInitializer( observable.getGetter() );
          observable.setInitializer( initializer );
        }
      }
    }
  }

  private void linkDependencies( @Nonnull final Collection<CandidateMethod> candidates )
  {
    _roObservables
      .stream()
      .filter( ObservableDescriptor::hasGetter )
      .filter( o -> hasDependencyAnnotation( o.getGetter() ) )
      .forEach( o -> addOrUpdateDependency( o.getGetter(), o ) );

    _roMemoizes
      .stream()
      .filter( MemoizeDescriptor::hasMemoize )
      .map( MemoizeDescriptor::getMethod )
      .filter( this::hasDependencyAnnotation )
      .forEach( this::addDependency );

    candidates
      .stream()
      .map( CandidateMethod::getMethod )
      .filter( this::hasDependencyAnnotation )
      .forEach( this::addDependency );
  }

  private boolean hasDependencyAnnotation( @Nonnull final ExecutableElement method )
  {
    return AnnotationsUtil.hasAnnotationOfType( method, Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME );
  }

  private void addOrUpdateDependency( @Nonnull final ExecutableElement method,
                                      @Nonnull final ObservableDescriptor observable )
  {
    final DependencyDescriptor dependencyDescriptor =
      _dependencies.computeIfAbsent( method, m -> createMethodDependencyDescriptor( method ) );
    dependencyDescriptor.setObservable( observable );
  }

  private void addReferenceId( @Nonnull final AnnotationMirror annotation,
                               @Nonnull final ObservableDescriptor observable,
                               @Nonnull final ExecutableElement method )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.REFERENCE_ID_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.REFERENCE_ID_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.REFERENCE_ID_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.REFERENCE_ID_ANNOTATION_CLASSNAME, method );

    findOrCreateReference( getReferenceIdName( annotation, method ) ).setObservable( observable );
  }

  private void addReferenceId( @Nonnull final AnnotationMirror annotation,
                               @Nonnull final ExecutableElement method,
                               @Nonnull final ExecutableType methodType )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.REFERENCE_ID_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.REFERENCE_ID_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.REFERENCE_ID_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.REFERENCE_ID_ANNOTATION_CLASSNAME, method );

    final String name = getReferenceIdName( annotation, method );
    findOrCreateReference( name ).setIdMethod( method, methodType );
  }

  @Nonnull
  private String getReferenceIdName( @Nonnull final AnnotationMirror annotation,
                                     @Nonnull final ExecutableElement method )
  {
    final String declaredName = getAnnotationParameter( annotation, "name" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      final String candidate = ProcessorUtil.deriveName( method, ID_GETTER_PATTERN, declaredName );
      if ( null == candidate )
      {
        final String candidate2 = ProcessorUtil.deriveName( method, RAW_ID_GETTER_PATTERN, declaredName );
        if ( null == candidate2 )
        {
          throw new ProcessorException( "@ReferenceId target has not specified a name and does not follow " +
                                        "the convention \"get[Name]Id\" or \"[name]Id\"", method );
        }
        else
        {
          name = candidate2;
        }
      }
      else
      {
        name = candidate;
      }
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@ReferenceId target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@ReferenceId target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }
    return name;
  }

  private void addInverse( @Nonnull final AnnotationMirror annotation,
                           @Nonnull final ExecutableElement method,
                           @Nonnull final ExecutableType methodType )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.INVERSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.INVERSE_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.INVERSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.INVERSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeAbstract( Constants.INVERSE_ANNOTATION_CLASSNAME, method );

    final String name = getInverseName( annotation, method );
    final ObservableDescriptor observable = findOrCreateObservable( name );
    observable.setGetter( method, methodType );

    addInverse( annotation, observable, method );
  }

  private void addInverse( @Nonnull final AnnotationMirror annotation,
                           @Nonnull final ObservableDescriptor observable,
                           @Nonnull final ExecutableElement method )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.INVERSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.INVERSE_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.INVERSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.INVERSE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeAbstract( Constants.INVERSE_ANNOTATION_CLASSNAME, method );

    final String name = getInverseName( annotation, method );
    final InverseDescriptor existing = _inverses.get( name );
    if ( null != existing )
    {
      throw new ProcessorException( "@Inverse target defines duplicate inverse for name '" + name +
                                    "'. The other inverse is " + existing.getObservable().getGetter(),
                                    method );
    }
    else
    {
      final TypeMirror type = method.getReturnType();

      final Multiplicity multiplicity;
      TypeElement targetType = getInverseManyTypeTarget( method );
      if ( null != targetType )
      {
        multiplicity = Multiplicity.MANY;
      }
      else
      {
        if ( !( type instanceof DeclaredType ) ||
             !AnnotationsUtil.hasAnnotationOfType( ( (DeclaredType) type ).asElement(),
                                                   Constants.COMPONENT_ANNOTATION_CLASSNAME ) )
        {
          throw new ProcessorException( "@Inverse target expected to return a type annotated with " +
                                        Constants.COMPONENT_ANNOTATION_CLASSNAME, method );
        }
        targetType = (TypeElement) ( (DeclaredType) type ).asElement();
        if ( ProcessorUtil.hasNonnullAnnotation( method ) )
        {
          multiplicity = Multiplicity.ONE;
        }
        else if ( AnnotationsUtil.hasAnnotationOfType( method, Constants.NULLABLE_ANNOTATION_CLASSNAME ) )
        {
          multiplicity = Multiplicity.ZERO_OR_ONE;
        }
        else
        {
          throw new ProcessorException( "@Inverse target expected to be annotated with either " +
                                        Constants.NULLABLE_ANNOTATION_CLASSNAME + " or " +
                                        Constants.NONNULL_ANNOTATION_CLASSNAME, method );
        }
      }
      final String referenceName = getInverseReferenceNameParameter( method );
      final InverseDescriptor descriptor =
        new InverseDescriptor( this, observable, referenceName, multiplicity, targetType );
      _inverses.put( name, descriptor );
      verifyMultiplicityOfAssociatedReferenceMethod( descriptor );
    }
  }

  private void verifyMultiplicityOfAssociatedReferenceMethod( @Nonnull final InverseDescriptor descriptor )
  {
    final Multiplicity multiplicity =
      ProcessorUtil
        .getMethods( descriptor.getTargetType(), _processingEnv.getElementUtils(), _processingEnv.getTypeUtils() )
        .stream()
        .map( m -> {
          final AnnotationMirror a =
            AnnotationsUtil.findAnnotationByType( m, Constants.REFERENCE_ANNOTATION_CLASSNAME );
          if ( null != a && getReferenceName( a, m ).equals( descriptor.getReferenceName() ) )
          {
            if ( null == AnnotationsUtil.findAnnotationValueNoDefaults( a, "inverse" ) &&
                 null == AnnotationsUtil.findAnnotationValueNoDefaults( a, "inverseName" ) &&
                 null == AnnotationsUtil.findAnnotationValueNoDefaults( a, "inverseMultiplicity" ) )
            {
              throw new ProcessorException( "@Inverse target found an associated @Reference on the method '" +
                                            m.getSimpleName() + "' on type '" +
                                            descriptor.getTargetType().getQualifiedName() + "' but the " +
                                            "annotation has not configured an inverse.",
                                            descriptor.getObservable().getGetter() );
            }
            ensureTargetTypeAligns( descriptor, m.getReturnType() );
            return getReferenceInverseMultiplicity( a );
          }
          else
          {
            return null;
          }
        } )
        .filter( Objects::nonNull )
        .findAny()
        .orElse( null );
    if ( null == multiplicity )
    {
      throw new ProcessorException( "@Inverse target expected to find an associated @Reference annotation with " +
                                    "a name parameter equal to '" + descriptor.getReferenceName() + "' on class " +
                                    descriptor.getTargetType().getQualifiedName() + " but is unable to " +
                                    "locate a matching method.", descriptor.getObservable().getGetter() );
    }

    if ( descriptor.getMultiplicity() != multiplicity )
    {
      throw new ProcessorException( "@Inverse target has a multiplicity of " + descriptor.getMultiplicity() +
                                    " but that associated @Reference has a multiplicity of " + multiplicity +
                                    ". The multiplicity must align.", descriptor.getObservable().getGetter() );
    }
  }

  private void ensureTargetTypeAligns( @Nonnull final InverseDescriptor descriptor, @Nonnull final TypeMirror target )
  {
    if ( !_processingEnv.getTypeUtils().isSameType( target, getElement().asType() ) )
    {
      throw new ProcessorException( "@Inverse target expected to find an associated @Reference annotation with " +
                                    "a target type equal to " + descriptor.getTargetType() + " but the actual " +
                                    "target type is " + target, descriptor.getObservable().getGetter() );
    }
  }

  @Nullable
  private TypeElement getInverseManyTypeTarget( @Nonnull final ExecutableElement method )
  {
    final TypeName typeName = TypeName.get( method.getReturnType() );
    if ( typeName instanceof ParameterizedTypeName )
    {
      final ParameterizedTypeName type = (ParameterizedTypeName) typeName;
      if ( isSupportedInverseCollectionType( type.rawType.toString() ) && !type.typeArguments.isEmpty() )
      {
        final TypeElement typeElement =
          _processingEnv.getElementUtils().getTypeElement( type.typeArguments.get( 0 ).toString() );
        if ( AnnotationsUtil.hasAnnotationOfType( typeElement, Constants.COMPONENT_ANNOTATION_CLASSNAME ) )
        {
          return typeElement;
        }
        else
        {
          throw new ProcessorException( "@Inverse target expected to return a type annotated with " +
                                        Constants.COMPONENT_ANNOTATION_CLASSNAME, method );
        }
      }
    }
    return null;
  }

  private boolean isSupportedInverseCollectionType( @Nonnull final String typeClassname )
  {
    return Collection.class.getName().equals( typeClassname ) ||
           Set.class.getName().equals( typeClassname ) ||
           List.class.getName().equals( typeClassname );
  }

  @Nonnull
  private String getInverseReferenceNameParameter( @Nonnull final ExecutableElement method )
  {
    final String declaredName =
      (String) AnnotationsUtil.getAnnotationValue( method,
                                                   Constants.INVERSE_ANNOTATION_CLASSNAME,
                                                   "referenceName" ).getValue();
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      name = ProcessorUtil.firstCharacterToLowerCase( getElement().getSimpleName().toString() );
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Inverse target specified an invalid referenceName '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Inverse target specified an invalid referenceName '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }
    return name;
  }

  @Nonnull
  private String getInverseName( @Nonnull final AnnotationMirror annotation,
                                 @Nonnull final ExecutableElement method )
  {
    final String declaredName = getAnnotationParameter( annotation, "name" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      final String candidate = ProcessorUtil.deriveName( method, GETTER_PATTERN, declaredName );
      name = null == candidate ? method.getSimpleName().toString() : candidate;
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Inverse target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Inverse target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }
    return name;
  }

  private void addReference( @Nonnull final AnnotationMirror annotation,
                             @Nonnull final ExecutableElement method,
                             @Nonnull final ExecutableType methodType )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.REFERENCE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.REFERENCE_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.REFERENCE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.REFERENCE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeAbstract( Constants.REFERENCE_ANNOTATION_CLASSNAME, method );

    final String name = getReferenceName( annotation, method );
    final String linkType = getLinkType( method );
    final String inverseName;
    final Multiplicity inverseMultiplicity;
    if ( hasInverse( annotation ) )
    {
      inverseMultiplicity = getReferenceInverseMultiplicity( annotation );
      inverseName = getReferenceInverseName( annotation, method, inverseMultiplicity );
      final TypeMirror returnType = method.getReturnType();
      if ( !( returnType instanceof DeclaredType ) ||
           !AnnotationsUtil.hasAnnotationOfType( ( (DeclaredType) returnType ).asElement(),
                                                 Constants.COMPONENT_ANNOTATION_CLASSNAME ) )
      {
        throw new ProcessorException( "@Reference target expected to return a type annotated with " +
                                      Constants.COMPONENT_ANNOTATION_CLASSNAME + " if there is an " +
                                      "inverse reference.", method );
      }
    }
    else
    {
      inverseName = null;
      inverseMultiplicity = null;
    }
    final ReferenceDescriptor descriptor = findOrCreateReference( name );
    descriptor.setMethod( method, methodType, linkType, inverseName, inverseMultiplicity );
    verifyMultiplicityOfAssociatedInverseMethod( descriptor );
  }

  private void verifyMultiplicityOfAssociatedInverseMethod( @Nonnull final ReferenceDescriptor descriptor )
  {
    final TypeElement element =
      (TypeElement) _processingEnv.getTypeUtils().asElement( descriptor.getMethod().getReturnType() );
    final String defaultInverseName =
      descriptor.hasInverse() ?
      null :
      ProcessorUtil.firstCharacterToLowerCase( getElement().getSimpleName().toString() ) + "s";
    final Multiplicity multiplicity =
      ProcessorUtil
        .getMethods( element, _processingEnv.getElementUtils(), _processingEnv.getTypeUtils() )
        .stream()
        .map( m -> {
          final AnnotationMirror a = AnnotationsUtil.findAnnotationByType( m, Constants.INVERSE_ANNOTATION_CLASSNAME );
          if ( null == a )
          {
            return null;
          }
          final String inverseName = getInverseName( a, m );
          if ( !descriptor.hasInverse() && inverseName.equals( defaultInverseName ) )
          {
            throw new ProcessorException( "@Reference target has not configured an inverse but there is an " +
                                          "associated @Inverse annotated method named '" + m.getSimpleName() +
                                          "' on type '" + element.getQualifiedName() + "'.",
                                          descriptor.getMethod() );
          }
          if ( descriptor.hasInverse() && inverseName.equals( descriptor.getInverseName() ) )
          {
            final TypeElement target = getInverseManyTypeTarget( m );
            if ( null != target )
            {
              ensureTargetTypeAligns( descriptor, target.asType() );
              return Multiplicity.MANY;
            }
            else
            {
              ensureTargetTypeAligns( descriptor, m.getReturnType() );
              return ProcessorUtil.hasNonnullAnnotation( m ) ? Multiplicity.ONE : Multiplicity.ZERO_OR_ONE;
            }
          }
          else
          {
            return null;
          }
        } )
        .filter( Objects::nonNull )
        .findAny()
        .orElse( null );

    if ( descriptor.hasInverse() )
    {
      if ( null == multiplicity )
      {
        throw new ProcessorException( "@Reference target expected to find an associated @Inverse annotation " +
                                      "with a name parameter equal to '" + descriptor.getInverseName() + "' on " +
                                      "class " + descriptor.getMethod().getReturnType() + " but is unable to " +
                                      "locate a matching method.", descriptor.getMethod() );
      }

      final Multiplicity inverseMultiplicity = descriptor.getInverseMultiplicity();
      if ( inverseMultiplicity != multiplicity )
      {
        throw new ProcessorException( "@Reference target has an inverseMultiplicity of " + inverseMultiplicity +
                                      " but that associated @Inverse has a multiplicity of " + multiplicity +
                                      ". The multiplicity must align.", descriptor.getMethod() );
      }
    }
  }

  private void ensureTargetTypeAligns( @Nonnull final ReferenceDescriptor descriptor, @Nonnull final TypeMirror target )
  {
    if ( !_processingEnv.getTypeUtils().isSameType( target, getElement().asType() ) )
    {
      throw new ProcessorException( "@Reference target expected to find an associated @Inverse annotation with " +
                                    "a target type equal to " + getElement().getQualifiedName() + " but " +
                                    "the actual target type is " + target, descriptor.getMethod() );
    }
  }

  private boolean hasInverse( @Nonnull final AnnotationMirror annotation )
  {
    final VariableElement variableElement = AnnotationsUtil.getAnnotationValue( annotation, "inverse" );
    switch ( variableElement.getSimpleName().toString() )
    {
      case "ENABLE":
        return true;
      case "DISABLE":
        return false;
      default:
        return null != AnnotationsUtil.findAnnotationValueNoDefaults( annotation, "inverseName" ) ||
               null != AnnotationsUtil.findAnnotationValueNoDefaults( annotation, "inverseMultiplicity" );
    }
  }

  @Nonnull
  private String getReferenceInverseName( @Nonnull final AnnotationMirror annotation,
                                          @Nonnull final ExecutableElement method,
                                          @Nonnull final Multiplicity multiplicity )
  {
    final String declaredName = AnnotationsUtil.getAnnotationValue( annotation, "inverseName" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      final String baseName = getElement().getSimpleName().toString();
      return ProcessorUtil.firstCharacterToLowerCase( baseName ) + ( Multiplicity.MANY == multiplicity ? "s" : "" );
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Reference target specified an invalid inverseName '" + name + "'. The " +
                                      "inverseName must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Reference target specified an invalid inverseName '" + name + "'. The " +
                                      "inverseName must not be a java keyword.", method );
      }
    }
    return name;
  }

  @Nonnull
  private Multiplicity getReferenceInverseMultiplicity( @Nonnull final AnnotationMirror annotation )
  {
    final VariableElement variableElement = AnnotationsUtil.getAnnotationValue( annotation, "inverseMultiplicity" );
    switch ( variableElement.getSimpleName().toString() )
    {
      case "MANY":
        return Multiplicity.MANY;
      case "ONE":
        return Multiplicity.ONE;
      default:
        return Multiplicity.ZERO_OR_ONE;
    }
  }

  @Nonnull
  private List<? extends VariableElement> getInjectedParameters( @Nonnull final ExecutableElement constructor )
  {
    return constructor
      .getParameters()
      .stream()
      .filter( f -> !AnnotationsUtil.hasAnnotationOfType( f, Constants.PER_INSTANCE_ANNOTATION_CLASSNAME ) )
      .collect( Collectors.toList() );
  }

  @Nonnull
  private String getReferenceName( @Nonnull final AnnotationMirror annotation, @Nonnull final ExecutableElement method )
  {
    final String declaredName = getAnnotationParameter( annotation, "name" );
    final String name;
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      final String candidate = ProcessorUtil.deriveName( method, GETTER_PATTERN, declaredName );
      if ( null == candidate )
      {
        name = method.getSimpleName().toString();
      }
      else
      {
        name = candidate;
      }
    }
    else
    {
      name = declaredName;
      if ( !SourceVersion.isIdentifier( name ) )
      {
        throw new ProcessorException( "@Reference target specified an invalid name '" + name + "'. The " +
                                      "name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( name ) )
      {
        throw new ProcessorException( "@Reference target specified an invalid name '" + name + "'. The " +
                                      "name must not be a java keyword.", method );
      }
    }
    return name;
  }

  @Nonnull
  private String getLinkType( @Nonnull final ExecutableElement method )
  {
    final VariableElement injectParameter = (VariableElement)
      AnnotationsUtil.getAnnotationValue( method,
                                          Constants.REFERENCE_ANNOTATION_CLASSNAME,
                                          "load" ).getValue();
    return injectParameter.getSimpleName().toString();
  }

  private void addDependency( @Nonnull final ExecutableElement method )
  {
    _dependencies.put( method, createMethodDependencyDescriptor( method ) );
  }

  private void addDependency( @Nonnull final VariableElement field )
  {
    _dependencies.put( field, createFieldDependencyDescriptor( field ) );
  }

  @SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
  boolean isDependencyDefined( @Nonnull final Element element )
  {
    return _dependencies.containsKey( element );
  }

  @Nonnull
  private DependencyDescriptor createMethodDependencyDescriptor( @Nonnull final ExecutableElement method )
  {
    MemberChecks.mustNotHaveAnyParameters( Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME, method );

    final boolean validateTypeAtRuntime =
      (Boolean) AnnotationsUtil.getAnnotationValue( method,
                                                    Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                                    "validateTypeAtRuntime" ).getValue();

    final TypeMirror type = method.getReturnType();
    if ( TypeKind.DECLARED != type.getKind() )
    {
      throw new ProcessorException( "@ComponentDependency target must return a non-primitive value", method );
    }
    if ( !validateTypeAtRuntime )
    {
      final TypeElement disposeNotifier =
        _processingEnv.getElementUtils().getTypeElement( Constants.DISPOSE_NOTIFIER_CLASSNAME );
      assert null != disposeNotifier;
      if ( !_processingEnv.getTypeUtils().isAssignable( type, disposeNotifier.asType() ) )
      {
        final TypeElement typeElement = (TypeElement) _processingEnv.getTypeUtils().asElement( type );
        if ( !isActAsComponentAnnotated( typeElement ) && !isDisposeTrackableComponent( typeElement ) )
        {
          throw new ProcessorException( "@ComponentDependency target must return an instance compatible with " +
                                        Constants.DISPOSE_NOTIFIER_CLASSNAME + " or a type annotated " +
                                        "with @ArezComponent(disposeNotifier=ENABLE) or @ActAsComponent", method );
        }
      }
    }

    final boolean cascade = isActionCascade( method );
    return new DependencyDescriptor( this, method, cascade );
  }

  @Nonnull
  private DependencyDescriptor createFieldDependencyDescriptor( @Nonnull final VariableElement field )
  {
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                         field );
    MemberChecks.mustBeFinal( Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME, field );

    final boolean validateTypeAtRuntime =
      (Boolean) AnnotationsUtil.getAnnotationValue( field,
                                                    Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                                    "validateTypeAtRuntime" ).getValue();

    final TypeMirror type = field.asType();
    if ( TypeKind.DECLARED != type.getKind() )
    {
      throw new ProcessorException( "@ComponentDependency target must be a non-primitive value", field );
    }
    if ( !validateTypeAtRuntime )
    {
      final TypeElement disposeNotifier =
        _processingEnv.getElementUtils().getTypeElement( Constants.DISPOSE_NOTIFIER_CLASSNAME );
      assert null != disposeNotifier;
      if ( !_processingEnv.getTypeUtils().isAssignable( type, disposeNotifier.asType() ) )
      {
        final TypeElement typeElement = (TypeElement) _processingEnv.getTypeUtils().asElement( type );
        if ( !isActAsComponentAnnotated( typeElement ) && !isDisposeTrackableComponent( typeElement ) )
        {
          throw new ProcessorException( "@ComponentDependency target must be an instance compatible with " +
                                        Constants.DISPOSE_NOTIFIER_CLASSNAME + " or a type annotated " +
                                        "with @ArezComponent(disposeNotifier=ENABLE) or @ActAsComponent", field );
        }
      }
    }

    if ( !isActionCascade( field ) )
    {
      throw new ProcessorException( "@ComponentDependency target defined an action of 'SET_NULL' but the " +
                                    "dependency is on a final field and can not be set to null.", field );

    }

    return new DependencyDescriptor( this, field );
  }

  @SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
  private boolean isActAsComponentAnnotated( @Nonnull final TypeElement typeElement )
  {
    return AnnotationsUtil.hasAnnotationOfType( typeElement, Constants.ACT_AS_COMPONENT_ANNOTATION_CLASSNAME );
  }

  @SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
  private boolean isDisposeTrackableComponent( @Nonnull final TypeElement typeElement )
  {
    return AnnotationsUtil.hasAnnotationOfType( typeElement, Constants.COMPONENT_ANNOTATION_CLASSNAME ) &&
           ProcessorUtil.isDisposableTrackableRequired( typeElement );
  }

  private boolean isActionCascade( @Nonnull final Element method )
  {
    final VariableElement injectParameter = (VariableElement)
      AnnotationsUtil.getAnnotationValue( method,
                                          Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                          "action" ).getValue();
    switch ( injectParameter.getSimpleName().toString() )
    {
      case "CASCADE":
        return true;
      case "SET_NULL":
      default:
        return false;
    }
  }

  private void ensureNoAbstractMethods( @Nonnull final Collection<CandidateMethod> candidateMethods )
  {
    candidateMethods
      .stream()
      .map( CandidateMethod::getMethod )
      .filter( m -> m.getModifiers().contains( Modifier.ABSTRACT ) )
      .forEach( m -> {
        throw new ProcessorException( "@ArezComponent target has an abstract method not implemented by " +
                                      "framework. The method is named " + m.getSimpleName(), getElement() );
      } );
  }

  private void linkCascadeDisposeObservables()
  {
    for ( final ObservableDescriptor observable : _observables.values() )
    {
      final CascadeDisposableDescriptor cascadeDisposableDescriptor = observable.getCascadeDisposableDescriptor();
      if ( null == cascadeDisposableDescriptor )
      {
        //@CascadeDisposable can only occur on getter so if we don't have it then we look in
        // cascadeDisposableDescriptor list to see if we can match getter
        final CascadeDisposableDescriptor descriptor = _cascadeDisposes.get( observable.getGetter() );
        if ( null != descriptor )
        {
          descriptor.setObservable( observable );
        }
      }
    }
  }

  private void linkCascadeDisposeReferences()
  {
    for ( final ReferenceDescriptor reference : _references.values() )
    {
      final CascadeDisposableDescriptor cascadeDisposableDescriptor = reference.getCascadeDisposableDescriptor();
      if ( null == cascadeDisposableDescriptor && reference.hasMethod() )
      {
        final CascadeDisposableDescriptor descriptor = _cascadeDisposes.get( reference.getMethod() );
        if ( null != descriptor )
        {
          descriptor.setReference( reference );
        }
      }
    }
  }

  private void linkObserverRefs()
  {
    for ( final Map.Entry<String, CandidateMethod> entry : _observerRefs.entrySet() )
    {
      final String key = entry.getKey();
      final CandidateMethod method = entry.getValue();
      final ObserveDescriptor observed = _observes.get( key );
      if ( null != observed )
      {
        observed.setRefMethod( method.getMethod() );
      }
      else
      {
        throw new ProcessorException( "@ObserverRef target defined observer named '" + key + "' but no " +
                                      "@Observe method with that name exists", method.getMethod() );
      }
    }
  }

  private void linkUnAnnotatedObservables( @Nonnull final Map<String, CandidateMethod> getters,
                                           @Nonnull final Map<String, CandidateMethod> setters )
    throws ProcessorException
  {
    for ( final ObservableDescriptor observable : _roObservables )
    {
      if ( !observable.hasSetter() && !observable.hasGetter() )
      {
        throw new ProcessorException( "@ObservableValueRef target unable to be associated with an " +
                                      "Observable property", observable.getRefMethod() );
      }
      else if ( !observable.hasSetter() && observable.expectSetter() )
      {
        final CandidateMethod candidate = setters.remove( observable.getName() );
        if ( null != candidate )
        {
          MemberChecks.mustBeOverridable( getElement(),
                                          Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                          Constants.OBSERVABLE_ANNOTATION_CLASSNAME,
                                          candidate.getMethod() );
          observable.setSetter( candidate.getMethod(), candidate.getMethodType() );
        }
        else if ( observable.hasGetter() )
        {
          throw new ProcessorException( "@Observable target defined getter but no setter was defined and no " +
                                        "setter could be automatically determined", observable.getGetter() );
        }
      }
      else if ( !observable.hasGetter() )
      {
        final CandidateMethod candidate = getters.remove( observable.getName() );
        if ( null != candidate )
        {
          MemberChecks.mustBeOverridable( getElement(),
                                          Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                          Constants.OBSERVABLE_ANNOTATION_CLASSNAME,
                                          candidate.getMethod() );
          observable.setGetter( candidate.getMethod(), candidate.getMethodType() );
        }
        else
        {
          throw new ProcessorException( "@Observable target defined setter but no getter was defined and no " +
                                        "getter could be automatically determined", observable.getSetter() );
        }
      }
    }
  }

  private void linkUnAnnotatedObserves( @Nonnull final Map<String, CandidateMethod> observes,
                                        @Nonnull final Map<String, CandidateMethod> onDepsChanges )
    throws ProcessorException
  {
    for ( final ObserveDescriptor observe : _roObserves )
    {
      if ( !observe.hasObserve() )
      {
        final CandidateMethod candidate = observes.remove( observe.getName() );
        if ( null != candidate )
        {
          observe.setObserveMethod( false,
                                    Priority.NORMAL,
                                    true,
                                    true,
                                    true,
                                    "AREZ",
                                    false,
                                    false,
                                    candidate.getMethod(),
                                    candidate.getMethodType() );
        }
        else
        {
          throw new ProcessorException( "@OnDepsChange target has no corresponding @Observe that could " +
                                        "be automatically determined", observe.getOnDepsChange() );
        }
      }
      else if ( !observe.hasOnDepsChange() )
      {
        final CandidateMethod candidate = onDepsChanges.remove( observe.getName() );
        if ( null != candidate )
        {
          observe.setOnDepsChange( candidate.getMethod() );
        }
      }
    }
  }

  private void linkPriorityOverrideMethods()
    throws ProcessorException
  {
    for ( final Map.Entry<String, CandidateMethod> entry : _priorityOverrides.entrySet() )
    {
      final String name = entry.getKey();
      final CandidateMethod method = entry.getValue();
      final MemoizeDescriptor memoize = _memoizes.get( name );
      if ( null != memoize )
      {
        memoize.setPriorityOverride( method );
      }
      else
      {
        final ObserveDescriptor observe = _observes.get( name );
        if ( null != observe )
        {
          observe.setPriorityOverride( method );
        }
        else
        {
          throw new ProcessorException( "@PriorityOverride target has no corresponding @Memoize or " +
                                        "@Observe methods", method.getMethod() );
        }
      }
    }
  }

  private boolean analyzeMethod( @Nonnull final ExecutableElement method,
                                 @Nonnull final ExecutableType methodType )
    throws ProcessorException
  {
    verifyNoDuplicateAnnotations( method );

    final AnnotationMirror action =
      AnnotationsUtil.findAnnotationByType( method, Constants.ACTION_ANNOTATION_CLASSNAME );
    final AnnotationMirror observed =
      AnnotationsUtil.findAnnotationByType( method, Constants.OBSERVE_ANNOTATION_CLASSNAME );
    final AnnotationMirror observable =
      AnnotationsUtil.findAnnotationByType( method, Constants.OBSERVABLE_ANNOTATION_CLASSNAME );
    final AnnotationMirror observableValueRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.OBSERVABLE_VALUE_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror memoize =
      AnnotationsUtil.findAnnotationByType( method, Constants.MEMOIZE_ANNOTATION_CLASSNAME );
    final AnnotationMirror computableValueRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPUTABLE_VALUE_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror contextRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.CONTEXT_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror stateRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_STATE_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror componentRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror componentId =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_ID_ANNOTATION_CLASSNAME );
    final AnnotationMirror componentIdRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_ID_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror componentTypeName =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_TYPE_NAME_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror componentName =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_NAME_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror postConstruct =
      AnnotationsUtil.findAnnotationByType( method, Constants.POST_CONSTRUCT_ANNOTATION_CLASSNAME );
    final AnnotationMirror priorityOverride =
      AnnotationsUtil.findAnnotationByType( method, Constants.PRIORITY_OVERRIDE_ANNOTATION_CLASSNAME );
    final AnnotationMirror ejbPostConstruct =
      AnnotationsUtil.findAnnotationByType( method, Constants.EJB_POST_CONSTRUCT_ANNOTATION_CLASSNAME );
    final AnnotationMirror preDispose =
      AnnotationsUtil.findAnnotationByType( method, Constants.PRE_DISPOSE_ANNOTATION_CLASSNAME );
    final AnnotationMirror postDispose =
      AnnotationsUtil.findAnnotationByType( method, Constants.POST_DISPOSE_ANNOTATION_CLASSNAME );
    final AnnotationMirror onActivate =
      AnnotationsUtil.findAnnotationByType( method, Constants.ON_ACTIVATE_ANNOTATION_CLASSNAME );
    final AnnotationMirror onDeactivate =
      AnnotationsUtil.findAnnotationByType( method, Constants.ON_DEACTIVATE_ANNOTATION_CLASSNAME );
    final AnnotationMirror onStale =
      AnnotationsUtil.findAnnotationByType( method, Constants.ON_STALE_ANNOTATION_CLASSNAME );
    final AnnotationMirror onDepsChange =
      AnnotationsUtil.findAnnotationByType( method, Constants.ON_DEPS_CHANGE_ANNOTATION_CLASSNAME );
    final AnnotationMirror observerRef =
      AnnotationsUtil.findAnnotationByType( method, Constants.OBSERVER_REF_ANNOTATION_CLASSNAME );
    final AnnotationMirror dependency =
      AnnotationsUtil.findAnnotationByType( method, Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME );
    final AnnotationMirror reference =
      AnnotationsUtil.findAnnotationByType( method, Constants.REFERENCE_ANNOTATION_CLASSNAME );
    final AnnotationMirror referenceId =
      AnnotationsUtil.findAnnotationByType( method, Constants.REFERENCE_ID_ANNOTATION_CLASSNAME );
    final AnnotationMirror inverse =
      AnnotationsUtil.findAnnotationByType( method, Constants.INVERSE_ANNOTATION_CLASSNAME );
    final AnnotationMirror cascadeDispose =
      AnnotationsUtil.findAnnotationByType( method, Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME );

    if ( null != observable )
    {
      final ObservableDescriptor descriptor = addObservable( observable, method, methodType );
      if ( null != referenceId )
      {
        addReferenceId( referenceId, descriptor, method );
      }
      if ( null != inverse )
      {
        addInverse( inverse, descriptor, method );
      }
      if ( null != cascadeDispose )
      {
        addCascadeDisposeMethod( method, descriptor );
      }
      return true;
    }
    else if ( null != observableValueRef )
    {
      addObservableValueRef( observableValueRef, method, methodType );
      return true;
    }
    else if ( null != action )
    {
      addAction( action, method, methodType );
      return true;
    }
    else if ( null != observed )
    {
      addObserve( observed, method, methodType );
      return true;
    }
    else if ( null != onDepsChange )
    {
      addOnDepsChange( onDepsChange, method );
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
    else if ( null != stateRef )
    {
      addComponentStateRef( stateRef, method );
      return true;
    }
    else if ( null != memoize )
    {
      addMemoize( memoize, method, methodType );
      return true;
    }
    else if ( null != computableValueRef )
    {
      addComputableValueRef( computableValueRef, method, methodType );
      return true;
    }
    else if ( null != reference )
    {
      if ( null != cascadeDispose )
      {
        addCascadeDisposeMethod( method, null );
      }
      addReference( reference, method, methodType );
      return true;
    }
    else if ( null != cascadeDispose )
    {
      addCascadeDisposeMethod( method, null );
      // Return false so that it can be picked as the getter of an @Observable or linked to a @Reference
      return false;
    }
    else if ( null != componentIdRef )
    {
      setComponentIdRef( method );
      return true;
    }
    else if ( null != componentRef )
    {
      setComponentRef( method );
      return true;
    }
    else if ( null != componentId )
    {
      setComponentId( method, methodType );
      return true;
    }
    else if ( null != componentName )
    {
      setComponentNameRef( method );
      return true;
    }
    else if ( null != componentTypeName )
    {
      setComponentTypeNameRef( method );
      return true;
    }
    else if ( null != priorityOverride )
    {
      addPriorityOverride( priorityOverride, method, methodType );
      return true;
    }
    else if ( null != ejbPostConstruct )
    {
      throw new ProcessorException( "@" + Constants.EJB_POST_CONSTRUCT_ANNOTATION_CLASSNAME + " annotation " +
                                    "not supported in components annotated with @ArezComponent, use the @" +
                                    Constants.POST_CONSTRUCT_ANNOTATION_CLASSNAME + " annotation instead.",
                                    method );
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
    else if ( null != dependency )
    {
      addDependency( method );
      return false;
    }
    else if ( null != referenceId )
    {
      addReferenceId( referenceId, method, methodType );
      return true;
    }
    else if ( null != inverse )
    {
      addInverse( inverse, method, methodType );
      return true;
    }
    else
    {
      return false;
    }
  }

  private void addPriorityOverride( @Nonnull final AnnotationMirror annotation,
                                    @Nonnull final ExecutableElement method,
                                    @Nonnull final ExecutableType methodType )
  {
    MemberChecks.mustNotBeAbstract( Constants.PRIORITY_OVERRIDE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustBeSubclassCallable( getElement(),
                                         Constants.COMPONENT_ANNOTATION_CLASSNAME,
                                         Constants.PRIORITY_OVERRIDE_ANNOTATION_CLASSNAME,
                                         method );
    MemberChecks.mustNotThrowAnyExceptions( Constants.PRIORITY_OVERRIDE_ANNOTATION_CLASSNAME, method );
    MemberChecks.mustReturnAValue( Constants.PRIORITY_OVERRIDE_ANNOTATION_CLASSNAME, method );

    final List<? extends VariableElement> parameters = method.getParameters();
    if ( !( parameters.isEmpty() || 1 == parameters.size() && parameters.get( 0 ).asType().getKind() == TypeKind.INT ) )
    {
      throw new ProcessorException( "@PriorityOverride target must have no parameters or a " +
                                    "single int parameter", method );
    }

    final TypeMirror type = method.getReturnType();
    if ( TypeKind.INT != type.getKind() )
    {
      throw new ProcessorException( "@PriorityOverride target must return an int value", method );
    }

    final String name = derivePriorityOverrideName( method, annotation );
    _priorityOverrides.put( name, new CandidateMethod( method, methodType ) );
  }

  @Nonnull
  private String derivePriorityOverrideName( @Nonnull final ExecutableElement method,
                                             @Nonnull final AnnotationMirror annotation )
  {
    final String declaredName = getAnnotationParameter( annotation, "name" );
    if ( Constants.SENTINEL.equals( declaredName ) )
    {
      final String name = ProcessorUtil.deriveName( method, PRIORITY_OVERRIDE_PATTERN, declaredName );
      if ( null == name )
      {
        throw new ProcessorException( "Method annotated with @PriorityOverride should specify name or be " +
                                      "named according to the convention [name]Priority", method );
      }
      return name;
    }
    else
    {
      if ( !SourceVersion.isIdentifier( declaredName ) )
      {
        throw new ProcessorException( "@PriorityOverride target specified an invalid name '" + declaredName +
                                      "'. The name must be a valid java identifier.", method );
      }
      else if ( SourceVersion.isKeyword( declaredName ) )
      {
        throw new ProcessorException( "@PriorityOverride target specified an invalid name '" + declaredName +
                                      "'. The name must not be a java keyword.", method );
      }
      return declaredName;
    }
  }

  @Nullable
  private Boolean isInitializerRequired( @Nonnull final ExecutableElement element )
  {
    final AnnotationMirror annotation =
      AnnotationsUtil.findAnnotationByType( element, Constants.OBSERVABLE_ANNOTATION_CLASSNAME );
    final AnnotationValue v =
      null == annotation ? null : AnnotationsUtil.findAnnotationValueNoDefaults( annotation, "initializer" );
    final String value = null == v ? "AUTODETECT" : ( (VariableElement) v.getValue() ).getSimpleName().toString();
    switch ( value )
    {
      case "ENABLE":
        return Boolean.TRUE;
      case "DISABLE":
        return Boolean.FALSE;
      default:
        return null;
    }
  }

  private boolean autodetectInitializer( @Nonnull final ExecutableElement element )
  {
    return element.getModifiers().contains( Modifier.ABSTRACT ) &&
           (
             (
               // Getter
               element.getReturnType().getKind() != TypeKind.VOID &&
               ProcessorUtil.hasNonnullAnnotation( element ) &&
               !AnnotationsUtil.hasAnnotationOfType( element, Constants.INVERSE_ANNOTATION_CLASSNAME )
             ) ||
             (
               // Setter
               1 == element.getParameters().size() &&
               AnnotationsUtil.hasAnnotationOfType( element.getParameters().get( 0 ),
                                                    Constants.NONNULL_ANNOTATION_CLASSNAME )
             )
           );
  }

  private void verifyNoDuplicateAnnotations( @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    final List<String> annotations =
      Arrays.asList( Constants.ACTION_ANNOTATION_CLASSNAME,
                     Constants.OBSERVE_ANNOTATION_CLASSNAME,
                     Constants.ON_DEPS_CHANGE_ANNOTATION_CLASSNAME,
                     Constants.OBSERVER_REF_ANNOTATION_CLASSNAME,
                     Constants.OBSERVABLE_ANNOTATION_CLASSNAME,
                     Constants.OBSERVABLE_VALUE_REF_ANNOTATION_CLASSNAME,
                     Constants.MEMOIZE_ANNOTATION_CLASSNAME,
                     Constants.COMPUTABLE_VALUE_REF_ANNOTATION_CLASSNAME,
                     Constants.COMPONENT_REF_ANNOTATION_CLASSNAME,
                     Constants.COMPONENT_ID_ANNOTATION_CLASSNAME,
                     Constants.COMPONENT_NAME_REF_ANNOTATION_CLASSNAME,
                     Constants.COMPONENT_TYPE_NAME_REF_ANNOTATION_CLASSNAME,
                     Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME,
                     Constants.CONTEXT_REF_ANNOTATION_CLASSNAME,
                     Constants.POST_CONSTRUCT_ANNOTATION_CLASSNAME,
                     Constants.PRE_DISPOSE_ANNOTATION_CLASSNAME,
                     Constants.POST_DISPOSE_ANNOTATION_CLASSNAME,
                     Constants.REFERENCE_ANNOTATION_CLASSNAME,
                     Constants.REFERENCE_ID_ANNOTATION_CLASSNAME,
                     Constants.ON_ACTIVATE_ANNOTATION_CLASSNAME,
                     Constants.ON_DEACTIVATE_ANNOTATION_CLASSNAME,
                     Constants.ON_STALE_ANNOTATION_CLASSNAME,
                     Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME );
    final Map<String, Collection<String>> exceptions = new HashMap<>();
    exceptions.put( Constants.OBSERVABLE_ANNOTATION_CLASSNAME,
                    Arrays.asList( Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                   Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME,
                                   Constants.REFERENCE_ID_ANNOTATION_CLASSNAME ) );
    exceptions.put( Constants.REFERENCE_ANNOTATION_CLASSNAME,
                    Collections.singletonList( Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME ) );

    MemberChecks.verifyNoOverlappingAnnotations( method, annotations, exceptions );
  }

  private void verifyNoDuplicateAnnotations( @Nonnull final VariableElement field )
    throws ProcessorException
  {
    MemberChecks.verifyNoOverlappingAnnotations( field,
                                                 Arrays.asList( Constants.COMPONENT_DEPENDENCY_ANNOTATION_CLASSNAME,
                                                                Constants.CASCADE_DISPOSE_ANNOTATION_CLASSNAME ),
                                                 Collections.emptyMap() );
  }

  @Nonnull
  private String getPropertyAccessorName( @Nonnull final ExecutableElement method, @Nonnull final String specifiedName )
    throws ProcessorException
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
   * Build the enhanced class for the component.
   */
  @Nonnull
  TypeSpec buildType( @Nonnull final ProcessingEnvironment processingEnv )
    throws ProcessorException
  {
    final TypeSpec.Builder builder = TypeSpec.classBuilder( getEnhancedClassName().simpleName() ).
      addTypeVariables( GeneratorUtil.getTypeArgumentsAsNames( asDeclaredType() ) ).
      addModifiers( Modifier.FINAL );
    GeneratorUtil.addOriginatingTypes( getElement(), builder );
    Generator.copyWhitelistedAnnotations( getElement(), builder );

    if ( isClassType() )
    {
      builder.superclass( TypeName.get( getElement().asType() ) );
    }
    else
    {
      builder.addSuperinterface( TypeName.get( getElement().asType() ) );
    }

    Generator.addGeneratedAnnotation( processingEnv, builder );
    if ( !_roMemoizes.isEmpty() &&
         !AnnotationsUtil.hasAnnotationOfType( getElement(), SuppressWarnings.class.getName() ) )
    {
      builder.addAnnotation( AnnotationSpec.builder( SuppressWarnings.class ).
        addMember( "value", "$S", "unchecked" ).
        build() );
    }
    final boolean publicType =
      (
        getElement().getModifiers().contains( Modifier.PUBLIC ) &&
        ProcessorUtil.getConstructors( getElement() ).
          stream().
          anyMatch( c -> c.getModifiers().contains( Modifier.PUBLIC ) )
      ) || (
        //Ahh dagger.... due the way we actually inject components that have to create a dagger component
        // extension, this class needs to be public
        needsDaggerComponentExtension()
      );
    final boolean hasInverseReferencedOutsideClass =
      _roInverses.stream().anyMatch( inverse -> {
        final PackageElement targetPackageElement = GeneratorUtil.getPackageElement( inverse.getTargetType() );
        final PackageElement selfPackageElement = GeneratorUtil.getPackageElement( getElement() );
        return !Objects.equals( targetPackageElement.getQualifiedName(), selfPackageElement.getQualifiedName() );
      } );
    final boolean hasReferenceWithInverseOutsidePackage =
      _roReferences
        .stream()
        .filter( ReferenceDescriptor::hasInverse )
        .anyMatch( reference -> {
          final TypeElement typeElement =
            (TypeElement) _processingEnv.getTypeUtils().asElement( reference.getMethod().getReturnType() );

          final PackageElement targetPackageElement = GeneratorUtil.getPackageElement( typeElement );
          final PackageElement selfPackageElement = GeneratorUtil.getPackageElement( getElement() );
          return !Objects.equals( targetPackageElement.getQualifiedName(), selfPackageElement.getQualifiedName() );
        } );
    if ( publicType || hasInverseReferencedOutsideClass || hasReferenceWithInverseOutsidePackage )
    {
      builder.addModifiers( Modifier.PUBLIC );
    }
    if ( null != _scopeAnnotation )
    {
      final DeclaredType annotationType = _scopeAnnotation.getAnnotationType();
      final TypeElement typeElement = (TypeElement) annotationType.asElement();
      builder.addAnnotation( ClassName.get( typeElement ) );
    }

    builder.addSuperinterface( Generator.DISPOSABLE_CLASSNAME );
    builder.addSuperinterface( ParameterizedTypeName.get( Generator.IDENTIFIABLE_CLASSNAME, getIdType().box() ) );
    if ( _observable )
    {
      builder.addSuperinterface( Generator.COMPONENT_OBSERVABLE_CLASSNAME );
    }
    if ( _verify )
    {
      builder.addSuperinterface( Generator.VERIFIABLE_CLASSNAME );
    }
    if ( _disposeNotifier )
    {
      builder.addSuperinterface( Generator.DISPOSE_TRACKABLE_CLASSNAME );
    }
    if ( needsExplicitLink() )
    {
      builder.addSuperinterface( Generator.LINKABLE_CLASSNAME );
    }

    if ( _injectFactory )
    {
      builder.addType( buildFactoryClass().build() );
    }

    buildFields( builder );

    if ( isClassType() )
    {
      buildConstructors( builder, processingEnv.getTypeUtils() );
    }
    else
    {
      builder.addMethod( buildConstructor( null, null, hasDeprecatedElements() ) );
    }

    if ( null != _contextRef )
    {
      builder.addMethod( buildContextRefMethod() );
    }
    if ( null != _componentRef )
    {
      builder.addMethod( buildComponentRefMethod() );
    }
    if ( null != _componentIdRef )
    {
      builder.addMethod( buildComponentIdRefMethod() );
    }
    if ( !_references.isEmpty() || hasInverses() )
    {
      builder.addMethod( buildLocatorRefMethod() );
    }
    if ( null == _componentId )
    {
      builder.addMethod( buildComponentIdMethod() );
    }
    builder.addMethod( buildArezIdMethod() );
    if ( null != _componentNameRef )
    {
      builder.addMethod( buildComponentNameRefMethod() );
    }
    if ( null != _componentTypeNameRef )
    {
      builder.addMethod( buildComponentTypeNameRefMethod() );
    }

    if ( _observable )
    {
      builder.addMethod( buildObserve() );
    }
    if ( hasInternalPreDispose() )
    {
      builder.addMethod( buildInternalPreDispose() );
    }
    if ( _disposeNotifier )
    {
      builder.addMethod( buildNativeComponentPreDispose() );
      builder.addMethod( buildAddOnDisposeListener() );
      builder.addMethod( buildRemoveOnDisposeListener() );
    }
    builder.addMethod( buildIsDisposed() );
    builder.addMethod( buildDispose() );
    if ( needsInternalDispose() )
    {
      builder.addMethod( buildInternalDispose() );
    }
    if ( _verify )
    {
      builder.addMethod( buildVerify() );
    }

    if ( needsExplicitLink() )
    {
      builder.addMethod( buildLink() );
    }

    _componentStateRefs.forEach( e -> e.buildMethods( processingEnv, _element, builder ) );
    _roObservables.forEach( e -> e.buildMethods( builder ) );
    _roObserves.forEach( e -> e.buildMethods( builder ) );
    _roActions.forEach( e -> e.buildMethods( builder ) );
    _roMemoizes.forEach( e -> e.buildMethods( builder ) );
    _roReferences.forEach( e -> e.buildMethods( builder ) );
    _roInverses.forEach( e -> e.buildMethods( builder ) );

    builder.addMethod( buildHashcodeMethod() );
    builder.addMethod( buildEqualsMethod() );

    if ( _generateToString )
    {
      builder.addMethod( buildToStringMethod() );
    }

    return builder.build();
  }

  boolean isClassType()
  {
    return ElementKind.CLASS == getElement().getKind();
  }

  boolean isInterfaceType()
  {
    return !isClassType();
  }

  boolean hasInverses()
  {
    return !_inverses.isEmpty();
  }

  @Nonnull
  private TypeSpec.Builder buildFactoryClass()
  {
    final TypeSpec.Builder factory = TypeSpec.classBuilder( "Factory" )
      .addModifiers( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL );

    final ExecutableElement constructor = ProcessorUtil.getConstructors( _element ).get( 0 );
    assert null != constructor;

    final List<? extends VariableElement> injectedParameters = getInjectedParameters( constructor );
    for ( final VariableElement perInstanceParameter : injectedParameters )
    {
      final FieldSpec.Builder field = FieldSpec
        .builder( TypeName.get( perInstanceParameter.asType() ),
                  perInstanceParameter.getSimpleName().toString(),
                  Modifier.PRIVATE,
                  Modifier.FINAL );
      Generator.copyWhitelistedAnnotations( perInstanceParameter, field );
      factory.addField( field.build() );
    }

    final MethodSpec.Builder ctor = MethodSpec.constructorBuilder();
    ctor.addAnnotation( Generator.INJECT_CLASSNAME );
    Generator.copyWhitelistedAnnotations( constructor, ctor );

    for ( final VariableElement perInstanceParameter : injectedParameters )
    {
      final String name = perInstanceParameter.getSimpleName().toString();
      final ParameterSpec.Builder param =
        ParameterSpec.builder( TypeName.get( perInstanceParameter.asType() ), name, Modifier.FINAL );
      Generator.copyWhitelistedAnnotations( perInstanceParameter, param );
      ctor.addParameter( param.build() );
      if ( ProcessorUtil.hasNonnullAnnotation( perInstanceParameter ) )
      {
        ctor.addStatement( "this.$N = $T.requireNonNull( $N )", name, Objects.class, name );
      }
      else
      {
        ctor.addStatement( "this.$N = $N", name, name );
      }
    }

    factory.addMethod( ctor.build() );

    {
      final MethodSpec.Builder creator = MethodSpec.methodBuilder( "create" );
      creator.addAnnotation( Generator.NONNULL_CLASSNAME );
      creator.addModifiers( Modifier.PUBLIC, Modifier.FINAL );
      creator.returns( getEnhancedClassName() );

      final StringBuilder sb = new StringBuilder();
      final ArrayList<Object> params = new ArrayList<>();
      sb.append( "return new $T(" );
      params.add( getEnhancedClassName() );

      boolean firstParam = true;

      for ( final VariableElement parameter : constructor.getParameters() )
      {
        final boolean perInstance =
          AnnotationsUtil.hasAnnotationOfType( parameter, Constants.PER_INSTANCE_ANNOTATION_CLASSNAME );

        final String name = parameter.getSimpleName().toString();

        if ( perInstance )
        {
          final ParameterSpec.Builder param =
            ParameterSpec.builder( TypeName.get( parameter.asType() ), name, Modifier.FINAL );
          Generator.copyWhitelistedAnnotations( parameter, param );
          creator.addParameter( param.build() );
        }

        if ( firstParam )
        {
          sb.append( " " );
        }
        else
        {
          sb.append( ", " );
        }
        firstParam = false;
        if ( perInstance && ProcessorUtil.hasNonnullAnnotation( parameter ) )
        {
          sb.append( "$T.requireNonNull( $N )" );
          params.add( Objects.class );
        }
        else
        {
          sb.append( "$N" );
        }
        params.add( name );
      }

      if ( !firstParam )
      {
        sb.append( " " );
      }

      sb.append( ")" );
      creator.addStatement( sb.toString(), params.toArray() );
      factory.addMethod( creator.build() );
    }

    return factory;
  }

  private boolean needsExplicitLink()
  {
    return _roReferences.stream().anyMatch( r -> r.getLinkType().equals( "EXPLICIT" ) );
  }

  @Nonnull
  private MethodSpec buildToStringMethod()
    throws ProcessorException
  {
    assert _generateToString;

    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "toString" ).
        addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
        addAnnotation( Override.class ).
        returns( TypeName.get( String.class ) );

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.beginControlFlow( "if ( $T.areNamesEnabled() )", Generator.AREZ_CLASSNAME );
    codeBlock.addStatement( "return $S + this.$N.getName() + $S",
                            "ArezComponent[",
                            Generator.KERNEL_FIELD_NAME,
                            "]" );
    codeBlock.nextControlFlow( "else" );
    if ( isInterfaceType() )
    {
      codeBlock.addStatement( "return $T.super.toString()", getClassName() );
    }
    else
    {
      codeBlock.addStatement( "return super.toString()" );
    }
    codeBlock.endControlFlow();
    method.addCode( codeBlock.build() );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildEqualsMethod()
    throws ProcessorException
  {
    final String idMethod = getIdMethodName();

    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "equals" ).
        addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
        addAnnotation( Override.class ).
        addParameter( Object.class, "o", Modifier.FINAL ).
        returns( TypeName.BOOLEAN );

    final ClassName generatedClass = getEnhancedClassName();
    final List<? extends TypeParameterElement> typeParameters = _element.getTypeParameters();

    if ( !typeParameters.isEmpty() )
    {
      method.addAnnotation( AnnotationSpec.builder( SuppressWarnings.class ).
        addMember( "value", "$S", "unchecked" ).
        build() );
    }

    final TypeName typeName =
      typeParameters.isEmpty() ?
      generatedClass :
      ParameterizedTypeName.get( generatedClass,
                                 typeParameters.stream().map( TypeVariableName::get ).toArray( TypeName[]::new ) );

    final CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.beginControlFlow( "if ( o instanceof $T )", generatedClass );
    codeBlock.addStatement( "final $T that = ($T) o", typeName, typeName );
    /*
     * If componentId is null then it is using synthetic id which is monotonically increasing and
     * thus if the id matches then the instance match. As a result no need to check isDisposed as
     * they will always match. Whereas if componentId is not null then the application controls the
     * id and there maybe be multiple entities with the same id where one has been disposed. They
     * should not match.
     */
    final String prefix = null != _componentId ? "isDisposed() == that.isDisposed() && " : "";
    final TypeKind kind = null != _componentId ? _componentId.getReturnType().getKind() : Generator.DEFAULT_ID_KIND;
    if ( kind == TypeKind.DECLARED || kind == TypeKind.TYPEVAR )
    {
      codeBlock.addStatement( "return " + prefix + "null != $N() && $N().equals( that.$N() )",
                              idMethod,
                              idMethod,
                              idMethod );
    }
    else
    {
      codeBlock.addStatement( "return " + prefix + "$N() == that.$N()",
                              idMethod,
                              idMethod );
    }
    codeBlock.nextControlFlow( "else" );
    codeBlock.addStatement( "return false" );
    codeBlock.endControlFlow();

    if ( _requireEquals )
    {
      method.addCode( codeBlock.build() );
    }
    else
    {
      final CodeBlock.Builder guardBlock = CodeBlock.builder();
      guardBlock.beginControlFlow( "if ( $T.areNativeComponentsEnabled() )", Generator.AREZ_CLASSNAME );
      guardBlock.add( codeBlock.build() );
      guardBlock.nextControlFlow( "else" );
      if ( isClassType() )
      {
        guardBlock.addStatement( "return super.equals( o )" );
      }
      else
      {
        guardBlock.addStatement( "return $T.super.equals( o )", getClassName() );
      }
      guardBlock.endControlFlow();
      method.addCode( guardBlock.build() );
    }
    return method.build();
  }

  @Nonnull
  private MethodSpec buildHashcodeMethod()
    throws ProcessorException
  {
    final String idMethod = getIdMethodName();

    final MethodSpec.Builder method =
      MethodSpec.methodBuilder( "hashCode" ).
        addModifiers( Modifier.PUBLIC, Modifier.FINAL ).
        addAnnotation( Override.class ).
        returns( TypeName.INT );
    final TypeKind kind = null != _componentId ? _componentId.getReturnType().getKind() : Generator.DEFAULT_ID_KIND;
    if ( _requireEquals )
    {
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
    }
    else
    {
      final CodeBlock.Builder guardBlock = CodeBlock.builder();
      guardBlock.beginControlFlow( "if ( $T.areNativeComponentsEnabled() )", Generator.AREZ_CLASSNAME );
      if ( kind == TypeKind.DECLARED || kind == TypeKind.TYPEVAR )
      {
        guardBlock.addStatement( "return null != $N() ? $N().hashCode() : $T.identityHashCode( this )",
                                 idMethod,
                                 idMethod,
                                 System.class );
      }
      else if ( kind == TypeKind.BYTE )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Byte.class, idMethod );
      }
      else if ( kind == TypeKind.CHAR )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Character.class, idMethod );
      }
      else if ( kind == TypeKind.SHORT )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Short.class, idMethod );
      }
      else if ( kind == TypeKind.INT )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Integer.class, idMethod );
      }
      else if ( kind == TypeKind.LONG )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Long.class, idMethod );
      }
      else if ( kind == TypeKind.FLOAT )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Float.class, idMethod );
      }
      else if ( kind == TypeKind.DOUBLE )
      {
        guardBlock.addStatement( "return $T.hashCode( $N() )", Double.class, idMethod );
      }
      else
      {
        // So very unlikely but will cover it for completeness
        assert kind == TypeKind.BOOLEAN;
        guardBlock.addStatement( "return $T.hashCode( $N() )", Boolean.class, idMethod );
      }
      guardBlock.nextControlFlow( "else" );
      if ( isClassType() )
      {
        guardBlock.addStatement( "return super.hashCode()" );
      }
      else
      {
        guardBlock.addStatement( "return $T.super.hashCode()", getClassName() );
      }
      guardBlock.endControlFlow();
      method.addCode( guardBlock.build() );
    }

    return method.build();
  }

  @Nonnull
  private MethodSpec buildContextRefMethod()
    throws ProcessorException
  {
    assert null != _contextRef;
    final MethodSpec.Builder method = GeneratorUtil.refMethod( _processingEnv, _element, _contextRef );
    Generator.generateNotInitializedInvariant( this, method, _contextRef.getSimpleName().toString() );
    method.addStatement( "return this.$N.getContext()", Generator.KERNEL_FIELD_NAME );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildLocatorRefMethod()
    throws ProcessorException
  {
    final String methodName = Generator.LOCATOR_METHOD_NAME;
    final MethodSpec.Builder method = MethodSpec.methodBuilder( methodName ).
      addAnnotation( Generator.NONNULL_CLASSNAME ).
      addModifiers( Modifier.FINAL ).
      returns( Generator.LOCATOR_CLASSNAME );

    Generator.generateNotInitializedInvariant( this, method, methodName );

    method.addStatement( "return this.$N.getContext().locator()", Generator.KERNEL_FIELD_NAME );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildComponentRefMethod()
    throws ProcessorException
  {
    assert null != _componentRef;

    final MethodSpec.Builder method = GeneratorUtil.refMethod( _processingEnv, _element, _componentRef );

    final String methodName = _componentRef.getSimpleName().toString();
    Generator.generateNotInitializedInvariant( this, method, methodName );
    Generator.generateNotConstructedInvariant( method, methodName );
    Generator.generateNotCompleteInvariant( method, methodName );
    Generator.generateNotDisposedInvariant( method, methodName );

    final CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow( "if ( $T.shouldCheckInvariants() )", Generator.AREZ_CLASSNAME );
    block.addStatement( "$T.invariant( () -> $T.areNativeComponentsEnabled(), () -> \"Invoked @ComponentRef " +
                        "method '$N' but Arez.areNativeComponentsEnabled() returned false.\" )",
                        Generator.GUARDS_CLASSNAME,
                        Generator.AREZ_CLASSNAME,
                        methodName );
    block.endControlFlow();

    method.addCode( block.build() );

    method.addStatement( "return this.$N.getComponent()", Generator.KERNEL_FIELD_NAME );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildComponentIdRefMethod()
    throws ProcessorException
  {
    assert null != _componentIdRef;
    return GeneratorUtil.refMethod( _processingEnv, _element, _componentIdRef )
      .addStatement( "return this.$N()", getIdMethodName() )
      .build();
  }

  @Nonnull
  private MethodSpec buildArezIdMethod()
    throws ProcessorException
  {
    return MethodSpec.methodBuilder( "getArezId" ).
      addAnnotation( Override.class ).
      addAnnotation( Generator.NONNULL_CLASSNAME ).
      addModifiers( Modifier.PUBLIC ).
      addModifiers( Modifier.FINAL ).
      returns( getIdType().box() ).
      addStatement( "return $N()", getIdMethodName() ).build();
  }

  @Nonnull
  private MethodSpec buildComponentIdMethod()
    throws ProcessorException
  {
    assert null == _componentId;

    final MethodSpec.Builder method = MethodSpec.methodBuilder( Generator.ID_FIELD_NAME ).
      addModifiers( Modifier.FINAL ).
      returns( Generator.DEFAULT_ID_TYPE );
    return method.addStatement( "return this.$N.getId()", Generator.KERNEL_FIELD_NAME ).build();
  }

  /**
   * Generate the getter for component name.
   */
  @Nonnull
  private MethodSpec buildComponentNameRefMethod()
    throws ProcessorException
  {
    assert null != _componentNameRef;
    final MethodSpec.Builder method = GeneratorUtil.refMethod( _processingEnv, _element, _componentNameRef );
    Generator.generateNotInitializedInvariant( this, method, _componentNameRef.getSimpleName().toString() );
    method.addStatement( "return this.$N.getName()", Generator.KERNEL_FIELD_NAME );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildComponentTypeNameRefMethod()
    throws ProcessorException
  {
    assert null != _componentTypeNameRef;
    return GeneratorUtil.refMethod( _processingEnv, _element, _componentTypeNameRef )
      .addStatement( "return $S", _type )
      .build();
  }

  @Nonnull
  private MethodSpec buildVerify()
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "verify" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class );

    Generator.generateNotDisposedInvariant( builder, "verify" );

    if ( !_roReferences.isEmpty() || !_roInverses.isEmpty() )
    {
      final CodeBlock.Builder block = CodeBlock.builder();
      block.beginControlFlow( "if ( $T.shouldCheckApiInvariants() && $T.isVerifyEnabled() )",
                              Generator.AREZ_CLASSNAME,
                              Generator.AREZ_CLASSNAME );

      block.addStatement( "$T.apiInvariant( () -> this == $N().findById( $T.class, $N() ), () -> \"Attempted to " +
                          "lookup self in Locator with type $T and id '\" + $N() + \"' but unable to locate " +
                          "self. Actual value: \" + $N().findById( $T.class, $N() ) )",
                          Generator.GUARDS_CLASSNAME,
                          Generator.LOCATOR_METHOD_NAME,
                          getElement(),
                          getIdMethodName(),
                          getElement(),
                          getIdMethodName(),
                          Generator.LOCATOR_METHOD_NAME,
                          getElement(),
                          getIdMethodName() );
      for ( final ReferenceDescriptor reference : _roReferences )
      {
        reference.buildVerify( block );
      }

      for ( final InverseDescriptor inverse : _roInverses )
      {
        inverse.buildVerify( block );
      }

      block.endControlFlow();
      builder.addCode( block.build() );
    }
    return builder.build();
  }

  @Nonnull
  private MethodSpec buildLink()
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "link" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class );

    Generator.generateNotDisposedInvariant( builder, "link" );

    final List<ReferenceDescriptor> explicitReferences =
      _roReferences.stream().filter( r -> r.getLinkType().equals( "EXPLICIT" ) ).collect( Collectors.toList() );
    for ( final ReferenceDescriptor reference : explicitReferences )
    {
      builder.addStatement( "this.$N()", reference.getLinkMethodName() );
    }
    return builder.build();
  }

  /**
   * Generate the dispose method.
   */
  @Nonnull
  private MethodSpec buildDispose()
    throws ProcessorException
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "dispose" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class );

    builder.addStatement( "this.$N.dispose()", Generator.KERNEL_FIELD_NAME );

    return builder.build();
  }

  @Nonnull
  private MethodSpec buildInternalDispose()
    throws ProcessorException
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( Generator.INTERNAL_DISPOSE_METHOD_NAME ).
        addModifiers( Modifier.PRIVATE );

    _roObserves.forEach( observe -> observe.buildDisposer( builder ) );
    _roMemoizes.forEach( memoize -> memoize.buildDisposer( builder ) );
    _roObservables.forEach( observable -> observable.buildDisposer( builder ) );

    return builder.build();
  }

  private boolean hasInternalPreDispose()
  {
    return !_roInverses.isEmpty() ||
           !_roCascadeDisposes.isEmpty() ||
           ( _disposeNotifier && !_roDependencies.isEmpty() ) ||
           _roReferences.stream().anyMatch( ReferenceDescriptor::hasInverse );
  }

  /**
   * Generate the isDisposed method.
   */
  @Nonnull
  private MethodSpec buildIsDisposed()
    throws ProcessorException
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "isDisposed" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class ).
        returns( TypeName.BOOLEAN );

    builder.addStatement( "return this.$N.isDisposed()", Generator.KERNEL_FIELD_NAME );
    return builder.build();
  }

  /**
   * Generate the observe method.
   */
  @Nonnull
  private MethodSpec buildObserve()
    throws ProcessorException
  {
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "observe" ).
        addModifiers( Modifier.PUBLIC ).
        addAnnotation( Override.class ).
        returns( TypeName.BOOLEAN );
    builder.addStatement( "return this.$N.observe()", Generator.KERNEL_FIELD_NAME );
    return builder.build();
  }

  /**
   * Generate the preDispose method only used when native components are enabled.
   */
  @Nonnull
  private MethodSpec buildNativeComponentPreDispose()
    throws ProcessorException
  {
    assert _disposeNotifier;
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( Generator.INTERNAL_NATIVE_COMPONENT_PRE_DISPOSE_METHOD_NAME ).
        addModifiers( Modifier.PRIVATE );

    if ( hasInternalPreDispose() )
    {
      builder.addStatement( "this.$N()", Generator.INTERNAL_PRE_DISPOSE_METHOD_NAME );
    }
    else if ( null != _preDispose )
    {
      if ( isClassType() )
      {
        builder.addStatement( "super.$N()", _preDispose.getSimpleName() );
      }
      else
      {
        builder.addStatement( "$T.super.$N()", getClassName(), _preDispose.getSimpleName() );
      }
    }
    builder.addStatement( "this.$N.notifyOnDisposeListeners()", Generator.KERNEL_FIELD_NAME );

    return builder.build();
  }

  /**
   * Generate the preDispose method.
   */
  @Nonnull
  private MethodSpec buildInternalPreDispose()
    throws ProcessorException
  {
    assert hasInternalPreDispose();
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( Generator.INTERNAL_PRE_DISPOSE_METHOD_NAME ).
        addModifiers( Modifier.PRIVATE );

    if ( null != _preDispose )
    {
      if ( isClassType() )
      {
        builder.addStatement( "super.$N()", _preDispose.getSimpleName() );
      }
      else
      {
        builder.addStatement( "$T.super.$N()", getClassName(), _preDispose.getSimpleName() );
      }
    }
    _roCascadeDisposes.forEach( r -> r.buildDisposer( builder ) );
    _roReferences.forEach( r -> r.buildDisposer( builder ) );
    _roInverses.forEach( r -> Generator.buildInverseDisposer( r, builder ) );
    if ( _disposeNotifier )
    {
      for ( final DependencyDescriptor dependency : _roDependencies )
      {
        final Element element = dependency.getElement();

        if ( dependency.isMethodDependency() )
        {
          final ExecutableElement method = dependency.getMethod();
          final String methodName = method.getSimpleName().toString();
          if ( ProcessorUtil.hasNonnullAnnotation( element ) )
          {
            builder.addStatement( "$T.asDisposeNotifier( $N() ).removeOnDisposeListener( this )",
                                  Generator.DISPOSE_TRACKABLE_CLASSNAME,
                                  methodName );
          }
          else
          {
            final String varName = Generator.VARIABLE_PREFIX + methodName + "_dependency";
            final boolean abstractObservables = method.getModifiers().contains( Modifier.ABSTRACT );
            if ( abstractObservables )
            {
              builder.addStatement( "final $T $N = this.$N",
                                    method.getReturnType(),
                                    varName,
                                    dependency.getObservable().getDataFieldName() );
            }
            else
            {
              if ( isClassType() )
              {
                builder.addStatement( "final $T $N = super.$N()", method.getReturnType(), varName, methodName );
              }
              else
              {
                builder.addStatement( "final $T $N = $T.super.$N()",
                                      method.getReturnType(),
                                      varName,
                                      getClassName(),
                                      methodName );
              }
            }
            final CodeBlock.Builder listenerBlock = CodeBlock.builder();
            listenerBlock.beginControlFlow( "if ( null != $N )", varName );
            listenerBlock.addStatement( "$T.asDisposeNotifier( $N ).removeOnDisposeListener( this )",
                                        Generator.DISPOSE_TRACKABLE_CLASSNAME,
                                        varName );
            listenerBlock.endControlFlow();
            builder.addCode( listenerBlock.build() );
          }
        }
        else
        {
          final VariableElement field = dependency.getField();
          final String fieldName = field.getSimpleName().toString();
          if ( ProcessorUtil.hasNonnullAnnotation( element ) )
          {
            builder.addStatement( "$T.asDisposeNotifier( this.$N ).removeOnDisposeListener( this )",
                                  Generator.DISPOSE_TRACKABLE_CLASSNAME,
                                  fieldName );
          }
          else
          {
            final CodeBlock.Builder listenerBlock = CodeBlock.builder();
            listenerBlock.beginControlFlow( "if ( null != this.$N )", fieldName );
            listenerBlock.addStatement( "$T.asDisposeNotifier( this.$N ).removeOnDisposeListener( this )",
                                        Generator.DISPOSE_TRACKABLE_CLASSNAME,
                                        fieldName );
            listenerBlock.endControlFlow();
            builder.addCode( listenerBlock.build() );
          }
        }
      }
    }

    return builder.build();
  }

  /**
   * Generate the addOnDisposeListener method.
   */
  @Nonnull
  private MethodSpec buildAddOnDisposeListener()
    throws ProcessorException
  {
    return MethodSpec.methodBuilder( "addOnDisposeListener" ).
      addModifiers( Modifier.PUBLIC ).
      addAnnotation( Override.class ).
      addParameter( ParameterSpec.builder( TypeName.OBJECT, "key", Modifier.FINAL )
                      .addAnnotation( Generator.NONNULL_CLASSNAME )
                      .build() ).
      addParameter( ParameterSpec.builder( Generator.SAFE_PROCEDURE_CLASSNAME, "action", Modifier.FINAL )
                      .addAnnotation( Generator.NONNULL_CLASSNAME )
                      .build() ).
      addStatement( "this.$N.addOnDisposeListener( key, action )", Generator.KERNEL_FIELD_NAME ).build();
  }

  /**
   * Generate the removeOnDisposeListener method.
   */
  @Nonnull
  private MethodSpec buildRemoveOnDisposeListener()
    throws ProcessorException
  {
    return MethodSpec.methodBuilder( "removeOnDisposeListener" ).
      addModifiers( Modifier.PUBLIC ).
      addAnnotation( Override.class ).
      addParameter( ParameterSpec.builder( TypeName.OBJECT, "key", Modifier.FINAL )
                      .addAnnotation( Generator.NONNULL_CLASSNAME )
                      .build() ).
      addStatement( "this.$N.removeOnDisposeListener( key )", Generator.KERNEL_FIELD_NAME ).build();
  }

  /**
   * Build the fields required to make class Observable. This involves;
   * <ul>
   * <li>the context field if there is any @Action methods.</li>
   * <li>the observable object for every @Observable.</li>
   * <li>the ComputableValue object for every @Memoize method.</li>
   * </ul>
   */
  private void buildFields( @Nonnull final TypeSpec.Builder builder )
  {

    final FieldSpec.Builder idField =
      FieldSpec.builder( Generator.KERNEL_CLASSNAME,
                         Generator.KERNEL_FIELD_NAME,
                         Modifier.FINAL,
                         Modifier.PRIVATE );
    builder.addField( idField.build() );

    // If we don't have a method for object id but we need one then synthesize it
    if ( null == _componentId )
    {
      final FieldSpec.Builder nextIdField =
        FieldSpec.builder( Generator.DEFAULT_ID_TYPE,
                           Generator.NEXT_ID_FIELD_NAME,
                           Modifier.VOLATILE,
                           Modifier.STATIC,
                           Modifier.PRIVATE );
      builder.addField( nextIdField.build() );
    }

    _roObservables.forEach( observable -> observable.buildFields( builder ) );
    _roMemoizes.forEach( memoize -> memoize.buildFields( builder ) );
    _roObserves.forEach( observe -> observe.buildFields( builder ) );
    _roReferences.forEach( r -> r.buildFields( builder ) );
  }

  /**
   * Build all constructors as they appear on the ArezComponent class.
   * Arez Observable fields are populated as required and parameters are passed up to superclass.
   */
  private void buildConstructors( @Nonnull final TypeSpec.Builder builder,
                                  @Nonnull final Types typeUtils )
  {
    final boolean requiresDeprecatedSuppress = hasDeprecatedElements();
    for ( final ExecutableElement constructor : ProcessorUtil.getConstructors( getElement() ) )
    {
      final ExecutableType methodType =
        (ExecutableType) typeUtils.asMemberOf( (DeclaredType) _element.asType(), constructor );
      builder.addMethod( buildConstructor( constructor, methodType, requiresDeprecatedSuppress ) );
    }
  }

  /**
   * Build a constructor based on the supplied constructor
   */
  @Nonnull
  private MethodSpec buildConstructor( @Nullable final ExecutableElement constructor,
                                       @Nullable final ExecutableType constructorType,
                                       final boolean requiresDeprecatedSuppress )
  {
    final MethodSpec.Builder builder = MethodSpec.constructorBuilder();
    if ( _injectFactory )
    {
      // The constructor is private as the factory is responsible for creating component.
      builder.addModifiers( Modifier.PRIVATE );
    }
    else if ( null != constructor &&
              constructor.getModifiers().contains( Modifier.PUBLIC ) &&
              getElement().getModifiers().contains( Modifier.PUBLIC ) )
    {
      /*
       * The constructor MUST be public if annotated class is public as that implies that we expect
       * that code outside the package may construct the component.
       */
      builder.addModifiers( Modifier.PUBLIC );
    }
    if ( null != constructorType )
    {
      GeneratorUtil.copyExceptions( constructorType, builder );
      GeneratorUtil.copyTypeParameters( constructorType, builder );
    }
    if ( null != constructor )
    {
      Generator.copyWhitelistedAnnotations( constructor, builder );
    }
    if ( requiresDeprecatedSuppress &&
         !AnnotationsUtil.hasAnnotationOfType( getElement(), SuppressWarnings.class.getName() ) )
    {
      builder.addAnnotation( AnnotationSpec.builder( SuppressWarnings.class )
                               .addMember( "value", "$S", "deprecation" )
                               .build() );
    }

    if ( InjectMode.NONE != _injectMode && !_injectFactory )
    {
      builder.addAnnotation( Generator.INJECT_CLASSNAME );
    }

    final List<ObservableDescriptor> initializers = getInitializers();

    final StringBuilder superCall = new StringBuilder();
    superCall.append( "super(" );
    final ArrayList<String> parameterNames = new ArrayList<>();

    boolean firstParam = true;
    if ( null != constructor )
    {
      for ( final VariableElement element : constructor.getParameters() )
      {
        final ParameterSpec.Builder param =
          ParameterSpec.builder( TypeName.get( element.asType() ), element.getSimpleName().toString(), Modifier.FINAL );
        Generator.copyWhitelistedAnnotations( element, param );
        builder.addParameter( param.build() );
        parameterNames.add( element.getSimpleName().toString() );
        if ( !firstParam )
        {
          superCall.append( "," );
        }
        firstParam = false;
        superCall.append( "$N" );
      }
    }

    superCall.append( ")" );
    builder.addStatement( superCall.toString(), parameterNames.toArray() );

    if ( !_references.isEmpty() )
    {
      final CodeBlock.Builder block = CodeBlock.builder();
      block.beginControlFlow( "if ( $T.shouldCheckApiInvariants() )", Generator.AREZ_CLASSNAME );
      block.addStatement( "$T.apiInvariant( () -> $T.areReferencesEnabled(), () -> \"Attempted to create instance " +
                          "of component of type '$N' that contains references but Arez.areReferencesEnabled() " +
                          "returns false. References need to be enabled to use this component\" )",
                          Generator.GUARDS_CLASSNAME,
                          Generator.AREZ_CLASSNAME,
                          getType() );
      block.endControlFlow();
      builder.addCode( block.build() );
    }

    buildComponentKernel( builder );

    for ( final ObservableDescriptor observable : initializers )
    {
      final String candidateName = observable.getName();
      final String name =
        null != constructor && isNameCollision( constructor, Collections.emptyList(), candidateName ) ?
        Generator.INITIALIZER_PREFIX + candidateName :
        candidateName;
      final ParameterSpec.Builder param =
        ParameterSpec.builder( TypeName.get( observable.getGetterType().getReturnType() ),
                               name,
                               Modifier.FINAL );
      Generator.copyWhitelistedAnnotations( observable.getGetter(), param );
      builder.addParameter( param.build() );
      final boolean isPrimitive = TypeName.get( observable.getGetterType().getReturnType() ).isPrimitive();
      if ( isPrimitive )
      {
        builder.addStatement( "this.$N = $N", observable.getDataFieldName(), name );
      }
      else if ( observable.isGetterNonnull() )
      {
        builder.addStatement( "this.$N = $T.requireNonNull( $N )",
                              observable.getDataFieldName(),
                              Objects.class,
                              name );
      }
      else
      {
        builder.addStatement( "this.$N = $N", observable.getDataFieldName(), name );
      }
    }

    _roObservables.forEach( observable -> observable.buildInitializer( builder ) );
    _roMemoizes.forEach( memoize -> memoize.buildInitializer( builder ) );
    _roObserves.forEach( observe -> observe.buildInitializer( builder ) );
    _roInverses.forEach( e -> e.buildInitializer( builder ) );
    _roDependencies.forEach( e -> e.buildInitializer( builder ) );

    builder.addStatement( "this.$N.componentConstructed()", Generator.KERNEL_FIELD_NAME );

    final List<ReferenceDescriptor> eagerReferences =
      _roReferences.stream().filter( r -> r.getLinkType().equals( "EAGER" ) ).collect( Collectors.toList() );
    for ( final ReferenceDescriptor reference : eagerReferences )
    {
      builder.addStatement( "this.$N()", reference.getLinkMethodName() );
    }

    if ( null != _postConstruct )
    {
      if ( isClassType() )
      {
        builder.addStatement( "super.$N()", _postConstruct.getSimpleName().toString() );
      }
      else
      {
        builder.addStatement( "$T.super.$N()", getClassName(), _postConstruct.getSimpleName().toString() );
      }
    }

    if ( !_deferSchedule && requiresSchedule() )
    {
      builder.addStatement( "this.$N.componentComplete()", Generator.KERNEL_FIELD_NAME );
    }
    else
    {
      builder.addStatement( "this.$N.componentReady()", Generator.KERNEL_FIELD_NAME );
    }
    return builder.build();
  }

  private void buildComponentKernel( @Nonnull final MethodSpec.Builder builder )
  {
    buildContextVar( builder );
    buildSyntheticIdVarIfRequired( builder );
    buildNameVar( builder );
    buildNativeComponentVar( builder );

    final StringBuilder sb = new StringBuilder();
    final ArrayList<Object> params = new ArrayList<>();

    sb.append( "this.$N = new $T( $T.areZonesEnabled() ? $N : null, $T.areNamesEnabled() ? $N : null, " );
    params.add( Generator.KERNEL_FIELD_NAME );
    params.add( Generator.KERNEL_CLASSNAME );
    params.add( Generator.AREZ_CLASSNAME );
    params.add( Generator.CONTEXT_VAR_NAME );
    params.add( Generator.AREZ_CLASSNAME );
    params.add( Generator.NAME_VAR_NAME );
    if ( null == _componentId )
    {
      sb.append( "$N, " );
      params.add( Generator.ID_VAR_NAME );
    }
    else
    {
      sb.append( "0, " );
    }
    sb.append( "$T.areNativeComponentsEnabled() ? $N : null, " );
    params.add( Generator.AREZ_CLASSNAME );
    params.add( Generator.COMPONENT_VAR_NAME );

    if ( hasInternalPreDispose() )
    {
      sb.append( "$T.areNativeComponentsEnabled() ? null : this::$N, " );
      params.add( Generator.AREZ_CLASSNAME );
      params.add( Generator.INTERNAL_PRE_DISPOSE_METHOD_NAME );
    }
    else if ( null != _preDispose )
    {
      if ( isClassType() )
      {
        sb.append( "$T.areNativeComponentsEnabled() ? null : () -> super.$N(), " );
        params.add( Generator.AREZ_CLASSNAME );
        params.add( _preDispose.getSimpleName() );
      }
      else
      {
        sb.append( "$T.areNativeComponentsEnabled() ? null : () -> $T.super.$N(), " );
        params.add( Generator.AREZ_CLASSNAME );
        params.add( getClassName() );
        params.add( _preDispose.getSimpleName() );
      }
    }
    else
    {
      sb.append( "null, " );
    }
    if ( needsInternalDispose() )
    {
      sb.append( "$T.areNativeComponentsEnabled() ? null : this::$N, " );
      params.add( Generator.AREZ_CLASSNAME );
      params.add( Generator.INTERNAL_DISPOSE_METHOD_NAME );
    }
    else
    {
      sb.append( "null, " );
    }

    if ( null != _postDispose )
    {
      if ( isClassType() )
      {
        sb.append( "$T.areNativeComponentsEnabled() ? null : () -> super.$N(), " );
        params.add( Generator.AREZ_CLASSNAME );
        params.add( _postDispose.getSimpleName() );
      }
      else
      {
        sb.append( "$T.areNativeComponentsEnabled() ? null : () -> $T.super.$N(), " );
        params.add( Generator.AREZ_CLASSNAME );
        params.add( getClassName() );
        params.add( _postDispose.getSimpleName() );
      }
    }
    else
    {
      sb.append( "null, " );
    }

    sb.append( _disposeNotifier );
    sb.append( ", " );
    sb.append( _observable );
    sb.append( ", " );
    sb.append( _disposeOnDeactivate );
    sb.append( " )" );

    builder.addStatement( sb.toString(), params.toArray() );
  }

  private boolean needsInternalDispose()
  {
    return !_roObserves.isEmpty() || !_roMemoizes.isEmpty() || !_roObservables.isEmpty();
  }

  private void buildContextVar( @Nonnull final MethodSpec.Builder builder )
  {
    builder.addStatement( "final $T $N = $T.context()",
                          Generator.AREZ_CONTEXT_CLASSNAME,
                          Generator.CONTEXT_VAR_NAME,
                          Generator.AREZ_CLASSNAME );
  }

  private void buildNameVar( @Nonnull final MethodSpec.Builder builder )
  {
    // This is the same logic used to synthesize name in the getName() method
    // Duplication is okay as it will be optimized out in production builds.
    if ( _nameIncludesId )
    {
      builder.addStatement( "final String $N = $T.areNamesEnabled() ? $S + $N : null",
                            Generator.NAME_VAR_NAME,
                            Generator.AREZ_CLASSNAME,
                            _type.isEmpty() ? "" : _type + ".",
                            Generator.ID_VAR_NAME );
    }
    else
    {
      builder.addStatement( "final String $N = $T.areNamesEnabled() ? $S : null",
                            Generator.NAME_VAR_NAME,
                            Generator.AREZ_CLASSNAME,
                            _type );
    }
  }

  private void buildSyntheticIdVarIfRequired( @Nonnull final MethodSpec.Builder builder )
  {
    if ( null == _componentId )
    {
      if ( _idRequired )
      {
        builder.addStatement( "final int $N = ++$N", Generator.ID_VAR_NAME, Generator.NEXT_ID_FIELD_NAME );
      }
      else if ( _nameIncludesId )
      {
        builder.addStatement( "final int $N = ( $T.areNamesEnabled() || $T.areRegistriesEnabled() || " +
                              "$T.areNativeComponentsEnabled() ) ? ++$N : 0",
                              Generator.ID_VAR_NAME,
                              Generator.AREZ_CLASSNAME,
                              Generator.AREZ_CLASSNAME,
                              Generator.AREZ_CLASSNAME,
                              Generator.NEXT_ID_FIELD_NAME );
      }
      else
      {
        builder.addStatement( "final int $N = ( $T.areRegistriesEnabled() || " +
                              "$T.areNativeComponentsEnabled() ) ? ++$N : 0",
                              Generator.ID_VAR_NAME,
                              Generator.AREZ_CLASSNAME,
                              Generator.AREZ_CLASSNAME,
                              Generator.NEXT_ID_FIELD_NAME );
      }
    }
    else
    {
      final ExecutableType methodType =
        (ExecutableType) _processingEnv.getTypeUtils().asMemberOf( (DeclaredType) _element.asType(), _componentId );
      builder.addStatement( "final $T $N = $N()",
                            methodType.getReturnType(),
                            Generator.ID_VAR_NAME,
                            _componentId.getSimpleName() );
    }
  }

  private void buildNativeComponentVar( final MethodSpec.Builder builder )
  {
    final StringBuilder sb = new StringBuilder();
    final ArrayList<Object> params = new ArrayList<>();
    sb.append( "final $T $N = $T.areNativeComponentsEnabled() ? $N.component( $S, $N, $N" );
    params.add( Generator.COMPONENT_CLASSNAME );
    params.add( Generator.COMPONENT_VAR_NAME );
    params.add( Generator.AREZ_CLASSNAME );
    params.add( Generator.CONTEXT_VAR_NAME );
    params.add( _type );
    params.add( Generator.ID_VAR_NAME );
    params.add( Generator.NAME_VAR_NAME );
    if ( _disposeNotifier || null != _preDispose || null != _postDispose )
    {
      sb.append( ", " );
      if ( _disposeNotifier )
      {
        sb.append( "() -> $N()" );
        params.add( Generator.INTERNAL_NATIVE_COMPONENT_PRE_DISPOSE_METHOD_NAME );
      }
      else if ( null != _preDispose )
      {
        if ( isClassType() )
        {
          sb.append( "() -> super.$N()" );
          params.add( _preDispose.getSimpleName().toString() );
        }
        else
        {
          sb.append( "() -> $T.super.$N()" );
          params.add( getClassName() );
          params.add( _preDispose.getSimpleName().toString() );
        }
      }
      else
      {
        sb.append( "null" );
      }

      if ( null != _postDispose )
      {
        if ( isClassType() )
        {
          sb.append( ",  () -> super.$N()" );
          params.add( _postDispose.getSimpleName().toString() );
        }
        else
        {
          sb.append( ",  () -> $T.super.$N()" );
          params.add( getClassName() );
          params.add( _postDispose.getSimpleName().toString() );
        }
      }
    }
    sb.append( " ) : null" );
    builder.addStatement( sb.toString(), params.toArray() );
  }

  @Nonnull
  private List<ObservableDescriptor> getInitializers()
  {
    return getObservables()
      .stream()
      .filter( ObservableDescriptor::requireInitializer )
      .collect( Collectors.toList() );
  }

  private boolean isNameCollision( @Nonnull final ExecutableElement constructor,
                                   @Nonnull final List<ObservableDescriptor> initializers,
                                   @Nonnull final String name )
  {
    return constructor.getParameters().stream().anyMatch( p -> p.getSimpleName().toString().equals( name ) ) ||
           initializers.stream().anyMatch( o -> o.getName().equals( name ) );
  }

  boolean needsDaggerIntegration()
  {
    return _dagger;
  }

  boolean shouldGenerateFactory()
  {
    return _injectFactory;
  }

  @Nonnull
  InjectMode getInjectMode()
  {
    return _injectMode;
  }

  boolean needsDaggerModule()
  {
    return needsDaggerIntegration() && InjectMode.PROVIDE == _injectMode && !needsDaggerComponentExtension();
  }

  boolean needsDaggerComponentExtension()
  {
    return needsDaggerIntegration() && _injectFactory;
  }

  @Nonnull
  TypeSpec buildComponentDaggerModule( @Nonnull final ProcessingEnvironment processingEnv )
    throws ProcessorException
  {
    assert needsDaggerIntegration();

    final TypeSpec.Builder builder = TypeSpec.interfaceBuilder( getComponentDaggerModuleName() ).
      addTypeVariables( GeneratorUtil.getTypeArgumentsAsNames( asDeclaredType() ) );
    Generator.copyWhitelistedAnnotations( getElement(), builder );
    GeneratorUtil.addOriginatingTypes( getElement(), builder );

    Generator.addGeneratedAnnotation( processingEnv, builder );
    builder.addAnnotation( Generator.DAGGER_MODULE_CLASSNAME );
    builder.addModifiers( Modifier.PUBLIC );

    final MethodSpec.Builder method = MethodSpec.methodBuilder( "bindComponent" ).
      addAnnotation( Generator.DAGGER_BINDS_CLASSNAME ).
      addModifiers( Modifier.ABSTRACT, Modifier.PUBLIC ).
      addParameter( getEnhancedClassName(), "component" ).
      returns( ClassName.get( getElement() ) );
    if ( null != _scopeAnnotation )
    {
      final DeclaredType annotationType = _scopeAnnotation.getAnnotationType();
      final TypeElement typeElement = (TypeElement) annotationType.asElement();
      method.addAnnotation( ClassName.get( typeElement ) );
    }

    builder.addMethod( method.build() );
    return builder.build();
  }

  boolean hasRepository()
  {
    return null != _repositoryExtensions;
  }

  @SuppressWarnings( "ConstantConditions" )
  void configureRepository( @Nonnull final String name,
                            @Nonnull final List<TypeElement> extensions,
                            @Nonnull final String repositoryInjectMode,
                            @Nonnull final String repositoryDaggerConfig )
  {
    assert null != name;
    assert null != extensions;
    _repositoryInjectMode = repositoryInjectMode;
    _repositoryDaggerConfig = repositoryDaggerConfig;
    for ( final TypeElement extension : extensions )
    {
      if ( ElementKind.INTERFACE != extension.getKind() )
      {
        throw new ProcessorException( "Class annotated with @Repository defined an extension that is " +
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
            throw new ProcessorException( "Class annotated with @Repository defined an extension that has " +
                                          "a non default method. Extension: " + extension.getQualifiedName() +
                                          " Method: " + method, getElement() );
          }
        }
      }
    }
    _repositoryExtensions = extensions;
  }

  /**
   * Build the enhanced class for the component.
   */
  @Nonnull
  TypeSpec buildRepository( @Nonnull final ProcessingEnvironment processingEnv )
    throws ProcessorException
  {
    assert null != _repositoryExtensions;
    final TypeElement element = getElement();

    final ClassName arezType = getEnhancedClassName();

    final TypeSpec.Builder builder = TypeSpec.classBuilder( getRepositoryClassName().simpleName() ).
      addTypeVariables( GeneratorUtil.getTypeArgumentsAsNames( asDeclaredType() ) );
    Generator.copyWhitelistedAnnotations( getElement(), builder );
    GeneratorUtil.addOriginatingTypes( element, builder );

    Generator.addGeneratedAnnotation( processingEnv, builder );

    final boolean addSingletonAnnotation =
      "CONSUME".equals( _repositoryInjectMode ) ||
      "PROVIDE".equals( _repositoryInjectMode ) ||
      ( "AUTODETECT".equals( _repositoryInjectMode ) &&
        null != _processingEnv.getElementUtils().getTypeElement( Constants.INJECT_ANNOTATION_CLASSNAME ) );

    final AnnotationSpec.Builder arezComponent =
      AnnotationSpec.builder( ClassName.bestGuess( Constants.COMPONENT_ANNOTATION_CLASSNAME ) );
    if ( !addSingletonAnnotation )
    {
      arezComponent.addMember( "nameIncludesId", "false" );
    }
    if ( !"AUTODETECT".equals( _repositoryInjectMode ) )
    {
      arezComponent.addMember( "inject", "$T.$N", Generator.INJECT_MODE_CLASSNAME, _repositoryInjectMode );
    }
    if ( !"AUTODETECT".equals( _repositoryDaggerConfig ) )
    {
      arezComponent.addMember( "dagger", "$T.$N", Generator.FEATURE_CLASSNAME, _repositoryDaggerConfig );
    }
    builder.addAnnotation( arezComponent.build() );
    if ( addSingletonAnnotation )
    {
      builder.addAnnotation( Generator.SINGLETON_CLASSNAME );
    }

    builder.superclass( ParameterizedTypeName.get( Generator.ABSTRACT_REPOSITORY_CLASSNAME,
                                                   getIdType().box(),
                                                   ClassName.get( element ),
                                                   getRepositoryClassName() ) );

    _repositoryExtensions.forEach( e -> builder.addSuperinterface( TypeName.get( e.asType() ) ) );

    GeneratorUtil.copyAccessModifiers( element, builder );

    /*
     * If the repository will be generated as a PROVIDE inject mode when dagger is present
     * but the type is not public, we still need to generate a public repository due to
     * constraints imposed by dagger.
     */
    if ( addSingletonAnnotation &&
         !"CONSUME".equals( _repositoryInjectMode ) &&
         !element.getModifiers().contains( Modifier.PUBLIC ) )
    {
      builder.addModifiers( Modifier.PUBLIC );
    }

    builder.addModifiers( Modifier.ABSTRACT );

    //Add the default access, no-args constructor
    builder.addMethod( MethodSpec.constructorBuilder().build() );

    // Add the factory method
    builder.addMethod( buildFactoryMethod() );

    if ( shouldRepositoryDefineCreate() )
    {
      for ( final ExecutableElement constructor : ProcessorUtil.getConstructors( element ) )
      {
        final ExecutableType methodType =
          (ExecutableType) processingEnv.getTypeUtils().asMemberOf( (DeclaredType) _element.asType(), constructor );
        builder.addMethod( buildRepositoryCreate( constructor, methodType, arezType ) );
      }
    }
    if ( shouldRepositoryDefineAttach() )
    {
      builder.addMethod( buildRepositoryAttach() );
    }

    if ( null != _componentId )
    {
      builder.addMethod( buildFindByIdMethod() );
      builder.addMethod( buildGetByIdMethod() );
    }

    if ( shouldRepositoryDefineDestroy() )
    {
      builder.addMethod( buildRepositoryDestroy() );
    }
    if ( shouldRepositoryDefineDetach() )
    {
      builder.addMethod( buildRepositoryDetach() );
    }
    return builder.build();
  }

  @Nonnull
  ClassName getEnhancedClassName()
  {
    return GeneratorUtil.getGeneratedClassName( getElement(), "Arez_", "" );
  }

  @Nonnull
  private String getComponentDaggerModuleName()
  {
    return GeneratorUtil.getGeneratedSimpleClassName( getElement(), "", "DaggerModule" );
  }

  @Nonnull
  ClassName getDaggerComponentExtensionClassName()
  {
    return GeneratorUtil.getGeneratedClassName( getElement(), "", "DaggerComponentExtension" );
  }

  @Nonnull
  private ClassName getRepositoryClassName()
  {
    return GeneratorUtil.getGeneratedClassName( getElement(), "", "Repository" );
  }

  @Nonnull
  private MethodSpec buildRepositoryAttach()
  {
    return MethodSpec.methodBuilder( "attach" ).
      addAnnotation( Override.class ).
      addModifiers( getElement().getModifiers().contains( Modifier.PUBLIC ) ? Modifier.PUBLIC : Modifier.PROTECTED ).
      addAnnotation( AnnotationSpec.builder( Generator.ACTION_CLASSNAME )
                       .addMember( "reportParameters", "false" )
                       .build() ).
      addParameter( ParameterSpec.builder( TypeName.get( getElement().asType() ), "entity", Modifier.FINAL )
                      .addAnnotation( Generator.NONNULL_CLASSNAME )
                      .build() ).
      addStatement( "super.attach( entity )" ).build();
  }

  @Nonnull
  private MethodSpec buildRepositoryDetach()
  {
    return MethodSpec.methodBuilder( "detach" ).
      addAnnotation( Override.class ).
      addModifiers( getElement().getModifiers().contains( Modifier.PUBLIC ) ? Modifier.PUBLIC : Modifier.PROTECTED ).
      addAnnotation( AnnotationSpec.builder( Generator.ACTION_CLASSNAME )
                       .addMember( "reportParameters", "false" )
                       .build() ).
      addParameter( ParameterSpec.builder( TypeName.get( getElement().asType() ), "entity", Modifier.FINAL )
                      .addAnnotation( Generator.NONNULL_CLASSNAME )
                      .build() ).
      addStatement( "super.detach( entity )" ).build();
  }

  @Nonnull
  private MethodSpec buildRepositoryDestroy()
  {
    final TypeName entityType = TypeName.get( getElement().asType() );
    final MethodSpec.Builder method = MethodSpec.methodBuilder( "destroy" ).
      addAnnotation( Override.class ).
      addAnnotation( AnnotationSpec.builder( Generator.ACTION_CLASSNAME )
                       .addMember( "reportParameters", "false" )
                       .build() ).
      addParameter( ParameterSpec.builder( entityType, "entity", Modifier.FINAL )
                      .addAnnotation( Generator.NONNULL_CLASSNAME )
                      .build() ).
      addStatement( "super.destroy( entity )" );
    GeneratorUtil.copyAccessModifiers( getElement(), method );
    final Set<Modifier> modifiers = getElement().getModifiers();
    if ( !modifiers.contains( Modifier.PUBLIC ) && !modifiers.contains( Modifier.PROTECTED ) )
    {
      /*
       * The destroy method inherited from AbstractContainer is protected and the override
       * must be at least the same access level.
       */
      method.addModifiers( Modifier.PROTECTED );
    }
    return method.build();
  }

  @Nonnull
  private MethodSpec buildFindByIdMethod()
  {
    assert null != _componentId;

    final MethodSpec.Builder method = MethodSpec.methodBuilder( "findBy" + getIdName() ).
      addModifiers( Modifier.FINAL ).
      addParameter( ParameterSpec.builder( getIdType(), "id", Modifier.FINAL ).build() ).
      addAnnotation( Generator.NULLABLE_CLASSNAME ).
      returns( TypeName.get( getElement().asType() ) ).
      addStatement( "return findByArezId( id )" );
    GeneratorUtil.copyAccessModifiers( getElement(), method );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildGetByIdMethod()
  {
    final TypeName entityType = TypeName.get( getElement().asType() );
    final MethodSpec.Builder method = MethodSpec.methodBuilder( "getBy" + getIdName() ).
      addModifiers( Modifier.FINAL ).
      addAnnotation( Generator.NONNULL_CLASSNAME ).
      addParameter( ParameterSpec.builder( getIdType(), "id", Modifier.FINAL ).build() ).
      returns( entityType ).
      addStatement( "return getByArezId( id )" );
    GeneratorUtil.copyAccessModifiers( getElement(), method );
    return method.build();
  }

  @Nonnull
  private MethodSpec buildFactoryMethod()
  {
    final MethodSpec.Builder method = MethodSpec.methodBuilder( "newRepository" ).
      addModifiers( Modifier.STATIC ).
      addAnnotation( Generator.NONNULL_CLASSNAME ).
      returns( getRepositoryClassName() ).
      addStatement( "return new $T()",
                    ClassName.get( getPackageName(), "Arez_" + getRepositoryClassName().simpleName() ) );
    GeneratorUtil.copyAccessModifiers( getElement(), method );
    return method.build();
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
      AnnotationSpec.builder( ClassName.bestGuess( Constants.ACTION_ANNOTATION_CLASSNAME ) ).
        addMember( "name", "$S", actionName ).build();
    final MethodSpec.Builder builder =
      MethodSpec.methodBuilder( "create" ).
        addAnnotation( annotationSpec ).
        addAnnotation( Generator.NONNULL_CLASSNAME ).
        returns( TypeName.get( asDeclaredType() ) );

    GeneratorUtil.copyAccessModifiers( getElement(), builder );
    GeneratorUtil.copyExceptions( methodType, builder );
    GeneratorUtil.copyTypeParameters( methodType, builder );

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
      Generator.copyWhitelistedAnnotations( element, param );
      builder.addParameter( param.build() );
      parameters.add( element.getSimpleName().toString() );
      if ( !firstParam )
      {
        newCall.append( "," );
      }
      firstParam = false;
      newCall.append( "$N" );
    }

    for ( final ObservableDescriptor observable : getInitializers() )
    {
      final String candidateName = observable.getName();
      final String name = isNameCollision( constructor, Collections.emptyList(), candidateName ) ?
                          Generator.INITIALIZER_PREFIX + candidateName :
                          candidateName;
      final ParameterSpec.Builder param =
        ParameterSpec.builder( TypeName.get( observable.getGetterType().getReturnType() ),
                               name,
                               Modifier.FINAL );
      Generator.copyWhitelistedAnnotations( observable.getGetter(), param );
      builder.addParameter( param.build() );
      parameters.add( name );
      if ( !firstParam )
      {
        newCall.append( "," );
      }
      firstParam = false;
      newCall.append( "$N" );
    }

    newCall.append( ")" );
    builder.addStatement( newCall.toString(), parameters.toArray() );
    builder.addStatement( "attach( entity )" );
    builder.addStatement( "return entity" );
    return builder.build();
  }

  @Nonnull
  String getPackageName()
  {
    return GeneratorUtil.getQualifiedPackageName( _element );
  }

  @Nonnull
  private String getIdMethodName()
  {
    /*
     * Note that it is a deliberate choice to not use getArezId() as that will box Id which for the
     * "normal" case involves converting a long to a Long and it was decided that the slight increase in
     * code size was worth the slightly reduced memory pressure.
     */
    return null != _componentId ? _componentId.getSimpleName().toString() : Generator.ID_FIELD_NAME;
  }

  @Nonnull
  private String getIdName()
  {
    assert null != _componentId;
    final String name = ProcessorUtil.deriveName( _componentId, GETTER_PATTERN, Constants.SENTINEL );
    if ( null != name )
    {
      return Character.toUpperCase( name.charAt( 0 ) ) + ( name.length() > 1 ? name.substring( 1 ) : "" );
    }
    else
    {
      return "Id";
    }
  }

  @Nonnull
  private TypeName getIdType()
  {
    return null == _componentIdMethodType ?
           Generator.DEFAULT_ID_TYPE :
           TypeName.get( _componentIdMethodType.getReturnType() );
  }

  @Nonnull
  private <T> T getAnnotationParameter( @Nonnull final AnnotationMirror annotation,
                                        @Nonnull final String parameterName )
  {
    return AnnotationsUtil.getAnnotationValue( annotation, parameterName );
  }

  private boolean shouldRepositoryDefineCreate()
  {
    final VariableElement injectParameter = (VariableElement)
      AnnotationsUtil.getAnnotationValue( getElement(),
                                          Constants.REPOSITORY_ANNOTATION_CLASSNAME,
                                          "attach" ).getValue();
    switch ( injectParameter.getSimpleName().toString() )
    {
      case "CREATE_ONLY":
      case "CREATE_OR_ATTACH":
        return true;
      default:
        return false;
    }
  }

  private boolean shouldRepositoryDefineAttach()
  {
    final VariableElement injectParameter = (VariableElement)
      AnnotationsUtil.getAnnotationValue( getElement(),
                                          Constants.REPOSITORY_ANNOTATION_CLASSNAME,
                                          "attach" ).getValue();
    switch ( injectParameter.getSimpleName().toString() )
    {
      case "ATTACH_ONLY":
      case "CREATE_OR_ATTACH":
        return true;
      default:
        return false;
    }
  }

  private boolean shouldRepositoryDefineDestroy()
  {
    final VariableElement injectParameter = (VariableElement)
      AnnotationsUtil.getAnnotationValue( getElement(),
                                          Constants.REPOSITORY_ANNOTATION_CLASSNAME,
                                          "detach" ).getValue();
    switch ( injectParameter.getSimpleName().toString() )
    {
      case "DESTROY_ONLY":
      case "DESTROY_OR_DETACH":
        return true;
      default:
        return false;
    }
  }

  private boolean shouldRepositoryDefineDetach()
  {
    final VariableElement injectParameter = (VariableElement)
      AnnotationsUtil.getAnnotationValue( getElement(),
                                          Constants.REPOSITORY_ANNOTATION_CLASSNAME,
                                          "detach" ).getValue();
    switch ( injectParameter.getSimpleName().toString() )
    {
      case "DETACH_ONLY":
      case "DESTROY_OR_DETACH":
        return true;
      default:
        return false;
    }
  }

  @Nonnull
  ProcessingEnvironment getProcessingEnv()
  {
    return _processingEnv;
  }

  @Nonnull
  private String suppressedBy( @Nonnull final String warning )
  {
    return MemberChecks.suppressedBy( warning, Constants.SUPPRESS_AREZ_WARNINGS_ANNOTATION_CLASSNAME );
  }

  private boolean isWarningSuppressed( @Nonnull final Element element, @Nonnull final String warning )
  {
    return ProcessorUtil.isWarningSuppressed( element,
                                              warning,
                                              Constants.SUPPRESS_AREZ_WARNINGS_ANNOTATION_CLASSNAME );
  }
}
