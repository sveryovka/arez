package arez.doc.examples.lifecycle;

import akasha.Global;
import akasha.HashChangeEvent;
import akasha.HashChangeEventListener;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.PostConstruct;
import arez.annotations.PreDispose;
import javax.annotation.Nonnull;

@ArezComponent
public abstract class BrowserLocation
{
  private final HashChangeEventListener _listener = this::onHashChangeEvent;
  //DOC ELIDE START
  //DOC ELIDE END

  @PostConstruct
  void postConstruct()
  {
    Global.addHashchangeListener( _listener, false );
    //DOC ELIDE START
    //DOC ELIDE END
  }

  @PreDispose
  void preDispose()
  {
    Global.removeHashchangeListener( _listener, false );
  }

  //DOC ELIDE START
  @Action
  void onHashChangeEvent( @Nonnull final HashChangeEvent e )
  {
  }
  //DOC ELIDE END
}
