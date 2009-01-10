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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.DocumentTraversal;

/**
 * Solves the "good matchings" problem for the FMES algorithm.
 *
 * Essentially pairs nodes that match between documents.
 * Uses the "fast match" algorithm is detailed in the paper
 * "Change Detection in Hierarchically Structure Information".
 *
 * WARNING: Will only work correctly with acylic documents.
 * TODO: Add alternate matching code for cylic documents.
 * See: http://www.devarticles.com/c/a/Development-Cycles/How-to-Strike-a-Match/
 * for information on how to match strings.
 *
 * @author Adrian Mouat
 */

public final class Match {

    /**
     * Private constructor.
     */
    private Match() {
        //Shouldn't be called
    }
    
    /**
     * Performs fast match algorithm on given DOM documents.
     * 
     *  TODO: May want to consider starting at same point in 2nd tree somehow, 
     *  may lead to better matches.
     * 
     * @param doc1
     *            The original document
     * @param doc2
     *            The modified document
     * 
     * @return NodeSet containing pairs of matching nodes.
     */
    public static NodePairs easyMatch(final Document doc1,
            final Document doc2) {

        NodePairs matchSet = new NodePairs();

        doc1.getDocumentElement().normalize();
        doc2.getDocumentElement().normalize();

        List<NodeDepth> list1 = initialiseAndOrderNodes(doc1);
        List<NodeDepth> list2 = initialiseAndOrderNodes(doc2);

        // Proceed bottom up on List 1
        for (NodeDepth nd1 : list1) {
            for (NodeDepth nd2 : list2) {
                
                Node n1 = nd1.getNode();
                Node n2 = nd2.getNode();
                
                if (compareNodes(n1, n2)) {
                    
                    matchSet.add(n1, n2);
                    
                    //Don't want to consider it again
                    list2.remove(nd2);
                    break;
                }
            }
        }

        return matchSet;
    }

    /**
     * Compares two elements two determine whether they should be matched.
     * 
     * TODO: This method is critical in getting good results. Will need to be
     * tweaked. In addition, it may be an idea to allow certain doc types to
     * override it. Consider comparing position, matching of kids etc.
     * 
     * @param a
     *            First element
     * @param b
     *            Potential match for b
     * @return true if nodes match, false otherwise
     */
    private static boolean compareElements(final Node a, final Node b) {

        boolean ret = false;
        
        if (a.getNodeName().equals(b.getNodeName())) {

            //Compare attributes
            
            //Attributes are equal until we find one that doesn't match
            ret = true;
            
            NamedNodeMap aAttrs = a.getAttributes();
            NamedNodeMap bAttrs = b.getAttributes();

            int numberAAttrs = 0;
            if (aAttrs != null) {
                numberAAttrs = aAttrs.getLength();
            }
            int numberBAttrs = 0;
            if (bAttrs != null) {
                numberBAttrs = bAttrs.getLength();
            }
            if (numberAAttrs != numberBAttrs) {
                ret = false;
            }

            int i = 0;
            while ((ret == true) && (i < numberAAttrs)) {
                // Check if attr exists in other tag
                if (bAttrs.getNamedItem(aAttrs.item(i).getNodeName()) == null) {
                    ret = false;
                }

                if (!bAttrs.getNamedItem(aAttrs.item(i).getNodeName())
                        .getNodeValue().equals(aAttrs.item(i).getNodeValue())) {
                    ret = false;
                }
                i++;
            }
        }
        
        return ret;
    }

    /**
     * Compares two text nodes to determine if they should be matched.
     * 
     * Takes into account whitespace options.
     * 
     * @param a
     *            First node
     * @param b
     *            Potential match for a
     * @return True if nodes match, false otherwise
     */

    private static boolean compareTextNodes(final Node a, final Node b) {

        String aString = a.getNodeValue();
        String bString = b.getNodeValue();

        if (DiffFactory.isIgnoreAllWhitespace()) {
            // Remove whitespace from nodes before comparison
            // TODO: Check nextToken doesn't skip first
            StringTokenizer st = new StringTokenizer(aString);
            StringBuffer stringBuf = new StringBuffer(aString.length());
            
            while (st.hasMoreTokens()) {
                stringBuf.append(st.nextToken());
            }
            aString = stringBuf.toString();

            st = new StringTokenizer(bString);
            stringBuf = new StringBuffer(bString.length());         
            while (st.hasMoreTokens()) {
                stringBuf.append(st.nextToken());
            }
            bString = stringBuf.toString();
            
        } else if (DiffFactory.isIgnoreLeadingWhitespace()) {
            // Ignore leading ws
            // just call trim
            aString = aString.trim();
            bString = bString.trim();
        }

        // Check case optn
        boolean ret;
        if (DiffFactory.isIgnoreCase()) {
            ret = (aString.equalsIgnoreCase(bString));
        } else {
            ret = (aString.equals(bString));
        }
        
        return ret;
    }

    /**
     * Compares 2 nodes to determine whether they should match.
     * 
     * TODO: Check if more comparisons are needed
     * TODO: Consider moving out to a separate class, implementing an interface
     * 
     * @param a
     *            first node
     * @param b
     *            potential match for a
     * @return true if nodes match, false otherwise
     */
    private static boolean compareNodes(final Node a, final Node b) {

        boolean ret = false;

        if (a.getNodeType() == b.getNodeType()) { 

            switch (a.getNodeType()) {
                case Node.ELEMENT_NODE :
                    ret = compareElements(a, b);
                    break;
                case Node.TEXT_NODE :
                    ret = compareTextNodes(a, b);
                    break;
                default :
                    ret = (a.getNodeValue().equals(b.getNodeValue()));
            }
        }
        
        return ret;
    }

    /**
     * Returns a list of Nodes sorted according to their depths.
     * 
     * TreeSet is sorted in reverse order of depth according to
     * NodeInfoComparator.
     * 
     * @param doc The document to be initialised and ordered.
     * @return A depth-ordered list of the nodes in the doc.
     */
    private static List<NodeDepth> initialiseAndOrderNodes(
            final Document doc) {

        NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(
                doc.getDocumentElement(), NodeFilter.SHOW_ALL, null, false);

        List<NodeDepth> depthSorted = new ArrayList<NodeDepth>();
             
        Node n;
        while ((n = ni.nextNode()) != null) {
            depthSorted.add(new NodeDepth(n));
        }
        
        ni.detach();
        Collections.sort(depthSorted, new NodeDepthComparator());
        
        return depthSorted;
    }
}

