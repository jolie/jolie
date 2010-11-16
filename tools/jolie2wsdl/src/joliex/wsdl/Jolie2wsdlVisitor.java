/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/
package joliex.wsdl;

import com.ibm.wsdl.DefinitionImpl;
import com.ibm.wsdl.MessageImpl;
import com.ibm.wsdl.TypesImpl;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import jolie.lang.NativeType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

public class Jolie2wsdlVisitor implements OLVisitor {

    static String tns = "http://www.italianasoftware.com/wsdl/FirstServiceByWSDL4J.wsdl";
    static String xsd = "http://www.w3.org/2001/XMLSchema";
    static String soap = "http://schemas.xmlsoap.org/wsdl/soap/";
    private Program program;
    private Definition wsdlDef = new DefinitionImpl();

    //TODO Aggiungere eventuali buffer o mappe per entità da riusare durante la visita

    public Jolie2wsdlVisitor(Program program) {
        this.program = program;
        /*
        Writer fw = null;
        try {
        WSDLFactory f = WSDLFactory.newInstance();
        WSDLReader r = f.newWSDLReader();
        //r.readWSDL("");
        WSDLWriter ww = f.newWSDLWriter();

        fw = new FileWriter(fileName);
        Definition wsdlDef = new DefinitionImpl();
         */
        init();//Meglio farla chiamare da fuori! così non abbiamo eccezioni NEL costruttore!!
    }

    public void init() {
        try {
            WSDLFactory f = WSDLFactory.newInstance();
            //WSDLReader r = f.newWSDLReader();
            //r.readWSDL("");
            //WSDLWriter ww = f.newWSDLWriter();
            //fw = new FileWriter(fileName);
            wsdlDef = new DefinitionImpl();
            //TODO Prelevarlo dal nome del file.ol (vedi convenzioni); Utilizzarlo come var isntaza
            String serviceName = "TwiceService";
            //String tns = "urn:xmltoday-delayed-quotes";

            //Veder se prendere def.createBlaBla
            QName servDefQN = new QName(serviceName);
            wsdlDef.setQName(servDefQN);

            String targetNS = tns;
            wsdlDef.setTargetNamespace(targetNS);
            wsdlDef.addNamespace("soap", soap);
            wsdlDef.addNamespace("tns", tns);
            wsdlDef.addNamespace("xsd", xsd);

            //TODO chiarire se è il posto migliore per mettere il service

            Service s = wsdlDef.createService();
            QName serviceQN = new QName(serviceName);

            s.setQName(serviceQN);
            wsdlDef.addService(s);
        } catch (WSDLException wsdlEx) {
            Logger.getLogger(Jolie2wsdlVisitor.class.getName()).log(Level.SEVERE, null, wsdlEx);

        } catch (Exception ex) {
            Logger.getLogger(Jolie2wsdlVisitor.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    @Override
    public void visit(Program n) {
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
    }

    @Override
    public void visit(OneWayOperationDeclaration decl) {
        System.out.println("OneWayOperationDeclaration ReqType=" + decl.requestType());
        System.out.println("OneWayOperationDeclaration id=" + decl.id());
    }

    @Override
    public void visit(RequestResponseOperationDeclaration decl) {
        //TO-ASK Attualmente il flusso si esecuzione NON passa per questo metodo... mi sono perso un accept?
        TypeDefinition reqT = decl.requestType();
        TypeDefinition resT = decl.responseType();
        System.out.println("RequestResponseOperationDeclaration ReqType=" + reqT);
        System.out.println("RequestResponseOperationDeclaration ReqType id=" + reqT.id());
        System.out.println("RequestResponseOperationDeclaration ReqType cardinality=" + reqT.cardinality());
        System.out.println("RequestResponseOperationDeclaration ReqType hasSubTypes=" + reqT.hasSubTypes());
        System.out.println("RequestResponseOperationDeclaration ResType=" + resT);
        System.out.println("RequestResponseOperationDeclaration ResType id=" + resT.id());
        System.out.println("RequestResponseOperationDeclaration ResType cardinality=" + resT.cardinality());
        System.out.println("RequestResponseOperationDeclaration ResType hasSubTypes=" + resT.hasSubTypes());

        System.out.println("RequestResponseOperationDeclaration id=" + decl.id());
        Operation operation = wsdlDef.createOperation();
        operation.setName(decl.id());
        operation.setStyle(OperationType.REQUEST_RESPONSE);
        //decl.accept(this);
    }

    @Override
    public void visit(DefinitionNode n) {
        OLSyntaxNode b = n.body();
        b.accept(this);
    }

    @Override
    public void visit(ParallelStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(SequenceStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(NDChoiceStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(OneWayOperationStatement n) {

        System.out.println("OneWayOperationStatement inputVarPathl=" + n.inputVarPath());

        System.out.println("OneWayOperationStatement inputVarPathl=" + n.context());
        //n.accept(this);
    }

    @Override
    public void visit(RequestResponseOperationStatement n) {
        /*
        System.out.println("RequestResponseOperationStatement id=" + n.id());
        System.out.println("RequestResponseOperationStatement toString=" + n.toString());
        System.out.println("RequestResponseOperationStatement outputExpression=" + n.outputExpression());
        System.out.println("RequestResponseOperationStatement inputVarPathl=" + n.inputVarPath());
        System.out.println("RequestResponseOperationStatement process=" + n.process());
        System.out.println("RequestResponseOperationStatement context=" + n.context());
         *
         */
        //n.accept(this);
    }

    @Override
    public void visit(NotificationOperationStatement n) {
        /*
        System.out.println("NotificationOperationStatement outputPortId=" + n.outputPortId());
        System.out.println("NotificationOperationStatement context=" + n.context());
         *
         */
        //n.accept(this);
    }

    @Override
    public void visit(SolicitResponseOperationStatement n) {
        /*
        System.out.println("SolicitResponseOperationStatement outputPortId=" + n.outputPortId());
        System.out.println("SolicitResponseOperationStatement context=" + n.context());
         * 
         */
        //n.accept(this);
    }

    @Override
    public void visit(LinkInStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(LinkOutStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(AssignStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(IfStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(DefinitionCallStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(WhileStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(OrConditionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(AndConditionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(NotConditionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(CompareConditionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ExpressionConditionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ConstantIntegerExpression n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ConstantRealExpression n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ConstantStringExpression n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ProductExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(SumExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(VariableExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(NullProcessStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(Scope n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(InstallStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(CompensateStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ThrowStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ExitStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ExecutionInfo n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(CorrelationSetInfo n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(OutputPortInfo n) {
        //System.out.println(" OutputPortInfo=" + n.operations().toString());
        //n.addOperation(null)
        //        n.id();
        n.accept(this);
    }

    @Override
    public void visit(InputPortInfo n) {
        Collection<OperationDeclaration> opDecls = n.operations();
        //n.accept(this);
         System.out.println(" InputPortInfo  " + n);
        //System.out.println(" InputPortInfo  n=" + n.id());
        //System.out.println(" InputPortInfo protocolId=" + n.protocolId());
        //System.out.println(" InputPortInfo protocolConfiguration=" + n.protocolConfiguration());
        //n.protocolConfiguration().accept(this);
        //System.out.println(" InputPortInfo location=" + n.location());
        PortType pt = wsdlDef.createPortType();


        Iterator<OperationDeclaration> it = opDecls.iterator();
        while (it.hasNext()) {
            OperationDeclaration opDecl = it.next();
            System.out.println("InputPortInfo Op id=" + opDecl.id());
            //System.out.println("InputPortInfo Op id=" + opDecl.);
            //TODO
            Operation op = wsdlDef.createOperation();
            //op.setName(opDecl.id());
            //opDecl.
            System.out.println(" Operation getName=" + op.getName());
            op.setUndefined(false);
            //op.s
            pt.addOperation(op);
            //wsdlDef.createPort();
        }
        pt.setUndefined(false);

        wsdlDef.addPortType(pt);

        Port p = wsdlDef.createPort();
        //TODO verificare il nome della porta; e' qiello giusto o devo usre la LOCATION <<===
        p.setName(n.id());
        //p.setBinding(null);
    }

    @Override
    public void visit(PointerStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(DeepCopyStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(RunStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(UndefStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ValueVectorSizeExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(PreIncrementStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(PostIncrementStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(PreDecrementStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(PostDecrementStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ForStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(ForEachStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(IsTypeExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(TypeCastExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(SynchronizedStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(CurrentHandlerStatement n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(EmbeddedServiceNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(InstallFixedVariableExpressionNode n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(SpawnStatement ss) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(VariablePathNode vpn) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(TypeInlineDefinition tid) {
        //System.out.println("TypeInlineDefinition  tid.id=" + tid.id());
        System.out.println("TypeInlineDefinition  tid=" + tid);
        //System.out.println("TypeInlineDefinition *** tid.hasSubTypes=" + tid.hasSubTypes());
        //System.out.println("TypeInlineDefinition *** tid.hasSubTypes=" +  tid.getSubType(""));
        //System.out.println("TypeInlineDefinition *** tid.subTypes=" + tid.subTypes());
        //System.out.println("TypeInlineDefinition  tid.context=" + tid.context());
//TODO aggiungere controlli  se ha sottotipi e per ogni sottoitpo
        //if (tid.hasSubTypes()){ }
        Iterator it = tid.subTypes().iterator();
        //while(it.hasNext()) {
        Entry<String, TypeDefinition> v = (Entry<String, TypeDefinition>) it.next();
        TypeDefinition subTypeDef = v.getValue();

        Part inputPart = wsdlDef.createPart();
        inputPart.setName(subTypeDef.id());
        //if (subTypeDef.untypedSubTypes());
        NativeType nt = subTypeDef.nativeType();

        //TODO Come prendo il tipo (jolie-nativo) a cui è assegnato uno slot del messaggeType;
        //String stypeDef=tid.id();
        //TypeDefinition st = subTypeDef.getSubType(subTypeDef.id());
        String subType =  nt.id();
        //TO ASK coem prendere il tipo del sottotipo (esempio int ...)
        //String subType=st.nativeType().id();

        inputPart.setTypeName(new QName(xsd, subType));
        //TO ASK: Cose questo element
        //inputPart.setElementName(null);
        Message msg_req = new MessageImpl();
        msg_req.setUndefined(false);
        QName msg_req_QN = new QName(tid.id());

        msg_req.setQName(msg_req_QN);
        //per ogni sottotipo
        /*
        Set<Entry<String, TypeDefinition>> st = tid.subTypes();
        for (int i=0;i<st.size(); i++){

        msg_req.addPart(inputPart);
        }
         */
        msg_req.addPart(inputPart);
        wsdlDef.addMessage(msg_req);
        Types t = new TypesImpl();
        //Element el=new Element();
        //t.setDocumentationElement(el)

        //t.addExtensibilityElement(null);
        //t.setExtensionAttribute(msg_req_QN, t);
        //TODO
        wsdlDef.createTypes();
        wsdlDef.setTypes(t);

        //accept(this)
    }

    @Override
    public void visit(TypeDefinitionLink tdl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void visit(InterfaceDefinition id) {
        System.out.println("InterfaceDefinition name=" + id.name());
        System.out.println("InterfaceDefinition name=" + id.operationsMap());
        //TODO   for  id.operationsMap()

    }

    /**
     * @return the wsdlDef
     */
    public Definition getWsdlDef() {
        return wsdlDef;
    }
}
