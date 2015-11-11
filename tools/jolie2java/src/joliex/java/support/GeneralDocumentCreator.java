package joliex.java.support;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import joliex.java.impl.Utils;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author balint
 */
public abstract class GeneralDocumentCreator
{
	private OutputPortInfo[] outputPortArray;
	private InputPortInfo[] inputPortArray;
	private InterfaceDefinition[] interfacesArray;
	private Set<RequestResponseOperationDeclaration> requestResponseOperationsSet;
	private Set<OneWayOperationDeclaration> oneWayOperationsSet;
	private HashMap<String, InterfaceDefinition> interfaceMap;
	private List<Map<String, TypeDefinition>> typeMap;
	private List<String> filesNameList;
	private GeneralProgramVisitor program;
	private List<treeOLObject> olTree;

	public GeneralDocumentCreator( GeneralProgramVisitor program )
	{
		oneWayOperationsSet = new HashSet<OneWayOperationDeclaration>();
		requestResponseOperationsSet = new HashSet<RequestResponseOperationDeclaration>();
		typeMap = new Vector<Map<String, TypeDefinition>>();
		filesNameList = new Vector<String>();
		interfaceMap = new HashMap<String, InterfaceDefinition>();
		this.program = program;
		//this.program.run();
		PopulateInterfaceLists();
		PopulatePortsLists();
		PopulateOperationsSet();
		PopulateTypesSet();
		PopulateFilesList();
		PopulateOlTree();


	}

	protected List<treeOLObject> GetOlTree()
	{
		return olTree;

	}

	private void PopulateTypesSet()
	{
		List<TypeDefinition> supportTypeDefList;
		String nameFile;
		TypeDefinition supportType;

		for( Iterator i = oneWayOperationsSet.iterator(); i.hasNext(); ) {
			OneWayOperationDeclaration operation = (OneWayOperationDeclaration) i.next();
			ScanTypes( operation.requestType() );
		}
		for( Iterator i = requestResponseOperationsSet.iterator(); i.hasNext(); ) {
			RequestResponseOperationDeclaration operation = (RequestResponseOperationDeclaration) i.next();
			ScanTypes( operation.requestType() );
			ScanTypes( operation.responseType() );

		}


	}

	private void PopulateOlTree()
	{
		olTree = new LinkedList<treeOLObject>();

		InterfaceDefinition supportInterface;
		Map<String, OperationDeclaration> supportMap;
		Iterator<Entry<String, OperationDeclaration>> iMapOp;
		OneWayOperationDeclaration operationOneWay;
		RequestResponseOperationDeclaration operationRequestResponse;

		// OneWayOperationDeclaration operation;
		for( OutputPortInfo OutInfo : outputPortArray ) {
			treeOLObject portSupportOLTreeObject = new treeOLObject( OutInfo, null );
			int counterIn = 0;
			List<InterfaceDefinition> interfaceList = OutInfo.getInterfaceList();
			Iterator iteratorInterfaceList = interfaceList.iterator();
			while( iteratorInterfaceList.hasNext() ) {


				portSupportOLTreeObject.SetLinkedObject( (InterfaceDefinition) iteratorInterfaceList.next() );


			}
			//System.out.print( "numero di int: " + counterIn + "\n" );
			for( int counterInterfaces = 0; counterInterfaces < portSupportOLTreeObject.GetLinkedObjetSize(); counterInterfaces++ ) {
				supportInterface = ((InterfaceDefinition) (portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetOLSyntaxNode()));
				supportMap = supportInterface.operationsMap();
				int con = 0;
				for( iMapOp = supportMap.entrySet().iterator(); iMapOp.hasNext(); ) {
					portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).SetLinkedObject( iMapOp.next().getValue() );
					con++;

				}
				//System.out.print( "numero di op: " + con + "\n" );
			}
			for( int counterInterfaces = 0; counterInterfaces < portSupportOLTreeObject.GetLinkedObjetSize(); counterInterfaces++ ) {
				// System.out.print(portSupportOLTreeObject.GetLinkedObject(counterOperation).GetLinkedObjetSize()+"\n");
				for( int counterOperation = 0; counterOperation < portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObjetSize(); counterOperation++ ) {
					if ( portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetOLSyntaxNode() instanceof OneWayOperationDeclaration ) {
						operationOneWay = (OneWayOperationDeclaration) (portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetOLSyntaxNode());
						portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).SetLinkedObject( operationOneWay.requestType() );
						ScanTypesOlTree( operationOneWay.requestType(), portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetLinkedObject( 0 ) );


					} else {


						operationRequestResponse = (RequestResponseOperationDeclaration) (portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetOLSyntaxNode());


						portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).SetLinkedObject( operationRequestResponse.requestType() );
						ScanTypesOlTree( operationRequestResponse.requestType(), portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetLinkedObject( 0 ) );
						portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).SetLinkedObject( operationRequestResponse.responseType() );
						ScanTypesOlTree( operationRequestResponse.responseType(), portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetLinkedObject( 1 ) );

					}


				}

			}

			olTree.add( portSupportOLTreeObject );

		}
/////Input Port
		for( InputPortInfo inputInfo : inputPortArray ) {
			treeOLObject portSupportOLTreeObject = new treeOLObject( inputInfo, null );
			int counterIn = 0;
			List<InterfaceDefinition> interfaceList = inputInfo.getInterfaceList();
			Iterator iteratorInterfaceList = interfaceList.iterator();
			while( iteratorInterfaceList.hasNext() ) {
			
					portSupportOLTreeObject.SetLinkedObject( (InterfaceDefinition)iteratorInterfaceList.next());
					
			}
		//	System.out.print( "numero di int: " + counterIn + "\n" );
			for( int counterInterfaces = 0; counterInterfaces < portSupportOLTreeObject.GetLinkedObjetSize(); counterInterfaces++ ) {
				supportInterface = ((InterfaceDefinition) (portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetOLSyntaxNode()));
				supportMap = supportInterface.operationsMap();
				int con = 0;
				for( iMapOp = supportMap.entrySet().iterator(); iMapOp.hasNext(); ) {
					portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).SetLinkedObject( iMapOp.next().getValue() );
					con++;

				}
	//			System.out.print( "numero di op: " + con + "\n" );
			}
			for( int counterInterfaces = 0; counterInterfaces < portSupportOLTreeObject.GetLinkedObjetSize(); counterInterfaces++ ) {
				// System.out.print(portSupportOLTreeObject.GetLinkedObject(counterOperation).GetLinkedObjetSize()+"\n");
				for( int counterOperation = 0; counterOperation < portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObjetSize(); counterOperation++ ) {
					if ( portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetOLSyntaxNode() instanceof OneWayOperationDeclaration ) {
						operationOneWay = (OneWayOperationDeclaration) (portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetOLSyntaxNode());
						portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).SetLinkedObject( operationOneWay.requestType() );
						ScanTypesOlTree( operationOneWay.requestType(), portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetLinkedObject( 0 ) );


					} else {
					//	System.out.print( "Here line 134 : " + counterOperation + "\n" );

						operationRequestResponse = (RequestResponseOperationDeclaration) (portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetOLSyntaxNode());


						portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).SetLinkedObject( operationRequestResponse.requestType() );
						// System.out.print( "Operation.id " + operationRequestResponse.id() + "Cou"\n" );

						//System.out.print( "Here line 134 : " + operationRequestResponse.requestType().id() + "\n" );

						//treeOLObject SupportOlObject= new treeOLObject(operationRequestResponse.requestType(),null);
						ScanTypesOlTree( operationRequestResponse.requestType(), portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetLinkedObject( 0 ) );
						//System.out.print( "sono a linea:138\n" );
						//System.out.print( counterInterfaces + " " + counterOperation + "\n" );
						portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).SetLinkedObject( operationRequestResponse.responseType() );
						ScanTypesOlTree( operationRequestResponse.responseType(), portSupportOLTreeObject.GetLinkedObject( counterInterfaces ).GetLinkedObject( counterOperation ).GetLinkedObject( 1 ) );
						//System.out.print( "sono a linea:145\n" );
					}


				}

			}

			olTree.add( portSupportOLTreeObject );

		}

	}

	private void ScanTypesOlTree( TypeDefinition typeDefinition, treeOLObject olObjetTree )
	{
		boolean addFlag;
		addFlag = true;

		if ( typeDefinition instanceof TypeDefinitionLink ) {


			if ( addFlag ) {
				String nameFile = ((TypeDefinitionLink) typeDefinition).linkedType().context().sourceName();
				TypeDefinition supportType = ((TypeDefinitionLink) typeDefinition).linkedType();
				//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );
				//Map<String, TypeDefinition> addingMap = new HashMap<String, TypeDefinition>();
				//addingMap.put( nameFile, supportType );
				//typeMap.add( addingMap );

				olObjetTree.SetLinkedObject( supportType );

				if ( Utils.hasSubTypes(supportType) ) {
					ScanTypes( supportType );
					Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(supportType);

					for( Iterator i = supportSet.iterator(); i.hasNext(); ) {
						Map.Entry me = (Map.Entry) i.next();


						if ( Utils.hasSubTypes(((TypeDefinition) me.getValue())) ) {
							//System.out.print( "element of the list Oltree  dentro al loop per il linked type " + me.getKey() + "\n" );
							ScanTypesOlTree( (TypeDefinition) me.getValue(), olObjetTree.GetLinkedObject( 0 ) );
						} else {

							olObjetTree.SetLinkedObject( (TypeDefinition) me.getValue() );

						}
					}
				}

			}

		} else {

			String nameFile = typeDefinition.context().sourceName();
			TypeDefinition supportType = typeDefinition;
			//System.out.print( "element of the list Oltree " + supportType.id() + "\n" );


			if ( Utils.hasSubTypes(supportType) ) {

				ScanTypes( supportType );
				Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(supportType);
				Iterator i = supportSet.iterator();
				while( i.hasNext() ) {
					Map.Entry me = (Map.Entry) i.next();

					if ( Utils.hasSubTypes(((TypeDefinition) me.getValue())) ) {
						//System.out.print( "element of the list loop 1 " + me.getKey() + "\n" );
						olObjetTree.SetLinkedObject( (TypeDefinition) me.getValue() );
						ScanTypesOlTree( (TypeDefinition) me.getValue(), olObjetTree.GetLinkedObject( 0 ) );

					} else {
						olObjetTree.SetLinkedObject( (TypeDefinition) me.getValue() );
					}

				}

			}
		}
	}

	private void ScanTypes( TypeDefinition typeDefinition )
	{
		boolean addFlag;
		addFlag = true;
		if ( typeDefinition instanceof TypeDefinitionLink ) {

			for( Map<String, TypeDefinition> supportMap : typeMap ) {
				if ( (supportMap.containsKey( ((TypeDefinitionLink) typeDefinition).linkedType().context().sourceName() ) && (supportMap.containsValue( ((TypeDefinitionLink) typeDefinition).linkedType() ))) ) {
					addFlag = false;
					break;
				}
			}
			if ( addFlag ) {
				String nameFile = ((TypeDefinitionLink) typeDefinition).linkedType().context().sourceName();
				TypeDefinition supportType = ((TypeDefinitionLink) typeDefinition).linkedType();
				Map<String, TypeDefinition> addingMap = new HashMap<String, TypeDefinition>();
				addingMap.put( nameFile, supportType );
				typeMap.add( addingMap );

				if ( Utils.hasSubTypes(supportType) ) {
					//ScanTypes( supportType );
					Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(supportType);

					for( Iterator i = supportSet.iterator(); i.hasNext(); ) {
						Map.Entry me = (Map.Entry) i.next();
						if ( Utils.hasSubTypes(((TypeDefinition) me.getValue())) ) {

							ScanTypes( (TypeDefinition) me.getValue() );
						}
					}
				}

			}

		} else {

			for( Map<String, TypeDefinition> supportMap : typeMap ) {
				if ( (supportMap.containsKey( typeDefinition.context().sourceName() )) && (supportMap.containsValue( typeDefinition )) ) {
					addFlag = false;
					break;
				}
			}
			if ( addFlag ) {
				String nameFile = typeDefinition.context().sourceName();
				TypeDefinition supportType = typeDefinition;


				if ( Utils.hasSubTypes(supportType) ) {
					//ScanTypes( supportType );
					Set<Map.Entry<String, TypeDefinition>> supportSet = Utils.subTypes(supportType);

					for( Iterator i = supportSet.iterator(); i.hasNext(); ) {
						Map.Entry me = (Map.Entry) i.next();

						if ( Utils.hasSubTypes(((TypeDefinition) me.getValue())) ) {
							Map<String, TypeDefinition> addingMap = new HashMap<String, TypeDefinition>();
							addingMap.put( nameFile, supportType );
							typeMap.add( addingMap );
							ScanTypes( (TypeDefinition) me.getValue() );
						}

					}
				}
			}
		}
	}

	private void PopulateOperationsSet()
	{
		Entry<String, OperationDeclaration> operation;



		for( InterfaceDefinition idef : interfacesArray ) {
			Map<String, OperationDeclaration> v = idef.operationsMap();
			//v.entrySet().
			for( Iterator i = v.entrySet().iterator(); i.hasNext(); ) {
				operation = (Entry<String, OperationDeclaration>) i.next();
				if ( operation.getValue() instanceof RequestResponseOperationDeclaration ) {
					requestResponseOperationsSet.add( (RequestResponseOperationDeclaration) operation.getValue() );


				} else {

					oneWayOperationsSet.add( (OneWayOperationDeclaration) operation.getValue() );
				}

			}


		}

	}

	private void PopulatePortsLists()
	{
		outputPortArray = program.getOutputPortInfo();
		inputPortArray = program.getInputPortInfo();

	}

	private void PopulateInterfaceLists()
	{

		interfacesArray = program.getInterfaceDefinitions();
		for( InterfaceDefinition idef : interfacesArray ) {

			interfaceMap.put( idef.name(), idef );


		}

	}

	private void PopulateFilesList()
	{

		for( InterfaceDefinition idef : interfacesArray ) {
			if ( !(filesNameList.contains( idef.context().sourceName() )) ) {
				// filesNameList.add(idef.);
			}
		}
		for( OutputPortInfo outInfo : outputPortArray ) {
			if ( !(filesNameList.contains( outInfo.context().sourceName() )) ) {
				// filesNameList.add(idef.);
			}
		}
		for( InputPortInfo inInfo : inputPortArray ) {
			if ( !(filesNameList.contains( inInfo.context().sourceName() )) ) {
				filesNameList.add( inInfo.context().sourceName() );
			}
		}
		Set<Entry<String, TypeDefinition>> suppotSet;
		for( Map<String, TypeDefinition> typeM : typeMap ) {
			suppotSet = typeM.entrySet();

			if ( !(filesNameList.contains( suppotSet.iterator().next().getKey() )) ) {
				filesNameList.add( suppotSet.iterator().next().getKey() );
			}
		}

	}

	protected List<Map<String, TypeDefinition>> GetTypesSet()
	{
		return typeMap;
	}

	protected Set<RequestResponseOperationDeclaration> GetRequestResponseOperations()
	{
		return requestResponseOperationsSet;
	}

	protected Set<OneWayOperationDeclaration> GetOneWayOperations()
	{
		return oneWayOperationsSet;
	}

	protected InterfaceDefinition[] GetInterfaceArray()
	{
		return interfacesArray;
	}

	protected OutputPortInfo[] GetOutputPortArray()
	{
		return outputPortArray;
	}

	protected InputPortInfo[] GetInputPortArray()
	{
		return inputPortArray;
	}

	protected List<String> GetFilesNameList()
	{
		return filesNameList;
	}

	abstract public void ConvertDocument();

	abstract public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException;

	;

	abstract public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException;

	;

	abstract public void ConvertInputPorts( InputPortInfo inputPortInfo, Writer writer )
		throws IOException;

	;

	abstract public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
		throws IOException;

	abstract public void ConvertTypes( TypeDefinition typesDefinition, Writer writer )
		throws IOException;
}
