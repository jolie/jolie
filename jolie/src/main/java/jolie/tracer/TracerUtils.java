package jolie.tracer;

import jolie.runtime.expression.Expression;
import jolie.util.Pair;

public final class TracerUtils {

    public enum TracerLevels {
        ALL,
        COMM,
        COMP
    }

    public static String getVarPathString( Pair<Expression,Expression>[] path ) {
        StringBuilder stringBuilder = new StringBuilder();
        for( int p = 0; p < path.length; p++ ) {
            if ( p > 0 ) {
                stringBuilder.append(".");
            }
            stringBuilder.append(path[p].key().evaluate().strValue());
            if ( path[p].value() != null ) {
                stringBuilder.append("[").append(path[p].value().evaluate().strValue()).append("]");
            }
        }
        return stringBuilder.toString();
    }
}
