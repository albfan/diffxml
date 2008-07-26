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

import org.w3c.dom.Node;

/**
 * Implements simple Longest Common Substring (LCS) algorithm.
 *
 * Very simple and inefficient.
 * TODO: Use better algorithm.
 * TODO: Create equals method.
 * TODO: Refactor into smaller functions.
 */

public final class Lcs
{

    /**
     * Do not allow instantiation.
     */

    private Lcs() { }

    /**
     * Finds the longest common subsequence of two Node[].
     *
     * @param a First array of nodes
     * @param b Second array of nodes
     * @param matchings Set of matchings used to determine equality
     * @return Node[] The longest subsequence common to both a and b
     */

    public static Node[] find(final Node[] a, final Node[] b,
            final NodeSet matchings)
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

