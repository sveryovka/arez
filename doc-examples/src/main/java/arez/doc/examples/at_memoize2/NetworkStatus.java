package arez.doc.examples.at_memoize2;

import arez.ComputableValue;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.ComputableValueRef;
import arez.annotations.DepType;
import arez.annotations.Memoize;
import arez.annotations.OnActivate;
import arez.annotations.OnDeactivate;
import elemental2.dom.DomGlobal;
import elemental2.dom.EventListener;

@ArezComponent
public abstract class NetworkStatus
{
  private final EventListener _listener = e -> updateOnlineStatus();

  // Specify depType so can explicitly trigger a recalculation
  // of method using reportPossiblyChanged()
  @Memoize( depType = DepType.AREZ_OR_EXTERNAL )
  public boolean isOnLine()
  {
    return DomGlobal.navigator.onLine;
  }

  @ComputableValueRef
  abstract ComputableValue getOnLineComputableValue();

  @OnActivate
  final void onOnLineActivate()
  {
    DomGlobal.window.addEventListener( "online", _listener );
    DomGlobal.window.addEventListener( "offline", _listener );
  }

  @OnDeactivate
  final void onOnLineDeactivate()
  {
    DomGlobal.window.removeEventListener( "online", _listener );
    DomGlobal.window.removeEventListener( "offline", _listener );
  }

  @Action
  void updateOnlineStatus()
  {
    // Explicitly trigger a recalculation of the OnLine value
    getOnLineComputableValue().reportPossiblyChanged();
  }
}