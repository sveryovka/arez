package com.example.inheritance.other;

import arez.annotations.ArezComponent;
import arez.annotations.Multiplicity;
import arez.annotations.Reference;
import arez.annotations.ReferenceId;
import com.example.inheritance.CompleteModel;

@ArezComponent
public abstract class Element
{
  @Reference( inverseMultiplicity = Multiplicity.MANY )
  protected abstract CompleteModel getCompleteModel();

  @ReferenceId
  protected int getCompleteModelId()
  {
    return 0;
  }
}