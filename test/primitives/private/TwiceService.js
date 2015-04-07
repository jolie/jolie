function twice( request )
{
	var number = request.getFirstChild("number").intValue();
	return number + number;
}
