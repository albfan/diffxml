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

/**
 * Implements a First In First Out list.
 *
 * Equivalent to a stack where elements are removed from
 * the *opposite* end to where the are added. Hence the
 * Stack terms "push" and pop" are used.
 */

public class Fifo
{
    /**
     * Underlying data structure is an ArrayList.
     */

    private ArrayList _fifo;

    /**
     * Default constructor.
     */

    Fifo()
        {
        _fifo = new ArrayList();
        }

    /**
     * Adds an object to the Fifo.
     *
     * @param o the object to added
     */

    public final void push(final Object o)
        {
        _fifo.add(o);
        }

    /**
     * Checks if the Fifo contains any objects.
     *
     * @return true if there are any objects in the Fifo
     */

    public final boolean isEmpty()
        {
        return _fifo.isEmpty();
        }

    /**
     * Remove an object from the Fifo.
     *
     * This object is always the oldest item in the array.
     *
     * @return the oldest item in the Fifo
     */

    public final Object pop()
        {
        if (_fifo.isEmpty())
            return null;

        return _fifo.remove(0);
        }
}




