package arez.doc.examples.inject2;

import javax.inject.Inject;

public class ExampleConsumer
{
  private final MyService _myService;

  @Inject
  ExampleConsumer( final MyService myService )
  {
    _myService = myService;
  }

  public void performSomeAction( int value )
  {
    _myService.performAction( value );
  }
}
