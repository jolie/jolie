importClass( java.lang.System );
importClass( java.lang.Integer );

function twice( request )
{
	var number = request.getFirstChild("number").intValue();
	return Integer.parseInt(number + number);
}
