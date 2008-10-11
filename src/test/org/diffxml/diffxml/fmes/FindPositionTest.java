package org.diffxml.diffxml.fmes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class FindPositionTest {

    private Document testDoc;
    private Element parent;
    private EditScript es;
    
    @Before
    public void setUp() throws Exception {
        
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        testDoc = fac.newDocumentBuilder().newDocument();

        parent = testDoc.createElement("parent");
        testDoc.appendChild(parent);
        
    }
    
    @Test
    public void testSimpleInsert() {
        /** 
         * Complicated test. Need two doc fragments that have been matched.
         * Check insert position is as expected 
         */
        fail("Need to write...");
   
    }
 
}
