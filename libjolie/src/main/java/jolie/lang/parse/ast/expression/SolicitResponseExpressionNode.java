package jolie.lang.parse.ast.expression;
// import jolie.lang.parse.OLVisitor;
// import jolie.lang.parse.context.ParsingContext;
// import java.util.Optional;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;

public class SolicitResponseExpressionNode extends OLSyntaxNode {
    private final String id, outputPortId;
    private final OLSyntaxNode outputExpression;

    public SolicitResponseExpressionNode(
        ParsingContext context, 
        String id, 
        String outputPortId,
        OLSyntaxNode outputExpression ) {
        super(context);
        this.id = id;
        this.outputPortId = outputPortId;
        this.outputExpression = outputExpression;
    }

    public String id() {
        return id;
    }

    public String outputPortId() {
        return outputPortId;
    }

    public OLSyntaxNode outputExpression() {
        return outputExpression;
    }

    @Override
    public <C, R> R accept( OLVisitor<C, R> visitor, C ctx ) {
        return visitor.visit( this, ctx );
    }
    
}
