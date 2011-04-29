include "exec.iol"
include "runtime.iol"
include "file.iol"
include "console.iol"
include "string_utils.iol"

define generateIndex
{
	listRequest.regex = ".*\\.html";
	listRequest.directory = execRequest.workingDirectory;
	list@File( listRequest)( listResult );
	sortRequest.item -> listResult.result;
	sort@StringUtils( sortRequest )( sortRequest );
	listResult.result -> sortRequest.item;
	content = "<html><body><ul>";
	for( i = 0, i < #listResult.result, i++ ) {
		content += "<li><a href=\"" + listResult.result[i] + "\">" + listResult.result[i] + "</a></li>"
	};
	content += "</body></html>";
	file.filename = execRequest.workingDirectory + sep + "index.html";
	file.content -> content;
	writeFile@File( file )()
}

main
{
	install( IOException => println@Console( main.IOException )() );
	getFileSeparator@File()( sep );
	getIncludePaths@Runtime()( paths );
	listRequest.regex = "[^\\.]+.*";
	execRequest = "joliedoc";
	execRequest.waitFor = 1;
	if ( is_defined( args[0] ) ) {
		execRequest.workingDirectory = args[0];
		// Check if the directory exists
		exists@File( args[0] )( exists );
		if ( !exists ) {
			mkdir@File( args[0] )( exists );
			if ( !exists ) {
				throw( IOException, "Could not create target directory" )
			}
		}
	} else {
		execRequest.workingDirectory = "."
	};
	for( i = 0, i < #paths.path, i++ ) {
		listRequest.directory = paths.path[i];
		list@File( listRequest )( listResult );
		for( j = 0, j < #listResult.result, j++ ) {
			execRequest.args = //paths.path[i] + sep +
				listResult.result[j];
			exec@Exec( execRequest )( execResult );
			println@Console( execResult )()
		}
	};
	generateIndex	
}
