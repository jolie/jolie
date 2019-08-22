/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliex.java.support;

import java.util.LinkedList;
import java.util.List;
import jolie.lang.parse.ast.OLSyntaxNode;

/**
 *
 * @author balint
 */
public class treeOLObject {
private OLSyntaxNode olSyntaxNode;
private List<treeOLObject> linkList;
private String nameFile;
private treeOLObject fatherObject;
public treeOLObject(OLSyntaxNode olSyntaxNode, treeOLObject fatherObject){

        this.olSyntaxNode=olSyntaxNode;
        this.nameFile=olSyntaxNode.context().sourceName();
        linkList= new LinkedList<treeOLObject>();
        this.fatherObject=fatherObject;
}
public void SetLinkedObject(OLSyntaxNode olSyntaxNode){
    //System.out.print(olSyntaxNode.getClass().toString()+"\n");
    treeOLObject supportObjet= new treeOLObject(olSyntaxNode,this);
    //System.out.print("fine del creatore\n");
    //System.out.print(linkList.size()+"\n");
    linkList.add(supportObjet);
    //System.out.print(linkList.size()+"\n");

   }
public treeOLObject GetLinkedObject(int i){
   //System.out.print("the size is "+this.linkList.size()+" the index request is " +i +"\n");
    return linkList.get(i);
}
public OLSyntaxNode GetOLSyntaxNode()
{

  return olSyntaxNode;
}



public int GetLinkedObjetSize(){

 return linkList.size();
}
public treeOLObject GetFatherObject(){
 return this.fatherObject;
}

}
