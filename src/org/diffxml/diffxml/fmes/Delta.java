/*
Class to create DUL operations
 
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

//Code to add stuff to EditScript

import org.diffxml.diffxml.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element; 
import org.apache.xerces.dom3.Node3;
import org.w3c.dom.NodeList; 

//Class to handle adding operations to output delta
//Should handle everything to do with es,
//But this is a late hack

public class Delta
{

public static Element addRevContext(Node n, Element op)
{
Document es=op.getOwnerDocument();
if (op.getNodeName().equals("delete"))
	{
        op.setAttribute("name",n.getNodeName());
        op.setAttribute("nodetype",""+n.getNodeType());
        Node txt=es.createTextNode(n.getNodeValue());
        if (txt!=null)
       		op.appendChild(txt);
        }
else if (op.getNodeName().equals("move"))
	{
        op.setAttribute("name",n.getNodeName());
        Element old=es.createElement("node");
        Node txt=es.createTextNode(n.getNodeValue());
        old.appendChild(txt);
        op.appendChild(old);
	}
return op;
}

//Takes node n and adds its context to Node op
public static Element addContext(Node n, Element op)
{
//db.on=true;
Document es=op.getOwnerDocument();
//What type of context?

//Must want full context
Node par=n;
int p=0, itmp=0;
int left=0, right=0;
int stack[]=new int[20]; //Just leave this as a static array for speed and ease
Node3 root=(Node3) n.getOwnerDocument().getDocumentElement();
Node con=es.createElement("context");

//Move up num of parents, keeping track of domcn
for(p=0;p<=DiffFactory.PARENT_CONTEXT;p++)
	{
        Node tmp=par.getParentNode();
        //Get domcn of *previous* node
        NodeList v=tmp.getChildNodes();
        int cn=0;
        for (cn=0;cn<v.getLength();cn++)
		{
       		if ( ((Node3)v.item(cn)).isSameNode(par))
                       break;
		}

	DiffXML.log.finer("Moving up stack: p=" +p + " Node " + par.getNodeName() + par.getNodeValue());
        stack[p]=cn;

        par=tmp;
        if (root.isSameNode(par))
		{
		DiffXML.log.finer("Exiting loop as reached root node");
       		break;
		}

       }

p++;
//get domcn of this node

if (!root.isSameNode(par))
	{
        int cn=0;
        NodeList v=par.getChildNodes();
	for (cn=0;cn<v.getLength();cn++)
		{
                if ( ((Node3)v.item(cn)).isSameNode(par))
                       break;
		}

	stack[p]=cn;
        }
else
	{
	DiffXML.log.finer("roo");
	stack[p]=0;	
	}

//Start by appending to context node
Node app_node=(Node) con;
while (p>=1)
	{
	DiffXML.log.finer("p=" +p + " par" + par.getNodeName() + par.getNodeValue());
        //Output any left sibs
        //If root node we have no siblings

        if (! root.isSameNode(par))
      		{
                NodeList par_sibs=par.getParentNode().getChildNodes();
                left=stack[p]-DiffFactory.PARENT_SIBLING_CONTEXT;
                if (left<0)
                       left=0;

                //Attach the nodes
		while (left <= stack[p])
			{
			DiffXML.log.finer("appending node" + par_sibs.item(left).getNodeName()+" " +  par_sibs.item(left).getNodeValue());
                       	app_node.appendChild( es.importNode(par_sibs.item(left), false) );
                        left++;
                        }
                }
	else
       		app_node.appendChild(es.importNode(par, false));

	//app_node should be last child of es
        //app_node=es.importNode(par,false);
	//if (p!=2)
       	 	app_node=app_node.getLastChild();
        p--;
        par=par.getChildNodes().item(stack[p]);
        }
//Now par should be n
//Add any sibling context
NodeList sibs=n.getParentNode().getChildNodes();
left=stack[0]-DiffFactory.SIBLING_CONTEXT;

if (left<0)
	left=0;

while(left<stack[0])
	{
        app_node.appendChild(es.importNode(sibs.item(left), false));
        left++;
        }
//add insert
DiffXML.log.fine("Appending operation to" + app_node.getNodeName());
app_node.appendChild(op);
//db.on=false;

//Add right siblings
right=stack[0]+DiffFactory.SIBLING_CONTEXT;
if (right>=sibs.getLength())
	right=sibs.getLength()-1;

itmp=stack[0];
//Want node after
itmp++;
while(itmp<=right)
	{
        app_node.appendChild(es.importNode(sibs.item(itmp), false));
        itmp++;
        }

//Move up parents adding context information
//Only a point if par_sib>0


if (DiffFactory.PARENT_SIBLING_CONTEXT>0)
	{
	for(p=1;p<=DiffFactory.PARENT_CONTEXT;p++)
		{
		par=n.getParentNode();

		if (root.isSameNode(par))
			break;

		//Get app_node
		//app_node=es.importNode(app_node.getParentNode(),false);
		app_node=app_node.getParentNode();
		NodeList par_sibs=n.getParentNode().getChildNodes();
		//Note that we now want to work outwards, not inwards
		//Note that we corrupt the stack here!!
		right=stack[p]+DiffFactory.PARENT_SIBLING_CONTEXT;
		if (right>=par_sibs.getLength())
			right=par_sibs.getLength()-1;

		//Want to start at next node
		stack[p]++;

		while (stack[p]<=right)
			{
			app_node.appendChild(es.importNode(par_sibs.item(stack[p]), false));
			stack[p]++;
			}
		}
	}
//db.on=false;
return (Element)con;
}
//Appends an insert operation to es given inserted node, xpath to parent, charpos & childno
public static void Insert(Node n, String parent, 
		int childno, int charpos, Document es)
{
if (DiffFactory.DUL)
	{
	Element ins=es.createElement("insert");
	ins.setAttribute("parent",parent);
	ins.setAttribute("nodetype", (""+n.getNodeType()) );

	if (n.getNodeType()!=Node.ATTRIBUTE_NODE)
		ins.setAttribute("childno", (""+childno) );

	ins.setAttribute("name", n.getNodeName() );

	if (charpos!=-1 )
		ins.setAttribute("charpos", (""+charpos) );

	Node txt=es.createTextNode(n.getNodeValue());
	ins.appendChild(txt);

	//Add any context information

	if (DiffFactory.CONTEXT)
		ins=addContext(n, ins);
	
	es.getDocumentElement().appendChild(ins);
	}
else
	{
	Element app=es.createElement("append");
	app.setAttribute("parent",parent);
	app.setAttribute("child",""+childno);
	
	//The child we want to insert is dependent on nodetype
	Element ch=es.createElement("tmp");
		
	switch (n.getNodeType())
		{
		case Node.ELEMENT_NODE:
			ch=es.createElement("element");
			ch.setAttribute("name",n.getNodeName());
			break;
		case Node.ATTRIBUTE_NODE:
			ch=es.createElement("attribute");
			ch.setAttribute("name",n.getNodeName());
			ch.appendChild(es.createTextNode(n.getNodeValue()));
			break;
		case Node.TEXT_NODE:
			ch=es.createElement("text");
			ch.appendChild(es.createTextNode(n.getNodeValue()));
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			ch=es.createElement("processing-instruction");
			ch.setAttribute("name",n.getNodeName());
			ch.appendChild(es.createTextNode(n.getNodeValue()));
			break;
		case Node.COMMENT_NODE:
			ch=es.createElement("comment");
			ch.appendChild(es.createTextNode(n.getNodeValue()));
			break;
	
		default:
			System.err.println("XUpdate does not support NodeType n.getNodeType()");			
			System.exit(2);
		}
	app.appendChild(ch);
	es.getDocumentElement().appendChild(app);
	
	}
}

public static void Delete(Node n, String path, int charpos, int length, Document es)
{
if (DiffFactory.DUL)
	{
	Element del=es.createElement("delete");
	del.setAttribute("node",path);

	if (charpos!=-1)
		del.setAttribute("charpos", (""+charpos) );
 
	if (length!=-1)
		del.setAttribute("length", (""+length) );

	if (DiffFactory.REVERSE_PATCH)
		del=addRevContext(n, del);
	else if (DiffFactory.CONTEXT)
		del=addContext(n,  del);

	es.getDocumentElement().appendChild(del);
	}
else
	{
	//There's going to be a problem with deleting
	//coalesced text nodes.....
	Element rem=es.createElement("remove");
	rem.setAttribute("select",path);
	es.getDocumentElement().appendChild(rem);
	}
}

//Moves are a little more complicated
//With context info we need old parent and DOM cn
//CHANGED
//now only need mark element
public static void Move(Element mark, Node n, String path, String parent, int childno, int ocharpos, int ncharpos, int length, Document es)
{
Element mov=es.createElement("move");
mov.setAttribute("node",path);

if (ocharpos!=-1)
	mov.setAttribute("old_charpos", (""+ocharpos) );

if (length!=-1)
	mov.setAttribute("length", (""+length) );

if (ncharpos!=-1)
	mov.setAttribute("new_charpos",(""+ncharpos));

mov.setAttribute("parent",parent);
mov.setAttribute("childno", ( ""+childno));

if (DiffFactory.REVERSE_PATCH)
	addRevContext(n,mov);
else if (DiffFactory.CONTEXT)
	{
	Element con=es.createElement("context");
	con.appendChild(mark);
	mov.appendChild(con);
	addContext(n,mov);	
	}
es.getDocumentElement().appendChild(mov);

}

}
