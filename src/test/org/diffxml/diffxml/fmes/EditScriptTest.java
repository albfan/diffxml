package org.diffxml.diffxml.fmes;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.TestCase;

public class EditScriptTest extends TestCase {
    
    Document testDoc;
    Element parent;
    EditScript es;
    
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        testDoc = fac.newDocumentBuilder().newDocument();

        parent = testDoc.createElement("parent");
        testDoc.appendChild(parent);
        es = new EditScript();
        
    }
    
    public void testGetInOrderIndex()
    {
        //InsertPosition and NodePos are both calculating XPath in different ways :(
        Element a = testDoc.createElement("a");
        Element b = testDoc.createElement("b");
        Element c = testDoc.createElement("c");
        NodeOps.setInOrder(a);
        NodeOps.setInOrder(b);
        NodeOps.setInOrder(c);
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        assertEquals(es.getInOrderIndex(a).xPath, 1);
        assertEquals(es.getInOrderIndex(b).xPath, 2);
        assertEquals(es.getInOrderIndex(c).xPath, 3);
    }
    
    public void testGetInOrderIndex2()
    {
        Element a = testDoc.createElement("a");
        Node b = testDoc.createTextNode("1");
        Node c = testDoc.createTextNode("2");
        Element d = testDoc.createElement("d");
        
        NodeOps.setInOrder(a);
        NodeOps.setInOrder(b);
        NodeOps.setInOrder(c);
        NodeOps.setInOrder(d);
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        parent.appendChild(d);
        
        assertEquals(es.getInOrderIndex(a).xPath, 1);
        assertEquals(es.getInOrderIndex(b).xPath, 2);
        assertEquals(es.getInOrderIndex(c).xPath, 2);
        assertEquals(es.getInOrderIndex(d).xPath, 3);

    }

    public void testGetInOrderIndex3()
    {
        Node a = testDoc.createTextNode("1");
        Node b = testDoc.createTextNode("2");
        Element c = testDoc.createElement("a");
        
        NodeOps.setInOrder(a);
        NodeOps.setInOrder(b);
        NodeOps.setInOrder(c);
        
        parent.appendChild(a);
        parent.appendChild(b);
        parent.appendChild(c);
        
        assertEquals(es.getInOrderIndex(a).xPath, 1);
        assertEquals(es.getInOrderIndex(b).xPath, 1);
        assertEquals(es.getInOrderIndex(c).xPath, 2);
    }
    
}
