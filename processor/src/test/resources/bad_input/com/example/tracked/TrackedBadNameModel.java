package com.example.tracked;

import org.realityforge.arez.annotations.ArezComponent;
import org.realityforge.arez.annotations.OnDepsChanged;
import org.realityforge.arez.annotations.Track;

@ArezComponent
public class TrackedBadNameModel
{
  @Track(name = "-ace")
  static void render()
  {
  }

  @OnDepsChanged
  public void onRenderDepsChanged()
  {
  }
}
