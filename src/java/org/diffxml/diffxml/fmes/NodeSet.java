/*
Class to hold set of nodes

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

import org.w3c.dom.Node;
import java.util.ArrayList;

/**
 * Class to hold pairs of nodes.
 *
 * TODO: Replace with more object oriented methods.
 */

public class NodeSet
{

    /**
     * Internal set to store nodes.
     */

    private ArrayList _set = new ArrayList();

    /**
     * Adds a pair of nodes to the set.
     *
     * @param x first node
     * @param y partner of first node
     */

    public final void add(final Node x, final Node y)
        {
        _set.add(x);
        _set.add(y);
        }

    /**
     * Debug routine to output contents of set.
     */

    public final void printSet()
        {
        for (int i = 0; i < _set.size(); i = i + 2)
            {
            //Print out each node
            System.out.println("Node: " + ((Node) _set.get(i)).getNodeName());
            PrintXML.print((Node) _set.get(i));
            System.out.println("Matches: "
                    + ((Node) _set.get(i + 1)).getNodeName());
            PrintXML.print((Node) _set.get(i + 1));
            }
        }

    /**
     * Returns the partner of a given node.
     * Returns null if the node does not exist.
     *
     * @param  n the node to find the partner of.
     * @return   the partner of n.
     */

    public final Node getPartner(final Node n)
        {
        int in = _set.indexOf(n);
        if (in == -1)
        {
            return null;    
        }

        if ((in % 2) == 1)
            in--;
        else
            in++;

        return (Node) _set.get(in);
        }
}

