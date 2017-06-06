include "services/plain_xml_manager/PlainXMLManagerInterface.iol"

outputPort PlainXMLManager {
Interfaces: PlainXMLManagerInterface
}

embedded {
  Jolie:
    "services/plain_xml_manager/plain_xml_manager.ol" in PlainXMLManager
}
