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
import java.util.Comparator;
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
        //Currently returns false but should throw exception

        if (a.getNodeType() != b.getNodeType())
            {
            System.out.println("Types not equal");
            return false;
            }


        //if Node is an element
        if (a.getNodeType() == Node.ELEMENT_NODE)
            {
            //Check NodeNames equal
            if (a.getNodeName() != b.getNodeName())
                return false;

            //Check attributes equal
            NamedNodeMap aAttrs = a.getAttributes();
            NamedNodeMap bAttrs = b.getAttributes();

            int numberAttrs = (aAttrs != null) ? aAttrs.getLength() : 0;
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
            //Consider comparng positions of elements, or if kids matched etc
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
                {
                //Just make it all lower
                aString.toLowerCase();
                bString.toLowerCase();
                }

            return (aString.equals(bString));
            }

        //Node is not a text node or element, so just compare value and return.

        //System.out.println("Equal: elements equal");
        return (a.getNodeValue().equals(b.getNodeValue()));
        }

    class tdComp implements Comparator
        {
        //Remember we want things stored in reverse order of depth!
        //We don't really care about order of strings but we need to
        //differentiate for a set

        public int compare(final Object o1, final Object o2)
            {
            NodeInfo td1 = (NodeInfo) o1;
            NodeInfo td2 = (NodeInfo) o2;

            if (td1.getDepth() == td2.getDepth())
                {
                return (td1.getTag().compareTo(td2.getTag()));
                }
            else
                {
                return (td2.getDepth() - td1.getDepth());
                }
            }

        public final boolean equals(final Object o)
            {
            return o.equals(this);
            }

        }

    /**
     * Performs fast match algorithm on given DOM documents.
     *
     * @param doc1   The original document
     * @param doc2   The modified document
     *
     * @return NodeSet containing pairs of matching nodes.
     */

    public final NodeSet fastMatch(final Document doc1, final Document doc2)
    throws Exception {

        NodeSet matchSet = new NodeSet();

        //Normalise documents
        doc1.getDocumentElement().normalize();
        doc2.getDocumentElement().normalize();

        TreeSet td1 = markElements(doc1);
        TreeSet td2 = markElements(doc2);

        String wanted = "";
        NodeInfo tg;

        //Iterate for nodes in Tree1
        Iterator it = td1.iterator();
        while (it.hasNext())
            {
            tg = (NodeInfo) it.next();

            wanted = tg.getTag();

            DiffXML.log.finer("Wanted Node: " + wanted);
            //Get all nodes in both trees with this tag
            if (wanted.equals("#text"))
                {
                //Use node iterator
                //Should really be bottom up, but shouldn't make big diff
                DiffXML.log.finer("Matching text nodes");
                NodeIterator ni1 = ((DocumentTraversal) doc1).createNodeIterator(
                        doc1.getDocumentElement(),
                        NodeFilter.SHOW_TEXT, null, false);
                NodeIterator ni2 = ((DocumentTraversal) doc2).createNodeIterator(
                        doc2.getDocumentElement(),
                        NodeFilter.SHOW_TEXT, null, false);

                NodeImpl na = (NodeImpl) ni1.nextNode();
                NodeImpl nb = (NodeImpl) ni2.nextNode();

                while (na != null)
                    {
                    //Should always be false but leave check in for mo
                    if (na.getUserData("matched").equals("false"))
                        {
                        while (nb != null)
                            {
                            if (nb.getUserData("matched").equals("false")
                                    && equal(na, nb))
                                {
                                //Add nodes to matching set
                                matchSet.add(na, nb);
                                //Mark nodes matched
                                na.setUserData("matched", "true", null);
                                nb.setUserData("matched", "true", null);

                                break;
                                }
                            nb = (NodeImpl) ni2.nextNode();
                            }
                        }
                    na = (NodeImpl) ni1.nextNode();

                    ni2.detach();
                    ni2 = ((DocumentTraversal) doc2).createNodeIterator(
                            doc2.getDocumentElement(),
                            NodeFilter.SHOW_TEXT, null, false);
                    nb = (NodeImpl) ni2.nextNode();
                    }


                }
            else if (wanted.equals("#comment"))
                {
                //Use node iterator
                //Should really be bottom up, but shouldn't make big diff
                DiffXML.log.finer("Matching comments");
                NodeIterator ni1 = ((DocumentTraversal) doc1).createNodeIterator(
                        doc1.getDocumentElement(),
                        NodeFilter.SHOW_COMMENT, null, false);
                NodeIterator ni2 = ((DocumentTraversal) doc2).createNodeIterator(
                        doc2.getDocumentElement(),
                        NodeFilter.SHOW_COMMENT, null, false);

                NodeImpl na = (NodeImpl) ni1.nextNode();
                NodeImpl nb = (NodeImpl) ni2.nextNode();

                while (na != null)
                    {
                    //Should always be false but leave check in for mo
                    if (na.getUserData("matched").equals("false"))
                        {
                        while (nb != null)
                            {
                            if (nb.getUserData("matched").equals("false")
                                    && equal(na, nb))
                                {
                                matchSet.add(na, nb);
                                na.setUserData("matched", "true", null);
                                nb.setUserData("matched", "true", null);

                                break;
                                }
                            nb = (NodeImpl) ni2.nextNode();
                            }
                        }
                    na = (NodeImpl) ni1.nextNode();
                    ni2.detach();
                    ni2 = ((DocumentTraversal) doc2).createNodeIterator(
                            doc2.getDocumentElement(),
                            NodeFilter.SHOW_TEXT, null, false);
                    nb = (NodeImpl) ni2.nextNode();
                    }
                }
            else
                {
                NodeList tg1 = doc1.getElementsByTagName(wanted);
                NodeList tg2 = doc2.getElementsByTagName(wanted);

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

                                //mark nodes matched
                                aNode.setUserData("matched", "true", null);
                                bNode.setUserData("matched", "true", null);

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
            matchSet.print_set();

        return matchSet;

    }

    private TreeSet markElements(final Document doc)
        {

        //Create tree walker to iterate through all nodes in docs.
        //Maybe want to set to SHOW_ELEMENT and do other elements separately
        TreeWalker walker = ((DocumentTraversal) doc).createTreeWalker(
                doc.getDocumentElement(), NodeFilter.SHOW_ALL, null, false);

        NodeImpl node;

        TreeSet td = new TreeSet(new tdComp());

        //Add root node (should change loop to include)
        node = (NodeImpl) walker.getCurrentNode();
        node.setUserData("matched", "false", null);
        node.setUserData("inorder", "false", null);

        NodeInfo rootInfo = new NodeInfo(node.getNodeName(), 0);
        td.add(rootInfo);
        DiffXML.log.finer("Added "  + rootInfo.getTag() + " Depth " + 0);

        while ((node = (NodeImpl) walker.nextNode()) != null)
            {
            node.setUserData("matched", "false", null);
            //Let children default to be "out of order"
            //Test with "inorder"
            node.setUserData("inorder", "true", null);

            //Get iterator for TreeSet
            Iterator it = td.iterator();

            NodeInfo ni = new NodeInfo(node.getNodeName(), 0);

            //Get depth
            int depth = 1;
            NodeImpl parentNode = (NodeImpl) node.getParentNode();
            while (parentNode != doc.getDocumentElement() && parentNode != null)
                {
                depth++;
                parentNode = (NodeImpl) parentNode.getParentNode();
                }

            ni.setDepth(depth);
            td.add(ni);
            DiffXML.log.finer("Added "  + ni.getTag()
                    + " Depth " + ni.getDepth());
            }
        return td;
        }

}

