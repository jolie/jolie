var jHome = {
	getWidgetProperties: function( id, callback ) {
		$.ajax({
			url: 'getWidgetProperties',
			dataType: 'json',
			data: { "$": id },
			success: callback //function(response) { $("#title").html( response.$ ) }
		});
	},
	widgets: {}
};
window.jHome = jHome;