package com.example.inject;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import javax.inject.Singleton;

@Singleton
@ArezComponent
public abstract class PublicCtorModel
{
  public PublicCtorModel( int i )
  {

  }

  @Action
  void myAction()
  {
  }
}
