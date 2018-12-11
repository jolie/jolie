package joliex.queryengine;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public final class UnwindQuery {

    public static ValueVector unwind(Value unwindRequest) {
        String query = unwindRequest.getChildren("query").first().strValue();
        Value data = unwindRequest.getChildren("data").first();

        return Utils.unwindPath(data, query);
    }
}
