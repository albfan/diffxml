/*
Program to create DUL delta from xmdiff output
 
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

package org.diffxml.diffxml.xmdiff;

import org.diffxml.diffxml.*;
import org.diffxml.diffxml.fmes.NodePos;
import org.diffxml.diffxml.fmes.Delta;
//import org.diffxml.diffxml.fmes.Pos;
import org.diffxml.diffxml.fmes.NodePos;

import org.diffxml.diffxml.fmes.Fmes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.xerces.dom.NodeImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.traversal.*;
import org.w3c.dom.NodeList;
import org.apache.xpath.XPathAPI;
import org.apache.xerces.parsers.DOMParser;
import javax.xml.transform.TransformerException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.NamedNodeMap;
import  org.apache.xerces.dom.DocumentImpl;
import java.io.UnsupportedEncodingException;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


//Should rewrite as external mem prog.
//All we really need to do is get Recover & pulldiff outputting values then reorder

public class Mark
{

public static void mark(Document xm, Document doc1, Document doc2)
{

NodeIterator ni = ((DocumentTraversal) xm).createNodeIterator
                        (xm.getDocumentElement(), NodeFilter.SHOW_ELEMENT, null, false);

Node op=ni.nextNode();

while (op!=null)
	{
	String op_name=op.getNodeName();
	if (op_name.equals("insert"))
		{

		//Change the insert instruction
		//Get the node
		
		try 
			{
			Node node=XPathAPI.selectSingleNode
                       		(doc2.getDocumentElement(), ((Element)op).getAttribute("node"));
		
			//Mark the node to be inserted in the tree
			((NodeImpl) node).setUserData("insert","true",null);
			}
		 catch (TransformerException e) {
			System.err.println("Create could not find node to insert: "+ ((Element)op).getAttribute("node"));}

		}
	else if (op_name.equals("delete"))
		{
		//Change the delete instruction
		try
                        {
                        Node node=XPathAPI.selectSingleNode
                                (doc1.getDocumentElement(), ((Element)op).getAttribute("node"));	
			//Mark node to be deleted
			if (node==null)
				{
				System.err.println("Could not find node: " +((Element)op).getAttribute("node"));
				}
			((NodeImpl) node).setUserData("delete","true",null);
			}
		catch (TransformerException e) {
                        System.err.println("Create could not find node to delete:" + ((Element)op).getAttribute("node"));}
			
		}
	op=ni.nextNode();
	}
		
}

public static void del(Node n, Document es)
{
//Special traversal
//Don't know name!

//Go to righmost nodes first
NodeList kids=n.getChildNodes();
if (kids!=null)
        {
        //Note that we loop *backward* through kids
        for (int i=(kids.getLength()-1); i>=0; i--)
                {
                del(kids.item(i),es);
                }
        }

if (((NodeImpl)n).getUserData("delete")!=null)
	{
	//Output delete
	//Pos del_pos=NodePos.get(n);
        NodePos delPos = new NodePos(n);
	Delta.Delete(n, delPos.getXPath(), delPos.getCharPos(), delPos.getLength(), es);
	}	
}

public static void ins(Node n, Document es)
{
//Think we want an in-order traversal here

//Check if node is inserted
if (((NodeImpl)n).getUserData("insert")!=null)
        {
        //Output insert

	//Get charpos
	int charpos=NodePos.getCharpos(n);
	
	//Get parent path
	Node par=n.getParentNode();
        //Pos par_pos=NodePos.get(par);
        NodePos parentPos = new NodePos(par);

	//Get XPath childno

	NodeList kids=par.getChildNodes();

        int index=0;
        for (int i=0;i<kids.getLength();i++)
        	{
                index++;

                if ( kids.item(i).getNodeType()==Node.TEXT_NODE && (i>0) && kids.item(i-1).getNodeType()==Node.TEXT_NODE)
                	index--;
 
		if ( ((NodeImpl) n).isSameNode(kids.item(i)))
                	break;
 
                }

        Delta.Insert(n, parentPos.getXPath(), index, charpos, es);

	//Insert any attributes
	NamedNodeMap attrs=n.getAttributes();
	if (attrs!=null)
		{
		if (attrs.getLength()>0)
			{
			//Get path
			NodePos nPos= new NodePos(n);
			for (int j=0; j<attrs.getLength(); j++)
				{
				Delta.Insert(attrs.item(j), nPos.getXPath(), 0, -1, es);
				}
			}
		}
        }

//Leftmost nodes first 
//Should be "inorder traversal"
NodeList babes=n.getChildNodes();
if (babes!=null)
        {
        //Note that we loop *forward* through kids
        for (int j=0; j<babes.getLength(); j++)
                {
                ins(babes.item(j),es);
                }
        }

}

public static void init(File xmf, String f1, String f2)
{
try
	{
	DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
	DocumentBuilder parser1 = fac.newDocumentBuilder();
        DOMParser parser2 = new DOMParser();
        DOMParser parser3 = new DOMParser();
        Document xm=parser1.parse(xmf);
        parser2.parse(f1);
        parser3.parse(f2);
 
        //Document xm=parser1.getDocument();
        Document doc1=parser2.getDocument();
        Document doc2=parser3.getDocument();

	mark(xm, doc1, doc2);
        //Make a document for EditScript
        Document es= new DocumentImpl();
        Element root = es.createElement("delta");
        es.appendChild(root);
 
        del(doc1.getDocumentElement(),es);
        ins(doc2.getDocumentElement(),es);
 
        //Transformer serializer = TransformerFactory.newInstance().newTransformer();
        //serializer.transform(new DOMSource(es), new StreamResult(System.out));
        /*
        Writer writer = new Writer();
                try {
                    writer.setOutput(System.out, "UTF8");
                }
                catch (UnsupportedEncodingException e) {
                    System.err.println("error: Unable to set output. Exiting.");
                    System.exit(1);
                }
         writer.setCanonical(false);
                writer.write(es);
                */
        Fmes.outputXML(es);
        }
catch (Exception e)
        {e.printStackTrace();}
}	


public static void main(String[] args)
{
try
	{
	DOMParser parser1 = new DOMParser();
	DOMParser parser2 = new DOMParser();
	DOMParser parser3 = new DOMParser();
	parser1.parse(args[0]);
	parser2.parse(args[1]);
	parser3.parse(args[2]);
	
	Document xm=parser1.getDocument();
	Document doc1=parser2.getDocument();
	Document doc2=parser3.getDocument();
	
	mark(xm, doc1, doc2);
	//Make a document called EditScript
	Document es= new DocumentImpl();
	Element root = es.createElement("delta");
	es.appendChild(root); 

	del(doc1.getDocumentElement(),es);
	ins(doc2.getDocumentElement(),es);

	//Transformer serializer = TransformerFactory.newInstance().newTransformer();
        //serializer.transform(new DOMSource(es), new StreamResult(System.out));
        /*
	Writer writer = new Writer();
                try {
                    writer.setOutput(System.out, "UTF8");
                }
                catch (UnsupportedEncodingException e) {
                    System.err.println("error: Unable to set output. Exiting.");
                    System.exit(1);
                }
         writer.setCanonical(false);
                writer.write(es);
                */
        Fmes.outputXML(es);
	}
catch (Exception e)
	{e.printStackTrace();}	
}
}

