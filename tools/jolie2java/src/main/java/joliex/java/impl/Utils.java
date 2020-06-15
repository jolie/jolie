package joliex.java.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

public class Utils {
	public static boolean hasSubTypes( TypeDefinition t ) {
		if( t instanceof TypeDefinitionLink ) {
			return hasSubTypes( ((TypeDefinitionLink) t).linkedType() );
		} else if( t instanceof TypeInlineDefinition ) {
			return ((TypeInlineDefinition) t).hasSubTypes();
		} else if( t instanceof TypeChoiceDefinition ) {
			return (hasSubTypes( ((TypeChoiceDefinition) t).left() )
				|| hasSubTypes( ((TypeChoiceDefinition) t).right() ));
		} else {
			throw new UnsupportedOperationException( "Unsupported type class: " + t.getClass().getName() );
		}
	}

	public static Set< Map.Entry< String, TypeDefinition > > subTypes( TypeDefinition t ) {
		if( t instanceof TypeDefinitionLink ) {
			return subTypes( ((TypeDefinitionLink) t).linkedType() );
		} else if( t instanceof TypeInlineDefinition ) {
			return ((TypeInlineDefinition) t).subTypes();
		} else if( t instanceof TypeChoiceDefinition ) {
			Set< Map.Entry< String, TypeDefinition > > leftEntries = subTypes( ((TypeChoiceDefinition) t).left() );
			Set< Map.Entry< String, TypeDefinition > > rightEntries = subTypes( ((TypeChoiceDefinition) t).right() );
			HashMap< String, TypeDefinition > choiceEntries = new HashMap<>();
			if( leftEntries != null ) {
				leftEntries.stream()
					.forEach( e -> {
						choiceEntries.put( e.getKey(), e.getValue() );
					} );
			}
			if( rightEntries != null ) {
				rightEntries.stream()
					.forEach( e -> {
						if( !choiceEntries.containsKey( e.getKey() ) ) {
							choiceEntries.put( e.getKey(), e.getValue() );
						} else {
							choiceEntries.put( e.getKey() + "_", e.getValue() );
						}
					} );
			}
			return choiceEntries.entrySet();
		} else {
			throw new UnsupportedOperationException( "Unsupported type class: " + t.getClass().getName() );
		}
	}

	public static NativeType nativeType( TypeDefinition t ) {
		if( t instanceof TypeDefinitionLink ) {
			return nativeType( ((TypeDefinitionLink) t).linkedType() );
		} else if( t instanceof TypeInlineDefinition ) {
			return ((TypeInlineDefinition) t).nativeType();
		} else if( t instanceof TypeChoiceDefinition ) {
			return NativeType.ANY;
		} else {
			throw new UnsupportedOperationException( "Unsupported type class: " + t.getClass().getName() );
		}

	}

	public static Set< NativeType > getTypes( TypeDefinition typeDefinition ) {
		Set< NativeType > choiceTypes = new HashSet<>();
		if( typeDefinition instanceof TypeChoiceDefinition ) {
			choiceTypes = getTypes( ((TypeChoiceDefinition) typeDefinition).left() );
			Set< NativeType > right = getTypes( ((TypeChoiceDefinition) typeDefinition).right() );
			if( right != null ) {
				choiceTypes.addAll( right );
			}
		} else if( typeDefinition instanceof TypeDefinitionLink ) {
			return getTypes( ((TypeDefinitionLink) typeDefinition).linkedType() );
		} else if( typeDefinition instanceof TypeInlineDefinition ) {
			choiceTypes.add( ((TypeInlineDefinition) typeDefinition).nativeType() );
		}
		return choiceTypes;
	}
}
