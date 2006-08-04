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

import org.w3c.dom.Node;
import java.util.ArrayList;

/**
 * Class to hold pairs of nodes.
 *
 * TODO: Check if should be implementing any interfaces and if "set"
 * correct. Implement printSet().
 */

public class NodePairSet
{

    class NodePair
        {
        public Node _a;
        public Node _b;

        NodePair(Node x, Node y)
            {
            _a = x;
            _b = y;
            }
        }

    ArrayList _set = new ArrayList();

    public final void add(final Node x, final Node y)
        {
        _set.add(new NodePair(x, y));
        }

    public final Node getPartner(final Node n)
        {
        return n;
        }

    public final void printSet()
        {
        System.out.println("Not implemented");
        }

}

