/*
Program to apply a DUL patch
 
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
package org.diffxml.patchxml;


/*
Program to apply DUL patches
*/

import org.diffxml.diffxml.*;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.*;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.NamedNodeMap;
import javax.xml.transform.TransformerException;
import org.apache.xerces.dom.NodeImpl;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import org.xml.sax.SAXException;
import java.io.File;

public class PatchXML
{
    private static final boolean OUTPUT_DEBUG=false;
static boolean reverse=false;

//Currently breaks from unix patch, file not implicitly written to
//To change this set the boolean dryrun to false
//You will then need to use -dry-run to avoid overwriting files

static boolean dryrun=true;
static String file1;
static String file2;

public static void parseArgs(String[] args)
{
int i=0;
char flag;
String arg;

while (i < args.length && args[i].startsWith("-"))
        {
        arg = args[i++];
 
        //Normalize multiple dashes
        //Don't understand point in differentiating between 1 and 2 dashes
        //We allow 2 in order to mimic patch util
 
        if (arg.startsWith("--"))
                arg=arg.substring(1);
 
        //"wordy" arguments
        if (arg.equals("-version"))
            printVersion();
        else if (arg.equals("-help"))
            printHelp();
        else if (arg.equals("-dry-run"))
            dryrun=true;
	else if (arg.equals("-reverse"))
	    reverse=true;

	//(series of) flag arguments
        else 
	    {
       	    for (int j = 1; j < arg.length(); j++) 
		{
                flag = arg.charAt(j);
                switch (flag) 
		    {
                    case 'V':
                        printVersion();
                        break;
		    case 'h':
			printHelp();
		    case 'd':
			dryrun=true;
		    case 'R':
			reverse=true;
		    
		    default:
			System.err.println("PatchXML: illegal option " + flag);
                        System.exit(2);
                        break;
		    }
		}
	    }
	}
if ((i+2) != args.length)
        printUsage();
 
file1=args[i];
//db.p("file1= " + file1);
file2=args[++i];
//db.p("file2= " + file2);
		    
}

public static void printUsage()
{
System.err.println("Usage: patch [OPTION]... [ORIGFILE [PATCHFILE]]");
System.exit(2);
}

public static void printHelp()
{
System.out.print("\nUsage: patch [OPTION]... [ORIGFILE [PATCHFILE]]\n\nApply a diffxml file to one of the original XML files.\n\n --version  -V  Output version number of program.\n --help     -h  Print summary of options and exit. \n --dry-run  -d  Print results of applying the changes without modifying any files. \n --reverse  -R  Assume that the delta file was created with the old and new files swapped.\n\tAttempt to reverse sense of change before applying it, e.g. inserts become deletes.\n\n");
System.out.print("\nThis product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).\n\n");
System.out.print("\nThis product includes software developed by the Apache Software Foundation (http://www.apache.org/).\n\n");
System.exit(0);
}

public static void printVersion()
{
System.out.println("patchxml Version 0.9");
System.out.print("\nThis product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).\n");
System.out.print("\nThis product includes software developed by the Apache Software Foundation (http://www.apache.org/).\n\n");
System.exit(0);
}

                                                                                                                     
public void apply(Document doc, Document patch)
{
NodeIterator ni = ((DocumentTraversal) patch).createNodeIterator
			(patch.getDocumentElement(), NodeFilter.SHOW_ELEMENT, null, false);

Node op = ni.nextNode();
//Check we have a delta
if (!op.getNodeName().equals("delta"))
	{
	System.err.println("Not a delta document!");
	return;
	}

//Cycle through elements apply ops
op = ni.nextNode();

int instr=0;
while (op!=null)
	{
	String op_name=op.getNodeName();
	NamedNodeMap op_attrs=op.getAttributes();
	instr++;
	DiffXML.log.finer(instr + " " + op);
	/*
	if (op_name.equals("update"))
		{
		Node node;	
		try {
			node=XPathAPI.selectSingleNode
				(doc.getDocumentElement(), op_attrs.getNamedItem("node").getNodeValue());

			//Currently all update does is change the name
			//This actually breaks the DUL but its a quick hack to get things working

			Element new_node=doc.createElement(op_attrs.getNamedItem("name").getNodeValue());
			
		    }
		catch (TransformerException e)
                        { System.err.println("Could not resolve XPath for parent for update");}
		}
	*/
	if (op_name.equals("insert"))
		{
		DiffXML.log.fine("Applying insert");
		/*
		============
		Apply Insert
		============
		*/
		//For all NodeTypes, find parent
		Node parent;
		Node ins;
		try {
		parent=XPathAPI.selectSingleNode
			(doc.getDocumentElement(), op_attrs.getNamedItem("parent").getNodeValue());
		DiffXML.log.finer("Insert as child of " + parent.getNodeName());

		//Different ops dependent on NodeType being inserted
		
		
		int type=new Integer( op_attrs.getNamedItem("nodetype").getNodeValue() ).intValue();
		//Note setting cn to 0 
		int xpath_cn=0;
		int dom_cn=0;
		if (op_attrs.getNamedItem("childno") != null)
			xpath_cn=new Integer( op_attrs.getNamedItem("childno").getNodeValue() ).intValue();

		//Convert xpath childno to DOM childno

		//Need child nodes in all cases but attr
		//dom_cn needs to store *first* node eqv to XPath
		NodeList doc_kids = parent.getChildNodes();
		if (type!=Node.ATTRIBUTE_NODE)
			{
			int index=0;
			int j;
			for (j=0;j<doc_kids.getLength();j++)
				{
				index++;		
				if (j>0 && doc_kids.item(j).getNodeType()==Node.TEXT_NODE 
					&& doc_kids.item(j-1).getNodeType()==Node.TEXT_NODE)
					index--;
				if (index==xpath_cn)
					break;
				}
			dom_cn=j;	
			
			}

		//Get value to insert if any
		String value="";
		NodeList op_kids = op.getChildNodes();

		if (op_kids.getLength() > 1)
			System.err.println("Unexpected kiddy winkles in insert operation");
		else if ( (op_kids.getLength() == 1) && (op_kids.item(0).getNodeType()==Node.TEXT_NODE) )
			value=op_kids.item(0).getNodeValue();
			
		int charpos=1;
                if (op_attrs.getNamedItem("charpos") != null)
                        charpos=new Integer( op_attrs.getNamedItem("charpos").getNodeValue() ).intValue();
		boolean append=false;	
		switch (type)
			{
			case Node.TEXT_NODE:
				ins=doc.createTextNode(value);
				DiffXML.log.finer("dom_cn=" + dom_cn + " Inserting text: " + value);
				//careful with cn
				if (dom_cn==doc_kids.getLength() &&
                                        doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
                                        {
                                        if (charpos<=1)
                                                {
                                                //Assume we actually mean to append node
                                                append=true;
                                                }
                                        else
                                                {
                                                //Move back to the start of the text nodes
                                                while (dom_cn>0 && doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
                                                        dom_cn--;
 
                                                //move to charpos
                                                while ( charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        charpos=charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if (dom_cn==doc_kids.getLength() || doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
                                                                {
                                                                //System.err.println("charpos beyond end of text");
                                                                append=true;
                                                                parent.appendChild(ins);
                                                                break;
                                                                }
                                                        }
                                                }
                                        if (!append)
                                                parent.insertBefore(ins,doc_kids.item(dom_cn));
 
                                        }
				else if (doc_kids.getLength()>(dom_cn) )
					{
					//We have a node to insert before
					//Do the charpos thingy.
					if (doc_kids.item(dom_cn).getNodeType()==Node.TEXT_NODE)
						{
						//dom_cn is first text node
						//move to charpos
						while ( charpos>doc_kids.item(dom_cn).getNodeValue().length())
							{	
							charpos=charpos-doc_kids.item(dom_cn).getNodeValue().length();
							dom_cn++;
							if (dom_cn==doc_kids.getLength() || doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
								{
								System.err.println("charpos beyond end of text");
								append=true;
								parent.appendChild(ins);
								break;
								}
							}
						//charpos in current node.
						//Won't consider splitting nodes at mo as unneccesary
						}
					if ( charpos!=1)
						System.err.println("charpos seems to be out");
					if (!append)
						parent.insertBefore(ins,doc_kids.item(dom_cn));
					}
				else
					parent.appendChild(ins);
				break;
			case Node.ELEMENT_NODE:
				String tag=op_attrs.getNamedItem("name").getNodeValue();
				
				ins=doc.createElement(tag);
				if (dom_cn==doc_kids.getLength() && 
					doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
					{
					if (charpos<=1)	
						{
						//Assume we actually mean to append node
						append=true;
						}
					else 
						{
						//Move back to the start of the text nodes
						while (dom_cn>0 && doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE) 
							dom_cn--;

						//move to charpos
						while ( charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        charpos=charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if (dom_cn==doc_kids.getLength() || doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
                                                                {
                                                                //System.err.println("charpos beyond end of text");
                                                                append=true;
                                                                parent.appendChild(ins);
                                                                break;
                                                                }
                                                        }	
						}	
					if (!append)
                                                parent.insertBefore(ins,doc_kids.item(dom_cn));
				
					}
				else if (doc_kids.getLength()>(dom_cn))
                                        {
                                        //We have a node to insert before
                                        //Do the charpos thingy.
                                        if (doc_kids.item(dom_cn).getNodeType()==Node.TEXT_NODE)
                                                {
                                                //dom_cn is first text node
                                                //move to charpos
                                                while ( charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        charpos=charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if ( dom_cn==doc_kids.getLength() || doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
								{
                                                                //System.err.println("charpos beyond end of text");
								append=true;
								parent.appendChild(ins);
								break;
								}
                                                        }
                                                //charpos in current node.
                                                //Won't consider splitting nodes at mo as unneccesary
                                                }
					if (!append)
                                        	parent.insertBefore(ins,doc_kids.item(dom_cn));
                                        }
                                else
                                        parent.appendChild(ins);	
				break;
			case Node.COMMENT_NODE:
				ins=doc.createComment(value);
				if (dom_cn==doc_kids.getLength() &&
                                        doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
                                        {
                                        if (charpos<=1)
                                                {
                                                //Assume we actually mean to append node
                                                append=true;
                                                }
                                        else
                                                {
                                                //Move back to the start of the text nodes
                                                while (dom_cn>0 && doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
                                                        dom_cn--;
 
                                                //move to charpos
                                                while ( dom_cn==doc_kids.getLength() || charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        charpos=charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if (doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
                                                                {
                                                                //System.err.println("charpos beyond end of text");
                                                                append=true;
                                                                parent.appendChild(ins);
                                                                break;
                                                                }
                                                        }
                                                }
                                        if (!append)
                                                parent.insertBefore(ins,doc_kids.item(dom_cn));
 
                                        }
				else if (doc_kids.getLength()>(dom_cn) )
                                        {
                                        //We have a node to insert before
                                        //Do the charpos thingy.
                                        if (doc_kids.item(dom_cn).getNodeType()==Node.TEXT_NODE)
                                                {
                                                //dom_cn is first text node
                                                //move to charpos
                                                while ( charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        charpos=charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if (dom_cn==doc_kids.getLength() || doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
								{
                                                                //System.err.println("charpos beyond end of text");
								append=true;
								parent.appendChild(ins);
								break;
								}
                                                        }
                                                //charpos in current node.
                                                //Won't consider splitting nodes at mo as unneccesary
                                                }
					if (!append)
                                        	parent.insertBefore(ins,doc_kids.item(dom_cn));
                                        }
                                else
                                        parent.appendChild(ins);
                                break;	
			case Node.ATTRIBUTE_NODE:
				String name=op_attrs.getNamedItem("name").getNodeValue();
				( (Element) parent).setAttribute(name, value);
				break;
			default:
				System.err.println("Unknown NodeType " + type);
				return;
			}	
					
				
		} catch (TransformerException e) 
			{ System.err.println("Could not resolve XPath for parent for insert");}

		}
	else if (op_name.equals("delete"))
		{
		/*=============
		Applying Delete
		=============*/
		DiffXML.log.fine("Applying delete");
		try {
			//According to API returns *first* match, so should be first text node if text node matched
			NodeImpl del_node=(NodeImpl) XPathAPI.selectSingleNode
                        	(doc.getDocumentElement(), op_attrs.getNamedItem("node").getNodeValue());

			if (del_node==null)
				{
				System.err.println("Could not find node to delete " + op_attrs.getNamedItem("node").getNodeValue());
				break;
				}
			int charpos=1;
                	if (op_attrs.getNamedItem("charpos") != null)
                       		charpos=new Integer( op_attrs.getNamedItem("charpos").getNodeValue() ).intValue();

			//Currently only consider deleting nodes, not part of nodes
			if (del_node.getNodeType()==Node.TEXT_NODE)
				{
				//Get del_node as item of parents child nodelist
				NodeList del_nodelist=del_node.getParentNode().getChildNodes();
				int i=0;
				while ( !del_node.isSameNode(del_nodelist.item(i)) )
					i++;

				/*
				while (charpos>0)
					{	
					charpos=charpos-del_nodelist.item(i).getNodeValue().length();
					i++;
					}
				*/
				while (charpos>del_nodelist.item(i).getNodeValue().length())
					{
					charpos=charpos-del_nodelist.item(i).getNodeValue().length();
					i++;
					}
				del_node=(NodeImpl) del_nodelist.item(i);	
				}
			DiffXML.log.finer("Deleting node " + del_node.getNodeName() + " " + del_node.getNodeValue() );
			if (op_attrs.getNamedItem("length") != null)
				{
				DiffXML.log.finer("Supposed length " + op_attrs.getNamedItem("length").getNodeValue());
				DiffXML.log.finer("Actual length " + del_node.getNodeValue().length());
				}
				
			del_node.getParentNode().removeChild(del_node);

		} catch (TransformerException e)
			{ System.err.println("Could not resolve XPath for node to be deleted");}

		}
	else if (op_name.equals("move"))
		{
		DiffXML.log.fine("Applying move");
		/*=========
		Apply Move
		=========*/
		DiffXML.log.finer("Node: " + op_attrs.getNamedItem("node").getNodeValue() + " Parent" + op_attrs.getNamedItem("parent").getNodeValue());
		DiffXML.log.finer("childno" + op_attrs.getNamedItem("childno").getNodeValue());
		if (op_attrs.getNamedItem("length") != null)
			DiffXML.log.finer("length " + op_attrs.getNamedItem("length").getNodeValue());
			
		//First find the node
		try {
			NodeImpl mov_node=(NodeImpl) XPathAPI.selectSingleNode
                                (doc.getDocumentElement(), op_attrs.getNamedItem("node").getNodeValue());
 
                        if (mov_node==null)
                                System.err.println("Could not find node to move " + op_attrs.getNamedItem("node").getNodeValue());
 
                        int old_charpos=1;
                        if (op_attrs.getNamedItem("old_charpos") != null)
                                old_charpos=new Integer( op_attrs.getNamedItem("old_charpos").getNodeValue() ).intValue(); 
			DiffXML.log.finer("old_charpos= " +old_charpos);
		
	
                        //Currently only consider deleting nodes, not part of nodes
                        if (mov_node.getNodeType()==Node.TEXT_NODE)
                                {
                                //Get del_node as item of parents child nodelist
                                NodeList mov_nodelist=mov_node.getParentNode().getChildNodes();
                                int i=0;
                                while ( !mov_node.isSameNode(mov_nodelist.item(i)) )
                                        i++;
 
				DiffXML.log.finer("i=" +i + " max=" + mov_nodelist.getLength());
				DiffXML.log.finer("length matched node=" + mov_node.getNodeValue().length());
				if (op_attrs.getNamedItem("length") != null)
					DiffXML.log.finer("Supposed length=" + op_attrs.getNamedItem("length").getNodeValue().length());
				if (i>0 && mov_nodelist.item(i-1).getNodeType()==Node.TEXT_NODE)
                                        System.err.println("Failed to find leftmost text node for match");

                                while (old_charpos>1)
                                        {
					DiffXML.log.finer("old_charpos="+old_charpos);
					DiffXML.log.finer("length="+mov_nodelist.item(i).getNodeValue().length());
                                        old_charpos=old_charpos-mov_nodelist.item(i).getNodeValue().length();
                                        i++;
					/*
					if (i==mov_nodelist.getLength())
						i--;
					*/
					if (i==mov_nodelist.getLength() || mov_nodelist.item(i).getNodeType()!=Node.TEXT_NODE)
						break;
					//Probably want to check not greater than 1 b4 break
                                        }

				if (i==mov_nodelist.getLength())
					{
					System.err.println("Something looks wrong in move"); 
					i--;
					}

				 if (op_attrs.getNamedItem("length") != null)
                                        DiffXML.log.finer("Supposed length=" + op_attrs.getNamedItem("length").getNodeValue().length());
				
                                mov_node=(NodeImpl) mov_nodelist.item(i);
				if (mov_node==null)
					System.err.println("old_charpos past end of text node");

				if (op_attrs.getNamedItem("length")!=null)
                                        if (mov_node.getNodeValue().length()!=op_attrs.getNamedItem("length").getNodeValue().length())
						{
                                                System.err.println("!!!!!!!!!\nMoving Wrong Text Node\n!!!!!!!!!");
						System.err.println("i=" + i);
						System.err.println("Actual length" + mov_node.getNodeValue().length());
						}
                                }

			//Find position to move to
			//Get parent
			Node parent=XPathAPI.selectSingleNode
                        	(doc.getDocumentElement(), op_attrs.getNamedItem("parent").getNodeValue());

			//Get XPath childno
			int xpath_cn=0;
			if (op_attrs.getNamedItem("childno") != null)
                        	xpath_cn=new Integer( op_attrs.getNamedItem("childno").getNodeValue() ).intValue();

			//Convert to DOM childno

			NodeList doc_kids = parent.getChildNodes();
			int index=0;
			int dom_cn=0;
                        int j;
                        for (j=0;j<doc_kids.getLength();j++)
                                {
                                index++;
                                if (j>0 && doc_kids.item(j).getNodeType()==Node.TEXT_NODE
                                        && doc_kids.item(j-1).getNodeType()==Node.TEXT_NODE)
                                        index--;
                                if (index==xpath_cn)
                                        break;
                                }
                        dom_cn=j;

			//Get new charpos
			int new_charpos=1;
                        if (op_attrs.getNamedItem("new_charpos") != null)
                                new_charpos=new Integer( op_attrs.getNamedItem("new_charpos").getNodeValue() ).intValue();
			DiffXML.log.finer("new_charpos" + new_charpos);

			//Perform insert
			//Remove should happen automagically
			//Need to check children are moved properly
			boolean append=false; 
			if (dom_cn==doc_kids.getLength() && doc_kids.getLength()!=0 &&
                                        doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
                                        {
                                        if (new_charpos<=1)
                                                {
                                                //Assume we actually mean to append node
                                                append=true;
                                                }
                                        else
                                                {
                                                //Move back to the start of the text nodes
                                                while (dom_cn>0 && doc_kids.item(dom_cn-1).getNodeType()==Node.TEXT_NODE)
                                                        dom_cn--;
 
                                                //move to charpos
                                                while ( new_charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        new_charpos=new_charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if (dom_cn==doc_kids.getLength() || doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
                                                                {
                                                                //System.err.println("charpos beyond end of text");
                                                                append=true;
                                                                parent.appendChild(mov_node);
                                                                break;
                                                                }
                                                        }
                                                }
                                        if (!append)
                                                parent.insertBefore(mov_node,doc_kids.item(dom_cn));
 
                                        }
			else if (doc_kids.getLength()>(dom_cn) )
                        	{
                                //We have a node to insert before
                                //Do the charpos thingy.
                                if (doc_kids.item(dom_cn).getNodeType()==Node.TEXT_NODE)
                                	{
                                        //dom_cn is first text node
                                        //move to charpos
                                                while ( new_charpos>doc_kids.item(dom_cn).getNodeValue().length())
                                                        {
                                                        new_charpos=new_charpos-doc_kids.item(dom_cn).getNodeValue().length();
                                                        dom_cn++;
                                                        if (doc_kids.item(dom_cn).getNodeType()!=Node.TEXT_NODE)
								{
                                                                //System.err.println("charpos beyond end of text");
								append=true;
								if (new_charpos != 1)
									System.err.println("Don't think charpos should be that....");
								//Want to append the node
								parent.appendChild(mov_node);
								break;
								}
                                                        }
                                                //charpos in current node.
                                                //Won't consider splitting nodes at mo as unneccesary
                                                }
					if (!append)
                                        	parent.insertBefore(mov_node,doc_kids.item(dom_cn));
                                        }
                                else
                                        parent.appendChild(mov_node);
			} catch (TransformerException e)
                        	{ System.err.println("Could not resolve XPath for node to be deleted");}
		}	
	else
		{
		System.err.println("Do not recognise element: " + op_name);
		System.exit(2);
		}
	if (OUTPUT_DEBUG)
		{
		try {
		Transformer serializer = TransformerFactory.newInstance().newTransformer();
        	serializer.transform(new DOMSource(doc), new StreamResult(System.out));
        	System.out.println();
		} catch (javax.xml.transform.TransformerException e) {}
		}
	op=ni.nextNode();	
	}

}
public static void main(String[] args)
{
 
parseArgs(args);
//Check files exist
File test=new File(file1);
if (!test.exists())
        {
        System.err.println("Could not find file: " +file1);
        System.exit(2);
        }
test=new File(file2);
if (!test.exists())
        {
        System.err.println("Could not find file: " +file2);
        System.exit(2);
        }

//db.on=true;
try
{
        DOMParser parser1 = new DOMParser();
        DOMParser parser2 = new DOMParser();

	
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
                   //parser1.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                   //                  false);
                   parser2.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                     false);
                   parser1.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes",
                                     false);
                   parser2.setFeature("http://apache.org/xml/features/dom/create-entity-ref-nodes",
                                     false);
               }
        catch (SAXException e) {
                   System.err.println("could not set parser feature");}
	
	parser1.parse(file1);
	parser2.parse(file2);
	Document doc=parser1.getDocument();
	Document patch=parser2.getDocument();
	doc.normalize();
	patch.normalize();
	if (reverse)
		patch=Reverse.go(patch);
	PatchXML p = new PatchXML();
	p.apply(doc, patch);
	//Write doc out to file again
	Transformer serializer = TransformerFactory.newInstance().newTransformer();
	if (dryrun)
		serializer.transform(new DOMSource(doc), new StreamResult(System.out));
	else
		{
		File f1=new File(file1);
		serializer.transform(new DOMSource(doc), new StreamResult(f1));
		}
	DiffXML.log.finer("PatchXML Doc");
	if (OUTPUT_DEBUG)	
		serializer.transform(new DOMSource(patch), new StreamResult(System.out));
	System.out.println();
}
catch (Exception e)
        {
        e.printStackTrace();
        }

}
}