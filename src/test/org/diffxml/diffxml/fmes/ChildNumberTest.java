package org.diffxml.diffxml.fmes;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ChildNumberTest {

    private Document testDoc;
    private Element parent;
    private DocumentBuilderFactory mFac;
    private XPathFactory mXPathFac;
    
    @Before
    public void setUp() throws Exception {

        mFac = DocumentBuilderFactory.newInstance();
        mXPathFac = XPathFactory.newInstance();
        testDoc = mFac.newDocumentBuilder().newDocument();
        parent = testDoc.createElement("parent");
        testDoc.appendChild(parent);
        
    }
    
    @Test
    public void testSimpleChildNo() {
        //InsertPosition and NodePos are both calculating XPath in different ways :(
        Element a = testDoc.createElement("a");
        Element b = testDoc.createElement("b");
        Element c = testDoc.createElement("c");
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        ChildNumber aChildNo = new ChildNumber(a);
        ChildNumber bChildNo = new ChildNumber(b);
        ChildNumber cChildNo = new ChildNumber(c);
        
        assertEquals(0, aChildNo.getDOM());
        assertEquals(1, bChildNo.getDOM());
        assertEquals(2, cChildNo.getDOM());
        
        XPath xpath = mXPathFac.newXPath();
        try {
            String pre = "//parent/node()[";
            Object ret = xpath.evaluate(
                    pre + aChildNo.getXPath() + "]", testDoc, 
                    XPathConstants.NODE);
            assertTrue(a.isSameNode(((Node) ret)));
            
            ret = xpath.evaluate(pre + bChildNo.getXPath() + "]",
                    testDoc, XPathConstants.NODE);
            assertTrue(b.isSameNode(((Node) ret)));
            
            ret = xpath.evaluate(pre + cChildNo.getXPath() + "]",
                    testDoc, XPathConstants.NODE);
            assertTrue(c.isSameNode(((Node) ret)));
            
        } catch (XPathExpressionException e) {
            fail("Caught XPathExpressionException: " + e.getMessage());
        }
        
    }
    
    @Test
    public void testTextNodeChildNo() {
        
        //<parent><a/>12</d></parent>
        Element a = testDoc.createElement("a");
        Node b = testDoc.createTextNode("1");
        Node c = testDoc.createTextNode("2");
        Element d = testDoc.createElement("d");
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        parent.appendChild(d);
        
        ChildNumber aChildNo = new ChildNumber(a);
        ChildNumber bChildNo = new ChildNumber(b);
        ChildNumber cChildNo = new ChildNumber(c);
        ChildNumber dChildNo = new ChildNumber(d);
        
        assertEquals(0, aChildNo.getDOM());
        assertEquals(1, bChildNo.getDOM());
        assertEquals(2, cChildNo.getDOM());
        assertEquals(3, dChildNo.getDOM());
        
        XPath xpath = mXPathFac.newXPath();
        try {
            String pre = "//parent/node()[";
            Object ret = xpath.evaluate(
                    pre + aChildNo.getXPath() + "]", testDoc, 
                    XPathConstants.NODE);
            assertTrue(a.isSameNode(((Node) ret)));
            
            ret = xpath.evaluate(
                    "substring(" + pre + bChildNo.getXPath()
                    + "]," + bChildNo.getXPathCharPos() + ",1)",
                    testDoc, XPathConstants.STRING);
            assertEquals(b.getTextContent(), ret.toString());
            
            ret = xpath.evaluate(
                    "substring(" + pre + cChildNo.getXPath() + "],"
                    + cChildNo.getXPathCharPos() + ",1)", testDoc, 
                    XPathConstants.STRING);
            assertEquals(c.getTextContent(), ret.toString());
            
            ret = xpath.evaluate(pre + dChildNo.getXPath() + "]",
                    testDoc, XPathConstants.NODE);
            assertTrue("Got: " + ret.toString(), d.isSameNode(((Node) ret)));
            
        } catch (XPathExpressionException e) {
            fail("Caught XPathExpressionException: " + e.getMessage());
        }

    }

    @Test
    public void testTwoInitialTextNodes() {
        
        Node a = testDoc.createTextNode("1234");
        Node b = testDoc.createTextNode("5");
        Element c = testDoc.createElement("a");
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        ChildNumber aChildNo = new ChildNumber(a);
        ChildNumber bChildNo = new ChildNumber(b);
        ChildNumber cChildNo = new ChildNumber(c);
        
        assertEquals(0, aChildNo.getDOM());
        assertEquals(1, bChildNo.getDOM());
        assertEquals(2, cChildNo.getDOM());
        
        XPath xpath = mXPathFac.newXPath();
        try {
            String pre = "//parent/node()[";
            Object ret = xpath.evaluate(
                    "substring(" + pre + aChildNo.getXPath()
                    + "]," + aChildNo.getXPathCharPos() + ",4)",
                    testDoc, XPathConstants.STRING);
            assertEquals(a.getTextContent(), ret.toString());
            
            ret = xpath.evaluate(
                    "substring(" + pre + bChildNo.getXPath() + "],"
                    + bChildNo.getXPathCharPos() + ",1)", testDoc, 
                    XPathConstants.STRING);
            assertEquals(b.getTextContent(), ret.toString());
            
            ret = xpath.evaluate(pre + cChildNo.getXPath() + "]",
                    testDoc, XPathConstants.NODE);
            assertTrue("Got: " + ret.toString(), c.isSameNode(((Node) ret)));
            
        } catch (XPathExpressionException e) {
            fail("Caught XPathExpressionException: " + e.getMessage());
        }
    }

    @Test
    public void testDOMInOrder() {
        
        Node a = testDoc.createTextNode("1234");
        NodeOps.setOutOfOrder(a);
        Node b = testDoc.createTextNode("5");
        NodeOps.setInOrder(b);
        Element c = testDoc.createElement("a");
        NodeOps.setInOrder(c);
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        ChildNumber aChildNo = new ChildNumber(a);
        ChildNumber bChildNo = new ChildNumber(b);
        ChildNumber cChildNo = new ChildNumber(c);
        
        assertEquals(0, aChildNo.getInOrderDOM());
        assertEquals(0, bChildNo.getInOrderDOM());
        assertEquals(1, cChildNo.getInOrderDOM());
        
        NodeOps.setInOrder(a);
        NodeOps.setOutOfOrder(b);
        NodeOps.setInOrder(c);

        aChildNo = new ChildNumber(a);
        bChildNo = new ChildNumber(b);
        cChildNo = new ChildNumber(c);

        assertEquals(0, aChildNo.getInOrderDOM());
        assertEquals(1, bChildNo.getInOrderDOM());
        assertEquals(1, bChildNo.getInOrderDOM());        
    }
    
    @Test
    public void testXPathInOrder() {
        
        Node a = testDoc.createTextNode("1234");
        NodeOps.setOutOfOrder(a);
        Node b = testDoc.createTextNode("56");
        NodeOps.setInOrder(b);
        Node c = testDoc.createTextNode("78");
        NodeOps.setInOrder(c);
        Element d = testDoc.createElement("nine");
        NodeOps.setInOrder(d);

        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        parent.appendChild(d);

        ChildNumber aChildNo = new ChildNumber(a);
        ChildNumber bChildNo = new ChildNumber(b);
        ChildNumber cChildNo = new ChildNumber(c);
        ChildNumber dChildNo = new ChildNumber(d);

        assertEquals(1, aChildNo.getInOrderXPath());
        assertEquals(1, aChildNo.getInOrderXPathCharPos());
        assertEquals(1, bChildNo.getInOrderXPath());
        assertEquals(1, bChildNo.getInOrderXPathCharPos());
        assertEquals(1, cChildNo.getInOrderXPath());
        assertEquals(3, cChildNo.getInOrderXPathCharPos());
        assertEquals(2, dChildNo.getInOrderXPath());
        //assertEquals(5, dChildNo.getInOrderXPathCharPos());
        
    }
    
    //Test handling nulls, nodes with no parent
}
