package joliex.openapi.impl.support;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;


import java.util.ArrayList;
import java.util.Map;

public class OperationRestDescriptor {
    private String path;
    private String method;
    private ArrayList<String> inPath = new ArrayList<>();
    private ArrayList<String> inQuery = new ArrayList<>();
    private ArrayList<String> inHeader = new ArrayList<>();

    public OperationRestDescriptor(Value v) {

        method = v.getFirstChild("method").strValue();
        String temporaryString = v.getFirstChild("template").strValue();
        String[] splitResult = temporaryString.split("[?]");
        path = splitResult[0];
        //no query parameters
        String[] splitResultInPath = splitResult[0].split("[/]\\w+");
        for (int counter = 0; counter < splitResultInPath.length; counter++) {
            if (splitResultInPath[counter].matches("[/][{]\\w+[}]")) {
                inPath.add(splitResultInPath[counter].replaceAll("[/{}]", ""));
            }

        }

        //query parameters
        if (splitResult.length == 2) {

            String[] splitResultInQuery = splitResult[1].split("[&]");
            for (int counter = 0; counter < splitResultInQuery.length; counter++) {
                inQuery.add(splitResultInQuery[counter].split("[=]")[1].replaceAll("[{}]", ""));
                System.out.println(splitResultInQuery[counter].split("[=]")[1].replaceAll("[{}]", ""));
            }
        }

        //header
        if (v.hasChildren("incomingHeaderMapping")) {
            Map<String, ValueVector> hearderMapperMap = v.getFirstChild("incomingHeaderMapping").children();
            hearderMapperMap.forEach((s, values) -> {

                inHeader.add(values.get(0).strValue());
            });
        }
    }

    public String method (){
        return method;
    }

    public String path() {
        return this.path;
    }

    public boolean isInPath(String parameter) {

        return inPath.contains(parameter);

    }


    public boolean isInHeader(String parameter) {

        return inHeader.contains(parameter);

    }

    public boolean isInQuery(String parameter) {
        return inPath.contains(parameter);
    }


}
