package joliex.wsdl;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author Francesco
 */
public enum NameSpacesEnum {

	//TNS("tns","http://www.italianasoftware.com/wsdl/FirstServiceByWSDL4J.wsdl"),
	//TNS_SCH("tnsxs","http://www.italianasoftware.com/wsdl/FirstServiceByWSDL4J.xsd"),
	XML_SCH("xs","http://www.w3.org/2001/XMLSchema"),
	SOAP("soap","http://schemas.xmlsoap.org/wsdl/soap/"),
	SOAPoverHTTP("soapOhttp","http://schemas.xmlsoap.org/soap/http/"),
	WSDL("wsdl","http://schemas.xmlsoap.org/wsdl/");

	
	private String prefix;
        private String nameSpaceURI;

	NameSpacesEnum(String prefix,String nameSpaceURI){
            this.prefix=prefix;
            this.nameSpaceURI=nameSpaceURI;
	}

	/**
	 * @return the nameSpace
	 */ public String getNameSpaceURI()
	{
		return nameSpaceURI;
	}

	/**
	 * @return the prefix
	 */ public String getNameSpacePrefix()
	{
		return prefix;
	}

	
}
