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

package org.diffxml.diffxml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;


/**
 * DiffXML finds the differences between 2 XML files.
 *
 * This class takes input from the command line and starts the
 * differencing algorithm.
 *
 * @author      Adrian Mouat
 */

public final class DiffXML {

    /** Version number. **/
    private static final String VERSION = "0.92 ALPHA";

    /** First file to be differenced. **/
    private static File mFile1;

    /** Second file to be differenced. **/
    private static File mFile2;

    /** Logger. **/
    public static final Logger LOG = Logger.getLogger("diffxml");

    /**
     * Factory used in outputting XML.
     */
    private static final TransformerFactory TRANSFORMER_FACTORY =
        TransformerFactory.newInstance();
    
    /**
     * Private constructor - shouldn't be called.
     */
    private DiffXML() {
        //Private constructor - shouldn't be called
    }

    /**
     * Checks and interprets the command line arguments.
     *
     * Code is based on Sun standard code for handling arguments.
     *
     * @param args    An array of the command line arguments
     */
    private static void parseArgs(final String[] args) {
        
        int argNo = 0;
        String currentArg;
        char flag;

        while (argNo < args.length && args[argNo].startsWith("-")) {
            currentArg = args[argNo++];

            /* Normalize multiple dashes
               I don't understand point in differentiating between 1
               and 2 dashes.
               We allow 2 to mimic diff util, but compress to 1 */

            if (currentArg.startsWith("--")) {
                currentArg = currentArg.substring(1);
            }

            //"wordy" arguments
            if (currentArg.equals("-brief")) {
                DiffFactory.setBrief(true);
            } else if (currentArg.equals("-ignore-all-whitespace")) {
                DiffFactory.setIgnoreAllWhitespace(true);
                DiffFactory.setIgnoreWhitespaceNodes(true);
            } else if (currentArg.equals("-ignore-leading-whitespace")) {
                DiffFactory.setIgnoreLeadingWhitespace(true);
                DiffFactory.setIgnoreWhitespaceNodes(true);
            } else if (currentArg.equals("-ignore-empty-nodes")) {
                DiffFactory.setIgnoreWhitespaceNodes(true);
            } else if (currentArg.equals("-ignore-case")) {
                DiffFactory.setIgnoreCase(true);
            } else if (currentArg.equals("-ignore-comments")) {
                DiffFactory.setIgnoreComments(true);
            } else if (currentArg.equals("-ignore-processing-instructions")) {
                DiffFactory.setIgnoreProcessingInstructions(true);
            } else if (currentArg.equals("-version")) {
                printVersionAndExit();
            } else if (currentArg.equals("-help")) {
                printHelpAndExit();
            } else if (currentArg.equals("-fmes")) {
                DiffFactory.setFMES(true);
            } else if (currentArg.equals("-xmdiff")) {
                DiffFactory.setFMES(false);
            } else if (currentArg.equals("-tagnames")) {
                DiffFactory.setUseTagnames(true);
            } else if (currentArg.equals("-reverse-patch")) {
                DiffFactory.setReversePatch(true);
            } else if (currentArg.equals("-sibling-context")) {
                DiffFactory.setContext(true);
            } else if (currentArg.equals("-parent-context")) {
                DiffFactory.setContext(true);
            } else if (currentArg.equals("-parent-sibling-context")) {
                DiffFactory.setContext(true);
                //Defaults to 0 if not specified at all, 1 if specified
                DiffFactory.setParentSiblingContext(1);
            } else if (currentArg.equals("-xupdate")) {
                DiffFactory.setDUL(false);
            } else if (currentArg.equals("-dul")) {
                DiffFactory.setDUL(true);
            } else if (currentArg.equals("-remove-entities")) {
                DiffFactory.setResolveEntities(false);
                System.err.println("STERN WARNING: Removing entities may lead"
                        + " to incorrect or misleading results");
            } else if (currentArg.startsWith("-sibling-context=")) {

                //Arguments with arguments

                //Get the number
                int num = Integer.parseInt((currentArg.substring(17)));

                if (num < 0) {
                    System.err.println(
                        "Sibling Context must positive integer > 0");
                    System.exit(2);
                }

                DiffFactory.setContext(true);
                DiffFactory.setSiblingContext(num);
            } else if (currentArg.startsWith("-parent-context=")) {
                //Get the number
                int num = Integer.parseInt(currentArg.substring(16));
                if (num < 0) {
                    System.err.println(
                    "Parent Context must positive integer > 0");
                    System.exit(2);
                }

                DiffFactory.setContext(true);
                DiffFactory.setParentContext(num);
            } else if (currentArg.startsWith("-parent-sibling-context=")) {
                //Get the number
                int num = Integer.parseInt(currentArg.substring(24));
                if (num < 0) {
                    System.err.println(
                    "Parent Sibling Context must positive integer > 0");
                    System.exit(2);
                }

                DiffFactory.setContext(true);
                DiffFactory.setParentSiblingContext(num);
            } else if (currentArg.equals("-C")) {

                //Short arguments with arguments

                DiffFactory.setContext(true);
                if (argNo < args.length) {
                    //Next argument must be a number
                    int num = Integer.parseInt(args[argNo++]);
                    if (num < 1) {
                        System.err.println(
                        "-C needs positive integer argument");
                        System.exit(2);
                    }
                    DiffFactory.setSiblingContext(num);

                } else {
                    System.err.println("-C needs positive integer argument");
                    System.exit(2);
                }

            } else if (currentArg.equals("-P")) {
                DiffFactory.setContext(true);
                if (argNo < args.length) {
                    //Next argument must be a number
                    int num = Integer.parseInt(args[argNo++]);
                    if (num < 1) {
                        System.err.println(
                        "-P needs positive integer argument");
                        System.exit(2);
                    }
                    DiffFactory.setParentContext(num);
                } else {
                    System.err.println("-P needs positive integer argument");
                    System.exit(2);
                }

            } else if (currentArg.equals("-S")) {
                DiffFactory.setContext(true);
                if (argNo < args.length) {
                    //Next argument must be a number
                    int num = Integer.parseInt(args[argNo++]);
                    if (num < 1) {
                        System.err.println(
                        "-S needs positive integer argument");
                        System.exit(2);
                    }
                    DiffFactory.setParentSiblingContext(num);
                } else {
                    System.err.println("-S needs positive integer argument");
                    System.exit(2);
                }
            } else {

                //(series of) flag arguments
                for (int charNo = 1; charNo < currentArg.length(); charNo++) {
                    flag = currentArg.charAt(charNo);
                    switch (flag) {
                        case 'q':
                            DiffFactory.setBrief(true);
                            break;
                        case 's':
                            DiffFactory.setIgnoreAllWhitespace(true);
                            break;
                        case 'w':
                            DiffFactory.setIgnoreLeadingWhitespace(true);
                            break;
                        case 'e':
                            DiffFactory.setIgnoreWhitespaceNodes(true);
                            break;
                        case 'i':
                            DiffFactory.setIgnoreCase(true);
                            break;
                        case 'r':
                            DiffFactory.setIgnoreComments(true);
                            break;
                        case 'I':
                            DiffFactory.setIgnoreProcessingInstructions(true);
                            break;
                        case 'V':
                            printVersionAndExit();
                            break;
                        case 'c':
                            DiffFactory.setContext(true);
                            break;
                        case 'h':
                            printHelpAndExit();
                            break;
                        case 'f':
                            DiffFactory.setFMES(true);
                            break;
                        case 'x':
                            DiffFactory.setFMES(false);
                            break;
                        case 't':
                            DiffFactory.setUseTagnames(true);
                            break;
                        case 'X':
                            DiffFactory.setDUL(false);
                            break;
                        case 'D':
                            DiffFactory.setDUL(true);
                            break;
                        case 'p':
                            DiffFactory.setReversePatch(true);
                            break;
                        case 'n':
                            DiffFactory.setResolveEntities(false);
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

        if ((argNo + 2) != args.length) {
            //Not given 2 files on input
            printUsage();
        }

        mFile1 = new File(args[argNo]);
        LOG.fine("mFile1= " + mFile1.getAbsolutePath());
        mFile2 = new File(args[++argNo]);
        LOG.fine("_file2= " + mFile2.getAbsolutePath());
    }

    /**
     * Outputs usage message to standard error.
     */
    public static void printUsage() {
        System.err.println("Usage: diffxml [OPTION]... XMLFILE1 XMLFILE2");
        System.exit(2);
    }

    /**
     * Outputs brief help message to standard out and exits.
     */

    public static void printHelpAndExit() {
        System.out.print("\nUsage: diffxml [OPTION]... XMLFILE1 XMLFILE2\n\n " +
                "Find the differences between two XML files.\n\n" +
                "--brief  -q  Report only if files differ, don't output the " +
                "delta.\n" +
                "--ignore-all-whitespace  -s  Ignore all whitespace when " +
                "comparing nodes.\n" +
                "--ignore-leading-whitespace  -w  Ignore leading and trailing" +
                " whitespace in \n\ttext nodes.\n\n" +
                "--ignore-empty-nodes  -e  Ignore text nodes that contain " +
                "only whitespace.\n" +
                "--ignore-case  -i  Consider upper and lower case to be the" +
                " same. \n" +
                "--ignore-comments  -r  Ignore changes made to comment " +
                "elements. \n" +
                "--ignore-processing-instructions  -I  Ignore changes made to" +
                " processing \n\tinstructions.\n\n" +
                "--version  -V  Output version number of program.\n" +
                "--help  -h  Output this help.\n" +
                "--fmes  -f  Use the FMES algorithm to compute the changes.\n" +
                "--xmdiff  -x  Use the xmdiff algorithm to compute the " +
                "changes.\n" +
                "--tagnames  -t  Output full tag names of elements.\n" +
                "--reverse-patch  -p  Create output that allows reversing of" +
                " a patch.\n--remove-entities  -n  Remove all external" +
        " entities when processing.\n");

        System.out.print("\n--sibling-context=NUM  -C NUM  " +
                "Create context information output, \n\twith NUM sibling " +
                "context (default 2).\n" +
                "--parent-context=NUM  -P NUM  Create context information" +
                " output, \n\twith NUM parent and child context (default 1)." +
                " \n" +
                "--parent-sibling-context=NUM  -S NUM  Create context" +
                " information output, \n\twith NUM parent sibling context" +
        " (default 1).\n");
        System.out.print("\nThis product includes software developed by the " +
                "Indiana University Extreme! Lab " +
        "(http://www.extreme.indiana.edu/).\n\n");

        System.exit(0);
    }

    /**
     * Outputs the current version of diffxml to standard out.
     */
    public static void printVersionAndExit() {
        System.out.println("diffxml Version " + VERSION + "\n");
        System.out.print("\nThis product includes software developed by the" +
                " Indiana University Extreme! Lab " +
        "(http://www.extreme.indiana.edu/).\n");
        System.exit(0);
    }

    /**
     * Attempts to initialise logging.
     *
     * Output is sent to file diffxml.log.
     * @throws IOException If the logfile can't be created
     */
    public static void initLog() throws IOException {
        FileHandler logFile = new FileHandler("diffxml.log");
        logFile.setFormatter(new java.util.logging.SimpleFormatter());

        // Send log output to our FileHandler.
        LOG.addHandler(logFile);

        // Request detail level
        LOG.setLevel(Level.ALL);

        // We only want messages sent to our file, nowhere else
        LOG.setUseParentHandlers(false);

    }

    /**
     * Main method. Takes command line arguments, parses them and performs diff.
     *
     * @param args Command line arguments. See printUsageAndExit() for details.
     */
    public static void main(final String[] args) {

        //Start logging
        try {
            initLog();
        } catch (IOException ex) {
            System.err.println("Unable to instantiate logger " + ex);
        }

        //Set options - instantiates _file1 and _file2
        parseArgs(args);

        //Check files
        if (!mFile1.exists()) {
            System.err.println("Could not find file: "
                    + mFile1.getAbsolutePath());
            System.exit(2);
        }
        if (!mFile2.exists()) {
            System.err.println("Could not find file: "
                    + mFile2.getAbsolutePath());
            System.exit(2);
        }
        
        Diff diffInstance = DiffFactory.createDiff();
        
        Document delta = null;
        try {
            delta = diffInstance.diff(mFile1, mFile2);
        } catch (DiffException e) {
            System.err.println("Internal error when differencing documents.");
            System.exit(2);
        }
        //Output XML if appropriate

        //Documents differ if there are any child nodes in the doc.
        boolean differ = delta.getDocumentElement().hasChildNodes();

        if (DiffFactory.isBrief()) {
            //If in brief mode, don't output delta, only whether files differ
            if (differ) {
                System.out.println("XML documents " + mFile1 + " and "
                        + mFile2 + " differ");
            }
        } else {
            outputXML(delta, System.out);
        }

        if (differ) {
            System.exit(1);
        } else {
            System.exit(0);
        }

    }

    /**
     * Writes given XML document to given stream.
     *
     * Uses UTF8 encoding, no indentation, preserves spaces.
     * Adds XML declaration with standalone set to "yes".
     *
     * @param doc DOM document to output
     * @param os  Stream to output to
     */
    public static void outputXML(final Document doc, final OutputStream os) {
        
        if (doc == null) {
            throw new IllegalArgumentException("Null document");
        }

        try {
            final Transformer transformer = 
                TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc),
                    new StreamResult(os));

        } catch (TransformerConfigurationException e1) {
            System.err.println("Failed to configure serializer " + e1);
        } catch (TransformerException e) {
            System.err.println("Failed to serialize document " + e);
        }

    }

}
