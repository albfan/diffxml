/*
Program to difference two XML files
   
Copyright (C) 2002-2004 Adrian Mouat
   
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

package org.diffxml.diffxml;


import org.diffxml.diffxml.fmes.Fmes;
import org.diffxml.diffxml.xmdiff.XmDiff;


/**
 * DiffFactory creates Diff instances.
 *
 * @author 	Adrian Mouat
 */

public class DiffFactory
{
    /*
     * Fields to set various options
     *
     * I have avoided setter/getter methods as unneccesary for
     * simple methods.
     *
     * Also the leading '_' convention has been ignored for
     * clarity.
     *
     * This may change in the future, especially concerning the
     * methods handling ints.
     *
     * TODO: Make state fixed for each diff instance. Currently 
     * changes to options affect in-process diffs.
     */
    

    //Report only if files differ
    //Default off
    public static boolean BRIEF=false;
 
    //Ignore all whitespace
    //default off
    public static boolean IGNORE_ALL_WHITESPACE=false;
 
    //Ignore leading whitespace
    //default off
    public static boolean IGNORE_LEADING_WHITESPACE=false;
 
    //Ignore whitespace only nodes
    //default off?
    public static boolean IGNORE_WHITESPACE_NODES=false;
 
    //Ignore changes in case only
    //default off
    public static boolean IGNORE_CASE=false;

    //Ignore comments
    //default off
    public static boolean IGNORE_COMMENTS=false;
 
    //Ignore processing instructions
    //default off
    public static boolean IGNORE_PROCESSING_INSTRUCTIONS=false;
 
    //Output tagnames
    //default off
    public static boolean TAGNAMES=false;
 
    //Output reverse patching context
    //default off
    public static boolean REVERSE_PATCH=false;
 
    //Whether or not to output context
    //default off
    public static boolean CONTEXT=false;
 
    //Amount of sibling context
    //default 2
    public static int SIBLING_CONTEXT= 2;
 
    //Amount of parent context
    //default 1
    public static int PARENT_CONTEXT=1;
 
    //Amount of parent sibling context
    //default 0
    public static int PARENT_SIBLING_CONTEXT=0;

    //Algorithm to use
    //default fmes
    public static boolean FMES=true;
 
    //Use DUL output format
    //Setting to false is eqv to XUpdate

    //XUpdate not currently supported!
    //default on
    public static boolean DUL=true;
 
    //Resolving of entities
    public static boolean ENTITIES=true;


    public static Diff createDiff()
        {
        if (DiffFactory.FMES)
            return (new Fmes());
        else
            return (new XmDiff());
        }

}
