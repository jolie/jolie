/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package support;

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
        treeOLObject supportObjet= new treeOLObject(olSyntaxNode,this);
        linkList.add(supportObjet);
   }
public OLSyntaxNode GetLinkedObject(int i){
return linkList.get(i).olSyntaxNode;
}

public int GetLinkedObjetSize(){

 return linkList.size();
}
public treeOLObject GetFatherObject(){
 return this.fatherObject;
}

}
