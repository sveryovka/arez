package com.example.component_dependency;

import arez.annotations.ArezComponent;
import arez.annotations.ComponentDependency;
import arez.annotations.Observable;
import arez.component.DisposeTrackable;

@ArezComponent
public abstract class SetNullObservableDependency
{
  @Observable
  @ComponentDependency( action = ComponentDependency.Action.SET_NULL )
  abstract DisposeTrackable getValue();

  abstract void setValue( DisposeTrackable value );
}