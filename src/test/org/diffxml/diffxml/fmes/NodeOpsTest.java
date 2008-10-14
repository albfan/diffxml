package org.diffxml.diffxml.fmes;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import org.diffxml.diffxml.TestDocHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    public void testGetXPath() {
        //Create an XML doc, loop through nodes, confirming that doing a
        //getXPath then a select returns the node
        XPathFactory xPathFac = XPathFactory.newInstance();
        XPath xpath = xPathFac.newXPath();
        
        Document testDoc = TestDocHelper.createDocument(
                "<a>aa<b attr='test'>b<!-- comment -->c<c/></b>d</a>");
        
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
        //System.out.println(xpath);
        try {
            Object ret = xp.evaluate(xpath, n.getOwnerDocument(), 
                    XPathConstants.NODE);
            Node nRet = (Node) ret;
            assertNotNull(nRet);
            assertTrue(
                    nRet.getNodeName() + ":" + nRet.getNodeValue() + " is not "
                    + n.getNodeName() + ":" + n.getNodeValue(), 
                    n.isSameNode((Node) ret));
        } catch (XPathExpressionException e) {
            fail("Caught exception: " + e.getMessage());
        }
        
        NodeList list = n.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            testXPathForNode(list.item(i), xp);
        }
    }
    
    @Test
    public void testGetXPathWithTextNodes() {
        
        Document testDoc = TestDocHelper.createDocument("<a>b</a>");
        Element root = testDoc.getDocumentElement();
        Node c = testDoc.createTextNode("c");
        root.appendChild(c);
        
        //Move to beforeclass method
        XPathFactory xPathFac = XPathFactory.newInstance();
        XPath xpath = xPathFac.newXPath();
 
        testXPathForNode(c, xpath);       
    }
    
    
}
