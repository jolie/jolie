package jolie.lang.nativeTypes;

/**
 * Created by lsafina on 13/02/16.
 */
public class StringType extends NativeType {

    public StringType() {
        type = NativeTypeEnum.STRING;
    }

    public StringType(String refinement) {
        type = NativeTypeEnum.STRING;
        this.refinement = refinement;
    }
}
