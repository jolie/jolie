package jolie.lang.parse.ast.types;

import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BasicType {

	private final NativeType nativeType;
	private final List< BasicTypeRefinement > basicTypeRefinementList;
	private static HashMap< NativeType, BasicType > staticBasicTypes = new HashMap<>();


	public BasicType( NativeType nativeType, List< BasicTypeRefinement > basicTypeRefinementList ) {
		this.nativeType = nativeType;
		if( basicTypeRefinementList == null ) {
			this.basicTypeRefinementList = Collections.emptyList();
		} else {
			this.basicTypeRefinementList = basicTypeRefinementList;
		}
	}

	public NativeType nativeType() {
		return nativeType;
	}

	public List< BasicTypeRefinement > basicTypeRefinementList() {
		return basicTypeRefinementList;
	}

	public boolean checkBasicTypeEqualness( BasicType basicType ) {
		boolean returnValue = false;
		if( this.nativeType == basicType.nativeType() ) {
			if( basicTypeRefinementList == null ) {
				return basicType.basicTypeRefinementList() == null;
			} else {
				if( basicTypeRefinementList.size() == basicType.basicTypeRefinementList().size() ) {
					returnValue = basicTypeRefinementList.stream()
						.allMatch( btr -> checkSingleTypeRefinement( btr, basicType.basicTypeRefinementList() ) );
				}
			}
		}
		return returnValue;

	}

	private static boolean checkSingleTypeRefinement( BasicTypeRefinement basicTypeRefinement,
		List< BasicTypeRefinement > targetList ) {
		boolean returnValue = false;
		BasicTypeRefinement foundBasicTypeRefinement = targetList.stream()
			.filter( btr -> btr.getClass().equals( basicTypeRefinement.getClass() ) ).findFirst().get();
		if( foundBasicTypeRefinement != null ) {
			returnValue = basicTypeRefinement.checkEqualness( foundBasicTypeRefinement );
		}
		return returnValue;
	}

	public static BasicType of( NativeType nativeType ) {
		if( staticBasicTypes.containsKey( nativeType ) ) {
			return staticBasicTypes.get( nativeType );
		} else {
			BasicType newStaticBasicType = new BasicType( nativeType, null );
			staticBasicTypes.put( nativeType, newStaticBasicType );
			return newStaticBasicType;
		}
	}
}
