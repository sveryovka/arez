package com.example.deprecated;

import arez.annotations.ArezComponent;
import arez.annotations.Computed;

@ArezComponent
public abstract class DeprecatedMemoizeModel
{
  @Deprecated
  @Computed
  public long count( final long time, float someOtherParameter )
  {
    return time;
  }
}
