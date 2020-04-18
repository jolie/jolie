package jolie.lang.parse.module;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.ImportSymbolTarget;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.util.ProgramInspector;

public class ModuleRecord
{

    public enum Status {
        LOADING, LOADED
    }

    private final URI source;
    private ProgramInspector programInspector;
    private Status status;

    public ModuleRecord( URI source )
    {
        this.source = source;
        this.status = Status.LOADING;
    }

    public void loadFinished( ProgramInspector pi ) throws ModuleException
    {
        if ( status == Status.LOADED ) {
            throw new ModuleException( "module " + this.source + " already is loaded." );
        }
        this.programInspector = pi;
        this.status = Status.LOADED;
    }


    private TypeDefinition findType( String id )
    {
        for (TypeDefinition td : programInspector.getTypes()) {
            if ( td.id().equals( id ) ) {
                return td;
            }
        }
        return null;
    }

    private InterfaceDefinition findInterface( String id )
    {
        for (InterfaceDefinition ifaceDef : programInspector.getInterfaces()) {
            if ( ifaceDef.name().equals( id ) ) {
                return ifaceDef;
            }
        }
        return null;
    }

    // private DefinitionNode findProcedureDefinition( String id )
    // {
    // for (DefinitionNode procedureDef : programInspector.getProcedureDefinitions()) {
    // if ( procedureDef.id().equals( id ) ) {
    // return procedureDef;
    // }
    // }
    // return null;
    // }

    // private ServiceNodeParameterize findParamService( String id )
    // {
    // for (ServiceNodeParameterize service : programInspector.getParamServices()) {
    // if ( service.name().equals( id ) ) {
    // return service;
    // }
    // }
    // return null;
    // }


    /**
     * find and resolve dependency of type definitions
     */
    private List< Importable > resolveDependency( TypeDefinition typeDefinition )
    {
        List< Importable > res = new ArrayList<>();
        // resolve linked type
        if ( typeDefinition instanceof TypeDefinitionLink ) {

            String linkedTypeName = ((TypeDefinitionLink) typeDefinition).linkedTypeName();
            res.addAll( List.of( this.findSymbol( linkedTypeName ) ) );

        } else if ( typeDefinition instanceof TypeInlineDefinition ) {
            TypeInlineDefinition inlineImport = ((TypeInlineDefinition) typeDefinition);
            if ( inlineImport.hasSubTypes() ) {
                for (Entry< String, TypeDefinition > subtypeEntry : inlineImport.subTypes()) {
                    if ( subtypeEntry.getValue() instanceof TypeDefinitionLink ) {
                        String linkedTypeName =
                                ((TypeDefinitionLink) subtypeEntry.getValue()).linkedTypeName();
                        res.addAll( List.of( this.findSymbol( linkedTypeName ) ) );
                    }
                }
            }
        }
        return res;
    }

    /**
     * find a symbol and returns list of importable node which contain the modules' symbol node and
     * its' dependencies
     */
    public Importable[] findSymbol( String id )
    {
        List< Importable > res = new ArrayList<>();
        TypeDefinition moduleTypeDef = this.findType( id );

        if ( moduleTypeDef != null ) {
            res.add( moduleTypeDef );
            List< Importable > typeDependencies = this.resolveDependency( moduleTypeDef );
            res.addAll( 0, typeDependencies );
            return res.toArray( new Importable[0] );
        }


        // InterfaceDefinition moduleInterfaceDef = this.findInterface( id );
        // if ( moduleInterfaceDef != null ) {
        // List< Importable > res = new ArrayList< Importable >();
        // for (OperationDeclaration op : moduleInterfaceDef.operationsMap().values()) {
        // if ( op instanceof OneWayOperationDeclaration ) {
        // OneWayOperationDeclaration ow = (OneWayOperationDeclaration) op;
        // if ( !ow.requestType().equals( TypeDefinitionUndefined.getInstance() ) ) {
        // res.add( ow.requestType() );
        // }
        // } else if ( op instanceof RequestResponseOperationDeclaration ) {
        // RequestResponseOperationDeclaration rr =
        // (RequestResponseOperationDeclaration) op;
        // if ( !rr.requestType().equals( TypeDefinitionUndefined.getInstance() ) ) {
        // res.add( rr.requestType() );
        // }
        // if ( !rr.responseType().equals( TypeDefinitionUndefined.getInstance() ) ) {
        // res.add( rr.responseType() );
        // }
        // }
        // }
        // res.add( moduleInterfaceDef );
        // return res.toArray( new Importable[0] );
        // }
        // DefinitionNode moduleProcedureDef = this.findProcedureDefinition( id );
        // if ( moduleProcedureDef != null ) {
        // return new Importable[] {moduleProcedureDef};
        // }
        // ServiceNode moduleService = this.findService( id );
        // if ( moduleService != null ) {
        // return new Importable[] {moduleService};
        // }
        // // ServiceNodeParameterize service = this.findParamService( id );
        // // if ( service != null ) {
        // // return new Importable[] {service};
        // // }

        return null;
    }

    public ImportResult resolveNameSpace( ParsingContext ctx ) throws ModuleException
    {
        List< ImportSymbolTarget > importPaths = new ArrayList< ImportSymbolTarget >();
        for (TypeDefinition td : this.programInspector.getTypes()) {
            importPaths.add( new ImportSymbolTarget( td.id(), td.id() ) );
        }

        // for (InterfaceDefinition interfaceDef : this.programInspector.getInterfaces()) {
        // importPaths
        // .add( new Pair< String, String >( interfaceDef.name(), interfaceDef.name() ) );
        // }

        // for (DefinitionNode definitionNode : this.programInspector.getProcedureDefinitions()) {
        // importPaths
        // .add( new Pair< String, String >( definitionNode.id(), definitionNode.id() ) );
        // }

        // for (ServiceNode service : this.programInspector.getServices()) {
        // importPaths.add( new Pair< String, String >( service.name(), service.name() ) );
        // }

        // for (ServiceNodeParameterize service : this.programInspector.getParamServices()) {
        // importPaths.add( new Pair< String, String >( service.name(), service.name() ) );
        // }
        return resolve( ctx, importPaths.toArray( new ImportSymbolTarget[0] ) );
    }

    public ImportResult resolve( ParsingContext ctx, ImportSymbolTarget[] importSymbols )
            throws ModuleException
    {
        ImportResult res = new ImportResult();
        for (ImportSymbolTarget importSymbol : importSymbols) {
            Importable[] moduleNodes = this.findSymbol( importSymbol.moduleSymbol() );
            if ( moduleNodes == null ) {
                throw new ModuleException( "unable to find " + importSymbol.moduleSymbol() + " in "
                        + this.programInspector.getSources().toString() );
            }
            for (Importable moduleNode : moduleNodes) {
                OLSyntaxNode node = null;

                // resolve import target symbol and it's dependency
                if ( moduleNode.name().equals( importSymbol.moduleSymbol() ) ) {
                    node = moduleNode.resolve( ctx, importSymbol.localSymbol() );
                } else {
                    // resolve dependency
                    node = moduleNode.resolve( ctx, moduleNode.name() );
                }

                res.addNode( node );
            }
        }

        return res;
    }


    public Status status()
    {
        return this.status;
    }

    /**
     * @return the source of imported module
     */
    public URI source()
    {
        return source;
    }

}
