/*
Class to generate edit-script from set of matchings and the xml files
 
Copyright (C) 2002  Adrian Mouat
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 
Author: Adrian Mouat
email: amouat@postmaster.co.uk
*/

package org.diffxml.diffxml.fmes;

import org.diffxml.diffxml.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.apache.xerces.dom3.Node3;
import org.apache.xerces.dom.NodeImpl;
import org.w3c.dom.NodeList;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.traversal.*;
import org.apache.xerces.dom.NodeImpl;
import org.apache.xerces.dom.DocumentImpl;

public class EditScript
{

final static int DOM=0;
final static int XPATH=1;
final static int CHAR=2;

public static Document create(Document doc1, Document doc2, NodeSet matchings)
{
//Create document to hold changes
Document es= new DocumentImpl();

Element root = es.createElement("delta");
//Append any context information
if (DiffFactory.CONTEXT)
	{
	root.setAttribute("sib_context",""+DiffFactory.SIBLING_CONTEXT);
	root.setAttribute("par_context",""+DiffFactory.PARENT_CONTEXT);
	root.setAttribute("par_sib_context",""+DiffFactory.PARENT_SIBLING_CONTEXT);
	}

if (DiffFactory.REVERSE_PATCH)
	root.setAttribute("reverse_patch","true");

if (!DiffFactory.ENTITIES)
	root.setAttribute("resolve_entities","false");	

if (!DiffFactory.DUL)
	{
	//Change root to dul style
	root=es.createElement("modifications");
	root.setAttribute("version","1.0");
	root.setAttribute("xmlns:xupdate","http://www.xmldb.org/xupdate");
	}	

es.appendChild(root); 

//Need to initalise node w to arbitrary value,
//which will be overwritten

Node w = doc1.createElement("unused");

//Do a breadth first traversal of doc2
//List used as FIFO or array queue

ArrayList fifo=new ArrayList();
fifo.add(doc2.getDocumentElement());

//We need to match roots if not already matched
//Algortihm says to create dummy node, but this will muck up xpaths.
//Use update operation to match the two nodes.
 
/*
if (!doc1.getDocumentElement().getNodeName().equals(doc2.getDocumentElement()))
        {
        //Add update operation
        //Need to add to Delta.java to get proper handling of args
        //But this is a quick hack to avoid probs
        //Need to add attributes
        Element upd=es.createElement("update");
        upd.setAttribute("node","/node()[1]");
        upd.setAttribute("name",doc2.getDocumentElement().getNodeName());
        root.appendChild(upd);
 
        //Set Matched
        ((Node3)doc1.getDocumentElement()).setUserData("matched","true",null);
        ((Node3)doc2.getDocumentElement()).setUserData("matched","true",null);
        }
*/
int index[]=new int[3];
while (!fifo.isEmpty())
	{
	//System.out.println("In breadth traversal");
	DiffXML.log.fine("In breadth traversal");
	//Take first element of the list
	//System.out.println(fifo.get(0));
	NodeImpl x= (NodeImpl) fifo.get(0);	
	DiffXML.log.finer("Past traversal");
	fifo.remove(0);	
	//Get children of element
	NodeList kiddies=x.getChildNodes();
	
	if (kiddies != null)
		{
		for (int i=0; i<kiddies.getLength(); i++)
			{
			//Don't add if banned Node
		
			if (Fmes.isBanned(kiddies.item(i)))
				continue;
			/*
			if (Table.ign_ws_nodes && kiddies.item(i).getNodeType()==Node.TEXT_NODE)
				{
				StringTokenizer st = new StringTokenizer(kiddies.item(i).getNodeValue());
     				if (!st.hasMoreTokens()) 
					continue;
				}
			*/
			fifo.add(kiddies.item(i));
			}		
		}

	//May first need processing to ensure document elements equal
	if ( x.isSameNode(doc2.getDocumentElement()))
		{
		//Mark "in order"
		x.setUserData("inorder","true",null);
		continue;
		}

	//Set y to be parent of x
	Node y=x.getParentNode();

	//Get z, y's partner	
	Node z=matchings.getPartner(y);

	DiffXML.log.finer("x=" + x.getNodeName() + " " + x.getNodeValue());
	DiffXML.log.finer("y=" + y.getNodeName() + " " + y.getNodeValue());
	DiffXML.log.finer("z=" + z.getNodeName() + " " + z.getNodeValue());
		
	if (z==null)
		System.out.println("Your matchings don't work you dumb mutha fucka \n or root"); 
	
	if (x.getUserData("matched").equals("false"))
		{
		//db.on=true;
		/*
		============
		Apply Insert	
		============
		*/
		Pos ins_pos;
		//Easier to mark as matched now than at end
		x.setUserData("matched","true",null);
		x.setUserData("inorder","true",null);
		
		//findpos returns node we want to insert as a child of, and child number
		//In XPath and DOM
		index=FindPos(x, matchings);

		//Get XPath for z
		ins_pos=NodePos.get(z);
		
                //Create XPath for node we are about to insert
		//(Needed to insert attrs if any)
		NamedNodeMap attrs=x.getAttributes();
		int a_len=(attrs != null) ? attrs.getLength() : 0;
		String path;

		//XPath different if expanding tagnames
		if (DiffFactory.TAGNAMES && a_len!=0)
			{
			//Find in order index of element
			NodeList k=x.getParentNode().getChildNodes();	
			String tag=x.getNodeName();
			int in=0;
			for(int ii=0;ii<k.getLength();ii++)
				{
				NodeImpl tmp=(NodeImpl) k.item(ii);
				if (tmp.getNodeName().equals(tag) && 
				    tmp.getUserData("inorder").equals("true"))
					in++;

				if ( tmp.isSameNode(x))
					break;
				}
			path=ins_pos.path + "/" + x.getNodeName() + "[" + in + "]";
			}
		else
                	path=ins_pos.path + "/node()[" + index[XPATH] + "]";

		//Apply insert to doc1
		//The node we want to insert is the import of x with all its text node children
		//Need to make sure this imports attrs - they should be

		w=doc1.importNode(x,false);
		( (NodeImpl) w).setUserData("matched","true",null);
		//Not sure if inorder should be true or false
		( (NodeImpl) w).setUserData("inorder","true",null);

		//Take match of parent (z), and insert
		//get the kids
		NodeList babes=z.getChildNodes();
		//If Node exists we want to insert before it
		if (babes.item(index[DOM])!=null)
			z.insertBefore(w,babes.item(index[DOM]));
		else
			{
			//Last node
			z.appendChild(w);
		
			}

		//Add to matching set
		matchings.add(w,x);

		//Add to delta
		Delta.Insert(w, ins_pos.path, index[XPATH], index[CHAR], es);

		//Add attributes to delta
		for (int i=0; i<a_len;i++)
                        {
			Delta.Insert(attrs.item(i), path, 0, -1, es);
                        }
		}
	else 
		{
		/*
		==========
		Apply Move
		==========
		*/
		DiffXML.log.fine("In move");
		w=matchings.getPartner(x);	
		Node v=w.getParentNode();
		NodeImpl tmp1= (NodeImpl) v;
		//Apply move if parents not matched
		NodeImpl tmp2=(NodeImpl) matchings.getPartner(y);
		if (!tmp1.isSameNode(tmp2))
			{
			index=FindPos(x, matchings);
			//Element mov=es.createElement("move");
			//db.p("In move line 236");
			Pos w_pos=NodePos.get(w);
			/*mov.setAttribute("node",w_pos.path);
			if (w_pos.charpos!=-1)
				mov.setAttribute("old_charpos", (""+w_pos.charpos) );

			if (w_pos.length!=-1)
                        	mov.setAttribute("length", (""+w_pos.length) );
			*/

			Pos z_pos=NodePos.get(z);
			/*mov.setAttribute("parent",z_pos.path);
			if ( index[CHAR]!=-1 )
                        	mov.setAttribute("new_charpos",(""+index[CHAR]));	

			mov.setAttribute("childno", ( ""+index[XPATH] ) );

			db.p("MOVE" + w.getNodeName() + " " + w.getNodeValue() + " , " + z.getNodeName() + " , " + index[XPATH]);
			root.appendChild(mov);
			*/

			//Get domcn w is of v
			NodeList k=v.getChildNodes();
			int domcn=0;
			for(domcn=0;domcn<k.getLength();domcn++)
				{
				if ( ((NodeImpl) w).isSameNode(k.item(domcn)))
					break;
				}


			Element mark=es.createElement("mark");
                	if (DiffFactory.CONTEXT)
                       		{
                        	mark.appendChild(es.importNode(w,true));
                        	mark=Delta.addContext(w, mark);
                        	}
			//Apply move to T1
				
                        //get the kids
                        NodeList babes2=z.getChildNodes();
                        //If Node exists we want to insert before it
                        if (babes2.item(index[DOM])!=null)
                        	z.insertBefore(w,babes2.item(index[DOM]));
                       	else
				{
                         	//Last node
                               	z.appendChild(w);
                        	}

			Delta.Move(mark, w, w_pos.path, z_pos.path, index[XPATH], 
				w_pos.charpos, index[CHAR], w_pos.length, es);
			}
		}
	//Call AlignChildren
	//May want to check value of w
	AlignChildren(w,x,es,matchings);
			
	}//Should have closed breadth first search


DeletePhase(doc1.getDocumentElement(), es);
/*Do post-order traversal of Tree 1(delete phase)
	If current node has no partner in matchings
	append DEL to edit script and apply DEL to doc1
*/
//Post - Condition es is a minimum cost edit script, Matchings is a total matching and
//doc1 is isomorphic to doc2

return es;
}

public static void DeletePhase(Node n, Document es)
{
//System.out.println("Entered Delete Phase");
//Deletes nodes in Post-order traversal
NodeList kids=n.getChildNodes();
if (kids!=null)
	{
	//Note that we loop *backward* through kids
	for (int i=(kids.getLength()-1); i>=0; i--)
		{
		//Don't call delete phase for ignored ndoes
		if (Fmes.isBanned(kids.item(i)))
			continue;

		DeletePhase(kids.item(i), es);
		}
	}

//If node isn't matched, delete it
if ( ( (NodeImpl) n).getUserData("matched").equals("false"))
	{
	/*
	============
	Apply Delete
	============
	*/
	
	
	//Element del=es.createElement("delete");
	Element par=(Element) n.getParentNode();
	Pos del_pos=NodePos.get(n);
	//del.setAttribute("node",del_pos.path);

	//Check if need charpos and length
	//if (del_pos.charpos!=-1)
	//	del.setAttribute("charpos", (""+del_pos.charpos) );
		
	//if (del_pos.length!=-1)
	//	 del.setAttribute("length", (""+del_pos.length) );
		
	//es.getDocumentElement().appendChild(del);
	//db.p("DELETE " + XPath.get(n,false) );

	//Add to es
	Delta.Delete(n, del_pos.path, del_pos.charpos, del_pos.length, es);
	//Apply remove
	par.removeChild(n);	
	}

//System.out.println("Exited Delete Phase");
}

public static void AlignChildren(Node w, Node x, Document es, NodeSet matchings)
{
//Calls LCS algorithm

//Order of w and x is important
//Mark all children of w and x "out of order"
//Immediate children only? Assume so.

NodeList w_kids=w.getChildNodes();
NodeList x_kids=x.getChildNodes();

//Only have something to do if we have kids
DiffXML.log.finer("no w_kids" + w_kids.getLength() );
DiffXML.log.finer("no x_kids" + x_kids.getLength() );
if (w_kids.getLength()==0)
	return;

for (int i=0; i<w_kids.getLength(); i++)
	{
	//Ignored nodes should be left inorder?
	//Ignored nodes are probably going to foul up moves etc.
	//May need to changed to nodes are only ignored when outputting delta
	if (Fmes.isBanned(w_kids.item(i)))
		continue;	
	/*
	if (Table.ign_ws_nodes && w_kids.item(i).getNodeType()==Node.TEXT_NODE)
                                {
                                StringTokenizer st = new StringTokenizer(w_kids.item(i).getNodeValue());
                                if (!st.hasMoreTokens())
                                        continue;
                                }
	*/
	((NodeImpl) w_kids.item(i)).setUserData("inorder","false",null);
	}
	
for (int i=0; i<x_kids.getLength(); i++)
	{
	//Ignored nodes should be left inorder
	if (Fmes.isBanned(x_kids.item(i)))
                continue;
	/*
        if (Table.ign_ws_nodes && x_kids.item(i).getNodeType()==Node.TEXT_NODE)
                                {
                                StringTokenizer st = new StringTokenizer(x_kids.item(i).getNodeValue());
                                if (!st.hasMoreTokens())
                                        continue;
                                }
	*/
	((NodeImpl) x_kids.item(i)).setUserData("inorder","false",null);
	}

Node[] seq = Lcs.find(w_kids, x_kids, matchings);
//Step through array, marking in order

for (int i=0; i<seq.length; i++)
	{
	if (seq[i]!=null)
		{
		DiffXML.log.finer("seq" + seq[i].getNodeName() + " " + seq[i].getNodeValue());
		((NodeImpl) seq[i]).setUserData("inorder","true",null);
		}
	}

//Go through children of w.
//If not inorder but matched, move
//Need to be careful if want x_kids or w_kids
//Check
for (int i=0; i<x_kids.getLength(); i++)
	{
	DiffXML.log.finer("x item i" + x_kids.item(i).getNodeName());
	DiffXML.log.finer("inorder" + ((NodeImpl) x_kids.item(i)).getUserData("inorder"));
	DiffXML.log.finer("matched" + ((NodeImpl) x_kids.item(i)).getUserData("matched"));
	if ( ((NodeImpl) x_kids.item(i)).getUserData("inorder").equals("false")
		&& ((NodeImpl) x_kids.item(i)).getUserData("matched").equals("true"))
		{
		/*
		==========
		Apply Move
		==========
		*/

		//Get childno for move	
		int index[]=FindPos(x_kids.item(i), matchings);
		//Get partner
		Node a=matchings.getPartner(x_kids.item(i));
		//Element mov=es.createElement("move");
		Pos a_pos=NodePos.get(a);

		//Get a's explicit DOM position
		Node a_par=a.getParentNode();
		NodeList k=a_par.getChildNodes();
		
		int domcn=0;
		for (domcn=0;domcn<k.getLength();domcn++)
			{
			if ( ((NodeImpl)k.item(domcn)).isSameNode(a))
				break;
			}

		/*mov.setAttribute("node",a_pos.path);
		if (a_pos.charpos!=-1)
                	mov.setAttribute("old_charpos", (""+a_pos.charpos) );
 
        	if (a_pos.length!=-1)
                	mov.setAttribute("length", (""+a_pos.length) );
		*/	
		Pos w_pos=NodePos.get(w);
		//mov.setAttribute("parent",w_pos.path);
		//if ( index[CHAR]!=-1 )
		//	mov.setAttribute("new_charpos",(""+index[CHAR]));

		//mov.setAttribute("childno", ( ""+index[XPATH] ) );
		
		//es.getDocumentElement().appendChild(mov);

		//For programming ease we actually want to get any old context now

		Element mark=es.createElement("mark");
		if (DiffFactory.CONTEXT)
			{
			mark.appendChild(es.importNode(a,true));
			mark=Delta.addContext(a, mark);
			}
 
               //get the kids
               NodeList babes=w.getChildNodes();
               //If Node exists we want to insert before it
               if (babes.item(index[DOM])!=null)
			w.insertBefore(a,babes.item(index[DOM]));
               else
                        {
                        //Last node
                        w.appendChild(a);
                        }	

		
		//Mark inorder
		((NodeImpl) x_kids.item(i)).setUserData("inorder","true",null);
		((NodeImpl) a).setUserData("inorder","true",null);
		
		//Note that now a is now at new position
		Delta.Move(mark, a, a_pos.path, w_pos.path, index[XPATH], a_pos.charpos, index[CHAR], a_pos.length, es); 
		}
	}	
}

//Findpos returns childnumber of node to insert BEFORE
//Change to return XPath childnum as well
//Change to return charpos as well
public static int[] FindPos(Node x, NodeSet matchings)
{
DiffXML.log.fine("Entered FindPos");
Node x_par=x.getParentNode();
NodeList kids=x_par.getChildNodes();
 
int index[]=new int[3];
//Loop through childnodes
//get rightmost left sibling of x marked "inorder"

//Default v to x
NodeImpl v=(NodeImpl) x;
for (int i=0;i<kids.getLength();i++)
	{
	NodeImpl test=(NodeImpl) kids.item(i);

	if ( test.isSameNode(x))
		break;	

	if (test.getUserData("inorder").equals("true"))
		v=test;
	}

if (v.isSameNode(x))
	{
	DiffXML.log.fine("Exiting FindPos normally1");
	//Return DOM childno of 0, XPath childno of 1
	//SHOULD CHAR be 1 or -1? depends on next node. Safest at 1? 
	index[DOM]=0;
	index[XPATH]=1;
	index[CHAR]=1;
	return index;
	}

//Get partner of v
NodeImpl u = (NodeImpl) matchings.getPartner((Node) v);
Node par_u = u.getParentNode();

//Find "in order" index of u
int dom_index=0;
int xpath_index=1;
int last_node=-1;
NodeList children=par_u.getChildNodes();
for (int i=0; i<children.getLength(); i++)
	{
	NodeImpl test = (NodeImpl) children.item(i);
	if ( u.isSameNode( children.item(i) ) ) 	
		break;

	
	/*db.on=true;
	db.p(test.getNodeName() + test.getUserData("matched"));
	if (test.getUserData("inorder")==null)
		db.p("WTF?");
	db.on=false;
	*/
	if (test.getUserData("inorder").equals("true"))
		{
		dom_index++;
		//Want to increment XPath index if not 
		//both this (inorder) node and last (inorder) node text nodes
		if (test.getNodeType()==Node.TEXT_NODE && 
			last_node!=-1 && children.item(last_node).getNodeType()==Node.TEXT_NODE)
			{
			xpath_index--;
			}

		xpath_index++;
		last_node=i;
			
		//Want to increment XPath index if not both this (inorder) node and last node text nodes	
		}
	}
//Need i+1 child
//childno[0]=++dom_index;
index[DOM]=++dom_index;
//childno[1]=++xpath_index;
//childno[XPATH]=++xpath_index;

//If this is a text node, and last node was a text node,
//don't increment xpath

//Get charpos
//Can't use NodePos func as node may not exist
int tmp_index=dom_index;
int last_ind=children.getLength()-1;

if (dom_index>last_ind)
	tmp_index=last_ind;

int charpos=1;
NodeImpl test = (NodeImpl) children.item(dom_index-1);
if (children.item(dom_index-1).getNodeType()!=Node.TEXT_NODE && test.getUserData("inorder").equals("true"))
	charpos=-1;
else
	{
	int j;
	for (j=(dom_index-1);j>=0;j--)
		{
		test = (NodeImpl) children.item(j);
		if (children.item(j).getNodeType()==Node.TEXT_NODE && test.getUserData("inorder").equals("true"))
       	        	{
                	charpos=charpos+children.item(j).getNodeValue().length();
                	DiffXML.log.finer(children.item(j).getNodeValue()+ " charpos "+charpos);
                	}
        	else
               		break;
		}
	}

//If this is a text node, and last node was a text node,
//don't increment xpath
if (! (x.getNodeType()==Node.TEXT_NODE && children.item(dom_index-1).getNodeType()==Node.TEXT_NODE) )
	xpath_index++;
	
index[XPATH]=xpath_index;
index[CHAR]=charpos;

DiffXML.log.fine("Exiting FindPos normally");
return index;
}

}
