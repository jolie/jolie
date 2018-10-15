include "database.iol"
include "runtime.iol"
include "console.iol"
include "file.iol"

define initDatabase
{
	q.statement[i++] = "create table layouts (
		id integer generated always as identity primary key,
		name varchar(128) unique not null
	)";
	q.statement[i++] = "create table pages (
	id integer generated always as identity primary key,
	name varchar(128) unique not null,
	layout_id integer references layouts(id)
)";
	q.statement[i++] = "create table widget_classes (
	id integer generated always as identity primary key,
	name varchar(128) unique not null
)";
	q.statement[i++] = "create table widgets (
	id integer generated always as identity primary key,
	class_id integer references widget_classes(id),
	page_id integer references pages(id),
	div_name varchar(128) not null
)";
	q.statement[i++] = "create table widget_properties (
	widget_id integer references widgets(id),
	name varchar(128) not null,
	value long varchar not null,
	primary key (widget_id, name)
)";
	q.statement[i++] = "create index widget_properties_name on widget_properties(name)";
	q.statement[i++] = "insert into layouts(name) values( 'default' )";
	q.statement[i++] = "insert into pages(name,layout_id) values(
	'home',
	(select id from layouts where name = 'default')
)";
	q.statement[i++] = "insert into widget_classes(name) values( 'HTMLWidget' )";
	q.statement[i++] = "insert into widgets(class_id,page_id,div_name) values (
	(select id from widget_classes where name='HTMLWidget'),
	(select id from pages where name='home'),
	'main'
)";
	q.statement[i++] = "insert into widget_properties(widget_id,name,value) values (
	(select id from widgets),
	'html',
	'<h1>Hello, World!</h1>'
)";
	executeTransaction@Database( q )()
}

main
{
	install( IOException => println@Console( main.IOException.stackTrace )() );
	install( ConnectionError => println@Console( main.ConnectionError )() );
	install( SQLException => println@Console( main.SQLException.stackTrace )() );
	loadLibrary@Runtime( "file:../lib/derby.jar" )();
	with( connectionInfo ) {
		.host = "";
		.driver = "derby_embedded";
		.port = 0;
		.database = "../database/data/jhome";
		.username = "";
		.password = "";
		.attributes = "create=true"
	};
	println@Console( "Creating database" )();
	connect@Database( connectionInfo )();
	println@Console( "Database created" )();

	initDatabase;

	/*getServiceDirectory@File()( dir );
	getFileSeparator@File()( sep );
	listRequest.directory = dir + sep + "sql";
	listRequest.regex = ".*";
	list@File( listRequest )( list );
	println@Console( "Initialising database" )();
	for( i = 0, i < #list.result, i++ ) {
		println@Console( "\tExecuting " + list.result[i] )();
		file.filename = listRequest.directory + sep + list.result[i];
		file.format = "text";
		readFile@File( file )( query );
		update@Database( query )()
	};*/
	println@Console( "Database initialised" )()
}
