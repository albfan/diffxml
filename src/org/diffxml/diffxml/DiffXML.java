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
                DiffFactory.BRIEF = true;
            else if (arg.equals("-ignore-all-whitespace"))
                {
                DiffFactory.IGNORE_ALL_WHITESPACE = true;
                DiffFactory.IGNORE_WHITESPACE_NODES = true;
                }
            else if (arg.equals("-ignore-leading-whitespace"))
                {
                DiffFactory.IGNORE_LEADING_WHITESPACE = true;
                DiffFactory.IGNORE_WHITESPACE_NODES = true;
                }
            else if (arg.equals("-ignore-empty-nodes"))
                DiffFactory.IGNORE_WHITESPACE_NODES = true;
            else if (arg.equals("-ignore-case"))
                DiffFactory.IGNORE_CASE = true;
            else if (arg.equals("-ignore-comments"))
                DiffFactory.IGNORE_COMMENTS = true;
            else if (arg.equals("-ignore-processing-instructions"))
                DiffFactory.IGNORE_PROCESSING_INSTRUCTIONS = true;
            else if (arg.equals("-version"))
                printVersion();
            else if (arg.equals("-help") || arg.equals("--help"))
                printHelp();
            else if (arg.equals("-fmes"))
                DiffFactory.FMES = true;
            else if (arg.equals("-xmdiff"))
                DiffFactory.FMES = false;
            else if (arg.equals("-tagnames"))
                DiffFactory.TAGNAMES = true;
            else if (arg.equals("-reverse-patch"))
                DiffFactory.REVERSE_PATCH = true;
            else if (arg.equals("-sibling-context"))
                DiffFactory.CONTEXT = true;
            else if (arg.equals("-parent-context"))
                DiffFactory.CONTEXT = true;
            else if (arg.equals("-parent-sibling-context"))
                {
                DiffFactory.CONTEXT = true;
                //Defaults to 0 if not specified at all, 1 if speced
                DiffFactory.PARENT_SIBLING_CONTEXT = 1;
                }
            else if (arg.equals("-xupdate"))
                DiffFactory.DUL = false;
            else if (arg.equals("-dul"))
                DiffFactory.DUL = true;
            else if (arg.equals("-remove-entities"))
                {
                DiffFactory.ENTITIES = false;
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

                DiffFactory.CONTEXT = true;
                DiffFactory.SIBLING_CONTEXT = num;
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

                DiffFactory.CONTEXT = true;
                DiffFactory.PARENT_CONTEXT = num;
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

                DiffFactory.CONTEXT = true;
                DiffFactory.PARENT_SIBLING_CONTEXT = num;
                }

            //Short arguments with arguments
            else if (arg.equals("-C"))
                {
                DiffFactory.CONTEXT = true;
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
                    DiffFactory.SIBLING_CONTEXT = num;
                    }
                else
                    {
                    System.err.println("-C needs positive integer argument");
                    System.exit(2);
                    }
                }
            else if (arg.equals("-P"))
                {
                DiffFactory.CONTEXT = true;
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
                    DiffFactory.PARENT_CONTEXT = num;
                    }
                else
                    {
                    System.err.println("-P needs positive integer argument");
                    System.exit(2);
                    }
                }
            else if (arg.equals("-S"))
                {
                DiffFactory.CONTEXT = true;
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
                    DiffFactory.PARENT_SIBLING_CONTEXT = num;
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
                            DiffFactory.BRIEF = true;
                            break;
                        case 's':
                            DiffFactory.IGNORE_ALL_WHITESPACE = true;
                            break;
                        case 'w':
                            DiffFactory.IGNORE_LEADING_WHITESPACE = true;
                            break;
                        case 'e':
                            DiffFactory.IGNORE_WHITESPACE_NODES = true;
                            break;
                        case 'i':
                            DiffFactory.IGNORE_CASE = true;
                            break;
                        case 'r':
                            DiffFactory.IGNORE_COMMENTS = true;
                            break;
                        case 'I':
                            DiffFactory.IGNORE_PROCESSING_INSTRUCTIONS = true;
                            break;
                        case 'V':
                            printVersion();
                            break;
                        case 'c':
                            DiffFactory.CONTEXT = true;
                            break;
                        case 'h':
                            printHelp();
                            break;
                        case 'f':
                            DiffFactory.FMES = true;
                            break;
                        case 'x':
                            DiffFactory.FMES = false;
                            break;
                        case 't':
                            DiffFactory.TAGNAMES = true;
                            break;
                        case 'X':
                            DiffFactory.DUL = false;
                            break;
                        case 'D':
                            DiffFactory.DUL = true;
                            break;
                        case 'p':
                            DiffFactory.REVERSE_PATCH = true;
                            break;
                        case 'n':
                            DiffFactory.ENTITIES = false;
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

        //Do diff and exit
        Diff d = DiffFactory.createDiff();
        if (d.diff(_file1, _file2))
            System.exit(1);
        else
            System.exit(0);

        }

}
