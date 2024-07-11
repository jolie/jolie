package joliex.java.parse.util;

import java.util.HashSet;
import java.util.function.Supplier;
import jolie.lang.parse.ast.types.TypeDefinition;

public class NameSupplier implements Supplier< String > {

	private final Supplier< String > name;
	private final HashSet< String > scope;
	private volatile String cachedName;

	public NameSupplier( Supplier< String > name ) {
		this( name, new HashSet<>() );
	}

	private NameSupplier( Supplier< String > name, HashSet< String > scope ) {
		this.name = name;
		this.scope = scope;
	}

	public NameSupplier resolve( Supplier< String > name ) {
		cacheName(); // ensure scope has been updated
		return new NameSupplier( name, new HashSet<>( scope ) );
	}

	@Override
	public String get() {
		return cachedName != null ? cachedName : cacheName();
	}

	public static NameSupplier from( String name, TypeDefinition typeDefinition ) {
		return new NameSupplier( () -> NameFormatter.getJavaName( name, typeDefinition ) );
	}

	private synchronized String cacheName() {
		if( cachedName != null )
			return cachedName;

		cachedName = name.get();
		if( !scope.add( cachedName ) )
			throw new InvalidNameException( "Class name would hide outer class, qualified name=\""
				+ NameFormatter.qualifiedName( cachedName, scope ) + "\"." );
		return cachedName;
	}
}
