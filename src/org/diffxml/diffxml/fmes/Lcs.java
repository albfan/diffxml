/*
Class to solve longest common subsequence for NodeLists
 
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

//Very Simple & ineffecient LCS implementation
//Want to make equal func that can passed in

import org.diffxml.diffxml.*; 
import org.w3c.dom.Node;
import org.apache.xerces.dom.NodeImpl;
import org.w3c.dom.NodeList;

public class Lcs
{
public static Node[] find(NodeList a, NodeList b, NodeSet matchings)
{
//Need to be careful in cases with empty string
//(Can they arise?)
//If either one is empty, so is LCS

int a_len= a.getLength();
int b_len= b.getLength();

if (a_len==0 || b_len==0)
	{
	Node L[]= new Node[1];
	return L;
	}

int i=0, j=0;
int M[][] = new int[a_len+1][b_len+1];
M[0][0]=0;

for (i=1; i<=a_len; i++)
	{
	for (j=1; j<=b_len; j++)
		{
		//if (( (Node3) a.item(i-1)).isEqualNode(b.item(j-1),false))
		if ( ((NodeImpl) matchings.getPartner(a.item(i-1))).isSameNode(b.item(j-1)))
			{
			M[i][j]=M[i-1][j-1] +1;
			}
		else
			{
			if (M[i-1][j] > M[i][j-1])
				M[i][j]=M[i-1][j];
			else
				M[i][j]=M[i][j-1];
			}
		}
	}

//reconstruction
i=(a_len-1);
j=(b_len-1);

int r=0;
DiffXML.log.finer("i=" + i + " j=" + j);
int seq_ln=M[i][j];

DiffXML.log.finer("seq_ln=" + seq_ln);
//Store pairs of sequence
Node L[] = new Node[(2*seq_ln)+2];
while (i>=0 && j>=0)
	{
	 if (( (NodeImpl) matchings.getPartner(a.item(i))).isSameNode(b.item(j)))
		{
		L[r]=a.item(i);
		L[r+1]=b.item(j);
		r=r+2;
		i--;
		j--;
		}
	else 
		{
		//Not sure about cases with zero
		if (j==0)
			i--;
		else if (i==0)
			j--;
		else if (M[i-1][j]>M[i][j-1])
			i--;
		else
			j--;
		}
	}
//Should now reverse list
//But it doesn't matter for our needs	
return L;
}
}
	
