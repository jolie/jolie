interface JHomeFrontendInterface {
RequestResponse:
	getPage(string)(string) throws PageNotFound,
	getWidgetProperties(int)(undefined)
}