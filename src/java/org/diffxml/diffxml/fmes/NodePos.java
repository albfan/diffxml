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
import org.diffxml.diffxml.DiffXML;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to deal with finding positions of nodes within document.
 */

public final class NodePos
{

    /** XPath of associated node. */
    private String mPath;

    /** Character position of associated node.*/
    private int mCharpos;

    /** Character length of associated node.*/
    private int mLength;

    /** Child number of associated node.*/
    private ChildNumber mChildNo;

    /**
     * Default constructor.
     *
     * @param n The node to find the position of
     */

    public NodePos(Node n)
        {
        assert(n != null);
        //TODO: Find out why these init values!

        mPath = "null";
        mCharpos = -1;
        mLength = -1;

        mChildNo = new ChildNumber(n);

        set(n);
        }

    /**
     * Returns the XPath of the associated node.
     *
     * @return String XPath to the associated node
     */

    public String getXPath()
        {
        return mPath;
        }

    /**
     * Returns the integer character position of the associated node.
     *
     * @return int character position of associated node.
     */

    public int getCharPos()
        {
        return mCharpos;
        }


    /**
     * Returns the length of the associated node.
     *
     * @return int character length of the node.
     */

    public int getLength()
        {
        return mLength;
        }

    /**
     * Finds the DOM child number of a node.
     *
     * @param n node to find the position of
     * @return the DOM child number of the node
     */

    private static int getDOMChildNumber(final Node n)
        {
        return getDOMChildNumber(n, n.getChildNodes());
        }

    /**
     * Finds the DOM child number of a node.
     *
     * @param n        the node to find the position of
     * @param siblings the siblings of n
     * @return         the DOM child number of the node
     */

    private static int getDOMChildNumber(final Node n, final NodeList siblings)
        {
        int cn;

        for (cn = 0; cn < siblings.getLength(); cn++)
            {
            if (NodeOps.checkIfSameNode(n, siblings.item(cn)))
                break;
            }
        return cn;
        }

    /**
     * Get the character position of a node.
     *
     * Finds the character offset at which a node starts.
     *
     * TODO: Findout what method is needed by
     *
     * @param  siblings NodeList containing the given node and its siblings
     * @param  childNum The DOM position of the node within the list
     * @return          the character position of the node
     */

    public static int getCharpos(final NodeList siblings, final int childNum)
        {
        int charpos = 1;
        for (int y = (childNum - 1); y >= 0; y--)
            {
            if (siblings.item(y).getNodeType() == Node.TEXT_NODE)
                {
                charpos = charpos + siblings.item(y).getNodeValue().length();

                DiffXML.LOG.finer(siblings.item(y).getNodeValue()
                        + " charpos " + charpos);
                }
            else
                break;
            }
        return charpos;
        }

    /**
     * Get the character position of a node.
     *
     * Finds the character offset at which a node starts.
     * TODO: Findout what method is needed by
     *
     * @param n the node to find the position of
     * @return int the character offset of the node, starting at 1
     */

    public static int getCharpos(final Node n)
        {
        NodeList siblings = n.getParentNode().getChildNodes();
        int cn = getDOMChildNumber(n, siblings);

        return getCharpos(siblings, cn);
        }


    /**
     * Calculates the piece of XPath for the current position.
     *
     * @param n     the current node
     * @param index the XPath position of the node
     * @param top   true if we are at the top of the XPath
     * @return      the current piece of XPath
     */

    private static String getXPath(final Node n, final int index,
            final boolean top)
        {
        String xpath = "[" + index + "]";
        if (DiffFactory.isUseTagnames())
            {
            //special case for top node
            if (top)
                {
                switch (n.getNodeType())
                    {
                    case Node.TEXT_NODE:
                        xpath = "/text()" + xpath;
                        break;
                    case Node.COMMENT_NODE:
                        xpath = "/comment()" + xpath;
                        break;
                    default:
                        xpath = "/" + n.getNodeName() + xpath;
                    }
                }
            else
                xpath = "/" + n.getNodeName() + xpath;
            }
        else
            xpath = "/node()" + xpath;

        return xpath;
        }

    /**
     * Determines if the previous node is a text node.
     *
     * @param  siblings the siblings of the node
     * @param  i        the DOM position of the node within siblings
     * @return          true if previous node is a text node, otherwise false
     */

    private static boolean isPrevNodeTextNode(final NodeList siblings,
            final int i)
        {
        return ((i > 0)
                && siblings.item(i - 1).getNodeType() == Node.TEXT_NODE);
        }

    /**
     * Determines if the next node is a text node.
     *
     * @param  siblings the siblings of the node
     * @param  i        the DOM position of the node within siblings
     * @return          true if next node is a text node, otherwise false
     */

    private static boolean isNextNodeTextNode(final NodeList siblings,
            final int i)
        {
        return (((i + 1) < siblings.getLength())
                && (siblings.item(i + 1).getNodeType() == Node.TEXT_NODE));
        }

    /**
     * Sets the character position and length values of the node.
     *
     * @param i        the position of node within siblings
     * @param siblings the siblings of the node
     * @param nPos     the NodePos object to set the values on
     */

    private static void setCharPos(final int i, final NodeList siblings,
            final NodePos nPos)
        {
        Node currSib = siblings.item(i);
        DiffXML.LOG.finer("In top i=" + i
                + " currSib.nodevalue=" + currSib.getNodeValue());

        if (isPrevNodeTextNode(siblings, i) || isNextNodeTextNode(siblings, i))
            {
            //Loop backwards through nodes getting lengths if text
            //TODO: check charpos correct for XPath
            nPos.mCharpos = 1;
            for (int y = (i - 1); y >= 0; y--)
                {
                if (siblings.item(y).getNodeType() == Node.TEXT_NODE)
                    {
                    DiffXML.LOG.finer("Extra Text value: "
                            + siblings.item(y).getNodeValue() + "END");
                    nPos.mCharpos = nPos.mCharpos
                            + siblings.item(y).getNodeValue().length();
                    DiffXML.LOG.finer(siblings.item(y).getNodeValue()
                            + " charpos " + nPos.mCharpos);
                    }
                else
                    break;
                }
            //Need length if currSib node text node
            if (currSib.getNodeType() == Node.TEXT_NODE)
                nPos.mLength = currSib.getNodeValue().length();

            }
        }

    /**
     * Calculates the XPath, length and character position of a node.
     *
     * @param n the node to find position of
     */

    public void set(Node n)
        {

        //TODO: Handle nulls better - throw exception?
        //Shouldn't be handed nulls atall

        if (n == null)
            return;

        Node curr = n;
        Node root = curr.getOwnerDocument().getDocumentElement();
        ChildNumber currChildNo = new ChildNumber(curr);

        String xpath = getXPath(curr, currChildNo.getXPath(), true);

        NodeList siblings = curr.getParentNode().getChildNodes();
        setCharPos(currChildNo.getDOM(), siblings, this);

        while (!NodeOps.checkIfSameNode(root, curr))
            {
            curr = curr.getParentNode();

            currChildNo.setChildNumber(curr);
            xpath = getXPath(curr, currChildNo.getXPath(), false) + xpath;
            }

        mPath = xpath;
        }
}

