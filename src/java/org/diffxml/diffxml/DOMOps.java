package org.diffxml.diffxml;

import java.io.IOException;
import java.io.OutputStream;

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
     * Uses UTF8 encoding, no indentation, preserves spaces.
     * Adds XML declaration with standalone set to "yes".
     * 
     * @param doc DOM document to output
     * @param os  Stream to output to
     * @throws IOException If an error occurs with serialization
     */
    public static void outputXML(final Document doc, final OutputStream os) 
    throws IOException {
        
        if (doc == null) {
            throw new IllegalArgumentException("Null document");
        }
    
        try {
            final Transformer transformer = 
                DOMOps.TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
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
