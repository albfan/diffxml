package org.diffxml.diffxml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.diffxml.diffxml.fmes.ParserInitialisationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class for DOM stuff.
 * 
 * @author Adrian Mouat
 *
 */
public final class DOMOps {

    /**
     * Factory used in outputting XML.
     */
    private static final TransformerFactory TRANSFORMER_FACTORY =
        TransformerFactory.newInstance();

    /**
     * Private constructor.
     */
    private DOMOps() {
        //Shouldn't be instantiated
    }
    
    /**
     * Writes given XML document to given stream.
     *
     * Uses UTF8 encoding, preserves spaces.
     * Adds XML declaration with standalone set to "yes".
     * 
     * @param doc DOM document to output
     * @param os  Stream to output to
     * @param indented Whether to indent the output
     * @throws IOException If an error occurs with serialization
     */
    public static void outputXML(final Document doc, final OutputStream os,
            final boolean indented) 
    throws IOException {
        
        if (doc == null) {
            throw new IllegalArgumentException("Null document");
        }
    
        try {
            final Transformer transformer = 
                DOMOps.TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            
            if (indented) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");    
            } else {
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
            }
            
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc),
                    new StreamResult(os));
    
        } catch (TransformerConfigurationException e1) {
            throw new IOException("Failed to configure serializer", e1);
        } catch (TransformerException e) {
            throw new IOException("Failed to serialize document", e);
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
     * @throws IOException If an error occurs with serialization
     */
    public static void outputXML(final Document doc, final OutputStream os) 
    throws IOException {
        outputXML(doc, os, false);
    }

    /**
     * Writes given XML document to given stream.
     *
     * Uses UTF8 encoding, indentation, preserves spaces.
     * Adds XML declaration with standalone set to "yes".
     * 
     * @param doc DOM document to output
     * @param os  Stream to output to
     * @throws IOException If an error occurs with serialization
     */
    public static void outputXMLIndented(final Document doc, 
            final OutputStream os) 
    throws IOException {
        outputXML(doc, os, true);
    }
    
    /**
     * Writes given XML Node to given stream.
     *
     * Uses UTF8 encoding, no indentation, preserves spaces.
     * Omits the XML declaration.
     * 
     * @param node Node to output
     * @param os  Stream to output to
     * @throws IOException If an error occurs with serialization
     */
    public static void outputXML(final Node node, final OutputStream os) 
    throws IOException {
        
        if (node == null) {
            throw new IllegalArgumentException("Null node");
        }
    
        try {
            final Transformer transformer = 
                DOMOps.TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, 
                    "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(node),
                    new StreamResult(os));
    
        } catch (TransformerConfigurationException e1) {
            throw new IOException("Failed to configure serializer", e1);
        } catch (TransformerException e) {
            throw new IOException("Failed to serialize document", e);
        }
    }

    /**
     * Gets the string representation of a Node and its children.
     *
     * Catches possible IOExceptions and rethrows as unchecked.
     * @param n The Node to get the String for
     * @return The String representation of the Node
     */
    public static String getNodeAsStringDeep(final Node n) {

        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
        try {
            DOMOps.outputXML(n, os);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "An error occured when serializing the node", e);
        }
        return os.toString();
    }

    /**
     * Gets the string representation of a Node but not its children.
     *
     * Catches possible IOExceptions and rethrows as unchecked.
     * @param n The Node to get the String for
     * @return The String representation of the Node
     */
    public static String getNodeAsString(final Node n) {

        Node out = n.cloneNode(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
        try {
            DOMOps.outputXML(out, os);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "An error occured when serializing the node", e);
        }
        return os.toString();
    }
    
    /**
     * Returns the NodeList as an array of Nodes.
     *  
     * @param nodeList The NodeList to convert
     * @return A Node[] representing the NodeList.
     */
    public static Node[] getElementsOfNodeList(final NodeList nodeList) {
        
        Node[] ret = null;
        if (nodeList != null) {
            ret = new Node[nodeList.getLength()];
            for (int i = 0; i < nodeList.getLength(); i++) {
                ret[i] = nodeList.item(i);
            }
        }
        
        return ret;
    }

    /**
     * Inserts a given node as numbered child of a parent node.
     *
     * If childNum doesn't exist the node is simply appended.
     *
     * @param childNum  the position to add the node to
     * @param parent    the node that is to be the parent
     * @param insNode   the node to be inserted
     * @return The inserted Node
     */
    public static Node insertAsChild(final int childNum, final Node parent,
            final Node insNode) {
    
        Node ret;
        NodeList kids = parent.getChildNodes();
        //Needed to first remove child to ensure DOM position is correct
        if (insNode.getParentNode() != null) {
            insNode.getParentNode().removeChild(insNode);
        }
    
        if (kids.item(childNum) != null) {
            ret = parent.insertBefore(insNode, kids.item(childNum));
        } else {
            ret = parent.appendChild(insNode);
        }
        
        return ret;
    }

    /**
     * Sets various features on the DOM Parser.
     *  
     * @param parserFactory
     *            The parser to be set up
     * @throws ParserInitialisationException
     *             If some feature can't be set
     */
    public static void initParser(final DocumentBuilderFactory parserFactory) 
    throws ParserInitialisationException {
    
        if (!DiffFactory.isResolveEntities()) {
            parserFactory.setExpandEntityReferences(false);
        }
    
        //Turn off DTD stuff - if DTD support changes reconsider
        parserFactory.setValidating(false);
        parserFactory.setNamespaceAware(true);
        
        parserFactory.setIgnoringComments(false);
    }

 

}
