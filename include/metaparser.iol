include "services/metaparser/public/interfaces/ParserInterface.iol"

outputPort Parser {
Interfaces: ParserInterface
}

embedded {
  Jolie:
    "services/metaparser/main_parser.ol" in Parser
}