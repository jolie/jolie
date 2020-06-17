package jolie.lang.parse.ast.types;

import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicType {

	private final NativeType nativeType;
	private List<BasicTypeRefinement> basicTypeRefinementList = Collections.emptyList();


	public BasicType( NativeType nativeType ) {
		this.nativeType = nativeType;
	}

	public NativeType nativeType() {
		return nativeType;
	}

	public List< BasicTypeRefinement > basicTypeRefinementList() {
		return basicTypeRefinementList;
	}

	public void addBasicTypeRefinement( BasicTypeRefinement basicTypeRefinement ) {
		if( basicTypeRefinementList.size() == 0 ) {
			basicTypeRefinementList = new ArrayList<>();
		}
		basicTypeRefinementList.add( basicTypeRefinement );
	}

	public boolean checkBasicTypeEqualness( BasicType basicType ) {
		boolean returnValue = false;
		if( this.nativeType == basicType.nativeType() ) {
			if( basicTypeRefinementList.size() == basicType.basicTypeRefinementList().size() ) {
				returnValue = basicTypeRefinementList.stream()
					.allMatch( btr -> checkSingleTypeRefinement( btr, basicType.basicTypeRefinementList() ) );
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
}
