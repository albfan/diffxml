/*
Class to generate Matchings for FMES algorithm
 
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

//Code to solve "Good Matchings Problem"
//Algorithm Fast Match
//21/02/02

import org.diffxml.diffxml.*; 
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.xerces.dom.NodeImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.dom3.Node3;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.w3c.dom.traversal.*; 
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.*;
import org.xml.sax.SAXParseException;
import java.io.UnsupportedEncodingException;

public class Match
{

    private final static boolean OUTPUT_MATCHED_NODES=false;

public boolean equal(Node a, Node b)
{
//Currently returns false but should throw exception

if (a.getNodeType()!=b.getNodeType())
	{
	System.out.println("Types not equal");
	return false;
	}
	

//if Node is an element
if (a.getNodeType()==Node.ELEMENT_NODE)
	{
	//Check NodeNames equal
	if (a.getNodeName()!=b.getNodeName())
		return false;

	//Check attributes equal
	NamedNodeMap attrs_a=a.getAttributes();
	NamedNodeMap attrs_b=b.getAttributes();

	int a_num = (attrs_a != null) ? attrs_a.getLength() : 0;
	for (int i=0; i<a_num; i++)
		{
		//Check if attr exists in other tag
		if (attrs_b.getNamedItem(attrs_a.item(i).getNodeName())==null)
			return false;

		if (! attrs_b.getNamedItem(attrs_a.item(i).getNodeName()).getNodeValue().equals
				(attrs_a.item(i).getNodeValue()) )
			{
			//System.out.println("Equal: attributes not equal");
			return false;
			}
		}
	//Should probably compare positions of elements, or if kids matched or sumink
	return true;
	}

//If node is a text node
if (a.getNodeType()==Node.TEXT_NODE)
	{
	//Need to check whitespace and case options
	String a_str=a.getNodeValue();
	String b_str=b.getNodeValue();
	
	if (Fmes.IGNORE_ALL_WHITESPACE)
		{
		//Ignore all whitespace
		//Remove whitespace from nodes before comparison
		StringTokenizer st = new StringTokenizer(a_str);
		a_str="";
		while (st.hasMoreTokens())
			a_str=a_str+st.nextToken();

		st = new StringTokenizer(b_str);
		b_str="";
		while (st.hasMoreTokens())
                        b_str=b_str+st.nextToken();	
		}		
	else if (Fmes.IGNORE_LEADING_WHITESPACE)
		{
		//Ignore leading ws
		//just call trim
		a_str=a_str.trim();
		b_str=b_str.trim();
		}	

	//Check case optn
	
	if (Fmes.IGNORE_CASE)
		{
		//Just make it all lower
		a_str.toLowerCase();
		b_str.toLowerCase();
		}
	
	return (a_str.equals(b_str));
	}

//Node is not a text node or element, so just compare value and return.

//System.out.println("Equal: elements equal");
return (a.getNodeValue().equals(b.getNodeValue()));
}

class tdComp implements Comparator
{
//Remember we want things stored in reverse order of depth!
//We don't really care about order of strings but we need to
//differentiate for a set

public int compare(Object o1, Object o2)
	{
	TagDepth td1 = (TagDepth)(o1);
	TagDepth td2 = (TagDepth)(o2);
	
	if (td1.depth==td2.depth)
		{
		return (td1.tag.compareTo(td2.tag));
		}
	else 
		{
		return (td2.depth-td1.depth);	
		}
	}

public boolean equals(Object o)
	{
	return o.equals(this);
	}

}

public NodeSet test(Document doc1, Document doc2) 
throws Exception {

	NodeSet match_set = new NodeSet();

	//Normalise documents
	doc1.getDocumentElement().normalize();
	doc2.getDocumentElement().normalize();

	TreeSet td1 = markElements(doc1);
	TreeSet td2 = markElements(doc2);
		

	String wanted="";
        TagDepth tg= new TagDepth();

	//Iterate for nodes in Tree1
	Iterator it=td1.iterator();
	while (it.hasNext())
		{
		tg=(TagDepth) it.next();		

		wanted=tg.tag;

		DiffXML.log.finer("Wanted Node: " + wanted);
		//Get all nodes in both trees with this tag
		if (wanted.equals("#text"))
			{
			//Use node iterator
			//Should really be bottom up, but shouldn't make big diff
			DiffXML.log.finer("Matching text nodes");
			NodeIterator ni1 = ((DocumentTraversal)doc1).createNodeIterator(doc1.getDocumentElement(), NodeFilter.SHOW_TEXT, null, false);	
			NodeIterator ni2 = ((DocumentTraversal)doc2).createNodeIterator(doc2.getDocumentElement(), NodeFilter.SHOW_TEXT,null, false);
			NodeImpl na= (NodeImpl) ni1.nextNode();
			NodeImpl nb= (NodeImpl) ni2.nextNode();
			while(na!=null)	
				{
				//Should always be false but leave check in for mo
				if (na.getUserData("matched").equals("false"))
					{
					while(nb!=null)
						{
						if (nb.getUserData("matched").equals("false") && equal(na,nb) )
							{
							//Add nodes to matching set
							//Node3 unq1 = na;
							//Node3 unq2 = nb;
							match_set.add(na,nb);	
							//Mark nodes matched
							na.setUserData("matched","true",null);
							nb.setUserData("matched","true",null);
			
							break;
							}
						nb=(NodeImpl) ni2.nextNode();
						}
					}	
				na=(NodeImpl) ni1.nextNode();
				ni2.detach();
				 ni2 = ((DocumentTraversal)doc2).createNodeIterator(doc2.getDocumentElement(), NodeFilter.SHOW_TEXT,null, false);
				nb=(NodeImpl) ni2.nextNode();
				}


			}
		else if (wanted.equals("#comment"))
                        {
                        //Use node iterator
                        //Should really be bottom up, but shouldn't make big diff
                        DiffXML.log.finer("Matching comments");
                        NodeIterator ni1 = ((DocumentTraversal)doc1).createNodeIterator(doc1.getDocumentElement(), NodeFilter.SHOW_COMMENT, null, false);
                        NodeIterator ni2 = ((DocumentTraversal)doc2).createNodeIterator(doc2.getDocumentElement(), NodeFilter.SHOW_COMMENT,null, false);
                        NodeImpl na= (NodeImpl) ni1.nextNode();
                        NodeImpl nb= (NodeImpl) ni2.nextNode();
                        while(na!=null)
                                {
                                //Should always be false but leave check in for mo
                                if (na.getUserData("matched").equals("false"))
                                        {
                                        while(nb!=null)
                                                {
                                                if (nb.getUserData("matched").equals("false") && equal(na,nb) )
                                                        {
                                                        match_set.add(na,nb);
                                                        na.setUserData("matched","true",null);
                                                        nb.setUserData("matched","true",null);
 
                                                        break;
                                                        }
                                                nb=(NodeImpl) ni2.nextNode();
                                                }
                                        }
                                na=(NodeImpl) ni1.nextNode();
                                ni2.detach();
                                 ni2 = ((DocumentTraversal)doc2).createNodeIterator(doc2.getDocumentElement(), NodeFilter.SHOW_TEXT,null, false);
                                nb=(NodeImpl) ni2.nextNode();
                                }
                        }
		else {
		//System.out.println("ENTERED!!!");
		NodeList tg1=doc1.getElementsByTagName(wanted);
		NodeList tg2=doc2.getElementsByTagName(wanted);

		//Cycle through tg1 looking for matches in tg2

		for(int a=0; a<tg1.getLength(); a++)
			{
			NodeImpl a_node= (NodeImpl) tg1.item(a);
			if (a_node.getUserData("matched").equals("false"))
				{
				//Cycle through tg2 looking for match
				//tg_tag:
				for (int b=0; b<tg2.getLength(); b++)
					{
					NodeImpl b_node= (NodeImpl) tg2.item(b);
					if (b_node.getUserData("matched").equals("false") && equal(tg1.item(a), tg2.item(b)))
						{
						 //Add nodes to matching set
                                                match_set.add(tg1.item(a), tg2.item(b));
 
                                                //mark nodes matched
						a_node.setUserData("matched","true",null);
						b_node.setUserData("matched","true",null);

						//Don't think this statement did nowt
						//continue tg_tag;
						break;
						}
					}
				}
			}
			}
		}

			
	//Output matched nodes
	if (OUTPUT_MATCHED_NODES)
		match_set.print_set();

	return match_set;		

}

public TreeSet markElements(Document doc)
{
//Maybe want to set to SHOW_ELEMENT and do other elements separately
TreeWalker t = ((DocumentTraversal)doc)
	.createTreeWalker(doc.getDocumentElement(),NodeFilter.SHOW_ALL,null,false);

NodeImpl n;
int depth;
String tmptag;
TagDepth tmp2 = new TagDepth();

TreeSet td = new TreeSet(new tdComp());

//Add root node (should change loop to include)
n= (NodeImpl) t.getCurrentNode();
n.setUserData("matched","false",null);
n.setUserData("inorder","false",null);
tmp2.tag=n.getNodeName();
tmp2.depth=0;
td.add(tmp2);
DiffXML.log.finer("Added "  + tmp2.tag + " Depth " + 0);

while ( (n=(NodeImpl) t.nextNode()) != null)
	{
	n.setUserData("matched","false",null);
	//Let children default to be "out of order"
	//Test with "inorder"
	n.setUserData("inorder","true",null);

	//Get iterator for TreeSet
	Iterator it = td.iterator();
	
	//Add to set
	TagDepth tmp = new TagDepth();
	tmp.tag=n.getNodeName();

	//Bit more trouble to get depth	
	depth=1;
	n=(NodeImpl) n.getParentNode();
	while(n!=doc.getDocumentElement() && n!=null)
		{
		depth++;	
		n=(NodeImpl) n.getParentNode();
		}
	
	tmp.depth=depth;	
	td.add(tmp);
	DiffXML.log.finer("Added "  + tmp.tag + " Depth " + depth);
		//}
	}
	return td;	
}

public int findLeaves(Document doc, String[] leaves)
{
TreeWalker t = ((DocumentTraversal)doc)
	.createTreeWalker(doc.getDocumentElement(),NodeFilter.SHOW_ELEMENT,new LeafFilter(),false);
//Node[] leaves = new Node[100];
Node n;
int i=0;
while ( (n=t.nextNode() ) != null)
	{//Note that only one occurence of any element in NamedNodeMap
	leaves[i]=n.getNodeName();
	for (int x=0;x<i;x++)
		{
		if (leaves[x].equals(leaves[i]))
			{
			i--;
			break;
			}
		}
	i++;
	}

return i;
	
}


class LeafFilter implements NodeFilter 
{
//Consider leaf nodes to be elements
//with no children other than text nodes
//Then the text nodes represent the value
//of the element

	public short acceptNode(Node n) 
		{
		//System.out.println("name " + n.getNodeName());
		if (n.hasChildNodes()==false)
			return FILTER_ACCEPT;
		
		NodeList nodes = n.getChildNodes();		
		for (int i=0; i<nodes.getLength(); i++)
			{
			if (nodes.item(i).getNodeType()!=Node.TEXT_NODE)
				{
				return FILTER_SKIP;
				}
		
			}
		return FILTER_ACCEPT;
		}
}
			
/*
public static void main(String[] args)
{

if (args.length !=2)
                System.exit(0);


try 
{
	//db.on=true;
	DOMParser parser1 = new DOMParser();
        DOMParser parser2 = new DOMParser();
	
	//Turn off entity resolving
	try {
                   parser1.setFeature("http://xml.org/sax/features/external-general-entities", 
                                     false);
		   parser2.setFeature("http://xml.org/sax/features/external-general-entities",
                                     false);
		   parser1.setFeature("http://xml.org/sax/features/external-parameter-entities",
				     false);
		   parser2.setFeature("http://xml.org/sax/features/external-parameter-entities",
                                     false);
		   parser1.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
				     false);
	 	   parser2.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                                     false);
	  	   parser1.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
				     false);
		   parser2.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                     false);
		   parser1.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes", 
				     false);
		   parser2.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes",
                                     false);
               } 
	catch (SAXException e) {
                   System.err.println("could not set parser feature");}

	//Ignore whitespace nodes
	Table.ign_ws_nodes=true;

        parser1.parse(args[0]);
        Document doc1=parser1.getDocument();
        parser2.parse(args[1]);
        Document doc2=parser2.getDocument();
	Match match=new Match();
	NodeSet matchings = match.test(doc1, doc2);
	//db.on=false;
	db.p("Creating Edit Script");
	Document delta= EditScript.create(doc1, doc2, matchings); 
	db.p("Created Edit Script");
	Writer writer = new Writer();
                try {
                    writer.setOutput(System.out, "UTF8");
                }
                catch (UnsupportedEncodingException e) {
                    System.err.println("error: Unable to set output. Exiting.");
                    System.exit(1);
                }
	 writer.setCanonical(false);
                writer.write(delta);
	
} catch (Exception e)
	{
	e.printStackTrace();
	}

}
*/
}
		
