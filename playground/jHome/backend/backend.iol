interface JHomeBackendInterface {
RequestResponse:
	getPageTemplate(string)(string) throws PageTemplateNotFound
}