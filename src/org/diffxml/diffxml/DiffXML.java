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

package org.diffxml.diffxml;

import org.diffxml.diffxml.fmes.Fmes;
import org.diffxml.diffxml.xmdiff.XmDiff;

import org.xml.sax.SAXException;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Level;

/**
 * DiffXML finds the differences between 2 XML files.
 *
 * This class takes input from the command line and starts the
 * differencing algorithm.
 *
 * @author      Adrian Mouat
 */

public class DiffXML
{

    private static final String VERSION = "0.92 ALPHA";
    //The files to be differenced
    private static String _file1;
    private static String _file2;

    //Logger
    public static Logger log = Logger.getLogger("diffxml");

    /**
     * Checks and interprets the command line arguments.
     *
     * Code is based on Sun standard code for handling arguments.
     *
     * @param args    An array of the command line arguments
     */

    private static void parseArgs(final String[] args)
        {
        int i = 0, j, num;
        String arg;
        char flag;
        String outputfile = "";

        while (i < args.length && args[i].startsWith("-"))
            {
            arg = args[i++];

            /* Normalize multiple dashes
               I don't understand point in differentiating between 1
               and 2 dashes.
               We allow 2 to mimic diff util, but compress to 1 */

            if (arg.startsWith("--"))
                arg = arg.substring(1);

            //"wordy" arguments
            if (arg.equals("-brief"))
                Fmes.BRIEF = true;
            else if (arg.equals("-ignore-all-whitespace"))
                {
                Fmes.IGNORE_ALL_WHITESPACE = true;
                Fmes.IGNORE_WHITESPACE_NODES = true;
                }
            else if (arg.equals("-ignore-leading-whitespace"))
                {
                Fmes.IGNORE_LEADING_WHITESPACE = true;
                Fmes.IGNORE_WHITESPACE_NODES = true;
                }
            else if (arg.equals("-ignore-empty-nodes"))
                Fmes.IGNORE_WHITESPACE_NODES = true;
            else if (arg.equals("-ignore-case"))
                Fmes.IGNORE_CASE = true;
            else if (arg.equals("-ignore-comments"))
                Fmes.IGNORE_COMMENTS = true;
            else if (arg.equals("-ignore-processing-instructions"))
                Fmes.IGNORE_PROCESSING_INSTRUCTIONS = true;
            else if (arg.equals("-version"))
                printVersion();
            else if (arg.equals("-help") || arg.equals("--help"))
                printHelp();
            else if (arg.equals("-fmes"))
                Fmes.FMES = true;
            else if (arg.equals("-xmdiff"))
                Fmes.FMES = false;
            else if (arg.equals("-tagnames"))
                Fmes.TAGNAMES = true;
            else if (arg.equals("-reverse-patch"))
                Fmes.REVERSE_PATCH = true;
            else if (arg.equals("-sibling-context"))
                Fmes.CONTEXT = true;
            else if (arg.equals("-parent-context"))
                Fmes.CONTEXT = true;
            else if (arg.equals("-parent-sibling-context"))
                {
                Fmes.CONTEXT = true;
                //Defaults to 0 if not specified at all, 1 if speced
                Fmes.PARENT_SIBLING_CONTEXT = 1;
                }
            else if (arg.equals("-xupdate"))
                Fmes.DUL = false;
            else if (arg.equals("-dul"))
                Fmes.DUL = true;
            else if (arg.equals("-remove-entities"))
                {
                Fmes.ENTITIES = false;
                System.err.println("STERN WARNING: Removing entities may lead to "
                        + "incorrect or misleading results");
                }

            //Arguments with arguments

            else if (arg.startsWith("-sibling-context="))
                {
                //Get the number
                num = Integer.parseInt((arg.substring(17)));
                if (num < 0)
                    {
                    System.err.println(
                            "Sibling Context must positive integer > 0");
                    System.exit(2);
                    }

                Fmes.CONTEXT = true;
                Fmes.SIBLING_CONTEXT = num;
                }
            else if (arg.startsWith("-parent-context="))
                {
                //Get the number
                num = Integer.parseInt(arg.substring(16));
                if (num < 0)
                    {
                    System.err.println(
                            "Parent Context must positive integer > 0");
                    System.exit(2);
                    }

                Fmes.CONTEXT = true;
                Fmes.PARENT_CONTEXT = num;
                }
            else if (arg.startsWith("-parent-sibling-context="))
                {
                //Get the number
                num = Integer.parseInt(arg.substring(24));
                if (num < 0)
                    {
                    System.err.println(
                            "Parent Sibling Context must positive integer > 0");
                    System.exit(2);
                    }

                Fmes.CONTEXT = true;
                Fmes.PARENT_SIBLING_CONTEXT = num;
                }

            //Short arguments with arguments
            else if (arg.equals("-C"))
                {
                Fmes.CONTEXT = true;
                if (i < args.length)
                    {
                    //Next argument must be a number
                    num = Integer.parseInt(args[i++]);
                    if (num < 1)
                        {
                        System.err.println(
                                "-C needs positive integer argument");
                        System.exit(2);
                        }
                    Fmes.SIBLING_CONTEXT = num;
                    }
                else
                    {
                    System.err.println("-C needs positive integer argument");
                    System.exit(2);
                    }
                }
            else if (arg.equals("-P"))
                {
                Fmes.CONTEXT = true;
                if (i < args.length)
                    {
                    //Next argument must be a number
                    num = Integer.parseInt(args[i++]);
                    if (num < 1)
                        {
                        System.err.println(
                                "-P needs positive integer argument");
                        System.exit(2);
                        }
                    Fmes.PARENT_CONTEXT = num;
                    }
                else
                    {
                    System.err.println("-P needs positive integer argument");
                    System.exit(2);
                    }
                }
            else if (arg.equals("-S"))
                {
                Fmes.CONTEXT = true;
                if (i < args.length)
                    {
                    //Next argument must be a number
                    num = Integer.parseInt(args[i++]);
                    if (num < 1)
                        {
                        System.err.println(
                                "-S needs positive integer argument");
                        System.exit(2);
                        }
                    Fmes.PARENT_SIBLING_CONTEXT = num;
                    }
                else
                    {
                    System.err.println("-S needs positive integer argument");
                    System.exit(2);
                    }
                }

            //(series of) flag arguments
            else {
                for (j = 1; j < arg.length(); j++) {
                    flag = arg.charAt(j);
                    switch (flag) {
                        case 'q':
                            Fmes.BRIEF = true;
                            break;
                        case 's':
                            Fmes.IGNORE_ALL_WHITESPACE = true;
                            break;
                        case 'w':
                            Fmes.IGNORE_LEADING_WHITESPACE = true;
                            break;
                        case 'e':
                            Fmes.IGNORE_WHITESPACE_NODES = true;
                            break;
                        case 'i':
                            Fmes.IGNORE_CASE = true;
                            break;
                        case 'r':
                            Fmes.IGNORE_COMMENTS = true;
                            break;
                        case 'I':
                            Fmes.IGNORE_PROCESSING_INSTRUCTIONS = true;
                            break;
                        case 'V':
                            printVersion();
                            break;
                        case 'c':
                            Fmes.CONTEXT = true;
                            break;
                        case 'h':
                            printHelp();
                            break;
                        case 'f':
                            Fmes.FMES = true;
                            break;
                        case 'x':
                            Fmes.FMES = false;
                            break;
                        case 't':
                            Fmes.TAGNAMES = true;
                            break;
                        case 'X':
                            Fmes.DUL = false;
                            break;
                        case 'D':
                            Fmes.DUL = true;
                            break;
                        case 'p':
                            Fmes.REVERSE_PATCH = true;
                            break;
                        case 'n':
                            Fmes.ENTITIES = false;
                            break;

                        default:
                            System.err.println("diffxml: illegal option "
                                    + flag);
                            printUsage();
                            break;
                    }
                }
            }
            }
        if ((i + 2) != args.length)
            printUsage();

        _file1 = args[i];
        log.fine("_file1= " + _file1);
        _file2 = args[++i];
        log.fine("_file2= " + _file2);
        }

    /**
     * Outputs usage message to standard error.
     */

    public static void printUsage()
        {
        System.err.println("Usage: diffxml [OPTION]... XMLFILE1 XMLFILE2");
        System.exit(2);
        }

    /**
     * Outputs brief help message to standard out.
     */

    public static void printHelp()
        {
        System.out.print("\nUsage: diffxml [OPTION]... XMLFILE1 XMLFILE2\n\n Find the differences between two XML files.\n\n --brief  -q  Report only if files differ, don't output the delta. \n--ignore-all-whitespace  -s  Ignore all whitespace when comparing nodes.\n--ignore-leading-whitespace  -w  Ignore leading and trailing whitespace in \n\ttext nodes.\n\n--ignore-empty-nodes  -e  Ignore text nodes that contain only whitespace.\n--ignore-case  -i  Consider upper and lower case to be the same. \n--ignore-comments  -r  Ignore changes made to comment elements. \n--ignore-processing-instructions  -I  Ignore changes made to processing \n\tinstructions.\n\n--version  -V  Output version number of program.\n--help  -h  Output this help.\n--fmes  -f  Use the FMES algorithm to compute the changes.\n--xmdiff  -x  Use the xmdiff algorithm to compute the changes.\n--tagnames  -t  Output full tag names of elements.\n--reverse-patch  -p  Create output that allows reversing of a patch.\n--remove-entities  -n  Remove all external entities when processing.\n");

        System.out.print("\n--sibling-context=NUM  -C NUM  Create context information output, \n\twith NUM sibling context (default 2).\n--parent-context=NUM  -P NUM  Create context information output, \n\twith NUM parent and child context (default 1). \n--parent-sibling-context=NUM  -S NUM  Create context information output, \n\twith NUM parent sibling context (default 1).\n");
        System.out.print("\nThis product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).\n\n");
        System.out.print("\nThis product includes software developed by the Apache Software Foundation (http://www.apache.org/).\n\n");
        System.exit(0);
        }

    /**
     * Outputs the current version of diffxml to standard out.
     */

    public static void printVersion()
        {
        System.out.println("diffxml Version " + VERSION + "\n");
        System.out.print("\nThis product includes software developed by the Indiana University Extreme! Lab (http://www.extreme.indiana.edu/).\n");
        System.out.print("\nThis product includes software developed by the Apache Software Foundation (http://www.apache.org/).\n\n");
        System.exit(0);
        }

    /**
     * Attempts to initialise logging.
     *
     * Output is sent to file diffxml.log.
     */
    private static void initLog() throws IOException
        {
        FileHandler logFile = new FileHandler("diffxml.log");
        logFile.setFormatter(new java.util.logging.SimpleFormatter());

        // Send log output to our FileHandler.
        log.addHandler(logFile);

        // Request detail level
        log.setLevel(Level.ALL);

        // We only want messages sent to our file, nowhere else
        log.setUseParentHandlers(false);

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
     * Checks if input files exist.
     *
     * Outputs error message if input not found.
     *
     * @return True only if both files are found.
     */

    private static boolean filesExist()
        {

        File test = new File(_file1);
        if (!test.exists())
            {
            System.err.println("Could not find file: " + _file1);
            return false;
            }
        test = new File(_file2);
        if (!test.exists())
            {
            System.err.println("Could not find file: " + _file2);
            return false;
            }
        return true;
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
            if (!Fmes.ENTITIES)
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

    public static void main(final String[] args)
        {

        //Start logging
        try { initLog(); }
        catch (IOException ex)
            { System.err.println("Unable to instantiate logger " + ex); }

        //Set options - instantiates _file1 and _file2
        parseArgs(args); 

        //Check files exist
        if (!filesExist())
            System.exit(2);

        /* If xmdiff only call diff and exit 
           STILL TO ADD FUNCTIONALITY FOR DETERMINING WHETHER ANY DIFFERENCES FOUND

           An abstract factory would be nice, but isn't used due to the difference in
           what is returned. Both produce a delta, but fmes returns an XML document, the
           other directly outputs the delta.

           This may have to change when xmdiff is updated to properly use options.
           */

        if (!Fmes.FMES)
            {
            try { (new XmDiff()).xmdiff(_file1, _file2); }

            catch (Exception e)
                {
                System.err.println("xmdiff failed: " + e);
                System.exit(2);
                }

            System.exit(1);
            }

        //otherwise use fmes
        //Consider putting code for parsing files in Fmes

        DOMParser parser = new DOMParser();

        initParser(parser);

        //Ignore whitespace nodes
        //Table.ign_ws_nodes=true;

        //Parse XML files
        Document doc1, doc2;

        try
            {
            parser.parse(_file1);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e);
            System.exit(2);
            }

        doc1 = parser.getDocument();

        try
            {
            parser.parse(_file2);
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

        if (!Fmes.BRIEF)
            outputXML(delta);

        if (differ)
            {
            //If in brief mode, don't output delta, only whether files differ
            if (Fmes.BRIEF)
                {
                System.out.println("XML documents " + _file1 + " and "
                        + _file2 + " differ");
                }

            System.exit(1);
            }
        else
            System.exit(0);

        }

}
