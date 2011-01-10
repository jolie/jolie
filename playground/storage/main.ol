include "xml_storage.iol"
include "console.iol"

main
{
	install( StorageFault => println@Console( main.StorageFault )() );
	connect.filename = "storage.xml";
	connect@XmlStorage( connect )();
	request.node[0].name = "site";
	request.node[0].index = 5;
//	load@XmlStorage( request )( c )
	nullProcess
}

