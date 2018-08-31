jHome.widgets.HTMLWidget = function( id, div ) {
	//div.html( "Hello, World!" );
	jHome.getWidgetProperties( id, function( response ) {
		div.html( response.html.$ )
	});
	
	jHome.callService( 'News', 'getNewsList', {}, function( response ) {
		alert( response.item[0].$ );
	});
}
