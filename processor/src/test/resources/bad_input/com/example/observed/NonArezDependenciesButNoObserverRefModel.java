package com.example.observed;

import arez.annotations.ArezComponent;
import arez.annotations.DepType;
import arez.annotations.Observed;

@ArezComponent
public abstract class NonArezDependenciesButNoObserverRefModel
{
  @Observed( depType = DepType.AREZ_OR_EXTERNAL )
  void doStuff()
  {
  }
}
