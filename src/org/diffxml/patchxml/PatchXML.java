/*
Program to apply a DUL patch

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
package org.diffxml.patchxml;

import org.diffxml.diffxml.DiffXML;
import org.diffxml.diffxml.fmes.NodeOps;
import org.diffxml.diffxml.fmes.Fmes;

import org.apache.xpath.XPathAPI;
import org.apache.xerces.parsers.DOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.NamedNodeMap;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import java.io.File;

/**
 * Applies a DUL patch to an XML document.
 */

public class PatchXML
{
    /** 
     * If true, extra debug data is output.
     */
    private static final boolean OUTPUT_DEBUG = false;

    /**
     * If true, attempt to reverse the sense of the patch.
     */
    private static boolean reverse = false;

    /**
     * Determines whether original file overwritten.
     *
     * Currently breaks from unix patch, file not implicitly written to.
     * To change this set the boolean dryrun to false
     * You will then need to use -dry-run to avoid overwriting files
     */
    private static boolean dryrun = true;

    /** Holds the name of the document to be patched. **/
    private static String _docFile;

    /** Holds the name of the DUL patch file. **/
    private static String _patchFile;

    /**
     * Parse command line arguments.
     *
     * Sets up file variables and options.
     *
     * @param args array of command line arguments
     */

    public static void parseArgs(final String[] args)
        {
        int i = 0;
        char flag;
        String arg;

        while (i < args.length && args[i].startsWith("-"))
            {
            arg = args[i++];

            //Normalize multiple dashes
            //Don't understand point in differentiating between 1 and 2 dashes
            //We allow 2 in order to mimic patch util

            if (arg.startsWith("--"))
                arg = arg.substring(1);

            //"wordy" arguments
            if (arg.equals("-version"))
                printVersion();
            else if (arg.equals("-help"))
                printHelp();
            else if (arg.equals("-dry-run"))
                dryrun = true;
            else if (arg.equals("-reverse"))
                reverse = true;

            //(series of) flag arguments
            else
                {
                for (int j = 1; j < arg.length(); j++)
                    {
                    flag = arg.charAt(j);
                    switch (flag)
                        {
                        case 'V':
                            printVersion();
                            break;
                        case 'h':
                            printHelp();
                        case 'd':
                            dryrun = true;
                        case 'R':
                            reverse = true;

                        default:
                            System.err.println("PatchXML: illegal option "
                                    + flag);
                            System.exit(2);
                            break;
                        }
                    }
                }
            }
        if ((i + 2) != args.length)
            printUsage();

        _docFile = args[i];
        _patchFile = args[++i];
        }

    /**
     * Output usage and exit.
     */

    private static void printUsage()
        {
        System.err.println("Usage: patch [OPTION]... [ORIGFILE [PATCHFILE]]");
        System.exit(2);
        }

    /**
     * Output help and exit.
     */

    private static void printHelp()
        {
        System.out.print("\nUsage: patch [OPTION]... [ORIGFILE [PATCHFILE]]\n");
        System.out.print(
                "\nApply a diffxml file to one of the original XML files.\n");
        System.out.print(
                "\n --version  -V  Output version number of program.");
        System.out.print(
                "\n --help     -h  Print summary of options and exit.");
        System.out.print(
                "\n --dry-run  -d  Print results of applying the changes ");
        System.out.print("without modifying any files.");
        System.out.print(
                "\n --reverse  -R  Assume that the delta file was created ");
        System.out.print("with the old and new files swapped.");
        System.out.print("\n\tAttempt to reverse sense of change before ");
        System.out.print("applying it, e.g. inserts become deletes.\n\n");


        printSoftware();
        System.exit(0);
        }

    /**
     * Output details of other software used in diffxml and patchxml.
     */

    private static void printSoftware()
        {
        System.out.print(
                "\nThis product includes software developed by the ");
        System.out.print("Indiana University Extreme! Lab ");
        System.out.print("(http://www.extreme.indiana.edu/).\n");
        System.out.print(
                "\nThis product includes software developed by the ");
        System.out.print(
                "Apache Software Foundation (http://www.apache.org/).\n\n");
        }

    /**
     * Output version and exit.
     */

    private static void printVersion()
        {
        System.out.println("patchxml Version 0.93 ALPHA");
        printSoftware();
        System.exit(0);
        }

    /**
     * Perform update operation.
     *
     * TODO: Write!
     */

    private void doUpdate()
        {
        //Currently not working
        return;
        /*
           if (opName.equals("update"))
           {
           Node node;
           try {
           node=XPathAPI.selectSingleNode
           (doc.getDocumentElement(),
           opAttrs.getNamedItem("node").getNodeValue());

        //Currently all update does is change the name
        //This actually breaks the DUL but is a quick hack to get things working

        Element new_node=doc.createElement(
            opAttrs.getNamedItem("name").getNodeValue());

        }
        catch (TransformerException e)
        { System.err.println("Could not resolve XPath for parent for update");}
        }
        */
        }

    /**
     * Get the parent node pointed to by the parent attributed.
     *
     * @param doc   document being patched
     * @param attrs attributes of operation node
     * @return the parent node
     */

    private Node getParentFromAttr(final Document doc, final NamedNodeMap attrs)
        {
        Node parent = null;
        try
            {
            parent = XPathAPI.selectSingleNode(
                    doc.getDocumentElement(),
                    attrs.getNamedItem("parent").getNodeValue());
            }
        catch (TransformerException e)
            {
            System.err.println("Could not resolve XPath for parent for insert");
            }
        return parent;
        }

    /**
     * Get value of nodetype attribute.
     *
     * @param attrs attributes of operation node
     * @return the value of nodetype
     */

    private int getNodeTypeFromAttr(final NamedNodeMap attrs)
        {
        //TODO: Deal with cases when no node type attr
        return new Integer(attrs.getNamedItem("nodetype").getNodeValue()
                ).intValue();
        }

    /**
     * Get the DOM Child Number equivalent of the XPath childnumber.
     *
     * @param siblings the NodeList we are interested in
     * @param xpathcn  the XPath child number
     * @return the equivalent DOM child number
     */

    private int getDOMChildNoFromXPath(final NodeList siblings,
            final int xpathcn)
        {
        //Doesn't cope with node names instead of numbers
        int index = 0;
        int j;
        for (j = 0; j < siblings.getLength(); j++)
            {
            index++;
            if (j > 0 && siblings.item(j).getNodeType() == Node.TEXT_NODE
                    && siblings.item(j - 1).getNodeType() == Node.TEXT_NODE)
                index--;
            if (index == xpathcn)
                break;
            }
        return j;
        }

    /**
     * Get the value associated with the operation node.
     *
     * @param op the operation node
     * @return the string value of the node
     */

    private String getOpValue(final Node op)
        {
        NodeList opKids = op.getChildNodes();

        String value = "";
        if (opKids.getLength() > 1)
            System.err.println("Unexpected children in insert operation");

        else if ((opKids.getLength() == 1)
                && (opKids.item(0).getNodeType() == Node.TEXT_NODE))
            value = opKids.item(0).getNodeValue();

        return value;
        }

    /**
     * Get value of old_charpos attribute.
     *
     * Defaults to 1.
     *
     * @param opAttrs attributes of operation node
     * @return the value of new_charpos
     */

    private int getOldCharPos(final NamedNodeMap opAttrs)
        {
        int oldCharPos = 1;

        Node a = opAttrs.getNamedItem("old_charpos");
        if (a != null)
            oldCharPos = new Integer(a.getNodeValue()).intValue();

        return oldCharPos;
        }

    /**
     * Get value of new_charpos attribute.
     *
     * Defaults to 1.
     *
     * @param opAttrs attributes of operation node
     * @return the value of new_charpos
     */

    private int getNewCharPos(final NamedNodeMap opAttrs)
        {
        int newCharPos = 1;

        Node a = opAttrs.getNamedItem("new_charpos");
        if (a != null)
            newCharPos = new Integer(a.getNodeValue()).intValue();

        return newCharPos;
        }

    /**
     * Get value of charpos attribute.
     *
     * Defaults to 1.
     *
     * @param opAttrs attributes of operation node
     * @return the value of charpos
     */

    private int getCharPos(final NamedNodeMap opAttrs)
        {
        int charpos = 1;

        Node a = opAttrs.getNamedItem("charpos");
        if (a != null)
            charpos = new Integer(a.getNodeValue()).intValue();

        return charpos;
        }

    /**
     * Tests if previous node is a text node.
     *
     * @param siblings siblings of current node
     * @param index    index of current node
     * @return true if previous node is a text node, false otherwise
     */

    private boolean prevNodeIsATextNode(final NodeList siblings,
            final int index)
        {
        return (index > 0
                && siblings.item(index - 1).getNodeType() == Node.TEXT_NODE);
        }

    /**
     * Inserts a node at the given character position.
     *
     * @param charpos  the character position to insert at
     * @param siblings the NodeList to insert the node into
     * @param domcn    the child number to insert the node as
     * @param ins      the node to insert
     * @param parent   the node to become the parent of the inserted node
     */

    private void insertAtCharPos(final int charpos, final NodeList siblings,
            final int domcn, final Node ins, final Node parent)
        {
        //TODO: Allow inserting into middle of text node
        //TODO: Handle errors better
        //TODO: What about appending text nodes

        //we know text node at domcn -1
        int cp = charpos;
        int textNodeIndex = domcn - 1;
        boolean append = false;

        while (prevNodeIsATextNode(siblings, textNodeIndex))
            textNodeIndex--;

        while (siblings.item(textNodeIndex).getNodeType() == Node.TEXT_NODE
                && cp > siblings.item(textNodeIndex).getNodeValue().length())
            {
            cp = cp - siblings.item(textNodeIndex).getNodeValue().length();
            textNodeIndex++;

            if (textNodeIndex == siblings.getLength())
                {
                append = true;
                parent.appendChild(ins);
                break;
                }
            }
        if (!append)
            {
            parent.insertBefore(ins, siblings.item(textNodeIndex));
            }

        }

    /**
     * Insert a node under parent node at given position.
     *
     * @param siblings the NodeList to insert the node into
     * @param parent   the parent to insert the node under
     * @param domcn    the child number to insert the node as
     * @param charpos  the character position at which to insert the node
     * @param ins      the node to be inserted
     */

    private void insertNode(final NodeList siblings, final Element parent,
            final int domcn, final int charpos, final Node ins)
        {
        //Note siblings(domcn) is node currently at the position we want
        //to put the node, not the node itself.

        if (domcn <= siblings.getLength())
            {
            //Check if inserting into text
            if (domcn >= 1
                    && siblings.item(domcn - 1).getNodeType() == Node.TEXT_NODE)
                {
                insertAtCharPos(charpos, siblings, domcn, ins, parent);
                }
            else
                {
                if (domcn == siblings.getLength())
                    parent.appendChild(ins);
                else
                    parent.insertBefore(ins, siblings.item(domcn));
                }
            }
        else
            parent.appendChild(ins);
        }

    /**
     * Get the DOM Child number of a node using "childno" attribute.
     *
     * @param opAttrs  the attributes of the operation
     * @param nodeType the nodeType to be inserted
     * @param siblings the siblings of the node
     * @return the DOM Child number of the node
     */

    private int getDOMChildNo(final NamedNodeMap opAttrs,
            final int nodeType, final NodeList siblings)
        {
        //Note init to zero
        int xpathcn = 0;
        int domcn = 0;

        if (opAttrs.getNamedItem("childno") != null)
            xpathcn = new Integer(opAttrs.getNamedItem("childno").getNodeValue()
                    ).intValue();

        //Convert xpath childno to DOM childno
        if (nodeType != Node.ATTRIBUTE_NODE)
            domcn = getDOMChildNoFromXPath(siblings, xpathcn);

        return domcn;
        }

    /**
     * Apply insert operation to document.
     *
     * @param doc the document to be patched
     * @param op  the insert operation node
     */

    private void doInsert(final Document doc, final Node op)
        {
        DiffXML.log.fine("Applying insert");
        Node ins;

        //Get various variables need for insert
        NamedNodeMap opAttrs = op.getAttributes();
        Node parent = getNamedParent(doc, opAttrs);
        int charpos = getCharPos(opAttrs);

        //TODO: handle null better
        if (parent == null)
            return;

        NodeList siblings = parent.getChildNodes();
        int nodeType = getNodeTypeFromAttr(opAttrs);

        int domcn = getDOMChildNo(opAttrs, nodeType, siblings);

        switch (nodeType)
            {
            case Node.TEXT_NODE:

                ins = doc.createTextNode(getOpValue(op));
                insertNode(siblings, (Element) parent, domcn, charpos, ins);
                break;

            case Node.ELEMENT_NODE:

                ins = doc.createElement(
                        opAttrs.getNamedItem("name").getNodeValue());
                insertNode(siblings, (Element) parent, domcn, charpos, ins);
                break;

            case Node.COMMENT_NODE:

                ins = doc.createComment(getOpValue(op));
                insertNode(siblings, (Element) parent, domcn, charpos, ins);
                break;

            case Node.ATTRIBUTE_NODE:

                String name = opAttrs.getNamedItem("name").getNodeValue();
                ((Element) parent).setAttribute(name, getOpValue(op));
                break;

            default:
                //TODO: consider throwing exception or exiting here
                System.err.println("Unknown NodeType " + nodeType);
                return;
            }
        }

    /**
     * Find the correct text node to delete.
     *
     * @param delNode the first text node pointed to
     * @param charpos the character position at which to delete
     * @return the text node which should be deleted
     */

    private Node getDelTextNode(final Node delNode, final int charpos)
        {
        int cp = charpos;
        //TODO: Allow deleting part of nodes, not just whole nodes
        NodeList siblings = delNode.getParentNode().getChildNodes();
        int i = 0;
        while (!NodeOps.checkIfSameNode(delNode, siblings.item(i)))
            i++;

        //TODO: Check conditional, consider charpos > 0
        while (cp > siblings.item(i).getNodeValue().length())
            {
            cp = cp - siblings.item(i).getNodeValue().length();
            i++;
            }
        return siblings.item(i);
        }

    /**
     * Log various attributes of delete operation.
     *
     * @param delNode the node to be deleted
     * @param opAttrs the attributes of the operation node
     */

    private void logDeleteVariables(final Node delNode,
            final NamedNodeMap opAttrs)
        {
        DiffXML.log.finer("Deleting node "
                + delNode.getNodeName() + " " + delNode.getNodeValue());
        if (opAttrs.getNamedItem("length") != null)
            {
            DiffXML.log.finer("Supposed length "
                    + opAttrs.getNamedItem("length").getNodeValue());
            DiffXML.log.finer("Actual length "
                    + delNode.getNodeValue().length());
            }
        }

    /**
     * Gets the node pointed to by the "parent" attribute.
     *
     * @param doc     document being patched
     * @param opAttrs attributes of operation node
     * @return        node pointed to by "parent" attribute
     */

    private Node getNamedParent(final Document doc, final NamedNodeMap opAttrs)
        {
        String xPath = opAttrs.getNamedItem("parent").getNodeValue();
        return getNodeFromXPath(doc, xPath);
        }

    /**
     * Returns the node pointed to by a given xPath.
     *
     * @param doc   document being patched
     * @param xPath xPath to the node
     * @return      the node pointed to by the xPath
     */

    private Node getNodeFromXPath(final Document doc, final String xPath)
        {
        Node n = null;
        try
            {
            //According to API returns *first* match,
            //so should be first text node if text node matched
            n = XPathAPI.selectSingleNode(doc.getDocumentElement(), xPath);
            }
        catch (TransformerException e)
            {
            //Consider more fault tolerant behaviour
            System.err.println("Could not resolve XPath for node");
            System.exit(1);
            }
        return n;
        }

    /**
     * Gets the node pointed to by the "node" attribute.
     *
     * @param doc     document being patched
     * @param opAttrs attributes of operation node
     * @return        node pointed to by "node" attribute
     */

    private Node getNamedNode(final Document doc, final NamedNodeMap opAttrs)
        {
        String xPath = opAttrs.getNamedItem("node").getNodeValue();
        return getNodeFromXPath(doc, xPath);
        }

    /**
     * Apply delete operation.
     *
     * @param doc document to be patched
     * @param op  node holding details of delete
     */

    private void doDelete(final Document doc, final Node op)
        {
        //TODO: test behaviour with text nodes
        NamedNodeMap opAttrs = op.getAttributes();
        DiffXML.log.fine("Applying delete");

        Node delNode = getNamedNode(doc, opAttrs);

        int charpos = getCharPos(opAttrs);

        //Text node may actually be different node
        if (delNode.getNodeType() == Node.TEXT_NODE)
            delNode = getDelTextNode(delNode, charpos);

        logDeleteVariables(delNode, opAttrs);

        delNode.getParentNode().removeChild(delNode);
        }

    /**
     * Log various attributes of move.
     *
     * @param opAttrs attributes of move
     */

    private void logMoveVars(final NamedNodeMap opAttrs)
        {
        DiffXML.log.finer("Node: "
                + opAttrs.getNamedItem("node").getNodeValue()
                + " Parent" + opAttrs.getNamedItem("parent").getNodeValue());
        DiffXML.log.finer("childno"
                + opAttrs.getNamedItem("childno").getNodeValue());

        if (opAttrs.getNamedItem("length") != null)
            DiffXML.log.finer("length "
                    + opAttrs.getNamedItem("length").getNodeValue());
        }

    /**
     * Apply move operation.
     *
     * @param doc document to be patched
     * @param op  node holding details of move
     */

    private void doMove(final Document doc, final Node op)
        {
        //TODO: Thorough testing - pretty sure not currently working properly
        DiffXML.log.fine("Applying move");

        NamedNodeMap opAttrs = op.getAttributes();
        logMoveVars(opAttrs);

        Node moveNode = getNamedNode(doc, opAttrs);

        int oldCharPos = getOldCharPos(opAttrs);

        //Currently only consider deleting nodes, not part of nodes
        if (moveNode.getNodeType() == Node.TEXT_NODE)
            {
            moveNode = getDelTextNode(moveNode, oldCharPos);
            }

        //Find position to move to
        //Get parent
        Node parent = getNamedParent(doc, opAttrs);

        NodeList newSiblings = parent.getChildNodes();
        int domcn = getDOMChildNo(opAttrs, moveNode.getNodeType(), newSiblings);

        //Get new charpos
        int newCharPos = getNewCharPos(opAttrs);

        //Perform insert
        //TODO: Check old node removed and children properly dealt with
        insertNode(newSiblings, (Element) parent, domcn, newCharPos, moveNode);
        }

    /**
     * Apply DUL patch to XML document.
     *
     * @param doc   the XML document to be patched
     * @param patch the DUL patch
     */

    public final void apply(final Document doc, final Document patch)
        {
        NodeIterator ni = ((DocumentTraversal) patch).createNodeIterator(
                patch.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
                null, false);

        Node op = ni.nextNode();
        //Check we have a delta
        if (!op.getNodeName().equals("delta"))
            {
            System.err.println("Not a delta document!");
            return;
            }

        //Cycle through elements applying ops
        op = ni.nextNode();

        while (op != null)
            {
            String opName = op.getNodeName();
            NamedNodeMap opAttrs = op.getAttributes();

            if (opName.equals("update"))
                {
                doUpdate();
                }
            else if (opName.equals("insert"))
                {
                doInsert(doc, op);
                }
            else if (opName.equals("delete"))
                {
                doDelete(doc, op);
                }
            else if (opName.equals("move"))
                {
                doMove(doc, op);
                }
            else
                {
                System.err.println("Do not recognise element: " + opName);
                System.exit(2);
                }
            if (OUTPUT_DEBUG)
                {
                try {
                    Transformer serializer = TransformerFactory.newInstance(
                            ).newTransformer();
                    serializer.transform(new DOMSource(doc),
                            new StreamResult(System.out));
                    System.out.println();
                    }
                catch (javax.xml.transform.TransformerException e)
                    {
                    System.err.println("Failed to do debug output");
                    System.exit(1);
                    }
                }
            op = ni.nextNode();
            }
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

        File test = new File(_docFile);
        if (!test.exists())
            {
            System.err.println("Could not find file: " + _docFile);
            return false;
            }
        test = new File(_patchFile);
        if (!test.exists())
            {
            System.err.println("Could not find file: " + _patchFile);
            return false;
            }
        return true;
        }

    /**
     * Output the patched document to stdout.
     *
     * Also outputs patch document if in debug.
     *
     * @param doc the patched document
     * @param patch the patch document
     */

    private static void outputDoc(final Document doc, final Document patch)
        {
        //Patch only needed for debug - remove later
        try
            {
        Transformer serializer = TransformerFactory.newInstance(
                ).newTransformer();
        if (dryrun)
            serializer.transform(new DOMSource(doc),
                    new StreamResult(System.out));
        else
            {
            File f1 = new File(_docFile);
            serializer.transform(new DOMSource(doc), new StreamResult(f1));
            }
        DiffXML.log.finer("PatchXML Doc");
        if (OUTPUT_DEBUG)
            serializer.transform(new DOMSource(patch),
                    new StreamResult(System.out));
        System.out.println();
            }
        catch (javax.xml.transform.TransformerException te)
            {
            System.err.println("Failed to output new document");
            System.exit(2);
            }
        }

    /**
     * Attempt to patch given document with given patch file.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args)
        {

        //Set options - instantiates _docFile and _patchFile
        parseArgs(args);

        //Check files exist
        if (!filesExist())
            System.exit(2);

        DOMParser parser = new DOMParser();
        Fmes.initParser(parser);

        try
            {
            parser.parse(_docFile);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e);
            System.exit(2);
            }

        Document doc = parser.getDocument();

        try
            {
            parser.parse(_patchFile);
            }
        catch (Exception e)
            {
            System.err.println("Failed to parse document: " + e);
            System.exit(2);
            }

        Document patch = parser.getDocument();

        doc.normalize();
        patch.normalize();

        if (reverse)
            patch = Reverse.go(patch);

        PatchXML patcher = new PatchXML();
        patcher.apply(doc, patch);

        outputDoc(doc, patch);
        }
}
