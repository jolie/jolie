package jolie.lang.parse.module;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.ImportSymbolTarget;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.util.ProgramInspector;

public class ModuleRecord
{

    public enum Status {
        LOADING, PARTIAL, LOADED
    }

    private final URI source;
    private ProgramInspector programInspector;
    private Status status;

    public ModuleRecord( URI source )
    {
        this.source = source;
        this.status = Status.LOADING;
    }

    public void loadPartial()
    {
        this.status = Status.PARTIAL;
    }

    public void loadFinished()
    {
        this.status = Status.LOADED;
    }

    public void setInspector( ProgramInspector pi ) throws ModuleException
    {
        if ( status == Status.LOADED ) {
            throw new ModuleException( "module " + this.source + " already is loaded." );
        }
        this.programInspector = pi;
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

    private DefinitionNode findProcedureDefinition( String id )
    {
        for (DefinitionNode procedureDef : programInspector.getProcedureDefinitions()) {
            if ( procedureDef.id().equals( id ) ) {
                return procedureDef;
            }
        }
        return null;
    }


    /**
     * find and resolve dependency of TypeDefinition
     */
    private List< Importable > resolveDependency( Set< String > parentSymbols,
            TypeDefinition typeDefinition ) throws ModuleException
    {
        List< Importable > res = new ArrayList<>();
        // resolve linked type
        if ( typeDefinition instanceof TypeDefinitionLink ) {

            String linkedTypeName = ((TypeDefinitionLink) typeDefinition).linkedTypeName();
            res.addAll( Arrays.asList( this.findSymbol( linkedTypeName ) ) );

        } else if ( typeDefinition instanceof TypeInlineDefinition ) {
            TypeInlineDefinition inlineImport = ((TypeInlineDefinition) typeDefinition);
            if ( inlineImport.hasSubTypes() ) {
                for (Entry< String, TypeDefinition > subtypeEntry : inlineImport.subTypes()) {
                    if ( subtypeEntry.getValue() instanceof TypeDefinitionLink ) {
                        String linkedTypeName =
                                ((TypeDefinitionLink) subtypeEntry.getValue()).linkedTypeName();
                        if ( parentSymbols.contains( linkedTypeName ) ) {
                            throw new ModuleException( "cyclic dependency detected: symbol "
                                    + typeDefinition.id() + " looking for " + linkedTypeName );
                        } else {
                            parentSymbols.add(typeDefinition.id());
                            res.addAll(
                                    Arrays.asList( this.findSymbol( parentSymbols, linkedTypeName ) ) );
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * find and resolve dependency of InterfaceDefinition
     */
    private List< Importable > resolveDependency( InterfaceDefinition interfaceDefinition )
            throws ModuleException
    {
        List< Importable > res = new ArrayList<>();
        for (OperationDeclaration op : interfaceDefinition.operationsMap().values()) {
            if ( op instanceof OneWayOperationDeclaration ) {
                OneWayOperationDeclaration ow = (OneWayOperationDeclaration) op;
                if ( !ow.requestType().id().equals( TypeDefinitionUndefined.UNDEFINED_KEYWORD ) ) {
                    res.addAll( Arrays.asList( this.findSymbol( ow.requestType().id() ) ) );
                }
            } else if ( op instanceof RequestResponseOperationDeclaration ) {
                RequestResponseOperationDeclaration rr = (RequestResponseOperationDeclaration) op;
                if ( !rr.requestType().id().equals( TypeDefinitionUndefined.UNDEFINED_KEYWORD ) ) {
                    res.addAll( Arrays.asList( this.findSymbol( rr.requestType().id() ) ) );
                }
                if ( !rr.responseType().id().equals( TypeDefinitionUndefined.UNDEFINED_KEYWORD ) ) {
                    res.addAll( Arrays.asList( this.findSymbol( rr.responseType().id() ) ) );
                }
                for (TypeDefinition fault : rr.faults().values()) {
                    if ( !fault.id().equals( TypeDefinitionUndefined.UNDEFINED_KEYWORD ) ) {
                        res.addAll( Arrays.asList( this.findSymbol( fault.id() ) ) );
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
    public Importable[] findSymbol( String id ) throws ModuleException
    {
        return this.findSymbol( new HashSet< String >(), id );
    }

    private Importable[] findSymbol( Set< String > parentSymbols, String id ) throws ModuleException
    {
        List< Importable > res = new ArrayList<>();
        TypeDefinition moduleTypeDef = this.findType( id );

        if ( moduleTypeDef != null ) {
            res.add( moduleTypeDef );
            List< Importable > dependency = this.resolveDependency( parentSymbols, moduleTypeDef );
            res.addAll( 0, dependency );
        }

        InterfaceDefinition moduleInterfaceDef = this.findInterface( id );
        if ( moduleInterfaceDef != null ) {
            res.add( moduleInterfaceDef );
            List< Importable > dependency = this.resolveDependency( moduleInterfaceDef );
            res.addAll( 0, dependency );
        }


        DefinitionNode moduleProcedureDef = this.findProcedureDefinition( id );
        if ( moduleProcedureDef != null ) {
            return new Importable[] {moduleProcedureDef};
        }

        return res.toArray( new Importable[0] );
    }

    public ImportResult resolveNameSpace( ParsingContext ctx ) throws ModuleException
    {
        List< ImportSymbolTarget > importPaths = new ArrayList< ImportSymbolTarget >();
        for (TypeDefinition td : this.programInspector.getTypes()) {
            importPaths.add( new ImportSymbolTarget( td.id(), td.id() ) );
        }

        for (InterfaceDefinition interfaceDef : this.programInspector.getInterfaces()) {
            importPaths.add( new ImportSymbolTarget( interfaceDef.name(), interfaceDef.name() ) );
        }

        for (DefinitionNode definitionNode : this.programInspector.getProcedureDefinitions()) {
            importPaths.add( new ImportSymbolTarget( definitionNode.id(), definitionNode.id() ) );
        }

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
        this.loadFinished();

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
