package joliex.queryengine;

import java.util.ArrayList;
import java.util.Arrays;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public final class Utils {
    private Utils() {}

    static ValueVector evaluatePath(Value data, String path) {
        ArrayList<String> paths = new ArrayList<>(Arrays.asList(path.split("\\.")));
        return evaluatePathRec(data, paths, ValueVector.create());
    }

    private static ValueVector evaluatePathRec(Value data, ArrayList<String> paths, ValueVector values) {
        ValueVector children = data.getChildren(paths.remove(0));
        if (paths.isEmpty()) {
            for (Value child : children) {
                if (child.hasChildren())
                    //for situations when data="awards.award", path="awards"
                    return ValueVector.create();
                else values.add(child);
            }
        } else {
            children.forEach(child -> evaluatePathRec(child, paths, values));
        }
        return values;
    }

    static ValueVector unwindPath(Value data, String path){
        ArrayList<String> pathList = new ArrayList<>(Arrays.asList(path.split("\\.")));
        return unwindPathRec(data, pathList);
    }

    private static ValueVector unwindPathRec(Value data, ArrayList<String> pathList) {
        String head = pathList.remove(0);
        ValueVector children = data.getChildren(head);
        ValueVector values = ValueVector.create();
        if (pathList.isEmpty()){
            for (Value child: children )values.add(flatChildren(data, head, child));
        } else {
            for (Value child: children){
                ValueVector unwinded = unwindPathRec(child, pathList);
                for (Value value: unwinded) values.add(flatChildren(data, head, value));
            }
        }

        return values;
    }

    private static Value flatChildren(Value data, String head, Value child) {
        Value dataCopy = Value.create();
        dataCopy.deepCopy(data);
        dataCopy.children().remove(head);
        ValueVector newChild = ValueVector.create();
        newChild.add(child);
        dataCopy.children().put(head, newChild);
        return dataCopy;
    }

    static boolean isGreater(Value first, String second) {
        if (isNumeric(first.strValue()) && isNumeric(second)) {
            return first.doubleValue() > Double.parseDouble(second);
        } else return first.strValue().length() > second.length();
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public static class Constants {
        public static String right = "right";
        public static String left = "left";
        public static String and = "and";
        public static String or = "or";
        public static String not = "not";
        public static String equal = "equal";
        public static String greaterThen = "greaterThen";
        public static String lowerThen = "lowerThen";
        public static String exists = "exists";
        public static String path = "path";
        public static String val = "value";
    }
}

/*private static ValueVector unwindPathRec(Value data, Value copy, ArrayList<String> pathList) {
        String head = pathList.remove(0);
        ValueVector children = data.getChildren(head);
        switch (children.size()){
            case 0: {
                return ValueVector.create();
            }
            case 1: {
                return unwindPathRec(children.first(), copy, pathList);
            }
            default: {
                for (Value child: children){

                }

            }
        }

    }*/
