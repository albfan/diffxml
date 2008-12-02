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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.diffxml.diffxml.DiffXML;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implements simple Longest Common Substring (LCS) algorithm.
 *
 * Very simple and inefficient.
 * TODO: Use better algorithm.
 * TODO: Create equals method.
 * TODO: Refactor into smaller functions.
 */

public final class NodeSequence
{

    /**
     * Do not allow instantiation.
     */

    private NodeSequence() { }

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
    public static Node[] getSequence(final NodeList set1, final NodeList set2,
            final NodePairs matchings) {
        
        List<Node> resultSet = new ArrayList<Node>(set1.getLength());

        List<Node> set2list = Arrays.asList(
                NodeOps.getElementsOfNodeList(set2));

        for (int i = 0; i < set1.getLength(); i++) {
            if (set2list.contains(matchings.getPartner(set1.item(i)))) {
                resultSet.add(set1.item(i));
            }            
        }
        
        return resultSet.toArray(new Node[resultSet.size()]);
    }
    
    /**
     * Finds the longest common subsequence of two Node[].
     *
     * @param a First array of nodes
     * @param b Second array of nodes
     * @param matchings Set of matchings used to determine equality
     * @return Node[] The longest subsequence common to both a and b
     */

    public static Node[] find(final Node[] a, final Node[] b,
            final NodePairs matchings)
        {
        //If either list is empty, so is LCS

        int i, j;

        if (a.length == 0 || b.length == 0)
            {
            Node[] tmp = new Node[1];
            return tmp;
            }

        int[][] matrix = new int[a.length + 1][b.length + 1];
        matrix[0][0] = 0;

        for (i = 1; i <= a.length; i++)
            {
            for (j = 1; j <= b.length; j++)
                {
                //TODO: Find out why null nodes occur here
                if (NodeOps.checkIfSameNode(matchings.getPartner(a[i - 1]),
                       b[j - 1]))
                    {
                    matrix[i][j] = matrix[i - 1][j - 1] + 1;
                    }
                else
                    {
                    if (matrix[i - 1][j] > matrix[i][j - 1])
                        matrix[i][j] = matrix[i - 1][j];
                    else
                        matrix[i][j] = matrix[i][j - 1];
                    }
                }
            }

        //reconstruction
        i = (a.length - 1);
        j = (b.length - 1);

        int r = 0;
        DiffXML.LOG.finer("i=" + i + " j=" + j);
        int seqLength = matrix[i][j];

        DiffXML.LOG.finer("seqLength=" + seqLength);
        //Store pairs of sequence
        Node[] lcs = new Node[(2 * seqLength) + 2];
        while (i >= 0 && j >= 0)
            {
            if (NodeOps.checkIfSameNode(matchings.getPartner(a[i]), b[j]))
                {
                lcs[r] = a[i];
                lcs[r + 1] = b[j];
                r = r + 2;
                i--;
                j--;
                }
            else
                {
                //Not sure about cases with zero
                if (j == 0)
                    i--;
                else if (i == 0)
                    j--;
                else if (matrix[i - 1][j] > matrix[i][j - 1])
                    i--;
                else
                    j--;
                }
            }
        //Should now reverse list
        //But it doesn't matter for our needs
        return lcs;
        }
}

