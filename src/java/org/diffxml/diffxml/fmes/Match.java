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
import java.util.TreeSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Solves the "good matchings" problem for the fmes algorithm.
 *
 * Essentially pairs nodes that match between documents.
 * Uses the "fast match" algorithm is detailed in the paper
 * "Change Detection in Hierarchically Structure Information".
 *
 * WARNING: Will only work correctly with acylic documents.
 * TODO: Add alternate matching code for cylic documents.
 *
 * @author Adrian Mouat
 */

public class Match
{

    /**
     * Internal flag decides whether or not to output matched nodes.
     *
     * Used for debug purposes.
     */

    private static final boolean OUTPUT_MATCHED_NODES = false;

    /**
     * Compares two elements two determine whether they should be matched.
     *
     * @param a  First element
     * @param b  Potential match for b
     * @return   true if nodes match, false otherwise
     */

    private static boolean compareElements(final Node a, final Node b)
        {
        //Check NodeNames equal
        if (a.getNodeName() != b.getNodeName())
            return false;

        //Check attributes equal
        NamedNodeMap aAttrs = a.getAttributes();
        NamedNodeMap bAttrs = b.getAttributes();

        int numberAttrs = 0;
        if (aAttrs != null)
            numberAttrs = aAttrs.getLength();

        for (int i = 0; i < numberAttrs; i++)
            {
            //Check if attr exists in other tag
            if (bAttrs.getNamedItem(aAttrs.item(i).getNodeName()) == null)
                return false;

            if (!bAttrs.getNamedItem(aAttrs.item(i).getNodeName())
                    .getNodeValue().equals(aAttrs.item(i).getNodeValue()))
                {
                return false;
                }
            }
        //TODO: Consider comparing positions of elements, or if kids matched etc
        return true;
        }

    /**
     * Compares two text nodes to determine if they should be matched.
     *
     * Takes into account whitespace options.
     *
     * @param a  First node
     * @param b  Potential match for a
     * @return   True if nodes match, false otherwise
     */

    private static boolean compareTextNodes(final Node a, final Node b)
        {

        String aString = a.getNodeValue();
        String bString = b.getNodeValue();

        if (DiffFactory.isIgnoreAllWhitespace())
            {
            //Remove whitespace from nodes before comparison
            //TODO: Check nextToken doesn't skip first
            StringTokenizer st = new StringTokenizer(aString);
            aString = "";
            while (st.hasMoreTokens())
                aString = aString + st.nextToken();

            st = new StringTokenizer(bString);
            bString = "";
            while (st.hasMoreTokens())
                bString = bString + st.nextToken();
            }
        else if (DiffFactory.isIgnoreLeadingWhitespace())
            {
            //Ignore leading ws
            //just call trim
            aString = aString.trim();
            bString = bString.trim();
            }

        //Check case optn

        if (DiffFactory.isIgnoreCase())
            return (aString.equalsIgnoreCase(bString));

        return (aString.equals(bString));
        }

    /**
     * Compares 2 nodes to determine wheter they should match.
     *
     * TODO: Verify each of the comparisons.
     * TODO: Check if more comparisons are needed
     *
     * @param a  first node
     * @param b  potential match for a
     * @return   true if nodes match, false otherwise
     */

    private static boolean compareNodes(final Node a, final Node b)
        {

        if (a.getNodeType() != b.getNodeType())
            return false;

        switch (a.getNodeType())
            {
            case Node.ELEMENT_NODE:
                return compareElements(a, b);
            case Node.TEXT_NODE:
                return compareTextNodes(a, b);
            default:
                return (a.getNodeValue().equals(b.getNodeValue()));
            }

        }


    /**
     * Creates a NodeIterator with an appropriate filter.
     *
     * @param  nodeType determines the type of filter to be used
     * @param  doc      the document to create the iterator on
     * @return          the resultant NodeIterator
     */

    private static NodeIterator makeIterator(final String nodeType, final Document doc)
        {

        //Should really be bottom up, but shouldn't make big diff
        int filter;
        if (nodeType.equals("#text"))
            filter = NodeFilter.SHOW_TEXT;
        else
            filter = NodeFilter.SHOW_COMMENT;

        DiffXML.LOG.finer("Matching text/comment nodes");
        return ((DocumentTraversal) doc).createNodeIterator(
                doc.getDocumentElement(),
                filter, null, false);
        }

    /**
     * Searches NodeIterator for a node matching given node.
     *
     * Marks nodes matched and adds nodes to NodeSet if match found.
     *
     * @param node     the node to be matched
     * @param ni       the NodeIterator to look through
     * @param matchSet the NodeSet to add the pair to
     *
     * @return true if a match is found, false if not
     */

    private static boolean matchNode(final Node node, final NodeIterator ni,
            final NodeSet matchSet)
        {
        if (NodeOps.isMatched(node))
            {
            return false;
            }

        Node currNode = ni.nextNode();

        while (currNode != null)
            {
            if (!NodeOps.isMatched(currNode) && compareNodes(node, currNode))
                {
                matchSet.add(node, currNode);
                NodeOps.setMatched(node, currNode);

                return true;
                }
            currNode =  ni.nextNode();
            }

        return false;
        }

    /**
     * Matches elements with a given name from two documents.
     *
     * @param elementName  The name of the elements to match
     * @param doc1         The original document
     * @param doc2         The modified document
     * @param matchSet     The NodeSet to add mathcing elements to
     */

    private static void matchElements(final String elementName, final Document doc1,
            final Document doc2, final NodeSet matchSet)
        {

        NodeList listA = doc1.getElementsByTagName(elementName);
        NodeList listB = doc2.getElementsByTagName(elementName);

        for (int a = 0; a < listA.getLength(); a++)
            {
            Node nodeA = listA.item(a);

            if (!NodeOps.isMatched(nodeA))
                {
                for (int b = 0; b < listB.getLength(); b++)
                    {
                    Node nodeB =  listB.item(b);

                    if (!NodeOps.isMatched(nodeB)
                            && compareNodes(nodeA, nodeB))
                        {
                        matchSet.add(nodeA, nodeB);
                        NodeOps.setMatched(nodeA, nodeB);

                        break;
                        }
                    }
                }
            }
        }

    /**
     * Matches nodes of nodeType in doc1 to doc2 and stores pairs in NodeSet.
     *
     * TODO: Ensure we have to detach iterator.
     *
     * @param nodeType  The type of nodes to match (text or comment)
     * @param doc1      The original document
     * @param doc2      The modified document
     * @param matchSet  The NodeSet to store the pairs in
     */

    private static void matchNodes(final String nodeType, final Document doc1,
            final Document doc2, final NodeSet matchSet)
        {
        NodeIterator ni1 = makeIterator(nodeType, doc1);
        NodeIterator ni2 = makeIterator(nodeType, doc2);

        Node nodeA = ni1.nextNode();

        while (nodeA != null)
            {
            matchNode(nodeA, ni2, matchSet);
            nodeA =  ni1.nextNode();

            ni2.detach();
            ni2 = makeIterator(nodeType, doc2);
            }
        }

    /**
     * Performs fast match algorithm on given DOM documents.
     *
     * TODO: Possible searching wrong way could be causing bad matches,
     *       especially of whitespace.
     *
     * @param doc1   The original document
     * @param doc2   The modified document
     *
     * @return NodeSet containing pairs of matching nodes.
     */

    public static final NodeSet fastMatch(final Document doc1, final Document doc2)
        {
        NodeSet matchSet = new NodeSet();

        doc1.getDocumentElement().normalize();
        doc2.getDocumentElement().normalize();

        //Mark all nodes unmatched
        markNodes(doc1);
        markNodes(doc2);

        //Proceed bottom up on Tree 1
        TreeSet tree1 = sortNodes(doc1);

        Iterator treeIter = tree1.iterator();

        String wantedName;
        NodeInfo curr;

        while (treeIter.hasNext())
            {
            curr = (NodeInfo) treeIter.next();
            wantedName = curr.getTag();

            DiffXML.LOG.finer("Wanted Node: " + wantedName);

            //Get all nodes in both documents with wantedName tag
            if (wantedName.equals("#text") || wantedName.equals("#comment"))
                {
                matchNodes(wantedName, doc1, doc2, matchSet);
                }
            else
                {
                matchElements(wantedName, doc1, doc2, matchSet);
                }
            }


        if (OUTPUT_MATCHED_NODES)
            matchSet.printSet();

        return matchSet;
    }

    /**
     * Sets default user data tags on each node in document.
     *
     * Marks each node matched and inorder.
     *
     * Nodes should be "inorder" by default, and be changed by EditScript
     * if needed.
     * Nodes should not be "matched" until node is matched with a node in the
     * other document.
     *
     * TODO: ensure we should be marking all nodes.
     *
     * @param doc the document containg the nodes to be marked.
     */

    private static void markNodes(final Document doc)
        {
        NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(
                doc.getDocumentElement(),
                NodeFilter.SHOW_ALL, null, false);

        Node n;
        while ((n = ni.nextNode()) != null)
            {
            NodeOps.setInOrder(n);
            NodeOps.setNotMatched(n);
            }
        }

    /**
     * Returns a sorted set of all nodes.
     *
     * TreeSet is sorted in reverse order of depth according to
     * NodeInfoComparator.
     *
     * @param doc  The document to sort.
     * @return     A set of all the documents nodes in reverse order of
     *             depth.
     */

    private static TreeSet sortNodes(final Document doc)
        {
        //Tree walker steps through all nodes in docs
        TreeWalker walker = ((DocumentTraversal) doc).createTreeWalker(
                doc.getDocumentElement(), NodeFilter.SHOW_ALL, null, false);

        Node node;
        TreeSet nodeInfoSet = new TreeSet(new NodeInfoComparator());

        node =  walker.getCurrentNode();

        while (node != null)
            {
            Node tmpNode = node;
            int depth = 0;

            while (tmpNode != doc.getDocumentElement())
                {
                depth++;
                tmpNode = tmpNode.getParentNode();
                }

            NodeInfo ni = new NodeInfo(node.getNodeName(), depth);
            nodeInfoSet.add(ni);

            DiffXML.LOG.finer("Added "  + ni.getTag()
                    + " Depth " + ni.getDepth());

            node =  walker.nextNode();
            }

        return nodeInfoSet;
        }
}

