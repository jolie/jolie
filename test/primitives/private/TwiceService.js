function twice( request )
{
	// var number = request.getFirstChild("number").intValue();
	var number = request.number;
	return number + number;
}

function pow( request )
{
	return Math.pow( request.x, request.y );
}

