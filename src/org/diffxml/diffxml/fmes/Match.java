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

import org.apache.xerces.dom.NodeImpl;

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

    private boolean equal(final Node a, final Node b)
        {
        //Currently returns false but should throw exception?

        if (a.getNodeType() != b.getNodeType())
            return false;


        //if Node is an element
        if (a.getNodeType() == Node.ELEMENT_NODE)
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
                    //System.out.println("Equal: attributes not equal");
                    return false;
                    }
                }
            //Consider comparing positions of elements, or if kids matched etc
            return true;
            }

        //If node is a text node
        if (a.getNodeType() == Node.TEXT_NODE)
            {
            //Need to check whitespace and case options
            String aString = a.getNodeValue();
            String bString = b.getNodeValue();

            if (DiffFactory.IGNORE_ALL_WHITESPACE)
                {
                //Ignore all whitespace
                //Remove whitespace from nodes before comparison
                //Check nextToken doesn't skip first
                StringTokenizer st = new StringTokenizer(aString);
                aString = "";
                while (st.hasMoreTokens())
                    aString = aString + st.nextToken();

                st = new StringTokenizer(bString);
                bString = "";
                while (st.hasMoreTokens())
                    bString = bString + st.nextToken();
                }
            else if (DiffFactory.IGNORE_LEADING_WHITESPACE)
                {
                //Ignore leading ws
                //just call trim
                aString = aString.trim();
                bString = bString.trim();
                }

            //Check case optn

            if (DiffFactory.IGNORE_CASE)
                return (aString.equalsIgnoreCase(bString));

            return (aString.equals(bString));
            }

        //Node is not a text node or element, so just compare value and return.

        //System.out.println("Equal: elements equal");
        return (a.getNodeValue().equals(b.getNodeValue()));
        }

    /**
     * Adds user data to nodes to denote they have been matched.
     *
     * @param nodeA  The unmatched partner of nodeB
     * @param nodeB  The unmatched partner of nodeA
     */

    private void markMatched(final NodeImpl nodeA, final NodeImpl nodeB)
        {
        nodeA.setUserData("matched", "true", null);
        nodeB.setUserData("matched", "true", null);
        }

    /**
     * Creates a NodeIterator with an appropriate filter.
     *
     * @param  nodeType determines the type of filter to be used
     * @param  doc      the document to create the iterator on
     * @return          the resultant NodeIterator
     */

    private NodeIterator makeIterator(String nodeType, Document doc)
        {

        //Should really be bottom up, but shouldn't make big diff
        int filter;
        if (nodeType.equals("#text"))
            filter = NodeFilter.SHOW_TEXT;
        else
            filter = NodeFilter.SHOW_COMMENT;

        DiffXML.log.finer("Matching text/comment nodes");
        return ((DocumentTraversal) doc).createNodeIterator(
                doc.getDocumentElement(),
                filter, null, false);
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

    public final NodeSet fastMatch(final Document doc1, final Document doc2)
    throws Exception {

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

            DiffXML.log.finer("Wanted Node: " + wantedName);

            //Get all nodes in both documents with this tag
            if (wantedName.equals("#text") || wantedName.equals("#comment"))
                {
                NodeIterator ni1 = makeIterator(wantedName, doc1);
                NodeIterator ni2 = makeIterator(wantedName, doc2);

                NodeImpl na = (NodeImpl) ni1.nextNode();
                NodeImpl nb = (NodeImpl) ni2.nextNode();

                while (na != null)
                    {
                    if (na.getUserData("matched").equals("false"))
                        {
                        while (nb != null)
                            {
                            if (nb.getUserData("matched").equals("false")
                                    && equal(na, nb))
                                {
                                //Add nodes to matching set
                                matchSet.add(na, nb);
                                markMatched(na, nb);

                                break;
                                }
                            nb = (NodeImpl) ni2.nextNode();
                            }
                        }
                    else
                        {
                        //Shouldn't be possible to get here
                        DiffXML.log.warning("Node should NOT be matched");
                        }

                    na = (NodeImpl) ni1.nextNode();

                    ni2.detach();
                    ni2 = makeIterator(wantedName, doc2);
                    nb = (NodeImpl) ni2.nextNode();
                    }

                }
            else
                {
                NodeList tg1 = doc1.getElementsByTagName(wantedName);
                NodeList tg2 = doc2.getElementsByTagName(wantedName);

                //Cycle through tg1 looking for matches in tg2

                for (int a = 0; a < tg1.getLength(); a++)
                    {
                    NodeImpl aNode = (NodeImpl) tg1.item(a);
                    if (aNode.getUserData("matched").equals("false"))
                        {
                        //Cycle through tg2 looking for match
                        //tg_tag:
                        for (int b = 0; b < tg2.getLength(); b++)
                            {
                            NodeImpl bNode = (NodeImpl) tg2.item(b);
                            if (bNode.getUserData("matched").equals("false")
                                    && equal(tg1.item(a), tg2.item(b)))
                                {
                                //Add nodes to matching set
                                matchSet.add(tg1.item(a), tg2.item(b));

                                markMatched(aNode, bNode);

                                //Don't think this statement did nowt
                                //continue tg_tag;
                                break;
                                }
                            }
                        }
                    }
                }
            }


        if (OUTPUT_MATCHED_NODES)
            matchSet.printSet();

        return matchSet;

    }

    /**
     * Sets default user data tags on each node in document.
     *
     * Marks each node with "matched" and "inorder" tags.
     *
     * Tag "inorder" should default to true, and be changed by EditScript
     * if needed.
     * Tag "matched" is false until node is matched with a node in the
     * other document.
     *
     * TODO: ensure we should be marking all nodes.
     *
     * @param doc the document containg the nodes to be marked.
     */

    private void markNodes(final Document doc)
        {
        NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(
                doc.getDocumentElement(),
                NodeFilter.SHOW_ALL, null, false);

        NodeImpl node;
        while ( (node = (NodeImpl) ni.nextNode()) != null)
            {
            node.setUserData("matched", "false", null);
            node.setUserData("inorder", "true", null);
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

    private TreeSet sortNodes(final Document doc)
        {
        //Tree walker steps through all nodes in docs
        TreeWalker walker = ((DocumentTraversal) doc).createTreeWalker(
                doc.getDocumentElement(), NodeFilter.SHOW_ALL, null, false);

        NodeImpl node;
        TreeSet nodeInfoSet = new TreeSet(new NodeInfoComparator());

        node = (NodeImpl) walker.getCurrentNode();

        while (node != null)
            {
            NodeImpl tmpNode = node;
            int depth = 0;

            while (tmpNode != doc.getDocumentElement())
                {
                depth++;
                tmpNode = (NodeImpl) tmpNode.getParentNode();
                }

            NodeInfo ni = new NodeInfo(node.getNodeName(), depth);
            nodeInfoSet.add(ni);

            DiffXML.log.finer("Added "  + ni.getTag()
                    + " Depth " + ni.getDepth());

            node = (NodeImpl) walker.nextNode();
            }

        return nodeInfoSet;
        }
}

