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

import java.util.List;
import java.io.IOException;

import org.diffxml.diffxml.DOMOps;
import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.DiffFactory;
import org.diffxml.diffxml.fmes.delta.DULDelta;
import org.diffxml.diffxml.fmes.delta.DeltaIF;
import org.diffxml.diffxml.fmes.delta.DeltaInitialisationException;

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
    
    /**
     * The EditScript.
     */
    private DeltaIF mDelta;
    
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
            mDelta = new DULDelta();
        } catch (DeltaInitialisationException e) {
            throw new DocumentCreationException("Failed to create edit script",
                    e);
        }

        matchRoots();

        // Fifo used to do a breadth first traversal of doc2
        NodeFifo fifo = new NodeFifo();
        fifo.addChildrenOfNode(mDoc2.getDocumentElement());
        //Special case for aligning children of document elements
        alignChildren(mDoc1.getDocumentElement(), mDoc2.getDocumentElement(),
                mMatchings);

        while (!fifo.isEmpty()) {
            
            DiffXML.LOG.fine("In breadth traversal");

            Node x = fifo.pop();
            fifo.addChildrenOfNode(x);

            Node y = x.getParentNode();
            Node z = mMatchings.getPartner(y);

            logNodes(x, y, z);
            Node w = mMatchings.getPartner(x);

            if (!mMatchings.isMatched(x)) {
                w = doInsert(x, z);
            } else {
                // TODO: Update should go here
                if (!mMatchings.getPartner(y).equals(w.getParentNode())) {
                    doMove(w, x, z, mMatchings);
                }
            }

            alignChildren(w, x, mMatchings);
        }

        deletePhase(mDoc1.getDocumentElement(), mMatchings);

        // TODO: Assert following
        // Post-Condition es is a minimum cost edit script,
        // Matchings is a total matching and
        // doc1 is isomorphic to doc2

        return mDelta.getDocument();
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
        
        //Find the child number (k) to insert w as child of z 
        FindPosition pos = new FindPosition(x, mMatchings);

        //Apply insert to doc1
        //The node we want to insert is the import of x with attributes but no
        //children
        Node w = mDoc1.importNode(x, false);

        //Need to set in order as won't be revisited
        NodeOps.setInOrder(w);
        NodeOps.setInOrder(x);

        mDelta.insert(w, z, pos.getXPathInsertPosition(), 
                pos.getCharInsertPosition());

        //Take match of parent (z), and insert
        w = DOMOps.insertAsChild(pos.getDOMInsertPosition(), z, w);

        outputDebug();
        //Add to matching set
        mMatchings.add(w, x);

        return w;
    }

    /**
     * Performs a move operation according to the algorithm and updates
     * the EditScript.
     *
     * @param w          the node to be moved
     * @param x          the matching node
     * @param z          the partner of x's parent
     * @param matchings  the set of matching nodes
     */
    private void doMove(final Node w, final Node x, final Node z, 
            final NodePairs matchings) {

        Node v = w.getParentNode();
        Node y = x.getParentNode();

        //Apply move if parents not matched and not null

        Node partnerY = matchings.getPartner(y);
        assert !NodeOps.checkIfSameNode(v, partnerY);

        FindPosition pos = new FindPosition(x, matchings);

        NodeOps.setInOrder(w);
        NodeOps.setInOrder(x);

        mDelta.move(w, z, pos.getXPathInsertPosition(), 
                pos.getCharInsertPosition());

        //Apply move to T1
        DOMOps.insertAsChild(pos.getDOMInsertPosition(), z, w);
        outputDebug();
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
    private static void logNodes(final Node x, final Node y, final Node z) {

        if (x == null) {
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
     * @param matchings  the set of matching nodes
     */
    private void deletePhase(final Node n, final NodePairs matchings) {
        
        // Deletes nodes in Post-order traversal
        NodeList kids = n.getChildNodes();
        if (kids != null) {
            // Note that we loop *backward* through kids
            for (int i = (kids.getLength() - 1); i >= 0; i--) {
                deletePhase(kids.item(i), matchings);
            }
        }

        // If node isn't matched, delete it
        if (!matchings.isMatched(n)) {
            mDelta.delete(n);
            n.getParentNode().removeChild(n);
            outputDebug();
        }
    }

    /**
     * Mark the children of a node out of order.
     *
     * @param n the parent of the nodes to mark out of order
     */
    private static void markChildrenOutOfOrder(final Node n) {

        NodeList kids = n.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            NodeOps.setOutOfOrder(kids.item(i));
        }
    }

    /**
     * Marks the Nodes in the given list and their partners "inorder".
     *
     * @param seq  the Nodes to mark "inorder"
     * @param matchings the set of matching Nodes
     */
    private static void setNodesInOrder(final List<Node> seq,
            final NodePairs matchings) {

        for (Node node : seq) {
            NodeOps.setInOrder(node);
            NodeOps.setInOrder(matchings.getPartner(node));
        }
    }

    /**
     * Moves nodes that are not in order to correct position.
     *
     * @param w Node with potentially misaligned children
     * @param wSeq Sequence of children of w that have matches in the children
     *             of x
     * @param stay The List of nodes not to be moved
     * @param matchings The set of matching nodes
     */
    private void moveMisalignedNodes(final Node w, final Node[] wSeq, 
            final List<Node> stay, final NodePairs matchings) {
        
        //Get Nodes that are not in LCS but are in wSeq (or xSeq)
        for (Node a : wSeq) {
            if (!stay.contains(a)) {

                Node b = matchings.getPartner(a);
                FindPosition pos = new FindPosition(b, matchings);

                mDelta.move(a, w, pos.getXPathInsertPosition(),
                        pos.getCharInsertPosition());

                DOMOps.insertAsChild(pos.getDOMInsertPosition(), w, a);

                NodeOps.setInOrder(a);
                NodeOps.setInOrder(b);
            }
        }
    }

    /**
     * Aligns children of current node that are not in order.
     *
     * @param w  the match of the current node.
     * @param x  the current node

     * @param matchings  the set of matchings
     */
    private void alignChildren(final Node w, final Node x,
            final NodePairs matchings) {
        
        DiffXML.LOG.fine("In alignChildren");

        //Order of w and x is important
        markChildrenOutOfOrder(w);
        markChildrenOutOfOrder(x);

        NodeList wKids = w.getChildNodes();
        NodeList xKids = x.getChildNodes();

        Node[] wSeq = NodeSequence.getSequence(wKids, xKids, matchings);
        Node[] xSeq = NodeSequence.getSequence(xKids, wKids, matchings);

        List<Node> lcsSeq = NodeSequence.getLCS(wSeq, xSeq, matchings);
        setNodesInOrder(lcsSeq, matchings);
        
        moveMisalignedNodes(w, wSeq, lcsSeq, matchings);
    }

    /**
     * Outputs debug information.
     */
    private final void outputDebug() {

        if (DiffFactory.isDebug()) {
            System.err.println("Result:");
            try {
                DOMOps.outputXML(mDoc1, System.err);
            } catch (IOException e) {
                System.err.println("Failed to print debug info");
            }
            System.err.println();
            System.err.println();
        }
    }
}
