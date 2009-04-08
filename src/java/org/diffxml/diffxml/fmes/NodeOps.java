/*
diffxml and patchxml - diff and patch for XML files

Copyright (C) 2002-2009  Adrian Mouat

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
email: adrian.mouat@gmail.com
 */

package org.diffxml.diffxml.fmes;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

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

 
        String xpath;
        
        if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
            //Slightly special case for attributes as they are considered to
            //have no parent
            ((Attr) n).getOwnerElement();
            xpath = getXPath(((Attr) n).getOwnerElement())
                 + "/@" + n.getNodeName();
            
        } else if (n.getNodeType() == Node.DOCUMENT_NODE) {
            
            xpath = "/";
        } else if (n.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            
            throw new IllegalArgumentException(
                    "DocumentType nodes cannot be identified with XPath");
            
        } else if (n.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            
            ChildNumber cn = new ChildNumber(n);
            xpath = "/node()[" + cn.getXPath() + "]"; 
            
        } else {

            ChildNumber cn = new ChildNumber(n);

            xpath = getXPath(n.getParentNode()) 
                + "/node()[" + cn.getXPath() + "]";
        }
        
        return xpath;
    }
    
    /**
     * Check if node is an empty text node.
     * 
     * @param n The Node to test.
     * @return True if it is a 0 sized text node
     */
    public static boolean nodeIsEmptyText(final Node n) {
        return (n.getNodeType() == Node.TEXT_NODE 
            && n.getNodeValue().length() == 0);
    }
}
