


package jolie.lang.parse.ast;

import java.util.Arrays;
import java.util.List;
import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;

/**
 * A class for holding the information of an import statement in jolie.
 */
public class ImportStatement extends OLSyntaxNode
{

    private static final long serialVersionUID = Constants.serialVersionUID();
    private final ImportSymbolTarget[] importSymbolTargets;
    private final String[] importTarget;
    private final boolean isNamespaceImport;

    /**
     * Constructor for namespace import
     * 
     * @param context      parsing context
     * @param importTarget tokenized import target, empty denote a dot token (
     *                     import target for .A.B should give this field ["", "A",
     *                     "", "B"])
     */
    public ImportStatement( ParsingContext context, String[] importTarget )
    {
        this( context, importTarget, true, null );
    }

    /**
     * Constructor for qualified import
     * 
     * @param context        a parsing context
     * @param importTarget[] tokenized import target, empty denote a dot token (
     *                       import target for .A.B should give this field ["", "A",
     *                       "", "B"])
     */
    public ImportStatement( ParsingContext context, String[] importTarget,
            List< Pair< String, String > > pathNodes )
    {
        this( context, importTarget, false, pathNodes );
    }

    public ImportStatement( ParsingContext context, String[] importTarget,
            boolean isNamespaceImport, List< Pair< String, String > > pathNodes )
    {
        super( context );
        this.importTarget = importTarget;
        this.isNamespaceImport = isNamespaceImport;
        if ( pathNodes != null ) {
            importSymbolTargets = new ImportSymbolTarget[pathNodes.size()];
            for (int i = 0; i < pathNodes.size(); i++) {
                importSymbolTargets[i] = new ImportSymbolTarget( pathNodes.get( i ).key(),
                        pathNodes.get( i ).value() );
            }
        } else {
            importSymbolTargets = null;
        }
    }

    public String[] importTarget()
    {
        return importTarget;
    }

    public boolean isNamespaceImport()
    {
        return isNamespaceImport;
    }

    public ImportSymbolTarget[] importSymbolTargets()
    {
        return importSymbolTargets;
    }

    @Override
    public String toString()
    {
        String importIDs = (this.isNamespaceImport) ? "*"
                : Arrays.toString( this.importSymbolTargets );
        return "from " + Arrays.toString( this.importTarget ) + " import " + importIDs;
    }

    // ImportStatement is resolved at OLParser.parse(), thus this function is
    // unused.
    @Override
    public void accept( OLVisitor visitor )
    {
    }
}
