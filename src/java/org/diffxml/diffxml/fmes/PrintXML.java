/*
Class to print some XML stuff if we need
 
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

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public class PrintXML
{
public static void print(Node n)
{
//Do different things depending on type of node

switch (n.getNodeType())
	{
	case Node.TEXT_NODE :
		System.out.println(n.getNodeValue());
		break;

	case Node.ATTRIBUTE_NODE :
		System.out.println(n.getNodeName() + " " + n.getNodeValue());
		break;

	case Node.ELEMENT_NODE :
		System.out.println(n.getNodeName());
		//Print out any attributes
		NamedNodeMap att=n.getAttributes();		
		if (att!=null)
			{
			System.out.println("Attributes:");
			for(int i=0; i<att.getLength(); i++)
				print(att.item(i));
			}
		//Print out any text node children
		/*
		NodeList cdr=n.getChildNodes();
		if (cdr!=null)
                        {
			System.out.println("Text Nodes:");
                        for(int i=0; i<cdr.getLength(); i++)
				{
				if (cdr.item(i).getNodeType()==Node.TEXT_NODE)
                                	print(cdr.item(i));
				}
                        }
		*/
		break;
	default: //Dont care
	}	
}
}
