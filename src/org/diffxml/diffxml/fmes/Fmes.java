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

import org.w3c.dom.Document;
import org.diffxml.diffxml.fmes.NodeSet;
import org.diffxml.diffxml.fmes.Match;
import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.fmes.EditScript;
import org.w3c.dom.Node;
import java.util.StringTokenizer;

/**
 * Fmes finds the differences between two DOM documents.
 *
 * Uses the Fast Match Edit Script algorithm (fmes).
 * Output is a DOM document representing the differeneces in
 * DUL format.
 *
 * @author 	Adrian Mouat
 */

public class Fmes
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
     * Also note these options should be on an abstract father
     * class to Fmes/pulldiff, not in Fmes only.
     * However, at present only Fmes makes use of them.
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


    /**
     * Determines if the given node should be ignored.
     *
     * Examines the node's type against settings.
     *
     * @return True if the node is banned, false otherwise
     */

    public static boolean isBanned(Node n)
        {
	//Check if ignorable whitespace
	if (IGNORE_WHITESPACE_NODES && n.getNodeType()==Node.TEXT_NODE)
	    {
            StringTokenizer st = new StringTokenizer(n.getNodeValue());
            if (!st.hasMoreTokens())
                return true;
            }
        //Check if ignorable comment
        if (IGNORE_COMMENTS && n.getNodeType()==Node.COMMENT_NODE)
            return true;
 
        if (IGNORE_PROCESSING_INSTRUCTIONS  && n.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE)
            return true;
 
        return false;
        }

    /**
     * Differences two DOM documents and returns the delta.
     *
     * The delta is in DUL format.
     *
     * @param doc1 	The original document
     * @param doc2	The new document
     *
     * @return 		A document describing the changes 
     * 			required to make doc1 into doc2.
     */

    public Document diff(Document doc1, Document doc2)
	{
	//Consider adding singleton methods?
	
	//Match Nodes
	Match match=new Match();
	//Consider changing not to throw exception
	NodeSet matchings = new NodeSet();
	try { matchings = match.test(doc1, doc2); }
	catch (Exception e)
	    { 
	    System.err.println("Failed to match nodes: " + e);
	    System.exit(2);
	    }

	//Create Edit Script
	DiffXML.log.entering("diff","EditScript.create");
	Document delta= EditScript.create(doc1, doc2, matchings);
	DiffXML.log.exiting("diff","EditScript.create");

	return delta;
	}
}
