package joliex.queryengine.lookup;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.common.TQueryExpression;
import joliex.queryengine.common.Utils;
import joliex.queryengine.match.EqualExpression;
import joliex.queryengine.project.ValueToPathProjectExpression;

import java.util.Optional;

public final class LookupQuery {
    private static class RequestType {

        private static final String LEFT_DATA = "leftData";
        private static final String RIGHT_DATA = "rightData";
        private static final String LEFT_PATH = "leftPath";
        private static final String RIGHT_PATH = "rightPath";
        private static final String DST_PATH = "dstPath";

    }

    public static ValueVector lookup(Value lookupRequest) throws FaultException {
        ValueVector leftData = lookupRequest.getChildren(RequestType.LEFT_DATA);
        ValueVector rightData = lookupRequest.getChildren(RequestType.RIGHT_DATA);

        Value leftPath = lookupRequest.getFirstChild(RequestType.LEFT_PATH);
        Value rightPath = lookupRequest.getFirstChild(RequestType.RIGHT_PATH);
        Value dstPath = lookupRequest.getFirstChild(RequestType.DST_PATH);

        ValueVector result = ValueVector.create();

        for (Value leftValue : leftData) {
            ValueVector responseVector = ValueVector.create();

            Path path = Path.parsePath(leftPath.strValue());
            Optional<ValueVector> values = path.apply(leftValue);
            if (values.isPresent()) {
                EqualExpression v = new EqualExpression(Path.parsePath(rightPath.strValue()), values.get());
                boolean[] mask = v.applyOn(rightData);
                for ( int i = 0; i < mask.length; i++ ) {
                    if ( mask[i] ) {
                        responseVector.add( values.get().get(i));
                    }
                }

                TQueryExpression beta = new ValueToPathProjectExpression(dstPath.strValue(), responseVector);
                Value value = beta.applyOn(leftValue);

                result.add(Utils.merge(leftValue, value));
            }

        }

        return result;
    }

}