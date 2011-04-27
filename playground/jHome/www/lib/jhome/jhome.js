// Global jHome object
var jHome = {
	// Calls an operation at the originating server using JSON
	call: function( operation, data, callback ) {
		$.ajax({
			url: '/' + operation,
			dataType: 'json',
			type: 'POST',
			contentType: 'application/json',
			success: callback,
			data: JSON.stringify( data )
		});
	},
	getWidgetProperties: function( id, callback ) {
		jHome.call( 'getWidgetProperties', { "$": id }, callback );
	},
	
	// Calls an operation at the specified
	// service published by the originating server using JSON
	callService: function( service, operation, data, callback ) {
		$.ajax({
			url: '/!/' + service + '!/' + operation,
			dataType: 'json',
			type: 'POST',
			contentType: 'application/json',
			success: callback,
			data: JSON.stringify( data )
		});
	},
	widgets: {}
};

// Make jHome global
window.jHome = jHome;