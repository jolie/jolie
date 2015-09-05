package jolie.runtime.typing;

import jolie.lang.NativeType;
import jolie.runtime.Value;
import jolie.util.Range;

import java.util.Map;

public class TypeChoice extends Type {
    public Type left() {
        return left;
    }
    public Type right() {
        return right;
    }

    private final Type left;
    private final Type right;

    public TypeChoice(Type left, Type right) {
        this.right = right;
        this.left = left;
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
        isValid = isCheck(this.left(), value, pathBuilder) || isCheck(this.right(), value, pathBuilder);
        if (!isValid){
            throw new TypeCheckingException( "Invalid types for node " + pathBuilder.toString() + ": expected " + this.left().nativeType() + " or " +  this.right().nativeType() + ", found " + (( value.valueObject() == null ) ? "void" : value.valueObject().getClass().getName()) );
        }
    }

    @Override
    protected Value cast(Value value, StringBuilder pathBuilder) throws TypeCastingException {
        Boolean isValid;
        isValid = isCast(this.left(), value, pathBuilder) || isCast(this.right(), value, pathBuilder);
        if (!isValid){
            throw new TypeCastingException( "Invalid types for node " + pathBuilder.toString() + ": expected " + this.left().nativeType() + " or " +  this.right().nativeType() + ", found " + (( value.valueObject() == null ) ? "void" : value.valueObject().getClass().getName()) );
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
