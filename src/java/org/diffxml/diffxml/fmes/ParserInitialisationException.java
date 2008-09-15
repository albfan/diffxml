/*
Program to create DUL delta from xmdiff output
 
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
 * Indicates a failure in configuring the parser.
 * 
 * @author Adrian Mouat
 *
 */
public class ParserInitialisationException extends Exception {
    /**
     * Serial ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param e Chained exception
     */
    ParserInitialisationException(Exception e) {
        super(e);
    }
    
    /**
     * Constructor.
     * 
     * @param s Description of error
     * @param e Chained exception
     */
    ParserInitialisationException(String s, Exception e) {
        super(s, e);
    }
}