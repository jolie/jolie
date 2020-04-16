
package jolie.lang.parse.ast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;

/**
 * A class for holding the information of an import statement in jolie.
 */
public class ImportStatement extends OLSyntaxNode {
    /**
     * A class for holding information of symbol target
     */
    public class ImportSymbolTarget {
        String moduleSymbol;
        String localSymbol;

        /**
         * @param moduleSymbol a symbol of targeting module
         * @param localSymbol  a symbol of to place in local exeuction
         */
        public ImportSymbolTarget(String moduleSymbol, String localSymbol) {
            this.moduleSymbol = moduleSymbol;
            this.localSymbol = localSymbol;
        }

        @Override
        public String toString() {
            if (this.moduleSymbol.equals(this.localSymbol)) {
                return this.moduleSymbol;
            }
            return this.moduleSymbol + "as" + this.localSymbol;
        }

    }

    private static final long serialVersionUID = Constants.serialVersionUID();
    private final Map<String, ImportSymbolTarget> importSymbolTargets;
    private final String[] importTarget;
    private final boolean isNamespaceImport;

    /**
     * Constructor for namespace import
     * 
     * @param context      a parsing context
     * @param importTarget importing target
     */
    public ImportStatement(ParsingContext context, String[] importTarget) {
        this(context, importTarget, true, null);
    }

    /**
     * Constructor for qualified import
     * 
     * @param context      a parsing context
     * @param importTarget importing target
     */
    public ImportStatement(ParsingContext context, String[] importTarget, List<Pair<String, String>> pathNodes) {
        this(context, importTarget, false, pathNodes);
    }

    public ImportStatement(ParsingContext context, String[] importTarget, boolean isNamespaceImport,
            List<Pair<String, String>> pathNodes) {
        super(context);
        this.importTarget = importTarget;
        this.isNamespaceImport = isNamespaceImport;
        importSymbolTargets = new HashMap<>();
        if (pathNodes != null) {
            for (Pair<String, String> node : pathNodes) {
                importSymbolTargets.put(node.key(), new ImportSymbolTarget(node.key(), node.value()));
            }
        }
    }

    public String[] importTarget() {
        return importTarget;
    }

    public boolean isNamespaceImport() {
        return isNamespaceImport;
    }

    @Override
    public String toString() {
        String importIDs = (this.isNamespaceImport) ? "*"
                : Arrays.toString(this.importSymbolTargets.values().toArray());
        return "from " + Arrays.toString(this.importTarget) + " import " + importIDs;
    }

    // ImportStatement is resolved at OLParser.parse(), thus this function is
    // unused.
    @Override
    public void accept(OLVisitor visitor) {
    }
}
