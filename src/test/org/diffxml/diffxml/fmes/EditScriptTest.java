package org.diffxml.diffxml.fmes;

import static org.junit.Assert.assertEquals;

import org.diffxml.diffxml.TestDocHelper;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test main EditScript class and methods.
 * 
 * @author Adrian Mouat
 *
 */
public class EditScriptTest {
    
   
    @Test
    public void testMatchingRootNodes() {
        
        //Shouldn't do anything if roots match
        Document test1 = TestDocHelper.createDocument("<a><b/></a>");
        Document test2 = TestDocHelper.createDocument("<a><b/></a>");
        NodePairs matchings = Match.easyMatch(test1, test2);
        (new EditScript(test1, test2, matchings)).matchRoots();
        
        assertEquals("a", test1.getDocumentElement().getNodeName());
        assertEquals("b", 
                test1.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("a", test2.getDocumentElement().getNodeName());
        assertEquals("b", 
                test2.getDocumentElement().getFirstChild().getNodeName());
        
        //Should add dummy node if they don't
        test1 = TestDocHelper.createDocument("<a><b/></a>");
        test2 = TestDocHelper.createDocument("<c><b/></c>");
        matchings = Match.easyMatch(test1, test2);
        (new EditScript(test1, test2, matchings)).matchRoots();
        
        assertEquals("DUMMY", test1.getDocumentElement().getNodeName());
        assertEquals("a", 
                test1.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("b", 
                test1.getDocumentElement().getFirstChild().getFirstChild(
                        ).getNodeName());

        assertEquals("DUMMY", test2.getDocumentElement().getNodeName());
        assertEquals("c", 
                test2.getDocumentElement().getFirstChild().getNodeName());
        assertEquals("b", 
                test2.getDocumentElement().getFirstChild().getFirstChild(
                        ).getNodeName());
        
        assertEquals(matchings.getPartner(test2.getDocumentElement()), 
                test1.getDocumentElement());
    }
    
}
