include "frontend.iol"
include "console.iol"
include "string_utils.iol"
include "../common/locations.iol"
include "file.iol"

execution { concurrent }

inputPort JHomeFrontendInput {
Location: "local"
Interfaces: JHomeFrontendInterface
}

include "../common/jhome_database.iol"

init
{
	global.header = "<html>
<head>
<link rel=\"stylesheet\" href=\"../../css/960.css\" />
<link rel=\"stylesheet\" href=\"../../css/reset.css\" />
<link rel=\"stylesheet\" href=\"../../css/text.css\" />
<script type=\"text/javascript\" src=\"../../lib/jquery/jquery-1.4.2.js\"></script>
<script type=\"text/javascript\" src=\"../../lib/jhome/jhome.js\"></script>";
//</head>";
	global.footer = "</body></html>";

	install( SQLException => println@Console( main.SQLException.stackTrace )() )
}

main
{
	[ getPage( pageName )( content ) {
		q = "select P.id as page_id, L.name as layout_name from
pages as P, layouts as L
where
P.name = :name and L.id = P.layout_id";
		q.name = pageName;
		query@Database( q )( result );
		if ( #result.row > 0 ) {
			f.filename = "www/layouts/" + result.row.LAYOUT_NAME + ".html";
			f.format = "text";
			readFile@File( f )( body );
			content = global.header;
			undef( q );
			q = "select C.name from
widgets as W, widget_classes as C where
W.class_id = C.id and W.page_id = :page_id group by C.name";
			q.page_id = result.row.PAGE_ID;
			query@Database( q )( result );
			for( i = 0, i < #result.row, i++ ) {
				content += "<script type=\"text/javascript\" src=\"../../widgets/" + result.row[i].NAME + ".js\"></script>"
			};
			content += "</head><body>";
			content += body;
			q = "select C.name, W.ID, W.div_name from
widgets as W, widget_classes as C where
W.class_id = C.id and W.page_id = :page_id";
			query@Database( q )( result );
			for( i = 0, i < #result.row, i++ ) {
				content += "<script type=\"text/javascript\">
$(function() {
	jHome.widgets." + result.row[i].NAME + "( " + result.row[i].ID + ", $('#" + result.row[i].DIV_NAME + "') );
});
</script>"
			};
			content += global.footer
		} else {
			throw( PageNotFound )
		}
	} ] { nullProcess }

	[ getWidgetProperties( id )( response ) {
		q = "select name, value from widget_properties where widget_id = :widget_id";
		q.widget_id = id;
		query@Database( q )( result );
		for( i = 0, i < #result.row, i++ ) {
			response.(result.row[i].NAME) = result.row[i].VALUE
		}
	} ] { nullProcess }
}
