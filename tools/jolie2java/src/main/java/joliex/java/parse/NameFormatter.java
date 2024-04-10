package joliex.java.parse;

import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.lang.model.SourceVersion;

import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;

import org.apache.commons.lang3.StringUtils;

public class NameFormatter {

    private static final Logger LOGGER = Logger.getLogger( NameFormatter.class.getName() );

    private static final Set<String> RESERVED_NAMES = Set.of(
        "content",
        "children",
        "construct",
        "constructList"
    );

    private static final Set<String> CONFLICTING_TYPES = Set.of(
        "Value",
        "ValueVector",
        "TypeCheckingException",
        "ArrayList",
        "Map",
        "SequencedCollection",
        "List",
        "Optional",
        "Function",
        "Predicate",
        "UnaryOperator",
        "BinaryOperator",
        "Stream",
        "Collectors",
        "JolieValue",
        "JolieNative",
        "JolieVoid",
        "Void",
        "JolieBool",
        "Boolean",
        "JolieInt",
        "Integer",
        "JolieLong",
        "Long",
        "JolieDouble",
        "Double",
        "JolieString",
        "String",
        "JolieRaw",
        "ByteArray",
        "ImmutableStructure",
        "StructureBuilder",
        "StructureListBuilder",
        "UntypedBuilder",
        "Builder",
        "InlineBuilder",
        "NestedBuilder",
        "ListBuilder",
        "InlineListBuilder",
        "NestedListBuilder",
        "FieldManager",
        "Refinement",
        "TypeValidationException",
        "ConversionFunction"
    );

    private static final String REGEN_MSG = "As it wasn't possible to assign it a name, the field will not be accessible in a type-safe way, and it is therefore recommended to resolve the naming issues and then generate the classes again.";

    public static String getFieldName( String formattedName, String originalName, boolean isUnique ) {
        if ( formattedName.isEmpty() ) {
            LOGGER.warning( "Removing reserved characters from the field \"" + originalName + "\" left it blank and it was therefore not possible to assign it a name. " + REGEN_MSG );
            return null;
        }
        if ( !isUnique && !originalName.equals( formattedName ) ) {
            LOGGER.warning( "Removing reserved characters from the field \"" + originalName + "\", resulting in the name: \"" + formattedName + "\", created a conflict with another field's name and it was therefore not possible to assign it a name. " + REGEN_MSG );
            return null;
        }
        if ( SourceVersion.isKeyword( formattedName ) || RESERVED_NAMES.contains( formattedName ) ) {
            LOGGER.warning( "Attempted to give the field \"" + originalName + "\" the name: \"" + formattedName + "\", but said name conflicts with a reserved name and it was therefore not possible to assign it a name. " + REGEN_MSG );
            return null;
        }

        return formattedName;
    }

    public static String getTypeClassName( String name ) {
        final String formattedName = StringUtils.capitalize( name );
        
        if ( CONFLICTING_TYPES.contains( formattedName ) ) {
            //logger.info( "Trying to create a class for the type \"" + name + "\", with name \"" + formattedName + "\", would create a naming conflict and the class has therefore been renamed to \"Custom" + formattedName + "\"." );
            return "Custom" + formattedName;
        }

        if ( formattedName.matches( "\\w\\d*" ) ) { 
            //logger.info( "Trying to create a class for the type \"" + name + "\", with name \"" + formattedName + "\", would create a potential naming conflict and the class has therefore been renamed to \"" + formattedName + "Type\"." );
            return formattedName + "Type";
        }

        if ( formattedName.startsWith( "Custom" ) )
            LOGGER.warning( "Having types with names like \"" + name + "\", which is given the class name \"" + formattedName + "\", that has \"Custom\" as a prefix is not recommended, as this prefix is used to handle naming conflicts." );

        return formattedName;
    }

    public static String getFaultClassName( String name, JolieType type, boolean isUnique ) {
        final String formattedName = StringUtils.capitalize( name );

        if ( isUnique && !CONFLICTING_TYPES.contains( formattedName ) )
            return formattedName;
            
        return formattedName + switch ( type ) {
            case Native n -> n.toString();
            case Undefined u -> "Undefined";
            case Definition d -> d.name();
        };
    }

    public static String cleanFieldName( String name ) { return StringUtils.uncapitalize( name.replaceAll( "\\W", "" ) ); }

    public static Supplier<String> typeClassNameSupplier( String name ) { return () -> getTypeClassName( name ); }
}
