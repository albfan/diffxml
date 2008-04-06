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

package org.diffxml.diffxml.fmes;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.diffxml.diffxml.Diff;
import org.diffxml.diffxml.DiffFactory;
import org.diffxml.diffxml.DiffXML;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import java.util.StringTokenizer;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Fmes finds the differences between two DOM documents.
 *
 * Uses the Fast Match Edit Script algorithm (fmes).
 * Output is a DOM document representing the differences in
 * DUL format.
 *
 * @author     Adrian Mouat
 */

public class Fmes extends Diff
{
    /**
     * Determines if the given node should be ignored.
     *
     * Examines the node's type against settings.
     *
     * @return True if the node is banned, false otherwise
     * @param  n   The node to be checked
     */

    public static boolean isBanned(final Node n)
        {
        //Check if ignorable whitespace
        if (DiffFactory.IGNORE_WHITESPACE_NODES
                && n.getNodeType() == Node.TEXT_NODE)
            {
            StringTokenizer st = new StringTokenizer(n.getNodeValue());
            if (!st.hasMoreTokens())
                return true;
            }

        //Check if ignorable comment
        if (DiffFactory.IGNORE_COMMENTS && n.getNodeType() == Node.COMMENT_NODE)
            return true;

        //Check if ignorable pi
        if (DiffFactory.IGNORE_PROCESSING_INSTRUCTIONS
                && n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)
            return true;

        return false;
        }

    /**
     * Sets various features on the DOM Parser.
     *
     * Sets features relevant to entities, DTD and entity-ref nodes
     * TODO: Consider moving to helper class.
     *
     * @param parser  The parser to be set
     * @throws ParserInitialisationException 
     */

    public static void initParser(final DocumentBuilderFactory parserFactory) 
    throws ParserInitialisationException
        {
        try
            {
            //These features affect whether entities are reolved or not
            if (!DiffFactory.ENTITIES)
                {
                parserFactory.setFeature(
                        "http://xml.org/sax/features/external-general-entities",
                        false);
                parserFactory.setFeature(
                        "http://xml.org/sax/features/external-parameter-entities",
                        false);
                }

            //Turn off DTD stuff - if DTD support changes recondsider
            parserFactory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
            parserFactory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

            //We don't want entity-ref-nodes, either text or no text
            parserFactory.setFeature(
                    "http://apache.org/xml/features/dom/create-entity-ref-nodes",
                    false);
            }
        catch (ParserConfigurationException e)
            {
            throw new ParserInitialisationException(
                    "Could not set parser feature", e);
            }
        }

    /**
     * Calls fmes diff on two files.
     *
     * @return       True if differences were found
     * @param file1  The original file
     * @param file2  The modified file
     *
     **/
    public final boolean diff(final File file1, final File file2)
        {
        return diff(file1.getPath(), file2.getPath());
        }

    /**
     * Calls fmes diff on two files.
     *
     * The files are pointed to by strings holding the path to the file.
     *
     * @return       True if differences were found
     * @param file1  The original file
     * @param file2  The modified file
     *
     **/
    public final boolean diff(final String file1, final String file2)
        {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        try {
            initParser(fac);
        } catch (ParserInitialisationException e) {
            System.err.println("Failed to initialise parser: " 
                    + e.getMessage());
            System.exit(2);
        }
        Document doc1 = null;
        Document doc2 = null;

        try
            {
            doc1 = fac.newDocumentBuilder().parse(file1);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e.getMessage());
            System.exit(2);
            }

        try
            {
            doc2 = fac.newDocumentBuilder().parse(file2);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e.getMessage());
            System.exit(2);
            }

        Document delta = (new Fmes()).diff(doc1, doc2);
        //Determine if documents differ
        //This could be done quicker inside Match

        boolean differ = false;
        if (delta.getDocumentElement().getChildNodes().getLength() > 0)
        {
            differ = true;
        }
        if (!DiffFactory.BRIEF)
        {
            outputXML(delta);
        }

        if (differ)
            {
            //If in brief mode, don't output delta, only whether files differ
            if (DiffFactory.BRIEF)
                {
                System.out.println("XML documents " + file1 + " and "
                        + file2 + " differ");
                }
            }
        
        return differ;
        }

    /**
     * Writes given XML document to standard out.
     *
     * TODO: Think there is now a more standard way to do this
     * TODO: Move to utility class
     * Uses UTF8 encoding, no indentation, preserves spaces.
     *
     * @param doc DOM document to output
     */

    public static void outputXML(final Document doc)
        {

        //Could create object once and store ref.

        XMLSerializer xs = new XMLSerializer();
        xs.setOutputByteStream(System.out);
        OutputFormat of = new OutputFormat(doc, "UTF-8", false);
        of.setPreserveSpace(true);
        xs.setOutputFormat(of);
        try { xs.serialize(doc); }
        catch (java.io.IOException e)
            {
            System.err.println("Failed to serialize document " + e);
            }
        System.out.println();

        }

    /**
     * Differences two DOM documents and returns the delta.
     *
     * The delta is in DUL format.
     *
     * @param doc1    The original document
     * @param doc2    The new document
     *
     * @return        A document describing the changes
     *                required to make doc1 into doc2.
     */

    public final Document diff(final Document doc1, final Document doc2)
        {
        //Consider adding singleton methods?

        //Match Nodes
        Match match = new Match();

        NodeSet matchings = match.fastMatch(doc1, doc2);

        //Create Edit Script
        EditScript es = new EditScript();
        DiffXML.log.entering("diff", "EditScript.create");
        Document delta = null;
        try {
            delta = es.create(doc1, doc2, matchings);
        } catch (DocumentCreationException e) {
            DiffXML.log.severe("Failed to create Edit Script: " 
                    + e.getMessage());
            System.err.println("Internal error when creating Edit Script");
            System.exit(2);
        }
        DiffXML.log.exiting("diff", "EditScript.create");

        return delta;
        }
}
