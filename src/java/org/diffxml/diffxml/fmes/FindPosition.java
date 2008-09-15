package org.diffxml.diffxml.fmes;

import org.diffxml.diffxml.DiffXML;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//TODO: Either inline, remove or move functionality here.

public class FindPosition {
 
    private int insertBefore;
    private int numXPath;
    private int charPosition;
    
    /**
     * Finds the childnumber to insert a node as.
     *
     * (Equivalent to the current childnumber of the node to insert
     * before)
     *
     * @param x         the node to find the position of? *check*
     * @param matchings the set of matching nodes
     */
    public FindPosition(final Node x, final NodePairs matchings) {
        
        DiffXML.LOG.fine("Entered findPos");
        //Node xParent = x.getParentNode();
        //NodeList xSiblings = xParent.getChildNodes();

        Node v = getInOrderLeftSibling(x);

        if (v == null)
            {
            //SHOULD CHAR be 1 or -1? depends on next node. Safest at 1?
            //TODO: Check logic - pretty sure wrong!
            insertBefore = 0;
            numXPath = 1;
            charPosition = 1;
            return;
            }

        //Get partner of v
        Node u = matchings.getPartner(v);
        Node uParent = u.getParentNode();
        NodeList children = uParent.getChildNodes();

        Index ind = getInOrderIndex(u);

        //Need i+1 child
        insertBefore = ++ind.dom;

        //If this is a text node, and last node was a text node,
        //don't increment xpath

        //Get charpos
        //Can't use NodePos func as node may not exist
        charPosition = getCharPosition(ind, u);

        //If this is a text node, and last node was a text node,
        //don't increment xpath
        if (!(x.getNodeType() == Node.TEXT_NODE
                    && (children.item(ind.dom - 1).getNodeType()
                        == Node.TEXT_NODE)))
            ind.xPath++;

        numXPath = ind.xPath;

        DiffXML.LOG.fine("Exiting findPos normally");
        }
    
    /**
     * Gets the "in order" index of the node.
     *
     * The "in order" index counts only nodes marked "in order".
     *
     * TODO: Temporarily made package level access for testing
     *
     * @param n the node to find the index of
     * @return  the "in order" index of the node
     */

    /*package*/ static Index getInOrderIndex(final Node n)
        {
        Index ind = new Index();
        ind.dom = ChildNumber.getInOrderDomChildNumber(n);
        ind.xPath  = ChildNumber.getInOrderXPathChildNumber(n);

        return ind;
        }
 
    /**
     * Calculates the character position at which to insert a node.
     *
     * TODO: Check logic and test
     *
     * @param n    The node to find the char position of
     * @param ind  The index of the node
     * @return     The integer character position to insert at
     */

    private static int getCharPosition(final Index ind, final Node n)
        {
        NodeList children = n.getParentNode().getChildNodes();
        //int tmpIndex = ind.dom;
        //int lastIndex = children.getLength() - 1;

        //if (ind.dom > lastIndex)
        //    tmpIndex = lastIndex;

        int charpos = 1;
        Node test = children.item(ind.dom - 1);
        if (children.item(ind.dom - 1).getNodeType() != Node.TEXT_NODE
                && NodeOps.isInOrder(test))
            charpos = -1;
        else
            {
            for (int j = (ind.dom - 1); j >= 0; j--)
                {
                test = children.item(j);
                if (children.item(j).getNodeType() == Node.TEXT_NODE
                        && NodeOps.isInOrder(test))
                    {
                    charpos = charpos
                            + children.item(j).getNodeValue().length();
                    DiffXML.LOG.finer(children.item(j).getNodeValue()
                            + " charpos " + charpos);
                    }
                else
                    break;
                }
            }
        return charpos;

        }
    
    /**
     * Gets the rightmost left sibling of n marked "inorder".
     *
     * @param n Node to find "in order" left sibling of
     * @return  Either the "in order" left sibling or null if none
     */

    private static Node getInOrderLeftSibling(final Node n)
    {
        //Default l to null
        Node inOrderLeftSib =  null;
        //Changed as I'm not sure nodelists are in order
        /*
         
         NodeList nSiblings = n.getParentNode().getChildNodes();
         
         for (int i = 0; i < nSiblings.getLength(); i++)
         {
         Node test = nSiblings.item(i);
         
         if (NodeOps.checkIfSameNode(test, n))
         break;
         
         if (NodeOps.isInOrder(test))
         l = test;
         }
         
         return l;
         */
        Node curr = n.getParentNode().getFirstChild();
        
        while (curr != null && !NodeOps.checkIfSameNode(curr, n))
        {
            if (NodeOps.isInOrder(curr))
            {
                inOrderLeftSib = curr;
            }
            curr = curr.getNextSibling();
        }
        return inOrderLeftSib;
    }

    public int getInsertBefore() {
        return insertBefore;
    }
    
    public int getXPathNum() {
        return numXPath;
    }
    
    public int getCharPosition() {
        return charPosition;
    }
}
