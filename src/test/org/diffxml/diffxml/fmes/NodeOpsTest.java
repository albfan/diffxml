package org.diffxml.diffxml.fmes;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import org.diffxml.diffxml.TestDocHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to test helper functions in NodeOps.
 * 
 * @author Adrian Mouat
 *
 */
public class NodeOpsTest {

    /**
     * Test getting the unique XPath for nodes.
     */
    @Test
    public final void testGetXPath() {
        //Create an XML doc, loop through nodes, confirming that doing a
        //getXPath then a select returns the node
        XPathFactory xPathFac = XPathFactory.newInstance();
        XPath xpath = xPathFac.newXPath();
        
        Document testDoc = TestDocHelper.createDocument(
                "<a>aa<b attr='test'>b<!-- comment -->c<c/></b>d</a>");
        
        Node b = testDoc.getDocumentElement().getFirstChild().getNextSibling();
        
        //Old test to ensure comment nodes are processed
        assertEquals(b.getFirstChild().getNextSibling().getNodeType(),
                Node.COMMENT_NODE);
        assertEquals(b.getChildNodes().item(1).getNodeType(), 
                Node.COMMENT_NODE); 
        
        testXPathForNode(testDoc.getDocumentElement(), xpath);
    }
    
    /**
     * Helper method for testGetXPath.
     * 
     * Gets the XPath for the node and evaluates it, checking if the same node
     * is returned. 
     * 
     * @param n The node to test
     * @param xp XPath expression (reused for efficiency only)
     */
    private void testXPathForNode(final Node n, final XPath xp) {
        
        String xpath = NodeOps.getXPath(n);
        
        try {
            Node ret = (Node) xp.evaluate(xpath, n.getOwnerDocument(), 
                    XPathConstants.NODE);
            assertNotNull(ret);

            if (n.getNodeType() == Node.TEXT_NODE) {
                assertTrue(ret.getTextContent() 
                        + " does not contain " + n.getTextContent(), 
                        ret.getTextContent().contains(n.getTextContent()));
            } else {
                assertTrue(
                        ret.getNodeName() + ":" + ret.getNodeValue() 
                        + " is not " + n.getNodeName() + ":" + n.getNodeValue(),
                        n.isSameNode((Node) ret));
            }
        } catch (XPathExpressionException e) {
            fail("Caught exception: " + e.getMessage());
        }
        
        if (!(n.getNodeType() == Node.ATTRIBUTE_NODE)) {
            NodeList list = n.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                testXPathForNode(list.item(i), xp);
            }
        }
    }
    
    /**
     * Test for the horrible coalesced text nodes issue.
     * 
     * Unfortunately, this test fails as the XPath context does not contain the
     * c node. Sigh.
     */
    @Test
    @Ignore
    public final void testGetXPathWithTextNodes() {
        
        Document testDoc = TestDocHelper.createDocument("<a>b</a>");
        Element root = testDoc.getDocumentElement();
        Node c = testDoc.createTextNode("c");
        root.appendChild(c);
        
        //Move to beforeclass method
        XPathFactory xPathFac = XPathFactory.newInstance();
        XPath xpath = xPathFac.newXPath();
 
        testXPathForNode(c, xpath);       
    }
    
    /**
     * Test getting XPath for attributes.
     */
    @Test
    public final void testGetXPathForAttributes() {
        
        Document testDoc = TestDocHelper.createDocument(
                "<a><b attr=\"test\"/></a>");
        Element root = testDoc.getDocumentElement();
        NamedNodeMap attrs = root.getFirstChild().getAttributes();
        
        //Move to beforeclass method
        XPathFactory xPathFac = XPathFactory.newInstance();
        XPath xpathExpr = xPathFac.newXPath();
 
        testXPathForNode(attrs.item(0), xpathExpr);
    }   
 
    /**
     * Test getElementsOfNodeList.
     */
    @Test
    public final void testGetElementsOfNodeList() {
        
        Document testDoc = TestDocHelper.createDocument(
                "<a><b/><c>1</c>23<!--comm--><d attr=\"1\"/></a>");
        Element root = testDoc.getDocumentElement();
        NodeList nodeList = root.getChildNodes();
        Node[] nodeArray = NodeOps.getElementsOfNodeList(nodeList);
        
        assertEquals(nodeList.getLength(), nodeArray.length);
        for (int i = 0; i < nodeArray.length; i++) {
            assertEquals(nodeArray[i], nodeList.item(i));
        }
        
        assertNull(NodeOps.getElementsOfNodeList(null));
        assertEquals(NodeOps.getElementsOfNodeList(root.getFirstChild(
                ).getChildNodes()).length, 0);
        
    }
}
