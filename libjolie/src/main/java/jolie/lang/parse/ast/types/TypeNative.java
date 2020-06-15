package jolie.lang.parse.ast.types;

import jolie.lang.NativeType;

public class TypeNative {

	private final NativeType nativeType;
	private TypeNativeConstraint nativeTypeConstraint = new TypeNativeUnconstrained();

	public TypeNative( NativeType nativeType ) {
		this.nativeType = nativeType;
	}

	public TypeNative( NativeType nativeType, TypeNativeConstraint nativeTypeConstraint ) {
		this.nativeType = nativeType;
		this.nativeTypeConstraint = nativeTypeConstraint;
	}

	public NativeType nativeType() {
		return nativeType;
	}

	public TypeNativeConstraint nativeTypeConstraint() {
		return nativeTypeConstraint;
	}

	public boolean checkTypeNativeEqualness( TypeNative typeNative ) {
		if( this.nativeType != typeNative.nativeType() ) {
			return false;
		} else {
			return nativeTypeConstraint.checkEqualness( typeNative.nativeTypeConstraint() );
		}

	}
}
