package joliex.queryengine.lookup;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.match.MatchQuery;
import joliex.queryengine.project.ProjectQuery;

import java.util.Optional;

public final class LookupQuery {
    private static class RequestType {

        private static final String DATA = "data";
        private static final String QUERY = "query";
        private static final String LEFT_DATA = "leftData";
        private static final String RIGHT_DATA = "rightData";
        private static final String PATH = "path";
        private static final String LEFT_PATH = "leftPath";
        private static final String RIGHT_PATH = "rightPath";
        private static final String DST_PATH = "dstPath";
        private static final String VALUE = "value";
        private static final String EQUAL = "equal";
    }

    public static Value lookup(Value lookupRequest) throws FaultException {
        ValueVector leftData = lookupRequest.getChildren(RequestType.LEFT_DATA);
        ValueVector rightData = lookupRequest.getChildren(RequestType.RIGHT_DATA);

        Value leftPath = lookupRequest.getFirstChild(RequestType.LEFT_PATH);
        Value rightPath = lookupRequest.getFirstChild(RequestType.RIGHT_PATH);
        Value dstPath = lookupRequest.getFirstChild(RequestType.DST_PATH);

        Value result = Value.create();

        for (Value leftValue : leftData) {
            Value beta = Value.create();

            Path path = Path.parsePath(leftPath.strValue());
            Optional<ValueVector> values = path.apply(leftValue);
            if (values.isPresent()) {
                beta = MatchQuery.match(createMatchEqRequest(rightData, rightPath, values.get().first()));
            }

            //else beta is empty. Do I need to express it somehow?
            Value projectRequest = createProjectRequest(leftData, beta, dstPath);
            result.add(ProjectQuery.project(projectRequest));
        }

        return result;
    }

    private static Value createProjectRequest(ValueVector leftData, Value beta, Value dstPath) {
        Value valuesToPath = Value.create();
        valuesToPath.getNewChild(RequestType.DST_PATH).setValue(dstPath);
        valuesToPath.getNewChild(RequestType.VALUE).setValue(beta);

        ValueVector projectExpression = ValueVector.create();
        projectExpression.add(valuesToPath);
        //how to add all others fields from leftData

        Value projectRequest = Value.create();
        projectRequest.getNewChild(RequestType.DATA).setValue(leftData);
        projectRequest.getNewChild(RequestType.QUERY).setValue(projectExpression);

        return projectRequest;
    }

    private static Value createMatchEqRequest(ValueVector rightData, Value rightPath, Value externalPath) {
        Value matchRequest = Value.create();
        Value matchQuery = Value.create();
        ValueVector equal = ValueVector.create();
        Value path = Value.create();
        Value value = Value.create();

        path.getNewChild(RequestType.PATH).setValue(rightPath.strValue()); //I doubt that it should work like this
        value.getNewChild(RequestType.VALUE).setValue(externalPath.strValue()); //I doubt that it should work like this

        matchQuery.getNewChild(RequestType.EQUAL).setValue(equal);

        matchRequest.getNewChild(RequestType.DATA).setValue(rightData);
        matchRequest.getNewChild(RequestType.QUERY).setValue(matchQuery);


        return matchRequest;
    }

}