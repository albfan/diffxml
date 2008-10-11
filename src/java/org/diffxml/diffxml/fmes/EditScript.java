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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.diffxml.diffxml.DiffFactory;
import org.diffxml.diffxml.DiffXML;


import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Creates the edit script for the fmes algorithm.
 *
 * Uses the algorithm described in the paper
 * "Change Detection in Hierarchically Structure Information".
 *
 * @author Adrian Mouat
 */
public final class EditScript {
    
    private static final String RESOLVE_ENTITIES = "resolve_entities";
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    private static final String REVERSE_PATCH = "reverse_patch";
    private static final String PARENT_SIBLING_CONTEXT = "par_sib_context";
    private static final String PARENT_CONTEXT = "par_context";
    private static final String SIBLING_CONTEXT = "sib_context";
    private static final String DELTA = "delta";
    
    /**
     * Whether dummy root nodes have been added to the tree.
     */
    private boolean mAddedDummyRoot = false;
    
    /**
     * The original document.
     */
    private final Document mDoc1;
    
    /**
     * The modified document.
     */
    private final Document mDoc2;
    
    /**
     * The set of matching nodes.
     */
    private NodePairs mMatchings;
    
    private Document mEditScript;
    
    /**
     * Constructor for EditScript.
     * Used to create a list of modifications that will turn doc1 into doc2,
     * given a set of matching nodes.
     * 
     * @param doc1      the original document
     * @param doc2      the modified document
     * @param matchings the set of matching nodes
     */
    public EditScript(final Document doc1, final Document doc2,
            final NodePairs matchings) {
        
        mDoc1 = doc1;
        mDoc2 = doc2;
        mMatchings = matchings;
    }
    
    /**
     * Creates an Edit Script conforming to matchings that transforms
     * doc1 into doc2.
     *
     * Uses algorithm in "Change Detection in Hierarchically Structured
     * Information".
     *
     * @return the resultant Edit Script
     * @throws DocumentCreationException When the output doc can't be made
     */
    public Document create() throws DocumentCreationException {

        try {
            mEditScript = makeEmptyEditScript();
        } catch (ParserConfigurationException e) {
            throw new DocumentCreationException("Failed to create edit script",
                    e);
        }

        matchRoots();

        // Fifo used to do a breadth first traversal of doc2
        NodeFifo fifo = new NodeFifo();
        fifo.addChildrenOfNode(mDoc2.getDocumentElement());

        while (!fifo.isEmpty()) {
            
            DiffXML.LOG.fine("In breadth traversal");

            Node x = fifo.pop();
            fifo.addChildrenOfNode(x);

            Node y = x.getParentNode();
            Node z = mMatchings.getPartner(y);

            logNodes(x, y, z);
            Node w;

            if (!mMatchings.isMatched(x)) {
                w = doInsert(x, z);
            } else {
                // TODO: Update should go here
                w = doMove(x, z, mEditScript, mMatchings);
            }

            alignChildren(w, x, mEditScript, mMatchings);
        }

        deletePhase(mDoc1.getDocumentElement(), mEditScript, mMatchings);

        // Post-Condition es is a minimum cost edit script,
        // Matchings is a total matching and
        // doc1 is isomorphic to doc2

        return mEditScript;
    }

    /** Prepares an empty Edit Script document.
     *
     * Makes root element, appends any necessary attributes
     * and context information.
     *
     * @return a properly formatted, empty edit script
     * @throws ParserConfigurationException If a new document can't be created
     */

    private static Document makeEmptyEditScript() 
    throws ParserConfigurationException {

        DocumentBuilder builder = 
            DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document editScript = builder.newDocument();

        Element root = editScript.createElement(DELTA);

        //Append any context information
        if (DiffFactory.isContext()) {
            root.setAttribute(SIBLING_CONTEXT, 
                    Integer.toString(DiffFactory.getSiblingContext()));
            root.setAttribute(PARENT_CONTEXT,
                    Integer.toString(DiffFactory.getParentContext()));
            root.setAttribute(PARENT_SIBLING_CONTEXT,
                    Integer.toString(DiffFactory.getParentSiblingContext()));
        }

        if (DiffFactory.isReversePatch()) {
            root.setAttribute(REVERSE_PATCH, TRUE);
        }

        if (!DiffFactory.isResolveEntities()) {
            root.setAttribute(RESOLVE_ENTITIES, FALSE);
        }

        editScript.appendChild(root);

        return editScript;
    }

    /**
     * Handles non-matching root nodes.
     *
     * TODO: Make private. Package for temporary testing.
     *
     */

    /*package*/ void matchRoots() {


        Element xRoot = mDoc2.getDocumentElement();
        Node xPartner = mMatchings.getPartner(xRoot);
        if (xPartner == null 
                || !(xPartner.equals(mDoc1.getDocumentElement()))) {
            
            Element dummy1 = mDoc1.createElement("DUMMY");
            dummy1.appendChild(mDoc1.getDocumentElement());
            mDoc1.appendChild(dummy1);
            Element dummy2 = mDoc2.createElement("DUMMY");
            dummy2.appendChild(xRoot);
            mDoc2.appendChild(dummy2);
            mMatchings.add(mDoc2.getDocumentElement(), 
                    mDoc1.getDocumentElement());
            NodeOps.setInOrder(mDoc1.getDocumentElement());
            NodeOps.setInOrder(mDoc2.getDocumentElement());
            
            mAddedDummyRoot = true;

        }

    }

    /**
     * Inserts (the import of) node x as child of z according to the algorithm 
     * and updates the Edit Script.
     *
     * @param x          current node
     * @param z          partner of x's parent
     * @return           the inserted node
     */
    private Node doInsert(final Node x, final Node z) {

        assert (x != null);
        assert (z != null);
        
        //Supposed to find the child number (k) to insert w as child of z 
        FindPosition pos = new FindPosition(x, mMatchings);
        NodePos zPath = new NodePos(z);

        //Apply insert to doc1
        //The node we want to insert is the import of x with all
        //its text node children
        //TODO: Ensure attributes properly added
        //TODO: Check we want text node children

        Node w = mDoc1.importNode(x, false);

        //Need to set in order as won't be revisited
        NodeOps.setInOrder(w);
        NodeOps.setInOrder(x);

        //Take match of parent (z), and insert
        NodeOps.insertAsChild(pos.getDOMInsertPosition(), z, w);

        //Add to matching set
        mMatchings.add(w, x);

        //System.err.println("insert: " + pos.numXPath + " " + pos.insertBefore + " " +w.getNodeValue());
        Delta.insert(w, zPath.getXPath(), pos.getXPathInsertPosition(), pos.getCharInsertPosition(), mEditScript);

        Delta.addAttrsToDelta(x.getAttributes(), new NodePos(w).getXPath(), mEditScript);

        return w;
        }

    /**
     * Performs a move operation according to the algorithm and updates
     * the EditScript.
     *
     * @param x          current node
     * @param z          the partner of x's parent
     * @param editScript the Edit Script to append operations to
     * @param matchings  the set of matching nodes
     * @return           the moved node
     */

    private static Node doMove(final Node x, final Node z,
            final Document editScript, final NodePairs matchings)
        {
        FindPosition pos;

        Node w = matchings.getPartner(x);

        Node v = w.getParentNode();
        Node y = x.getParentNode();

        //UPDATE would be here if implemented

        //Apply move if parents not matched and not null

        Node partnerY = matchings.getPartner(y);
        if ((v.getNodeType() != Node.DOCUMENT_NODE) && !NodeOps.checkIfSameNode(v, partnerY))
            {
            pos = new FindPosition(x, matchings);
            NodePos wPath = new NodePos(w);
            NodePos zPath = new NodePos(z);

            //Following two statements may be unnecessary
            NodeOps.setInOrder(w);
            NodeOps.setInOrder(x);

            //TODO: Make function for following
            //Check not already one!
            Element context = editScript.createElement("mark");
            if (DiffFactory.isContext())
                {
                context.appendChild(editScript.importNode(w, true));
                context = Delta.addContext(w, context);
                }

            //Apply move to T1
            NodeOps.insertAsChild(pos.getDOMInsertPosition(), z, w);

            Delta.move(context, w, wPath.getXPath(), zPath.getXPath(), pos.getXPathInsertPosition(),
                     wPath.getCharPos(), pos.getCharInsertPosition(), wPath.getLength(), editScript);
            }
        return w;
        }

    /**
     * Logs the names and values of 3 nodes.
     *
     * Debug thang.
     * Note we stupidly do the same thing 3 times and lose generality.
     * TODO: Move to helper class.
     *
     * @param x  first node
     * @param y  second node
     * @param z  third node
     */

    private static void logNodes(final Node x, final Node y, final Node z)
    {
        if (x == null) 
        {
            DiffXML.LOG.warning("x= null");
        } else {
            DiffXML.LOG.finer("x=" + x.getNodeName() + " " + x.getNodeValue());
        }
        if (y == null) {
            DiffXML.LOG.warning("y= null");
        } else {
            DiffXML.LOG.finer("y=" + y.getNodeName() + " " + y.getNodeValue());
        }

        if (z == null) {
            DiffXML.LOG.warning("z= null. Check matchings may be root prob");
        } else {
            DiffXML.LOG.finer("z=" + z.getNodeName() + " " + z.getNodeValue());
        }
    }


    /**
     * Performs the deletePhase of the algorithm.
     *
     * @param n          the current node
     * @param editScript the Edit Script to append operations to
     */

    private static void deletePhase(final Node n, final Document editScript, 
            final NodePairs matchings)
        {
        //Deletes nodes in Post-order traversal
        NodeList kids = n.getChildNodes();
        if (kids != null)
            {
            //Note that we loop *backward* through kids
            for (int i = (kids.getLength() - 1); i >= 0; i--)
                {
                //Don't call delete phase for ignored ndoes
                if (Fmes.isBanned(kids.item(i)))
                    continue;

                deletePhase(kids.item(i), editScript, matchings);
                }
            }

        //If node isn't matched, delete it
        //TODO: Make function for following.
        if (!matchings.isMatched(n))
            {
            Element par = (Element) n.getParentNode();
            NodePos delPos = new NodePos(n);

            Delta.delete(n, delPos.getXPath(), delPos.getCharPos(),
                    delPos.getLength(), editScript);
            par.removeChild(n);
            }

        }

    /**
     * Mark the children of a node out of order.
     *
     * Not sure about the ignoring of banned nodes.
     * May very well f up move.
     * Move to helper class if of use to other classes.
     *
     * @param n the parent of the nodes to mark out of order
     */

    private static void markChildrenOutOfOrder(final Node n)
        {
        NodeList kids = n.getChildNodes();

        for (int i = 0; i < kids.getLength(); i++)
            {
            if (Fmes.isBanned(kids.item(i)))
                continue;

            NodeOps.setOutOfOrder(kids.item(i));
            }
        }

    /**
     * Marks the nodes in the given array "inorder".
     *
     * Move to helper class if of use to other classes.
     *
     * @param seq  the nodes to mark "inorder"
     */

    private static void setInOrderNodes(final Node[] seq)
        {
        for (int i = 0; i < seq.length; i++)
            {
            if (seq[i] != null)
                {
                DiffXML.LOG.finer("seq" + seq[i].getNodeName()
                        + " " + seq[i].getNodeValue());

                NodeOps.setInOrder(seq[i]);
                }
            }
        }

    /**
     * Gets the nodes in set1 which have matches in set2.
     *
     * This is done in a way that is definitely sub-optimal.
     * May need to shrink array size at end.
     *
     * Move to helper class?
     * Should probably be in own class, actual algorithm should be hidden.
     *
     * @param set1      the first set of nodes
     * @param set2      the set of nodes to match against
     * @param matchings the set of matching nodes
     *
     * @return      the nodes in set1 which have matches in set2
     */

    private static Node[] getSequence(final NodeList set1, final NodeList set2,
            final NodePairs matchings)
        {
        Node[] resultSet = new Node[set1.getLength()];

        int index = 0;
        for (int i = 0; i < set1.getLength(); i++)
            {
            Node wanted = matchings.getPartner(set1.item(i));

            for (int j = 0; j < set2.getLength(); j++)
                {
                if (NodeOps.checkIfSameNode(wanted, set2.item(j)))
                    {
                    resultSet[index++] = set1.item(i);
                    break;
                    }
                }
            }
        return resultSet;
        }

    /**
     * Gets any required context nodes before performing move.
     *
     * @param editScript needed to create element
     * @param a          the node to get context of
     * @return           the context element
     */

    private static Element getContextBeforeMove(final Document editScript, final Node a)
        {
        Element context = editScript.createElement("mark");
        if (DiffFactory.isContext())
            {
            context.appendChild(editScript.importNode(a, true));
            context = Delta.addContext(a, context);
            }

        return context;
        }

    /**
     * Moves nodes that are not in order to correct position.
     *
     * TODO: Check logic.
     *
     * @param xKids
     * @param w
     * @param editScript
     * @param matchings
     */

    private static void moveMisalignedNodes(final NodeList xKids, final Node w,
            final Document editScript, final NodePairs matchings)
        {
        //Select nodes matched but not in order
        for (int i = 0; i < xKids.getLength(); i++)
            {
            if ((!NodeOps.isInOrder(xKids.item(i)))
                    && matchings.isMatched(xKids.item(i)))
                {
                //Get childno for move
                FindPosition pos = new FindPosition(xKids.item(i), matchings);

                //Get partner and position
                Node a = matchings.getPartner(xKids.item(i));
                NodePos aPos = new NodePos(a);

                NodePos wPos = new NodePos(w);

                //Easiest to get context now
                Element context = getContextBeforeMove(editScript, a);

                NodeOps.insertAsChild(pos.getDOMInsertPosition(), w, a);

                NodeOps.setInOrder(xKids.item(i));
                NodeOps.setInOrder(a);

                //Note that now a is now at new position
                Delta.move(context, a, aPos.getXPath(), wPos.getXPath(), pos.getXPathInsertPosition(),
                        aPos.getCharPos(), pos.getCharInsertPosition(),
                        aPos.getLength(), editScript);
                }
            }
        }

    /**
     * Aligns children of current node that are not in order.
     *
     * TODO: Check logic.
     *
     * @param w  the match of the current node.
     * @param x  the current node
     * @param editScript the Edit Script to append the operation to
     * @param matchings  the set of matchings
     */

    private static void alignChildren(final Node w, final Node x,
            final Document editScript, final NodePairs matchings)
        {
        DiffXML.LOG.fine("In alignChildren");
        //How does alg deal with out of order nodes not matched with
        //children of partner?
        //Calls LCS algorithm

        //Order of w and x is important
        //Mark children of w and x "out of order"
        NodeList wKids = w.getChildNodes();
        NodeList xKids = x.getChildNodes();

        DiffXML.LOG.finer("w Name: " + w.getNodeName() + "no wKids" + wKids.getLength());
        DiffXML.LOG.finer("x Name: " + x.getNodeName() + "no xKids" + xKids.getLength());

        //build will break if following statement not here!
        //PROBABLY SHOULDN'T BE NEEDED - INDICATIVE OF BUG
        //Theory - single text node children are being matched,
        //Need to be moved, but aren't.
        //if ((wKids.getLength() == 0) || (xKids.getLength() == 0))
        //    return;

        markChildrenOutOfOrder(w);
        markChildrenOutOfOrder(x);

        if ((wKids.getLength() == 0) || (xKids.getLength() == 0))
                return;

            
        Node[] wSeq = getSequence(wKids, xKids, matchings);
        Node[] xSeq = getSequence(xKids, wKids, matchings);

        Node[] seq = Lcs.find(wSeq, xSeq, matchings);
        setInOrderNodes(seq);

        //Go through children of w.
        //If not inorder but matched, move
        //Need to be careful if want xKids or wKids
        //Check

        moveMisalignedNodes(xKids, w, editScript, matchings);
        }




 
}
