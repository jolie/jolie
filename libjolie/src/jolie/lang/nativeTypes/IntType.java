package jolie.lang.nativeTypes;

/**
 * Created by lsafina on 13/02/16.
 */
public class IntType extends NativeType {

    public IntType() {
        type = NativeTypeEnum.INT;
    }

    public IntType(String refinement) {
        type = NativeTypeEnum.INT;
        this.refinement = refinement;
    }
}
