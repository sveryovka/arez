package com.example.computed;

import arez.annotations.ArezComponent;
import arez.annotations.Computed;
import arez.annotations.DepType;

@ArezComponent
public abstract class MissingComputedValueRefModel
{
  @Computed( depType = DepType.AREZ_OR_EXTERNAL )
  public Integer getValue()
  {
    return null;
  }
}
