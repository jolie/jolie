package jolie.lang.parse.module;

import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;

public interface Importable
{
    String name();
    OLSyntaxNode resolve( ParsingContext context, String localID );
}
