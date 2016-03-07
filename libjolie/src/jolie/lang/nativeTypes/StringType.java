package jolie.lang.nativeTypes;

import java.util.regex.Pattern;

/**
 * Created by lsafina on 13/02/16.
 */
public class StringType extends NativeType {

    public StringType() {
        super(NativeTypeEnum.STRING);
    }

    private Pattern refinement;

    public Pattern getRefinement() {
        return refinement;
    }

    public void setRefinement(String refinement) {
        this.refinement = Pattern.compile(refinement);
    }
}
