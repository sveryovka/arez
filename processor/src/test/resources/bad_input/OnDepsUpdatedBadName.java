import org.realityforge.arez.annotations.Container;
import org.realityforge.arez.annotations.OnDepsUpdated;
import org.realityforge.arez.annotations.Tracked;

@Container
public class OnDepsUpdatedBadName
{
  @Tracked
  public void render()
  {
  }

  @OnDepsUpdated( name = "-ace" )
  void onRenderDepsUpdated()
  {
  }
}
