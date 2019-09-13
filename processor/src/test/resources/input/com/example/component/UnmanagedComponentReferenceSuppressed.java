package com.example.component;

import arez.annotations.ArezComponent;
import arez.component.DisposeNotifier;

@ArezComponent( allowEmpty = true )
public abstract class UnmanagedComponentReferenceSuppressed
{
  @SuppressWarnings( "Arez:UnmanagedComponentReference" )
  final DisposeNotifier time = null;
}
