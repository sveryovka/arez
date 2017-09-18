import org.realityforge.arez.annotations.Action;
import org.realityforge.arez.annotations.Container;
import org.realityforge.arez.annotations.PostDispose;

@Container
public class ActionAndPostDisposeMethodModel
{
  @Action
  @PostDispose
  public long doStuff()
  {
    return 22;
  }
}
