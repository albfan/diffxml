/*
Program to difference two XML files

Copyright (C) 2002-2005  Adrian Mouat

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

import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to handle general diffxml operations on Nodes.
 *
 */
public final class NodeOps {
    
    /**
     * Key for user data on whether the node is in order.
     */
    private static final String INORDER = "inorder";
    
    /**
     * Disallow instantiation.
     */
    private NodeOps() {
    }
    
    /**
     * Inserts a given node as numbered child of a parent node.
     *
     * If childNum doesn't exist the node is simply appended.
     *
     * @param childNum  the position to add the node to
     * @param parent    the node that is to be the parent
     * @param insNode   the node to be inserted
     * @return The inserted Node
     */
    public static Node insertAsChild(final int childNum, final Node parent,
            final Node insNode) {

        Node ret;
        NodeList kids = parent.getChildNodes();

        if (kids.item(childNum) != null) {
            ret = parent.insertBefore(insNode, kids.item(childNum));
        } else {
            ret = parent.appendChild(insNode);
        }
        
        return ret;
    }

    /**
     * Mark the node as being "inorder".
     *
     * @param n the node to mark as "inorder"
     */
    public static void setInOrder(final Node n) {

        n.setUserData(INORDER, true, null);
    }

    /**
     * Mark the node as not being "inorder".
     *
     * @param n the node to mark as not "inorder"
     */
    public static void setOutOfOrder(final Node n) {
        n.setUserData(INORDER, false, null);
    }

    /**
     * Check if node is marked "inorder".
     *
     * Note that nodes are inorder by default.
     *
     * @param n node to check
     * @return false if UserData set to False, true otherwise
     */
    public static boolean isInOrder(final Node n) {
        
        boolean ret;
        Object data = n.getUserData(INORDER);
        if (data == null) {
            ret = true;
        } else {
            ret = (Boolean) data;
        }
        return ret;
    }


    /**
     * Check if nodes are the same.
     *
     * Does not test if data equivalent, but if same node in same doc.
     * TODO: Handle null cases.
     *
     * @param x first node to check
     * @param y second node to check
     * @return true if same node, false otherwise
     */

    public static boolean checkIfSameNode(final Node x, final Node y) {
        return x.isSameNode(y);
    }

    /**
     * Calculates an XPath that uniquely identifies the given node.
     * For text nodes note that the given node may only be part of the returned
     * node due to coalescing issues; use an offset and length to identify it
     * unambiguously.
     * 
     * @param n The node to calculate the XPath for.
     * @return The XPath to the node as a String
     */
    public static String getXPath(final Node n) {

        Node root = n.getOwnerDocument().getDocumentElement();
        Node curr = n;        

        String xpath;
        
        if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
            //Slightly special case for attributes as they are considered to
            //have no parent
            ((Attr) n).getOwnerElement();
            xpath = getXPath(((Attr) n).getOwnerElement())
                 + "/@" + n.getNodeName();
        } else {
            ChildNumber cn = new ChildNumber(curr);
            if (NodeOps.checkIfSameNode(root, curr)) {
                // Not clear if node() *always* matches the root node 
                xpath = "/" + n.getNodeName();
            } else {
                xpath = getXPath(curr.getParentNode()) 
                + "/node()[" + cn.getXPath() + "]";
            }
        }
        
        return xpath;
    }

    
}
