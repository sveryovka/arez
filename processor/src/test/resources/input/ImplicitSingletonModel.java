import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Computed;
import arez.annotations.Executor;
import arez.annotations.Observable;
import arez.annotations.Observe;
import arez.annotations.OnDepsChanged;
import javax.inject.Singleton;

@Singleton
@ArezComponent
public abstract class ImplicitSingletonModel
{
  @Observable
  public long getTime()
  {
    return 0;
  }

  @Observable
  public void setTime( final long time )
  {
  }

  @Action
  public void doStuff( final long time )
  {
  }

  @Computed
  public int someValue()
  {
    return 0;
  }

  @Observe( executor = Executor.APPLICATION )
  public void render()
  {
  }

  @OnDepsChanged
  public void onRenderDepsChanged()
  {
  }

  @Observe
  protected void myAutorun()
  {
  }
}
