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

import java.util.Comparator;

/**
 * Comparator for NodeInfo objects.
 *
 * Based on reverse depth.
 */

class NodeInfoComparator implements Comparator
{

    /**
     * Compares two NodeInfo objects.
     *
     * Stores in reverse order of depth.
     * Strings sorted to preserve set logic.
     * Consider adding serializable, in case TreeSet is serialized.
     *
     * @param o1 First NodeInfo object
     * @param o2 Second NodeInfo object
     * @return Negative if o1 is at a greater depth than 02,
     *         Positive if smaller depth,
     *         Tags are compared if same depth.
     */

    public final int compare(final Object o1, final Object o2)
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
}

