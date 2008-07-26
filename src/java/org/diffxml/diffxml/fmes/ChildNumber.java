/*
Program to difference two XML files

Copyright (C) 2002-2004  Adrian Mouat

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

import org.diffxml.diffxml.DiffFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to hold and calculate DOM and XPath child numbers of node.
 */

public final class ChildNumber
{
    /** DOM child number. */
    private int mDOMChildNumber;

    /** XPath child number. */
    private int mXPathChildNumber;

    /**
     * Default constructor.
     *
     * @param n Node to find the child numbers of
     */

    public ChildNumber(final Node n)
        {
        assert(n != null);
        setChildNumber(n);
        }

    /**
     * Get the DOM child number.
     *
     * @return DOM child number of associated node.
     */

    public int getDOM()
        {
        return mDOMChildNumber;
        }

    /**
     * Get the XPath child number.
     *
     * @return XPath child number of associated node.
     */

    public int getXPath()
        {
        return mXPathChildNumber;
        }

    /**
     * Determines whether XPath index should be incremented.
     *
     * Handles differences between DOM index and XPath index
     *
     * @param tag      the tag of the node we are looking for
     * @param siblings the siblings of the node we are looking for
     * @param i        the current position in siblings
     * @return         true if index should be incremented
     */

    private static boolean incIndex(final String tag,
            final NodeList siblings, final int i)
        {
        //TODO: Handle comments if needed.

        Node currSib = siblings.item(i);

        boolean inc = true;
        if (DiffFactory.isUseTagnames())
            {
            if (!tag.equals(currSib.getNodeName()))
                inc = false;
            }
        if (currSib.getNodeType() == Node.DOCUMENT_TYPE_NODE)
        {
            inc = false;
        }

        //Handle non-coalescing of text nodes
        if ((i > 0) && (inc == true))
            {
            if (currSib.getNodeType() == Node.TEXT_NODE
                    && siblings.item(i - 1).getNodeType() == Node.TEXT_NODE)
                inc = false;
            }

        return inc;
        }

    /**
     * Calculate child numbers of node.
     *
     * @param n the node to find the child numbers of
     */

    public void setChildNumber(final Node n)
        {
  
        mXPathChildNumber = getXPathChildNumber(n);
        mDOMChildNumber= getDomChildNumber(n);
        }
    
    public static int getDomChildNumber(final Node n)
    {
        //This isn't checking tagnames mofo 
        NodeList siblings = n.getParentNode().getChildNodes();
        int childNo = 0;
        
        for (childNo = 0; childNo < siblings.getLength(); childNo++)
        {
            if (NodeOps.checkIfSameNode(siblings.item(childNo), n))
            {
                break;
            }
        }
        
        return childNo;
    }

    public static int getInOrderDomChildNumber(final Node n)
    {
        //This isn't checking tagnames mofo 
        NodeList siblings = n.getParentNode().getChildNodes();
        int childNo = 0;
        
        for (childNo = 0; childNo < siblings.getLength(); childNo++)
        {
            if (NodeOps.isInOrder(siblings.item(childNo)) &&
                    NodeOps.checkIfSameNode(siblings.item(childNo), n))
            {
                break;
            }
        }
        
        return childNo;
    }
    
    public static int getXPathChildNumber(final Node n)
    {
        assert(n != null);

        NodeList siblings = n.getParentNode().getChildNodes();
        int childNo = 0;
        
        for (int i = 0; i < siblings.getLength(); i++)
        {
            if (incIndex(n.getNodeName(), siblings, i))
            {
                childNo++;
            }
            if (NodeOps.checkIfSameNode(siblings.item(i), n))
            {
                break;
            }

        }
        return childNo;
    }
    
    public static int getInOrderXPathChildNumber(final Node n)
    {
        NodeList siblings = n.getParentNode().getChildNodes();
        int childNo = 0;
        
        for (int i = 0; i < siblings.getLength(); i++)
        {
            if (NodeOps.isInOrder(siblings.item(i)) &&
                    incIndex(n.getNodeName(), siblings, i))
            {
                childNo++;
            }
            if (NodeOps.checkIfSameNode(siblings.item(i), n))
            {
                break;
            }

        }
        return childNo;
    }

}
