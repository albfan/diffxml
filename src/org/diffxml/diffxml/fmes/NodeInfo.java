/*
Program to difference two XML files

Copyright (C) 2002  Adrian Mouat

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

/**
 * Holds the name and depth associated with a node.
 *
 * @author Adrian Mouat
 */

public class NodeInfo
{
    /**
     * Field holding nodes depth.
     */

    private int _depth;

    /**
     * Field holding value of associated tag.
     */

    private String _tag;

    /**
     * Constructor taking initial values.
     *
     * @param tag Initial value for Tag
     * @param depth Initial value for depth
     */

    NodeInfo(final String tag, final int depth)
        {
        _tag = tag;
        _depth = depth;
        }

    /**
     * Determines if two NodeInfo objects are equal.
     *
     * @param ni NodeInfo to compare with
     * @return True if nodes are equal, otherwise false
     */

    public final boolean equals(final NodeInfo ni)
        {
        return ((ni.getDepth() == this._depth)
                && (ni.getTag().equals(this._tag)));
        }

    /**
     * Sets the tag value.
     *
     * @param tag A string containing the new tag value
     */

    public final void setTag(final String tag)
        {
        _tag = tag;
        }

    /**
     * Returns the tag value.
     *
     * @return The current value of the tag
     */

    public final String getTag()
        {
        return _tag;
        }

    /**
     * Sets the depth value.
     *
     * @param depth An int with the new depth value
     */

    public final void setDepth(final int depth)
        {
        _depth = depth;
        }

    /**
     * Returns the depth value.
     *
     * @return The current depth value
     */

    public final int getDepth()
        {
        return _depth;
        }

    public final boolean equals(final Object o)
        {
        return o.equals(this);
        }

    public final int hashCode()
        {
        return _tag.length() + _depth;
        }
}
