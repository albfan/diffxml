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
import org.diffxml.diffxml.Diff;
import org.diffxml.diffxml.fmes.Writer;
import org.diffxml.diffxml.DiffFactory;
import org.diffxml.diffxml.fmes.NodeSet;
import org.diffxml.diffxml.fmes.Match;
import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.fmes.EditScript;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import java.util.StringTokenizer;
import java.io.UnsupportedEncodingException;
import java.io.File;

/**
 * Fmes finds the differences between two DOM documents.
 *
 * Uses the Fast Match Edit Script algorithm (fmes).
 * Output is a DOM document representing the differeneces in
 * DUL format.
 *
 * @author 	Adrian Mouat
 */

public class Fmes extends Diff
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

    */

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
	if (DiffFactory.IGNORE_WHITESPACE_NODES && n.getNodeType()==Node.TEXT_NODE)
	    {
            StringTokenizer st = new StringTokenizer(n.getNodeValue());
            if (!st.hasMoreTokens())
                return true;
            }
        //Check if ignorable comment
        if (DiffFactory.IGNORE_COMMENTS && n.getNodeType()==Node.COMMENT_NODE)
            return true;
 
        if (DiffFactory.IGNORE_PROCESSING_INSTRUCTIONS  && n.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE)
            return true;
 
        return false;
        }

    /**
     * Sets various features on the DOM Parser.
     *
     * Sets features relevant to entities, DTD and entity-ref nodes
     *
     * @param parser
     */
 
    private static void initParser(final DOMParser parser)
        {
        try
            {
            //These features affect whether entities are reolved or not
            if (!DiffFactory.ENTITIES)
                {
                parser.setFeature(
                        "http://xml.org/sax/features/external-general-entities",
                        false);
                parser.setFeature(
                        "http://xml.org/sax/features/external-parameter-entities",
                        false);
                }

            //Turn off DTD stuff - if DTD support changes recondsider
            parser.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
            parser.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

            //We don't want entity-ref-nodes, either text or no text
            parser.setFeature(
                    "http://apache.org/xml/features/dom/create-entity-ref-nodes",
                    false);
            }
        catch (SAXException e)
            {
            System.err.println("Could not set parser feature" + e);
            }
        }

    public boolean diff(File file1, File file2)
        {
        return diff(file1.getPath(), file2.getPath());
        }

    public boolean diff(String file1, String file2)
        {
        DOMParser parser = new DOMParser();
        initParser(parser);
        Document doc1, doc2;

        try
            {
            parser.parse(file1);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e);
            System.exit(2);
            }

        doc1 = parser.getDocument();

        try
            {
            parser.parse(file2);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e);
            System.exit(2);
            }

        doc2 = parser.getDocument();

        Document delta = (new Fmes()).diff(doc1, doc2);
        //Determine if documents differ
        //This could be done quicker inside Match

        boolean differ = false;
        if (delta.getDocumentElement().getChildNodes().getLength() > 0)
            differ = true;

        if (!DiffFactory.BRIEF)
            outputXML(delta);

        if (differ)
            {
            //If in brief mode, don't output delta, only whether files differ
            if (DiffFactory.BRIEF)
                {
                System.out.println("XML documents " + file1 + " and "
                        + file2 + " differ");
                }

            return true;
            }
        else
            return false;
        }

    /**
     * Writes given XML document to standard out.
     *
     * Uses UTF8 encoding.
     *
     * @param doc
     */
 
    private static void outputXML(final Document doc)
        {
        Writer writer = new Writer();
        try {
            writer.setOutput(System.out, "UTF8");
        }
        catch (UnsupportedEncodingException e) {
            System.err.println("Unable to set output. Exiting.");
            System.exit(1);
        }
        writer.setCanonical(false);
        writer.write(doc);
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
