include "console.iol"

interface EmbeddedIface {
RequestResponse:
  twice(int)(int)
}

outputPort Embedded {
Location: "local://A"
Interfaces: EmbeddedIface
}

embedded {
Jolie:
  "test_embedded.ol"
}

main
{
  twice@Embedded( 5 )( x );
  println@Console(x)()
}
