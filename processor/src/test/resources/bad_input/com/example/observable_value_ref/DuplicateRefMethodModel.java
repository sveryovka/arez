package com.example.observable_value_ref;

import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import javax.annotation.Nonnull;

@ArezComponent
public abstract class DuplicateRefMethodModel
{
  @Observable
  public long getTime()
  {
    return 0;
  }

  public void setTime( final long time )
  {
  }

  @Nonnull
  @ObservableValueRef
  public abstract ObservableValue getTimeObservableValue();

  @Nonnull
  @ObservableValueRef( name = "time" )
  public abstract ObservableValue getTimeObservable2Value();
}