package joliex.openapi.impl.support;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.io.FileService;


import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Map;

public class RestDescriptor {
	HashMap< String, OperationRestDescriptor > operationRestDescriptorHashMap = new HashMap<>();

	public void loadDescriptor( String filename ) throws FaultException {
		try {
			Value errorValue = Value.create();
			FileService fileService = new FileService();
			Value request = Value.create();
			request.getFirstChild( "filename" ).setValue( filename );
			request.getFirstChild( "format" ).setValue( "json" );
			Value response = fileService.readFile( request );
			Map< String, ValueVector > operationsBindingMap = response.children();
			operationsBindingMap.forEach( ( operationName, values ) -> {
				try {
					checkValidity( values.get( 0 ) );
					OperationRestDescriptor operationRestDescriptor = new OperationRestDescriptor( values.get( 0 ) );
					operationRestDescriptorHashMap.put( operationName, operationRestDescriptor );
				} catch( FaultException e ) {
					errorValue.getFirstChild( operationName ).deepCopy( e.value() );
				}


			} );
			if( errorValue.hasChildren() ) {
				throw new FaultException( "RestTemplateError", errorValue );
			}

		} catch( ParserConfigurationException e ) {
			throw new FaultException( e );
		}
	}

	public OperationRestDescriptor getOperationRestDescriptor( String operationName ) throws FaultException {
		if( !operationRestDescriptorHashMap.containsKey( operationName ) ) {
			throw new FaultException( "OperationNoPresent", operationName );
		}
		return operationRestDescriptorHashMap.get( operationName );
	}


	private void checkValidity( Value request ) throws FaultException {

		Value errorValue = Value.create();
		if( !request.hasChildren( "template" ) ) {
			errorValue.getFirstChild( "MissingTemplate" ).setValue( true );

		}

		if( !request.hasChildren( "method" ) ) {
			errorValue.getFirstChild( "MissingMethod" ).setValue( true );

		} else if( !(("PUT".equalsIgnoreCase( request.getFirstChild( "method" ).strValue() )) ||
			("POST".equalsIgnoreCase( request.getFirstChild( "method" ).strValue() )) ||
			("GET".equalsIgnoreCase( request.getFirstChild( "method" ).strValue() )) ||
			("DELETE".equalsIgnoreCase( request.getFirstChild( "method" ).strValue() ))) ) {
			errorValue.getFirstChild( "NotSupportedHttpVerb" ).setValue( true );

		}

		if( errorValue.hasChildren() ) {
			throw new FaultException( "ValidationApiMapper", errorValue );
		}


	}
}
