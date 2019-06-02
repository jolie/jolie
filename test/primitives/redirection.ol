include "../AbstractTestUnit.iol"
include "redirection/SumInterface.iol"
include "console.iol"
include "time.iol"

outputPort Sum {
  /* the client calls the sum service which is located at the redirector socket://localhost:2002
  teh resource is identified by the suffix /!/Sum */
  Location: "socket://localhost:20000/!/Sum"
  Protocol: sodep
  Interfaces: SumInterface
}

outputPort Redirector {
  /* the client calls the sum service which is located at the redirector socket://localhost:2002
  teh resource is identified by the suffix /!/Sum */
  Location: "socket://localhost:20000"
  Protocol: sodep
  OneWay: shutdown
}

embedded {
  Jolie:
  "redirection/redirector.ol"
}

define doTest
{
  sleep@Time(2000)();
  rq.x = double( args[ 0 ] );
  rq.y = double( args[ 1 ] );
  sum@Sum( rq )( result );
  shutdown@Redirector()

}
