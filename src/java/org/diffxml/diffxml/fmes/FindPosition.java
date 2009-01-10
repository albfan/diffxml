package org.diffxml.diffxml.fmes;

import org.diffxml.diffxml.DiffXML;
import org.w3c.dom.Node;

/**
 * Finds the position to insert a Node at.
 * 
 * Calculates XPath, DOM and character position.
 * @author Adrian Mouat
 *
 */
public class FindPosition {
 
    /** The DOM position. */
    private int mInsertPositionDOM;
    
    /** The XPath position. */
    private int mInsertPositionXPath;
    
    /** The character position. */
    private int mCharInsertPosition;
    
    /**
     * Finds the child number to insert a node as.
     *
     * (Equivalent to the current child number of the node to insert
     * before)
     *
     * @param x         the node with no partner
     * @param matchings the set of matching nodes
     */
    public FindPosition(final Node x, final NodePairs matchings) {

        DiffXML.LOG.fine("Entered findPos");

        Node v = getInOrderLeftSibling(x);

        if (v == null) {
            
            mInsertPositionDOM = 0;
            mInsertPositionXPath = 1;
            mCharInsertPosition = 1;
            
        } else {

            /**
             * Get partner of v and return index after
             * (we want to insert after the previous in-order node, so that
             * w's position is equivalent to x's).
             */
            Node u = matchings.getPartner(v);
            assert (u != null);

            ChildNumber uChildNo = new ChildNumber(u);

            //Need position after u
            mInsertPositionDOM = uChildNo.getInOrderDOM() + 1;
            mInsertPositionXPath = uChildNo.getInOrderXPath() + 1;

            //For xpath, character position is used if node is text node
            if (u.getNodeType() == Node.TEXT_NODE) {
                mCharInsertPosition = uChildNo.getInOrderXPathCharPos()
                        + u.getTextContent().length();
            } else {
                mCharInsertPosition = 1;
            }
        }

        DiffXML.LOG.fine("Exiting findPos normally");
    }
    
    /**
     * Gets the rightmost left sibling of n marked "inorder".
     *
     * @param n Node to find "in order" left sibling of
     * @return  Either the "in order" left sibling or null if none
     */
    private static Node getInOrderLeftSibling(final Node n) {
        
        Node curr = n.getPreviousSibling();
        while (curr != null && !NodeOps.isInOrder(curr)) {
            curr = curr.getPreviousSibling();
        }

        return curr;
    }

    /**
     * Returns the DOM number the node should have when inserted.
     * 
     * @return the DOM number to insert the node as
     */
    public final int getDOMInsertPosition() {
        return mInsertPositionDOM;
    }
    
    /**
     * Returns the XPath number the node should have when inserted.
     * 
     * @return The XPath number to insert the node as
     */
    public final int getXPathInsertPosition() {
        return mInsertPositionXPath;
    }
    
    /**
     * Returns the character position to insert the node as.
     * 
     * @return The character position to insert the node at
     */
    public final int getCharInsertPosition() {
        return mCharInsertPosition;
    }
}
