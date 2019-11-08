package arez.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

@SuppressWarnings( { "SameParameterValue", "WeakerAccess", "unused" } )
final class MethodChecks
{
  private MethodChecks()
  {
  }

  /**
   * Verifies that the method is not final, static, abstract or private.
   * The intent is to verify that it can be overridden and wrapped in a sub-class in the same package.
   */
  static void mustBeWrappable( @Nonnull final TypeElement targetType,
                               @Nonnull final String scopeAnnotationName,
                               @Nonnull final String annotationName,
                               @Nonnull final Element element )
    throws ArezProcessorException
  {
    mustBeOverridable( targetType, scopeAnnotationName, annotationName, element );
    mustNotBeAbstract( annotationName, element );
  }

  /**
   * Verifies that the method is not final, static or abstract.
   * The intent is to verify that it can be overridden in sub-class in the same package.
   */
  static void mustBeOverridable( @Nonnull final TypeElement targetType,
                                 @Nonnull final String scopeAnnotationName,
                                 @Nonnull final String annotationName,
                                 @Nonnull final Element element )
    throws ArezProcessorException
  {
    mustNotBeFinal( annotationName, element );
    mustBeSubclassCallable( targetType, scopeAnnotationName, annotationName, element );
  }

  /**
   * Verifies that the method is not static, abstract or private.
   * The intent is to verify that it can be instance called by sub-class in the same package as the targetType.
   */
  static void mustBeSubclassCallable( @Nonnull final TypeElement targetType,
                                      @Nonnull final String scopeAnnotationName,
                                      @Nonnull final String annotationName,
                                      @Nonnull final Element element )
    throws ArezProcessorException
  {
    mustNotBeStatic( annotationName, element );
    mustNotBePrivate( annotationName, element );
    mustNotBePackageAccessInDifferentPackage( targetType, scopeAnnotationName, annotationName, element );
  }

  /**
   * Verifies that the method follows conventions of a lifecycle hook.
   * The intent is to verify that it can be instance called by sub-class in same
   * package at a lifecycle stage. It should not raise errors, return values or accept
   * parameters.
   */
  static void mustBeLifecycleHook( @Nonnull final TypeElement targetType,
                                   @Nonnull final String scopeAnnotationName,
                                   @Nonnull final String annotationName,
                                   @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    mustNotBeAbstract( annotationName, method );
    mustBeSubclassCallable( targetType, scopeAnnotationName, annotationName, method );
    mustNotHaveAnyParameters( annotationName, method );
    mustNotReturnAnyValue( annotationName, method );
    mustNotThrowAnyExceptions( annotationName, method );
  }

  static void mustBeStatic( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.STATIC ) )
    {
      throw new ArezProcessorException( must( annotationName, "be static" ), element );
    }
  }

  static void mustNotBeStatic( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( element.getModifiers().contains( Modifier.STATIC ) )
    {
      throw new ArezProcessorException( mustNot( annotationName, "be static" ), element );
    }
  }

  static void mustBeAbstract( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.ABSTRACT ) )
    {
      throw new ArezProcessorException( must( annotationName, "be abstract" ), element );
    }
  }

  static void mustNotBeAbstract( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( element.getModifiers().contains( Modifier.ABSTRACT ) )
    {
      throw new ArezProcessorException( mustNot( annotationName, "be abstract" ), element );
    }
  }

  static void mustBeFinal( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.FINAL ) )
    {
      throw new ArezProcessorException( must( annotationName, "be final" ), element );
    }
  }

  static void mustNotBeFinal( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( element.getModifiers().contains( Modifier.FINAL ) )
    {
      throw new ArezProcessorException( mustNot( annotationName, "be final" ), element );
    }
  }

  static void mustBePrivate( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.PRIVATE ) )
    {
      throw new ArezProcessorException( must( annotationName, "be private" ), element );
    }
  }

  static void mustNotBePrivate( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ArezProcessorException
  {
    if ( element.getModifiers().contains( Modifier.PRIVATE ) )
    {
      throw new ArezProcessorException( mustNot( annotationName, "be private" ), element );
    }
  }

  static void mustNotBePackageAccessInDifferentPackage( @Nonnull final TypeElement element,
                                                        @Nonnull final String scopeAnnotationName,
                                                        @Nonnull final String annotationName,
                                                        @Nonnull final Element other )
    throws ArezProcessorException
  {
    final Set<Modifier> modifiers = other.getModifiers();
    final boolean isPackageAccess =
      !modifiers.contains( Modifier.PRIVATE ) &&
      !modifiers.contains( Modifier.PROTECTED ) &&
      !modifiers.contains( Modifier.PUBLIC );

    if ( isPackageAccess )
    {
      final PackageElement packageElement = GeneratorUtil.getPackageElement( element );
      final PackageElement otherPackageElement =
        GeneratorUtil.getPackageElement( (TypeElement) other.getEnclosingElement() );
      if ( !Objects.equals( packageElement.getQualifiedName(), otherPackageElement.getQualifiedName() ) )
      {
        throw new ArezProcessorException( mustNot( annotationName,
                                                   "be package access if the " +
                                                   ( other instanceof ExecutableElement ? "method" : "field" ) +
                                                   " is in a different package from the type annotated with the " +
                                                   toSimpleName( scopeAnnotationName ) + " annotation" ),
                                          other );
      }
    }
  }

  static void mustNotHaveAnyParameters( @Nonnull final String annotationName, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    if ( !method.getParameters().isEmpty() )
    {
      throw new ArezProcessorException( mustNot( annotationName, "have any parameters" ), method );
    }
  }

  static void mustNotReturnAnyValue( @Nonnull final String annotationName, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    if ( TypeKind.VOID != method.getReturnType().getKind() )
    {
      throw new ArezProcessorException( mustNot( annotationName, "return a value" ), method );
    }
  }

  static void mustReturnAValue( @Nonnull final String annotationName, @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    if ( TypeKind.VOID == method.getReturnType().getKind() )
    {
      throw new ArezProcessorException( must( annotationName, "return a value" ), method );
    }
  }

  static void mustNotThrowAnyExceptions( @Nonnull final String annotationName,
                                         @Nonnull final ExecutableElement method )
    throws ArezProcessorException
  {
    if ( !method.getThrownTypes().isEmpty() )
    {
      throw new ArezProcessorException( mustNot( annotationName, "throw any exceptions" ), method );
    }
  }

  /**
   * Ensure that the element is not annotated with multiple annotations from the specified set.
   * The exceptions map exists to allow exceptions to this rule.
   *
   * @param element     the element to check.
   * @param annotations the set of annotation names that must not overlap.
   * @param exceptions  the annotations names that are allowed to overlap.
   */
  static void verifyNoOverlappingAnnotations( @Nonnull final Element element,
                                              @Nonnull final Collection<String> annotations,
                                              @Nonnull final Map<String, Collection<String>> exceptions )
    throws ArezProcessorException
  {
    final String[] annotationTypes = annotations.toArray( new String[ 0 ] );

    for ( int i = 0; i < annotationTypes.length; i++ )
    {
      final String type1 = annotationTypes[ i ];
      final Object annotation1 = ProcessorUtil.findAnnotationByType( element, type1 );
      if ( null != annotation1 )
      {
        for ( int j = i + 1; j < annotationTypes.length; j++ )
        {
          final String type2 = annotationTypes[ j ];
          if ( !isException( exceptions, type1, type2 ) )
          {
            final Object annotation2 = ProcessorUtil.findAnnotationByType( element, type2 );
            if ( null != annotation2 )
            {
              final String message =
                "Method can not be annotated with both " + toSimpleName( type1 ) + " and " + toSimpleName( type2 );
              throw new ArezProcessorException( message, element );
            }
          }
        }
      }
    }
  }

  private static boolean isException( @Nonnull final Map<String, Collection<String>> exceptions,
                                      @Nonnull final String type1,
                                      @Nonnull final String type2 )
  {
    return ( exceptions.containsKey( type1 ) && exceptions.get( type1 ).contains( type2 ) ) ||
           exceptions.containsKey( type2 ) && exceptions.get( type2 ).contains( type1 );
  }

  @Nonnull
  private static String must( @Nonnull final String annotationName, @Nonnull final String message )
  {
    return toSimpleName( annotationName ) + " target must " + message;
  }

  @Nonnull
  private static String mustNot( @Nonnull final String annotationName, @Nonnull final String message )
  {
    return must( annotationName, "not " + message );
  }

  @Nonnull
  private static String toSimpleName( @Nonnull final String annotationName )
  {
    return "@" + annotationName.replaceAll( ".*\\.", "" );
  }
}
