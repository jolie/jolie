package jolie.runtime.typing;

import jolie.lang.NativeType;
import jolie.runtime.Value;
import jolie.util.Range;

import java.util.Map;

/**
 * Created by User on 01.08.2015.
 */
public class TypePair extends Type {
    public Type getT1() {
        return t1;
    }
    public Type getT2() {
        return t2;
    }

    private final Type t1;
    private final Type t2;

    public TypePair(Type t1, Type t2) {
        this.t2 = t2;
        this.t1 = t1;
    }

    @Override
    public void cutChildrenFromValue(Value value) {

    }

    @Override
    public NativeType nativeType() {
        return null;
    }

    @Override
    public Range cardinality() {
        return null;
    }

    @Override
    public Map<String, Type> subTypes() {
        return null;
    }

    @Override
    protected void check(Value value, StringBuilder pathBuilder) throws TypeCheckingException {
        Boolean isValid;
        isValid = isCheck(this.getT1(), value, pathBuilder) || isCheck(this.getT2(), value, pathBuilder);
        if (!isValid){
            throw new TypeCheckingException( "Invalid types for node " + pathBuilder.toString() + ": expected " + this.getT1().nativeType() + " or " +  this.getT2().nativeType() + ", found " + (( value.valueObject() == null ) ? "void" : value.valueObject().getClass().getName()) );
        }
    }

    @Override
    protected Value cast(Value value, StringBuilder pathBuilder) throws TypeCastingException {
        Boolean isValid;
        isValid = isCast(this.getT1(), value, pathBuilder) || isCast(this.getT2(), value, pathBuilder);
        if (!isValid){
            throw new TypeCastingException( "Invalid types for node " + pathBuilder.toString() + ": expected " + this.getT1().nativeType() + " or " +  this.getT2().nativeType() + ", found " + (( value.valueObject() == null ) ? "void" : value.valueObject().getClass().getName()) );
        }
        return value;
    }

    protected Boolean isCheck(Type type, Value value, StringBuilder pathbuilder){
        try{
            type.check(value, pathbuilder);
        } catch (TypeCheckingException e){
            return false;
        }
        return true;
    }

    protected Boolean isCast(Type type, Value value, StringBuilder pathbuilder){
        try{
            type.cast(value, pathbuilder);
        } catch (TypeCastingException e) {
            return false;
        }
        return true;
    }
}
