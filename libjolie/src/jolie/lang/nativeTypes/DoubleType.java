package jolie.lang.nativeTypes;

/**
 * Created by lsafina on 13/02/16.
 */
public class DoubleType extends NativeType {

    public DoubleType() {
        type = NativeTypeEnum.DOUBLE;
    }

    public DoubleType(String refinement) {
        type = NativeTypeEnum.DOUBLE;
        this.refinement = refinement;
    }
}
