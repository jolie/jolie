package jolie.lang.nativeTypes;

/**
 * Created by lsafina on 13/02/16.
 */
public class LongType extends NativeType {

    public LongType() {
        type = NativeTypeEnum.LONG;
    }

    public LongType(String refinement) {
        type = NativeTypeEnum.LONG;
        this.refinement = refinement;
    }
}
