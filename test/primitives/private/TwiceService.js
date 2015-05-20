function twice( request )
{
	// var number = request.getFirstChild("number").intValue();
	var number = request.number;
	return number + number;
}
