$(document).ready(function(){
	/*$.ajax({
		url: 'getPageTitle',
		dataType: 'xml',
		success: function(response) { $("#title").html( $(response).find('getPageTitleResponse').text() ) }
	});*/
	
	$.ajax({
		url: 'getPageTitle',
		dataType: 'json',
		success: function(response) { $("#title").html( response.$ ) }
	});
});