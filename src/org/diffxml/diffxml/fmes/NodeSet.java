/*
Class to hold set of nodes
 
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

import org.diffxml.*; 
import org.w3c.dom.Node;
import org.apache.xerces.dom.NodeImpl;
import java.util.ArrayList;

public class NodeSet
{

private ArrayList set= new ArrayList();
public void add(Node x, Node y)
{
set.add((NodeImpl) x);
set.add((NodeImpl) y);
}

public void print_set()
{
for (int i=0; i<set.size(); i=i+2)
	{
	//Print out each node
	System.out.println("Node: " + ( (Node) set.get(i)).getNodeName());
	PrintXML.print((Node) set.get(i));
	System.out.println("Matches: " + ( (Node) set.get(i+1)).getNodeName());
	PrintXML.print((Node) set.get(i+1));
	}
}

public Node getPartner(Node n)
{
    NodeImpl x = (NodeImpl) n;
int in=set.indexOf( n);
//in++;
if ((in % 2) == 1)
	in--;
else
	in++;

return (Node) set.get(in);
}
}

