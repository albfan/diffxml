package org.diffxml.diffxml.fmes;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ChildNumberTest extends TestCase 
{

    Document testDoc;
    Element parent;
    
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        testDoc = fac.newDocumentBuilder().newDocument();
        parent = testDoc.createElement("parent");
        testDoc.appendChild(parent);
        
    }
    
    public void testSimpleChildNo()
    {
        //InsertPosition and NodePos are both calculating XPath in different ways :(
        Element a = testDoc.createElement("a");
        Element b = testDoc.createElement("b");
        Element c = testDoc.createElement("c");
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        assertEquals(ChildNumber.getXPathChildNumber(a), 1);
        assertEquals(ChildNumber.getXPathChildNumber(b), 2);
        assertEquals(ChildNumber.getXPathChildNumber(c), 3);
    }
    
    public void testTextNodeChildNo()
    {
        Element a = testDoc.createElement("a");
        Node b = testDoc.createTextNode("1");
        Node c = testDoc.createTextNode("2");
        Element d = testDoc.createElement("d");
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        parent.appendChild(d);
        
        assertEquals(ChildNumber.getXPathChildNumber(a), 1);
        assertEquals(ChildNumber.getXPathChildNumber(b), 2);
        assertEquals(ChildNumber.getXPathChildNumber(c), 2);
        assertEquals(ChildNumber.getXPathChildNumber(d), 3);

    }

    public void testTwoInitialTextNodes()
    {
        Node a = testDoc.createTextNode("1");
        Node b = testDoc.createTextNode("2");
        Element c = testDoc.createElement("a");
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        assertEquals(ChildNumber.getXPathChildNumber(a), 1);
        assertEquals(ChildNumber.getXPathChildNumber(b), 1);
        assertEquals(ChildNumber.getXPathChildNumber(c), 2);
    }

}
