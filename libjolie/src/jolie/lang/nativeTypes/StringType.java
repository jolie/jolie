package jolie.lang.nativeTypes;

import java.util.regex.Pattern;

/**
 * Created by lsafina on 13/02/16.
 */
public class StringType extends NativeType {

    public StringType() {
        super(NativeTypeEnum.STRING);
    }

    Pattern refinement;

    public StringType(String refinement) {
        super(NativeTypeEnum.STRING);
        this.refinement = Pattern.compile(refinement);
    }

    public Pattern getRefinement() {
        return refinement;
    }

    public void setRefinement(String refinement) {
        this.refinement = Pattern.compile(refinement);
    }
}
