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

import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.DiffFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.xerces.dom.DocumentImpl;

/**
 * Creates the edit script for the fmes algorithm.
 *
 * Uses the algorithm described in the paper
 * "Change Detection in Hierarchically Structure Information".
 *
 * @author Adrian Mouat
 */

public class EditScript
{
    /** Prepares an empty Edit Script document.
     *
     * Makes root element, appends any neccessary attributes
     * and context information.
     *
     * Move to delta.java?
     *
     * @return a properly formatted, empty edit script
     */

    private Document makeEmptyEditScript()
        {
        Document editScript = new DocumentImpl();

        Element root = editScript.createElement("delta");

        //Append any context information
        if (DiffFactory.CONTEXT)
            {
            root.setAttribute("sib_context", "" + DiffFactory.SIBLING_CONTEXT);
            root.setAttribute("par_context", "" + DiffFactory.PARENT_CONTEXT);
            root.setAttribute("par_sib_context",
                    "" + DiffFactory.PARENT_SIBLING_CONTEXT);
            }

        if (DiffFactory.REVERSE_PATCH)
            root.setAttribute("reverse_patch", "true");

        if (!DiffFactory.ENTITIES)
            root.setAttribute("resolve_entities", "false");

        if (!DiffFactory.DUL)
            {
            //Change root to xupdate style
            root = editScript.createElement("modifications");
            root.setAttribute("version", "1.0");
            root.setAttribute("xmlns:xupdate", "http://www.xmldb.org/xupdate");
            }

        editScript.appendChild(root);

        return editScript;
        }

    /**
     * Handles non-matching root nodes.
     *
     * TODO: Write! Commented out code may give hint.
     *
     * @param editScript  the Edit Script to write changes to
     * @param doc1        the original document
     * @param doc2        the modified document
     */

    private void matchRoots(final Document editScript, final Document doc1,
            final Document doc2)
        {

        //We need to match roots if not already matched
        //Algortihm says to create dummy node, but this will muck up xpaths.
        //Use update operation to match the two nodes.

        /*
           if (!doc1.getDocumentElement().getNodeName()
               .equals(doc2.getDocumentElement()))
           {
        //Add update operation
        //Need to add to Delta.java to get proper handling of args
        //But this is a quick hack to avoid probs
        //Need to add attributes
        Element upd=es.createElement("update");
        upd.setAttribute("node","/node()[1]");
        upd.setAttribute("name",doc2.getDocumentElement().getNodeName());
        root.appendChild(upd);

        //Set Matched
        ((Node3)doc1.getDocumentElement()).setUserData("matched","true",null);
        ((Node3)doc2.getDocumentElement()).setUserData("matched","true",null);
        }
        */
        }

    /**
     * Inserts a node according to the algorithm and updates
     * the Edit Script.
     *
     * @param x          current node
     * @param z          partner of x's parent
     * @param doc1       the original document
     * @param editScript the Edit Script to append operations to
     * @param matchings  the set of matching nodes
     * @return           the inserted node
     */

    private Node doInsert(final Node x, final Node z,
            final Document doc1, final Document editScript,
            final NodeSet matchings)
        {
        InsertPosition pos = findPos(x, matchings);
        NodePos zPath = new NodePos(z);

        //Apply insert to doc1
        //The node we want to insert is the import of x with all
        //its text node children
        //TODO: Ensure attributes properly added

        Node w = doc1.importNode(x, false);
        NodeOps.setMatched(w);
        NodeOps.setInOrder(w);

        //Take match of parent (z), and insert
        NodeOps.insertAsChild(pos.insertBefore, z, w);

        //Add to matching set
        NodeOps.setMatched(x);
        matchings.add(w, x);

        Delta.Insert(w, zPath.getXPath(), pos.numXPath, pos.charPosition, editScript);

        Delta.addAttrsToDelta(x.getAttributes(), new NodePos(w).getXPath(), editScript);

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

    private Node doMove(final Node x, final Node z,
            final Document editScript, final NodeSet matchings)
        {
        DiffXML.log.fine("In move");

        InsertPosition pos = new InsertPosition();

        Node w = matchings.getPartner(x);
        Node v = w.getParentNode();
        Node y = x.getParentNode();

        //UPDATE would be here if implemented

        //Apply move if parents not matched
        Node partnerY = matchings.getPartner(y);
        if (!NodeOps.checkIfSameNode(v, partnerY))
            {
            pos = findPos(x, matchings);
            NodePos wPath = new NodePos(w);
            NodePos zPath = new NodePos(z);

            //Following two statements may be unnecessary
            NodeOps.setInOrder(w);
            NodeOps.setInOrder(x);

            //TODO: Make function for following
            //Check not already one!
            Element context = editScript.createElement("mark");
            if (DiffFactory.CONTEXT)
                {
                context.appendChild(editScript.importNode(w, true));
                context = Delta.addContext(w, context);
                }

            //Apply move to T1
            NodeOps.insertAsChild(pos.insertBefore, z, w);

            Delta.Move(context, w, wPath.getXPath(), zPath.getXPath(), pos.numXPath,
                     wPath.getCharPos(), pos.charPosition, wPath.getLength(), editScript);
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

    private void logNodes(final Node x, final Node y, final Node z)
        {
        DiffXML.log.finer("x=" + x.getNodeName() + " " + x.getNodeValue());
        DiffXML.log.finer("y=" + y.getNodeName() + " " + y.getNodeValue());
        DiffXML.log.finer("z=" + z.getNodeName() + " " + z.getNodeValue());

        if (z == null)
            DiffXML.log.warning("Your matchings don't work you dumb"
                    + "mutha fucka \n or root");
        }


    /**
     * Creates an Edit Script conforming to matchings that transforms
     * doc1 into doc2.
     *
     * Uses algorithm in "Chnage Detection in Hierarchically Structured
     * Information".
     *
     * @param doc1      the original document
     * @param doc2      the modified document
     * @param matchings the set of matching nodes
     * @return          the resultant Edit Script
     */

    public final Document create(final Document doc1, final Document doc2,
            final NodeSet matchings)
        {
        Document editScript = makeEmptyEditScript();

        if (!doc1.getDocumentElement().getNodeName()
                .equals(doc2.getDocumentElement()))
            matchRoots(editScript, doc1, doc2);

        //Fifo used to do a breadth first traversal of doc2
        Fifo fifo = new Fifo();
        fifo.push(doc2.getDocumentElement());

        while (!fifo.isEmpty())
            {
            DiffXML.log.fine("In breadth traversal");

            Node x = (Node) fifo.pop();
            fifo.addChildrenToFifo(x);

            //May need to do more with root
            if (NodeOps.checkIfSameNode(x, doc2.getDocumentElement()))
                {
                NodeOps.setInOrder(x);
                continue;
                }

            Node y = x.getParentNode();
            Node z = matchings.getPartner(y);

            logNodes(x, y, z);
            Node w;

            if (!NodeOps.isMatched(x))
                w = doInsert(x, z, doc1, editScript, matchings);
            else
                w = doMove(x, z, editScript, matchings);

            //May want to check value of w
            alignChildren(w, x, editScript, matchings);
            }

        deletePhase(doc1.getDocumentElement(), editScript);

        //Post-Condition es is a minimum cost edit script,
        //Matchings is a total matching and
        //doc1 is isomorphic to doc2

        return editScript;
        }

    /**
     * Performs the deletePhase of the algorithm.
     *
     * @param n          the current node
     * @param editScript the Edit Script to append operations to
     */

    private void deletePhase(final Node n, final Document editScript)
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

                deletePhase(kids.item(i), editScript);
                }
            }

        //If node isn't matched, delete it
        //TODO: Make function for following.
        if (!NodeOps.isMatched(n))
            {
            Element par = (Element) n.getParentNode();
            NodePos delPos = new NodePos(n);

            Delta.Delete(n, delPos.getXPath(), delPos.getCharPos(),
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

    private void markChildrenOutOfOrder(final Node n)
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

    private void setInOrderNodes(final Node[] seq)
        {
        for (int i = 0; i < seq.length; i++)
            {
            if (seq[i] != null)
                {
                DiffXML.log.finer("seq" + seq[i].getNodeName()
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

    private Node[] getSequence(final NodeList set1, final NodeList set2,
            final NodeSet matchings)
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

    private Element getContextBeforeMove(final Document editScript, final Node a)
        {

        Element context = editScript.createElement("mark");
        if (DiffFactory.CONTEXT)
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

    private void moveMisalignedNodes(final NodeList xKids, final Node w,
            final Document editScript, final NodeSet matchings)
        {
        //Select nodes matched but not in order
        for (int i = 0; i < xKids.getLength(); i++)
            {
            if ((!NodeOps.isInOrder(xKids.item(i)))
                    && NodeOps.isMatched(xKids.item(i)))
                {
                //Get childno for move
                InsertPosition pos = new InsertPosition();
                pos = findPos(xKids.item(i), matchings);

                //Get partner and position
                Node a = matchings.getPartner(xKids.item(i));
                NodePos aPos = new NodePos(a);

                NodePos wPos = new NodePos(w);

                //Easiest to get context now
                Element context = getContextBeforeMove(editScript, a);

                NodeOps.insertAsChild(pos.insertBefore, w, a);

                NodeOps.setInOrder(xKids.item(i));
                NodeOps.setInOrder(a);

                //Note that now a is now at new position
                Delta.Move(context, a, aPos.getXPath(), wPos.getXPath(), pos.numXPath,
                        aPos.getCharPos(), pos.charPosition,
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

    private void alignChildren(final Node w, final Node x,
            final Document editScript, final NodeSet matchings)
        {
        //How does alg deal with out of order nodes not matched with
        //children of partner?
        //Calls LCS algorithm

        //Order of w and x is important
        //Mark children of w and x "out of order"
        NodeList wKids = w.getChildNodes();
        NodeList xKids = x.getChildNodes();

        DiffXML.log.finer("no wKids" + wKids.getLength());
        DiffXML.log.finer("no xKids" + xKids.getLength());

        //build will break if following statement not here!
        //PROBABLY SHOULDN'T BE NEEDED - INDICATIVE OF BUG
        //Theory - single text node children are being matched,
        //Need to be moved, but aren't.
        if ((wKids.getLength() == 0) || (xKids.getLength() == 0))
            return;

        markChildrenOutOfOrder(w);
        markChildrenOutOfOrder(x);

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

    /**
     * Gets the rightmost left sibling of n marked "inorder".
     *
     * @param n Node to find "in order" left sibling of
     * @return  Either the "in order" left sibling or null if none
     */

    private Node getInOrderLeftSibling(final Node n)
        {
        //Default l to n
        Node l =  n;
        NodeList nSiblings = n.getParentNode().getChildNodes();

        for (int i = 0; i < nSiblings.getLength(); i++)
            {
            Node test = nSiblings.item(i);

            if (NodeOps.checkIfSameNode(test, n))
                break;

            if (NodeOps.isInOrder(test))
                l = test;
            }

        if (NodeOps.checkIfSameNode(l, n))
            return null;

        return l;
        }

    class Index
        {
        public int dom = 0;
        public int xPath = 0;
        }

    /**
     * Gets the "in order" index of the node.
     *
     * The "in order" index counts only nodes marked "in order".
     *
     * @param n the node to find the index of
     * @return  the "in order" index of the node
     */

    private Index getInOrderIndex(final Node n)
        {
        Index ind = new Index();
        ind.dom = 0;
        ind.xPath  = 1;
        int lastNode = -1;

        NodeList children = n.getParentNode().getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
            {
            Node test = children.item(i);
            if (NodeOps.checkIfSameNode(n, children.item(i)))
                break;

            if (NodeOps.isInOrder(test))
                {
                ind.dom++;
                //Want to increment XPath index if not
                //both this (inorder) node and last (inorder) node text nodes
                if ((test.getNodeType() == Node.TEXT_NODE) && (lastNode != -1)
                        && (children.item(lastNode).getNodeType()
                            == Node.TEXT_NODE))
                    {
                    ind.xPath--;
                    }

                ind.xPath++;
                lastNode = i;

                //Want to increment XPath index if not both this
                //(inorder) node and last node text nodes
                }
            }
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

    private int getCharPosition(final Index ind, final Node n)
        {

        NodeList children = n.getParentNode().getChildNodes();
        int tmpIndex = ind.dom;
        int lastIndex = children.getLength() - 1;

        if (ind.dom > lastIndex)
            tmpIndex = lastIndex;

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
                    DiffXML.log.finer(children.item(j).getNodeValue()
                            + " charpos " + charpos);
                    }
                else
                    break;
                }
            }
        return charpos;

        }
    /**
     * Finds the childnumber to insert a node as.
     *
     * (Equivalent to the current childnumber of the node to insert
     * before)
     *
     * @param x         the node to find the position of? *check*
     * @param matchings the set of matching nodes
     *
     * @return          the position to insert the node at
     */

    private InsertPosition findPos(final Node x, final NodeSet matchings)
        {
        DiffXML.log.fine("Entered findPos");
        Node xParent = x.getParentNode();
        NodeList xSiblings = xParent.getChildNodes();

        InsertPosition pos = new InsertPosition();

        Node v = getInOrderLeftSibling(x);

        if (v == null)
            {
            DiffXML.log.fine("Exiting findPos normally1");
            //SHOULD CHAR be 1 or -1? depends on next node. Safest at 1?
            //TODO: Check logic - pretty sure wrong!
            pos.insertBefore = 0;
            pos.numXPath = 1;
            pos.charPosition = 1;
            return pos;
            }

        //Get partner of v
        Node u = matchings.getPartner(v);
        Node uParent = u.getParentNode();
        NodeList children = uParent.getChildNodes();

        Index ind = getInOrderIndex(u);

        //Need i+1 child
        pos.insertBefore = ++ind.dom;

        //If this is a text node, and last node was a text node,
        //don't increment xpath

        //Get charpos
        //Can't use NodePos func as node may not exist
        pos.charPosition = getCharPosition(ind, u);

        //If this is a text node, and last node was a text node,
        //don't increment xpath
        if (!(x.getNodeType() == Node.TEXT_NODE
                    && (children.item(ind.dom - 1).getNodeType()
                        == Node.TEXT_NODE)))
            ind.xPath++;

        pos.numXPath = ind.xPath;

        DiffXML.log.fine("Exiting findPos normally");
        return pos;
        }
}
