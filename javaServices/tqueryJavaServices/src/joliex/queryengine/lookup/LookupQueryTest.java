package joliex.queryengine.lookup;

import java.io.IOException;
import java.io.StringReader;
import static jolie.js.JsUtils.parseJsonIntoValue;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class LookupQueryTest {
    public static void main(String[] args) throws IOException, FaultException {
        String tkb = "{\n" +
                "  \"ref\": 4,\n" +
                "  \"awards\": [\n" +
                "    {\n" +
                "      \"award\": \"Rosing Prize\",\n" +
                "      \"by\": \"Norwegian Data Association\",\n" +
                "      \"year\": 1999\n" +
                "    },\n" +
                "    {\n" +
                "      \"award\": \"Turing Award\",\n" +
                "      \"by\": \"ACM\",\n" +
                "      \"year\": 2001\n" +
                "    },\n" +
                "    {\n" +
                "      \"award\": \"IEEE John von Neumann Medal\",\n" +
                "      \"by\": \"IEEE\",\n" +
                "      \"year\": 2001\n" +
                "    }\n" +
                "  ],\n" +
                "  \"birth\": \"1926-08-27\",\n" +
                "  \"contributions\": [\n" +
                "    \"OOP\",\n" +
                "    \"Simula\"\n" +
                "  ],\n" +
                "  \"death\": \"2002-08-10\",\n" +
                "  \"name\": {\n" +
                "    \"fisrt\": \"Kristen\",\n" +
                "    \"last\": \"Nyygard\"\n" +
                "  }\n" +
                "}";

        String ttw = "[\n" +
                "  {\n" +
                "    \"p_id\": 4,\n" +
                "    \"name\": \"Rosing Prize\",\n" +
                "    \"in\": \"1999\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"p_id\": 4,\n" +
                "    \"name\": \"Turing Award\",\n" +
                "    \"in\": \"2001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"p_id\": 4,\n" +
                "    \"name\": \"IEEE Medal\",\n" +
                "    \"in\": \"2001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"p_id\": 6,\n" +
                "    \"name\": \"Adv. of FSS\",\n" +
                "    \"in\": \"2001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"p_id\": 6,\n" +
                "    \"name\": \"NLUUG Award\",\n" +
                "    \"in\": \"2003\"\n" +
                "  }\n" +
                "]";

        Value lookupRequest = Value.create();

		Value tkbValue = lookupRequest.getNewChild( "rightData" );
        parseJsonIntoValue( new StringReader( tkb ), tkbValue, false );
        Value leftData = Value.create();
        parseJsonIntoValue( new StringReader( ttw ), leftData, false );
		
		lookupRequest.children().put( "leftData", leftData.getChildren( "_" ) );
        lookupRequest.setFirstChild( "leftPath", "p_id" );
        lookupRequest.setFirstChild( "rightPath", "ref" );
        lookupRequest.setFirstChild( "dstPath", "awards_info" );
		
		System.out.println( lookupRequest.toPrettyString() );

        ValueVector lookup = LookupQuery.lookup(lookupRequest);
        lookup.forEach(it -> System.out.println(it.toPrettyString()));
    }
}